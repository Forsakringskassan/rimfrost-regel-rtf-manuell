package se.fk.github.manuellregelratttillforsakring.container;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import se.fk.github.manuellregelratttillforsakring.TestConfig;
import se.fk.rimfrost.OperativtUppgiftslagerRequestMessage;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.PutKundbehovsflodeRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.UppgiftStatus;
import se.fk.rimfrost.framework.regel.RegelRequestMessagePayload;
import se.fk.rimfrost.framework.regel.RegelRequestMessagePayloadData;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
public class RegelManuellContainerIT
{

   private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
         .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
   private static ConfluentKafkaContainer kafka;
   private static GenericContainer<?> regelManuell;
   private static GenericContainer<?> wiremock;
   private static final String kafkaImage = TestConfig.get("kafka.image");
   private static final String regelImage = System.getenv().getOrDefault("REGEL_IMAGE", TestConfig.get("regel.image"));
   private static final String regelRequestsTopic = ConfigProvider.getConfig()
         .getValue("mp.messaging.incoming.regel-requests.topic", String.class);
   private static final String regelResponsesTopic = ConfigProvider.getConfig()
         .getValue("mp.messaging.outgoing.regel-responses.topic", String.class);
   private static final String oulRequestsTopic = TestConfig.get("oul.requests.topic");
   private static final String oulResponsesTopic = ConfigProvider.getConfig()
         .getValue("mp.messaging.incoming.operativt-uppgiftslager-responses.topic", String.class);
   private static final String oulStatusNotificationTopic = ConfigProvider.getConfig()
         .getValue("mp.messaging.incoming.operativt-uppgiftslager-status-notification.topic", String.class);
   private static final String oulStatusControlTopic = TestConfig.get("oul.status-control.topic");
   private static final String networkAlias = TestConfig.get("network.alias");
   private static final int kafkaPort = Integer.parseInt(TestConfig.get("kafka.port"));
   private static final String smallryeKafkaBootstrapServers = networkAlias + ":" + kafkaPort;
   private static final Network network = Network.newNetwork();
   private static final String wiremockUrl = "http://wiremock:8080";
   private static WireMock wiremockClient;
   private static final String kundbehovsflodeEndpoint = "/kundbehovsflode/";

   private static final HttpClient httpClient = HttpClient.newHttpClient();

   @BeforeAll
   static void setup()
   {
      setupKafka();
      setupWiremock();
      setupRegel();
   }

   static void setupKafka()
   {
      kafka = new ConfluentKafkaContainer(DockerImageName.parse(kafkaImage)
            .asCompatibleSubstituteFor("apache/kafka"))
            .withNetwork(network)
            .withNetworkAliases(networkAlias);
      kafka.addExposedPort(kafkaPort);
      kafka.start();
      System.out.println("Kafka host bootstrap servers: " + kafka.getBootstrapServers());
      try
      {
         createTopic(regelRequestsTopic);
         createTopic(regelResponsesTopic);
         createTopic(oulRequestsTopic);
         createTopic(oulResponsesTopic);
         createTopic(oulStatusNotificationTopic);
         createTopic(oulStatusControlTopic);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Failed to create Kafka topics", e);
      }
   }

   static void setupWiremock()
   {
      try
      {
         wiremock = new GenericContainer<>("wiremock/wiremock:3.3.1")
               .withNetwork(network)
               .withNetworkAliases("wiremock")
               .withExposedPorts(8080)
               .withEnv("WIREMOCK_OPTIONS", "--local-response-templating --verbose")
               .withCopyFileToContainer(
                     MountableFile.forHostPath("src/test/resources/mappings"),
                     "/home/wiremock/mappings")
               .waitingFor(Wait.forHttp("/__admin").forStatusCode(200));
      }
      catch (NullPointerException e)
      {
         throw new RuntimeException("Failed to setup wiremock container");
      }
      wiremock.start();
      int wmPort = wiremock.getMappedPort(8080);
      wiremockClient = new WireMock("localhost", wmPort);
      WireMock.configureFor("localhost", wmPort);
   }

