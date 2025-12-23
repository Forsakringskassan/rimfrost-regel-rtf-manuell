package se.fk.github.manuellregelratttillforsakring;

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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import se.fk.rimfrost.OperativtUppgiftslagerRequestMessage;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.PutKundbehovsflodeRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.UppgiftStatus;
import se.fk.rimfrost.regel.rtf.manuell.*;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;
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
import static org.junit.jupiter.api.Assertions.assertFalse;

@SuppressWarnings("deprecation")
@Testcontainers
public class RtfManuellContainerSmokeIT
{

   private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
         .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
   private static KafkaContainer kafka;
   private static GenericContainer<?> rtfManuell;
   private static GenericContainer<?> wiremock;
   private static final String kafkaImage = TestConfig.get("kafka.image");
   private static final String rtfManuellImage = TestConfig.get("rtfManuell.image");
   private static final String rtfManuellRequestsTopic = TestConfig.get("rtf.manuell.requests.topic");
   private static final String rtfManuellResponsesTopic = TestConfig.get("rtf.manuell.responses.topic");
   private static final String oulRequestsTopic = TestConfig.get("oul.requests.topic");
   private static final String oulResponsesTopic = TestConfig.get("oul.responses.topic");
   private static final String oulStatusNotificationTopic = TestConfig.get("oul.status-notification.topic");
   private static final String oulStatusControlTopic = TestConfig.get("oul.status-control.topic");
   private static final String networkAlias = TestConfig.get("network.alias");
   private static final String smallryeKafkaBootstrapServers = networkAlias + ":9092";
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
      setupRtfManuell();
   }

   static void setupKafka()
   {
      kafka = new KafkaContainer(DockerImageName.parse(kafkaImage)
            .asCompatibleSubstituteFor("apache/kafka"))
            .withNetwork(network)
            .withNetworkAliases(networkAlias);
      kafka.start();
      System.out.println("Kafka host bootstrap servers: " + kafka.getBootstrapServers());
      try
      {
         createTopic(rtfManuellRequestsTopic, 1, (short) 1);
         createTopic(rtfManuellResponsesTopic, 1, (short) 1);
         createTopic(oulRequestsTopic, 1, (short) 1);
         createTopic(oulResponsesTopic, 1, (short) 1);
         createTopic(oulStatusNotificationTopic, 1, (short) 1);
         createTopic(oulStatusControlTopic, 1, (short) 1);
      }
      catch (Exception e)
      {
         throw new RuntimeException("Failed to create Kafka topics", e);
      }
   }

   static KafkaConsumer<String, String> createKafkaConsumer(String topic)
   {
      String bootstrap = kafka.getBootstrapServers().replace("PLAINTEXT://", "");
      Properties props = new Properties();
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
      props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + System.currentTimeMillis());
      props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
      KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
      consumer.subscribe(Collections.singletonList(topic));
      return consumer;
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

   static void setupRtfManuell()
   {
      Properties props = new Properties();
      try (InputStream in = RtfManuellContainerSmokeIT.class.getResourceAsStream("/test.properties"))
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

      //noinspection resource
      rtfManuell = new GenericContainer<>(DockerImageName.parse(rtfManuellImage))
            .withNetwork(network)
            .withExposedPorts(8080)
            .withEnv("MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_BOOTSTRAP_SERVERS", smallryeKafkaBootstrapServers)
            .withEnv("QUARKUS_PROFILE", "test") // force test profile
            .withEnv("FOLKBOKFORD_API_BASE_URL", wiremockUrl)
            .withEnv("ARBETSGIVARE_API_BASE_URL", wiremockUrl)
            .withEnv("KUNDBEHOVSFLODE_API_BASE_URL", wiremockUrl);
      rtfManuell.start();
   }

   @SuppressWarnings("SameParameterValue")
   static void createTopic(String topicName, int numPartitions, short replicationFactor) throws Exception
   {
      String bootstrap = kafka.getBootstrapServers().replace("PLAINTEXT://", "");
      Properties props = new Properties();
      props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
      try (AdminClient admin = AdminClient.create(props))
      {
         NewTopic topic = new NewTopic(topicName, numPartitions, replicationFactor);
         admin.createTopics(List.of(topic)).all().get();
         System.out.printf("Created topic: %S%n", topicName);
      }
   }

   @AfterAll
   static void tearDown()
   {
      if (rtfManuell != null)
         rtfManuell.stop();
      if (kafka != null)
         kafka.stop();
      if (wiremock != null)
         wiremock.stop();
   }

   private String readKafkaMessage(String topic)
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
         return kafkaMessage;
      }
   }

   private void sendRtfManuellRequest(String kundbehovsflodeId) throws Exception
   {
      RtfManuellRequestMessagePayload payload = new RtfManuellRequestMessagePayload();
      RtfManuellRequestMessageData data = new RtfManuellRequestMessageData();
      data.setKundbehovsflodeId(kundbehovsflodeId);
      payload.setSpecversion(SpecVersion.NUMBER_1_DOT_0);
      payload.setId("99994567-89ab-4cde-9012-3456789abcde");
      payload.setSource("TestSource-001");
      payload.setType(rtfManuellRequestsTopic);
      payload.setKogitoprocid("234567");
      payload.setKogitorootprocid("123456");
      payload.setKogitorootprociid("77774567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoparentprociid("88884567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoprocinstanceid("66664567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoprocist("345678");
      payload.setKogitoprocversion("111");
      payload.setKogitoproctype(KogitoProcType.BPMN);
      payload.setKogitoprocrefid("56789");
      payload.setData(data);
      // Serialize entire payload to JSON
      String eventJson = mapper.writeValueAsString(payload);

      Properties props = new Properties();
      props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
      props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
      props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

      try (KafkaProducer<String, String> producer = new KafkaProducer<>(props))
      {
         ProducerRecord<String, String> record = new ProducerRecord<>(
               rtfManuellRequestsTopic,
               eventJson);
         System.out.printf("Kafka sending to topic : %s, json: %s%n", rtfManuellRequestsTopic, eventJson);
         producer.send(record).get();
      }
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
         int minRequests)
   {
      List<LoggedRequest> requests = Collections.emptyList();
      int retries = 20;
      long sleepMs = 250;
      for (int i = 0; i < retries; i++)
      {
         requests = wiremockClient.findAll(WireMock.anyRequestedFor(WireMock.urlMatching(urlRegex)));
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

   public HttpResponse<String> sendGetRtfManuell(HttpClient httpClient, String kundbehovsflodeId)
         throws IOException, InterruptedException
   {
      var url = "http://" + rtfManuell.getHost() + ":" + rtfManuell.getMappedPort(8080) + "/regel/rtf-manuell/"
            + kundbehovsflodeId;
      System.out.printf("Sending GET rtf manuell to: %s%n", url);
      HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode());
      return response;
   }

   public HttpResponse<String> sendPatchRtfManuell(HttpClient httpClient, String kundbehovsflodeId,
         PatchDataRequest patchDataRequest) throws IOException, InterruptedException
   {
      var url = "http://" + rtfManuell.getHost() + ":" + rtfManuell.getMappedPort(8080) + "/regel/rtf-manuell/"
            + kundbehovsflodeId;
      var json = mapper.writeValueAsString(patchDataRequest);
      System.out.printf("Sending PATCH rtf manuell to: %s%n", url);
      HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
            .header("Content-Type", "application/json")
            .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      assertEquals(204, response.statusCode());
      return response;
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void TestRtfManuellSmoke(String kundbehovsflodeId) throws Exception
   {
      System.out.printf("Starting TestRtfManuellSmoke. %S%n", kundbehovsflodeId);
      // Send Rtf manuell request to start workflow
      sendRtfManuellRequest(kundbehovsflodeId);
      // Start background Kafka responder handling request to Operativt uppgiftslager
      ExecutorService executorOul = Executors.newSingleThreadExecutor();
      CompletableFuture<OulCorrelation> responderOul = startKafkaResponderOul(executorOul);
      OulCorrelation oulCorrelation = responderOul.join();
      String uppgiftId = oulCorrelation.uppgiftId();
      //
      // Verify GET kundbehovsflöde requested
      //
      List<LoggedRequest> kundbehovsflodeRequests = waitForWireMockRequest(wiremockClient,
            kundbehovsflodeEndpoint + kundbehovsflodeId, 3);
      var getRequests = kundbehovsflodeRequests.stream().filter(p -> p.getMethod().equals(RequestMethod.GET)).toList();
      assertFalse(getRequests.isEmpty());
      //
      // Verify oul message produced
      //
      String kafkaMessage = readKafkaMessage(oulRequestsTopic);
      OperativtUppgiftslagerRequestMessage oulRequestMessage = mapper.readValue(kafkaMessage,
            OperativtUppgiftslagerRequestMessage.class);
      assertEquals(kundbehovsflodeId, oulRequestMessage.getKundbehovsflodeId());
      //
      // Verify PUT kundbehovsflöde requested
      //
      kundbehovsflodeRequests = waitForWireMockRequest(wiremockClient, kundbehovsflodeEndpoint + kundbehovsflodeId, 3);
      var putRequests = kundbehovsflodeRequests.stream().filter(p -> p.getMethod().equals(RequestMethod.PUT)).toList();
      assertEquals(1, putRequests.size());
      var sentJson = putRequests.getLast().getBodyAsString();
      var sentPutKundbehovsflodeRequest = mapper.readValue(sentJson, PutKundbehovsflodeRequest.class);
      assertEquals(kundbehovsflodeId, sentPutKundbehovsflodeRequest.getUppgift().getKundbehovsflode().getId().toString());
      assertEquals(UppgiftStatus.AVSLUTAD, sentPutKundbehovsflodeRequest.getUppgift().getUppgiftStatus()); // TODO borde ej vara AVSLUTAD nu ??
      // TODO Add more checks above
      //
      // mock status update from OUL
      //
      OperativtUppgiftslagerStatusMessage statusMessage = new OperativtUppgiftslagerStatusMessage();
      statusMessage.setStatus(Status.NY);
      statusMessage.setUppgiftId(oulCorrelation.uppgiftId);
      statusMessage.setKundbehovsflodeId(oulCorrelation.kundbehovsflodeId);
      sendOulStatus(oulCorrelation.kafkaKey, oulStatusNotificationTopic, statusMessage);
      //
      // verify expected actions from rtf manual as result of new status reported
      //
      kundbehovsflodeRequests = waitForWireMockRequest(wiremockClient, kundbehovsflodeEndpoint + kundbehovsflodeId, 5);
      putRequests = kundbehovsflodeRequests.stream().filter(p -> p.getMethod().equals(RequestMethod.PUT)).toList();
      assertEquals(2, putRequests.size());
      sentJson = putRequests.getLast().getBodyAsString();
      sentPutKundbehovsflodeRequest = mapper.readValue(sentJson, PutKundbehovsflodeRequest.class);
      assertEquals(kundbehovsflodeId, sentPutKundbehovsflodeRequest.getUppgift().getKundbehovsflode().getId().toString());
      assertEquals(UppgiftStatus.AVSLUTAD, sentPutKundbehovsflodeRequest.getUppgift().getUppgiftStatus());
      //
      // mock GET operation requested from portal FE
      //
      var httpResponse = sendGetRtfManuell(httpClient, kundbehovsflodeId);
      //
      // Verify GET operation response
      //
      var getDataResponse = mapper.readValue(httpResponse.body(), GetDataResponse.class);
      assertEquals(kundbehovsflodeId, getDataResponse.getKundbehovsflodeId().toString());
      // TODO more assertions of content above
      //
      // verify that rule performed requests to kundbehovsflode
      //
      kundbehovsflodeRequests = waitForWireMockRequest(wiremockClient, kundbehovsflodeEndpoint + kundbehovsflodeId, 8);
      putRequests = kundbehovsflodeRequests.stream().filter(p -> p.getMethod().equals(RequestMethod.PUT)).toList();
      assertEquals(3, putRequests.size());
      sentJson = putRequests.getLast().getBodyAsString();
      sentPutKundbehovsflodeRequest = mapper.readValue(sentJson, PutKundbehovsflodeRequest.class);
      assertEquals(kundbehovsflodeId, sentPutKundbehovsflodeRequest.getUppgift().getKundbehovsflode().getId().toString());
      assertEquals(UppgiftStatus.AVSLUTAD, sentPutKundbehovsflodeRequest.getUppgift().getUppgiftStatus());
      //
      // mock PATCH operation from portal FE
      //
      PatchDataRequest patchDataRequest = new PatchDataRequest();
      patchDataRequest.setErsattningId(UUID.fromString("67c5ded8-7697-41fd-b943-c58a1be15c93"));
      patchDataRequest.setAvslagsanledning("");
      patchDataRequest.setSignera(true);
      patchDataRequest.setBeslutsutfall(Beslutsutfall.JA);
      httpResponse = sendPatchRtfManuell(httpClient, kundbehovsflodeId, patchDataRequest);
      //
      // verify that rule performed requests to kundbehovsflode
      //
      kundbehovsflodeRequests = waitForWireMockRequest(wiremockClient, kundbehovsflodeEndpoint + kundbehovsflodeId, 10);
      putRequests = kundbehovsflodeRequests.stream().filter(p -> p.getMethod().equals(RequestMethod.PUT)).toList();
      assertEquals(4, putRequests.size());
      //
      // verify kafka status message sent to oul
      //
      var kafkaOulStatusMessage = readKafkaMessage(oulStatusControlTopic);
      OperativtUppgiftslagerStatusMessage oulStatusMessage = mapper.readValue(kafkaOulStatusMessage,
            OperativtUppgiftslagerStatusMessage.class);
      assertEquals(uppgiftId, oulStatusMessage.getUppgiftId());
      assertEquals(Status.AVSLUTAD, oulStatusMessage.getStatus());
      //
      // verify kafka manuell response message sent to VAH
      //
      var kafkaRtfManuellResponseMessage = readKafkaMessage(rtfManuellResponsesTopic);
      RtfManuellResponseMessagePayload rtfManuellResponseMessagePayload = mapper.readValue(kafkaRtfManuellResponseMessage,
            RtfManuellResponseMessagePayload.class);
      assertEquals(kundbehovsflodeId, rtfManuellResponseMessagePayload.getData().getKundbehovsflodeId());
      assertEquals(RattTillForsakring.JA, rtfManuellResponseMessagePayload.getData().getRattTillForsakring());
   }
}
