package se.fk.github.manuellregelratttillforsakring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
import se.fk.rimfrost.regel.rtf.manuell.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("deprecation")
@Testcontainers
public class RtfManuellContainerSmokeIT
{

   private static final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
         .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
   private static KafkaContainer kafka;
   private static int kafkaPort;
   private static GenericContainer<?> rtfManuell;
   private static GenericContainer<?> wiremock;
   private static final String kafkaImage = TestConfig.get("kafka.image");
   private static final String rtfManuellImage = TestConfig.get("rtfManuell.image");
   private static final String rtfManuellRequestsTopic = TestConfig.get("rtf.manuell.requests.topic");
   private static final String rtfManuellResponsesTopic = TestConfig.get("rtf.manuell.responses.topic");
   private static final int topicTimeout = TestConfig.getInt("topic.timeout");
   private static final String networkAlias = TestConfig.get("network.alias");
   private static final String smallryeKafkaBootstrapServers = networkAlias + ":9092";
   private static Network network = Network.newNetwork();
   private static final String wiremockUrl = "http://wiremock:8080";
   private static WireMock wiremockClient;
   private static final String kundbehovsflodeEndpoint = "/kundbehovsflode/";
   private static final String folkbokforingEndpoint = "/folkbokforing/";
   private static final String arbetsgivareEndpoint = "/arbetsgivare/";
   private static KafkaConsumer rtfManuellResponsesConsumer;

   @BeforeAll
   static void setup()
   {
      setupKafka();
      setupWiremock();
      setupRtfManuell();
      rtfManuellResponsesConsumer = createKafkaConsumer(rtfManuellResponsesTopic);
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
      wiremock = new GenericContainer<>("wiremock/wiremock:3.3.1")
            .withNetwork(network)
            .withNetworkAliases("wiremock")
            .withExposedPorts(8080)
            .withEnv("WIREMOCK_OPTIONS", "--local-response-templating")
            .withCopyFileToContainer(
                  MountableFile.forHostPath("src/test/resources/mappings"),
                  "/home/wiremock/mappings")
            .waitingFor(Wait.forHttp("/__admin").forStatusCode(200));
      ;
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
            .withStartupTimeout(Duration.ofMinutes(2))
            .withEnv("MP_MESSAGING_CONNECTOR_SMALLRYE_KAFKA_BOOTSTRAP_SERVERS", smallryeKafkaBootstrapServers)
            .withEnv("QUARKUS_PROFILE", "test") // force test profile
            .withEnv("FOLKBOKFORD_API_BASE_URL", wiremockUrl)
            .withEnv("ARBETSGIVARE_API_BASE_URL", wiremockUrl)
            .withEnv("KUNDBEHOVSFLODE_API_BASE_URL", wiremockUrl);
      rtfManuell.start();
   }

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
   }

   private String readKafkaMessage(KafkaConsumer<String, String> consumer)
   {
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(120));
      if (records.isEmpty())
      {
         throw new IllegalStateException("No Kafka message received on topic ");
      }
      // return the first new record
      return records.iterator().next().value();
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

   public static List<LoggedRequest> waitForWireMockRequest(
         WireMock wiremockClient,
         String urlRegex)
   {
      List<LoggedRequest> requests = Collections.emptyList();
      int retries = 20;
      long sleepMs = 250;
      for (int i = 0; i < retries; i++)
      {
         requests = wiremockClient.findAll(
               WireMock.getRequestedFor(
                     WireMock.urlMatching(urlRegex)));

         if (!requests.isEmpty())
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

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 19990101-1234, Ja",
         "5367f6b8-cc4a-11f0-8de9-199901013333, 19990101-3333, Utredning",
         "5367f6b8-cc4a-11f0-8de9-199901012222, 19990101-2222, Ja",
         "5367f6b8-cc4a-11f0-8de9-199901014444, 19990101-4444, Nej"
   })
   void TestRtfManuellSmoke(String kundbehovsflodeId,
         String persnr,
         String expectedRattTillForsakring) throws Exception
   {

      System.out.printf("Starting TestRtfManuellSmoke. %S%n", kundbehovsflodeId);
      // Send Rtf manuell request to start workflow
      sendRtfManuellRequest(kundbehovsflodeId);

//      // Verify kundbehovsflöde requested
//      List<LoggedRequest> kundbehovsflodeRequests = waitForWireMockRequest(wiremockClient,
//            kundbehovsflodeEndpoint + kundbehovsflodeId);
//      assertEquals(kundbehovsflodeEndpoint + kundbehovsflodeId, kundbehovsflodeRequests.getFirst().getUrl());
//      // Verify folkbokföring requests
//      List<LoggedRequest> folkbokforingRequests = waitForWireMockRequest(wiremockClient, folkbokforingEndpoint + persnr);
//      assertEquals(folkbokforingEndpoint + persnr, folkbokforingRequests.getFirst().getUrl());
//      // Verify arbetsgivare requests
//      List<LoggedRequest> arbetsgivareRequests = waitForWireMockRequest(wiremockClient, arbetsgivareEndpoint + persnr);
//      assertEquals(arbetsgivareEndpoint + persnr, arbetsgivareRequests.getFirst().getUrl());
//      // Log sent requests
//      List<LoggedRequest> requests = wiremockClient.findAll(WireMock.getRequestedFor(WireMock.urlMatching(".*")));
//      System.out.printf("Wiremock received requests:%n");
//      for (LoggedRequest req : requests)
//      {
//         System.out.println("Request URL: " + req.getUrl());
//         System.out.println("Body: " + req.getBodyAsString());
//
//      }
//      System.out.printf("Wiremock generated responses:%n");
//      wiremockClient.getServeEvents()
//            .forEach(e -> {
//               System.out.printf("URL: %s%n", e.getRequest().getUrl());
//               System.out.printf("STATUS: %s%n", e.getResponse().getStatus());
//               System.out.printf("BODY: %s%n", e.getResponse().getBodyAsString());
//            });
//
//      String rtfManuellResponseJson = readKafkaMessage(rtfManuellResponsesConsumer);
//      var rtfManuellResponse = mapper.readValue(rtfManuellResponseJson, RtfManuellResponseMessagePayload.class);
//      assertEquals(kundbehovsflodeId, rtfManuellResponse.getData().getKundbehovsflodeId());
//      assertEquals(expectedRattTillForsakring, rtfManuellResponse.getData().getRattTillForsakring().getValue());

   }
}