   static void setupRegel()
   {
      Properties props = new Properties();
      try (InputStream in = RegelManuellContainerIT.class.getResourceAsStream("/test.properties"))
      {
         if (in == null)
         {
            throw new RuntimeException("Could not find /test.properties in classpath");
         }
         props.load(in);
      }
      catch (IOException e)
      {
         throw new RuntimeException("Failed to load test.properties", e);
      }

      String containerConfigPath = "/deployments/test-config.yaml";

      regelManuell = new GenericContainer<>(DockerImageName.parse(regelImage))
            .withNetwork(network)
            .withExposedPorts(8080)
            .withEnv("REGEL_CONFIG_PATH", containerConfigPath)
            .withCopyFileToContainer(
                  MountableFile.forClasspathResource("config-test.yaml"),
                  containerConfigPath)
            .withEnv("MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_BOOTSTRAP_SERVERS", smallryeKafkaBootstrapServers)
            .withEnv("FOLKBOKFORD_API_BASE_URL", wiremockUrl)
            .withEnv("ARBETSGIVARE_API_BASE_URL", wiremockUrl)
            .withEnv("KUNDBEHOVSFLODE_API_BASE_URL", wiremockUrl)
            .withImagePullPolicy(new NeverPullPolicy());
      regelManuell.start();
   }

   static void createTopic(String topicName) throws Exception
   {
      String bootstrap = kafka.getBootstrapServers().replace("PLAINTEXT://", "");
      Properties props = new Properties();
      props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
      try (AdminClient admin = AdminClient.create(props))
      {
         NewTopic topic = new NewTopic(topicName, 1, (short) 1);
         admin.createTopics(List.of(topic)).all().get();
         System.out.printf("Created topic: %S%n", topicName);
      }
   }

   @AfterAll
   static void tearDown()
   {
      if (regelManuell != null)
         regelManuell.stop();
      if (kafka != null)
         kafka.stop();
      if (wiremock != null)
         wiremock.stop();
   }

   private void waitForKafkaMessage(String topic)
   {
      String bootstrap = kafka.getBootstrapServers().replace("PLAINTEXT://", "");
      Properties props = new Properties();
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
      props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + System.currentTimeMillis());
      props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props))
      {
         System.out.printf("New kafka consumer subscribing to topic: %s%n", topic);
         consumer.subscribe(Collections.singletonList(topic));
         ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(120));
         if (records.isEmpty())
         {
            throw new IllegalStateException("No Kafka message received on topic " + topic);
         }
         var kafkaMessage = records.iterator().next().value();
         System.out.printf("Received kafkaMessage on %s: %s%n", topic, kafkaMessage);
      }
   }

   private void sendRegelRequest(String kundbehovsflodeId) throws Exception
   {
      RegelRequestMessagePayload payload = getRegelRequestMessagePayload(kundbehovsflodeId);
      // Serialize entire payload to JSON
      String eventJson = mapper.writeValueAsString(payload);

      Properties props = new Properties();
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

      try (KafkaProducer<String, String> producer = new KafkaProducer<>(props))
      {
         ProducerRecord<String, String> record = new ProducerRecord<>(
               regelRequestsTopic,
               eventJson);
         System.out.printf("Kafka sending to topic : %s, json: %s%n", regelRequestsTopic, eventJson);
         producer.send(record).get();
      }
   }

   private static RegelRequestMessagePayload getRegelRequestMessagePayload(String kundbehovsflodeId)
   {
      RegelRequestMessagePayload payload = new RegelRequestMessagePayload();
      RegelRequestMessagePayloadData data = new RegelRequestMessagePayloadData();
      data.setKundbehovsflodeId(kundbehovsflodeId);
      payload.setSpecversion(se.fk.rimfrost.framework.regel.SpecVersion.NUMBER_1_DOT_0);
      payload.setId("99994567-89ab-4cde-9012-3456789abcde");
      payload.setSource("TestSource-001");
      payload.setType(regelRequestsTopic);
      payload.setKogitoprocid("234567");
      payload.setKogitorootprocid("123456");
      payload.setKogitorootprociid("77774567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoparentprociid("88884567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoprocinstanceid("66664567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoprocist("345678");
      payload.setKogitoprocversion("111");
      payload.setKogitoproctype(se.fk.rimfrost.framework.regel.KogitoProcType.BPMN);
      payload.setKogitoprocrefid("56789");
      payload.setData(data);
      return payload;
   }

   public record OulCorrelation(
         String kundbehovsflodeId,
         String uppgiftId,
         String kafkaKey)
   {
   }

   private KafkaConsumer<String, String> createConsumer()
   {
      Properties props = new Properties();
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
      props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + System.currentTimeMillis());
      props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      return new KafkaConsumer<>(props);
   }

   private CompletableFuture<OulCorrelation> startKafkaResponderOul(ExecutorService executor)
   {
      return CompletableFuture.supplyAsync(() -> {
         try (KafkaConsumer<String, String> consumer = createConsumer())
         {

            consumer.subscribe(Collections.singletonList(oulRequestsTopic));

            ConsumerRecord<String, String> record = pollForKafkaMessage(consumer, oulRequestsTopic);

            // Deserialize request
            OperativtUppgiftslagerRequestMessage request = mapper.readValue(record.value(),
                  OperativtUppgiftslagerRequestMessage.class);

            String kundbehovsflodeId = request.getKundbehovsflodeId();
            String uppgiftId = UUID.randomUUID().toString();

            // Build response
            OperativtUppgiftslagerResponseMessage responseMessage = new OperativtUppgiftslagerResponseMessage();
            responseMessage.setKundbehovsflodeId(kundbehovsflodeId);
            responseMessage.setUppgiftId(uppgiftId);

            sendOulResponse(record.key(), request, oulResponsesTopic, responseMessage);

            System.out.printf(
                  "Sent mock Kafka response for kundbehovsflodeId=%s, uppgiftId=%s%n",
                  kundbehovsflodeId, uppgiftId);

            // Return correlation info to the test
            return new OulCorrelation(
                  kundbehovsflodeId,
                  uppgiftId,
                  record.key());

         }
         catch (Exception e)
         {
            throw new RuntimeException("Kafka responder failed", e);
         }
      }, executor);
   }

   @SuppressWarnings("SameParameterValue")
   private ConsumerRecord<String, String> pollForKafkaMessage(
         KafkaConsumer<String, String> consumer,
         String topic)
   {
      long deadline = System.currentTimeMillis() + 30000;
      while (System.currentTimeMillis() < deadline)
      {
         ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
         if (!records.isEmpty())
         {
            return records.iterator().next();
         }
      }
      throw new IllegalStateException("No Kafka message received on " + topic);
   }

   public void sendOulResponse(String key,
         OperativtUppgiftslagerRequestMessage request,
         String topic,
         OperativtUppgiftslagerResponseMessage response) throws Exception
   {
      String eventJson = mapper.writeValueAsString(response);
      Properties props = new Properties();
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

      try (KafkaProducer<String, String> producer = new KafkaProducer<>(props))
      {
         ProducerRecord<String, String> record = new ProducerRecord<>(
               topic,
               key, // message key
               eventJson);
         System.out.printf("Kafka mock sending: %s\n", eventJson);
         producer.send(record).get();
      }
   }

   public void sendOulStatus(String key,
         String topic,
         OperativtUppgiftslagerStatusMessage response) throws Exception
   {
      String eventJson = mapper.writeValueAsString(response);
      Properties props = new Properties();
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

      try (KafkaProducer<String, String> producer = new KafkaProducer<>(props))
      {
         ProducerRecord<String, String> record = new ProducerRecord<>(
               topic,
               key, // message key
               eventJson);
         System.out.printf("Kafka mock sending: %s\n", eventJson);
         producer.send(record).get();
      }
   }

   public static List<LoggedRequest> waitForWireMockRequest(
         WireMock wiremockClient,
         String urlRegex,
         int minRequests,
         RequestMethod method)
   {
      List<LoggedRequest> requests = Collections.emptyList();
      int retries = 20;
      long sleepMs = 250;
      for (int i = 0; i < retries; i++)
      {
         requests = WireMock.findAll(WireMock.anyRequestedFor(WireMock.urlMatching(urlRegex))).stream()
               .filter(r -> r.getMethod().equals(method)).toList();
         if (requests.size() >= minRequests)
         {
            return requests;
         }
         try
         {
            Thread.sleep(sleepMs);
         }
         catch (InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for WireMock request", e);
         }
      }
      return requests; // empty if nothing received
   }

   public void sendPostRegelManuell(HttpClient httpClient, String kundbehovsflodeId)
         throws IOException, InterruptedException
   {
      var url = "http://" + regelManuell.getHost() + ":" + regelManuell.getMappedPort(8080) + TestConfig.get("rest.api.base-path")
            + kundbehovsflodeId + "/done";
      System.out.printf("Sending POST regel manuell to: %s%n", url);
      HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method("POST", HttpRequest.BodyPublishers.ofString(""))
            .header("Content-Type", "application/json")
            .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(204, response.statusCode());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void TestRegelManuellContainer(String kundbehovsflodeId) throws Exception
   {
      System.out.printf("Starting TestRegelManuellContainer. %S%n", kundbehovsflodeId);

      // Send regel request to start workflow
      sendRegelRequest(kundbehovsflodeId);

      // Start background Kafka responder handling request to Operativt uppgiftslager
      ExecutorService executorOul = Executors.newSingleThreadExecutor();
      CompletableFuture<OulCorrelation> responderOul = startKafkaResponderOul(executorOul);
      OulCorrelation oulCorrelation = responderOul.join();

      //
      // Wait for PUT request before sending OUL status update in order to avoid flaky
      // test
      //
      var kundbehovsflodeRequests = waitForWireMockRequest(wiremockClient, kundbehovsflodeEndpoint + kundbehovsflodeId, 1,
            RequestMethod.PUT);
      assertEquals(1, kundbehovsflodeRequests.size());
      var sentJson = kundbehovsflodeRequests.getLast().getBodyAsString();
      var sentPutKundbehovsflodeRequest = mapper.readValue(sentJson, PutKundbehovsflodeRequest.class);
      assertEquals(kundbehovsflodeId, sentPutKundbehovsflodeRequest.getUppgift().getKundbehovsflodeId().toString());
      assertEquals(UppgiftStatus.PLANERAD, sentPutKundbehovsflodeRequest.getUppgift().getUppgiftStatus());

      // Clear previous requests
      wiremockClient.resetRequests();

      //
      // mock status update from OUL
      //
      OperativtUppgiftslagerStatusMessage statusMessage = new OperativtUppgiftslagerStatusMessage();
      statusMessage.setStatus(Status.NY);
      statusMessage.setUppgiftId(oulCorrelation.uppgiftId);
      statusMessage.setKundbehovsflodeId(oulCorrelation.kundbehovsflodeId);
      statusMessage.setUtforarId(UUID.randomUUID().toString());
      sendOulStatus(oulCorrelation.kafkaKey, oulStatusNotificationTopic, statusMessage);

      //
      // Wait for PUT request to verify that kafka message was received
      //
      kundbehovsflodeRequests = waitForWireMockRequest(wiremockClient, kundbehovsflodeEndpoint + kundbehovsflodeId, 1,
            RequestMethod.PUT);
      assertEquals(1, kundbehovsflodeRequests.size());
      sentJson = kundbehovsflodeRequests.getLast().getBodyAsString();
      sentPutKundbehovsflodeRequest = mapper.readValue(sentJson, PutKundbehovsflodeRequest.class);
      assertEquals(kundbehovsflodeId, sentPutKundbehovsflodeRequest.getUppgift().getKundbehovsflodeId().toString());
      assertEquals(UppgiftStatus.PLANERAD, sentPutKundbehovsflodeRequest.getUppgift().getUppgiftStatus());

      // Clear previous requests
      wiremockClient.resetRequests();

      //
      // mock POST operation from portal FE
      //
      sendPostRegelManuell(httpClient, kundbehovsflodeId);

      //
      // verify kafka status message sent to oul
      //
      waitForKafkaMessage(oulStatusControlTopic);

      //
      // verify kafka manuell response message sent to VAH
      //
      waitForKafkaMessage(regelResponsesTopic);
   }
}
