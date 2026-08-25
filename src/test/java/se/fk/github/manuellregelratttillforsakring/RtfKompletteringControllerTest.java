package se.fk.github.manuellregelratttillforsakring;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.fk.rimfrost.framework.regel.WireMockHandlaggning.waitForRequest;

/**
 * Integration tests for {@link se.fk.github.manuellregelratttillforsakring.presentation.rest.RtfKompletteringController}.
 *
 * <p>GET and PATCH endpoints are tested directly as REST calls without a Kafka trigger since
 * they have no dependency on {@code KompletteringTillstand}. POST done requires an active
 * {@code KompletteringTillstand} in the database, which is established by sending a Kafka
 * message for a handlaggning with missing data.
 */
@QuarkusTest
@QuarkusTestResource(WireMockRtfManuell.class)
class RtfKompletteringControllerTest extends AbstractRegelManuellTest
{
   private static final String COMPLETE_HANDLAGGNING_ID = "5367f6b8-cc4a-11f0-8de9-199901011234";
   private static final String MISSING_DATA_HANDLAGGNING_ID = "5367f6b8-cc4a-11f0-8de9-199901015555";

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @BeforeEach
   void resetWireMock()
   {
      WireMockRtfManuell.getWireMockServer().resetRequests();
      WireMockRtfManuell.getWireMockServer().resetScenarios();
   }

   @Test
   @DisplayName("RTF-FR-06.3: GET komplettering returnerar personnummer och avsikt för ett ärende")
   void get_komplettering_returns_personnummer_and_avsikt()
   {
      var response = given().when()
            .get("/regel/rtf-manuell/{id}/komplettering", COMPLETE_HANDLAGGNING_ID)
            .then().statusCode(200)
            .extract().as(RtfKompletteringData.class);

      assertEquals("19901010-1234", response.getPersonnummer());
      assertEquals("NY", response.getAvsikt());
   }

   @Test
   @DisplayName("RTF-FR-06.3: PATCH komplettering tillåter korrigering av personnummer och avsikt")
   void patch_komplettering_returns_204()
   {
      var request = new RtfKompletteringData("19901010-9999", "FRAN_ARBETE");

      given().contentType(ContentType.JSON).body(request)
            .when().patch("/regel/rtf-manuell/{id}/komplettering", COMPLETE_HANDLAGGNING_ID)
            .then().statusCode(204);
   }

   @Test
   @DisplayName("RTF-FR-06.3: POST done returnerar 409 när inget kompletteringstillstånd finns")
   void post_done_returns_409_when_no_tillstand()
   {
      given().when()
            .post("/regel/rtf-manuell/{id}/komplettering/done", COMPLETE_HANDLAGGNING_ID)
            .then().statusCode(409);
   }

   @Test
   @DisplayName("RTF-FR-06.1, RTF-FR-06.2: Komplettering initieras via Kafka när personnummer eller avsikt saknas")
   void komplettering_is_triggered_via_kafka_when_data_is_missing()
   {
      regelKafkaConnector.sendRegelRequest(MISSING_DATA_HANDLAGGNING_ID, responseTopic);

      // Komplettering OUL uppgift is created synchronously after the Kafka message is processed
      var oulRequests = waitForRequest("/uppgifter", RequestMethod.POST, 1);
      assertEquals(1, oulRequests.size());
   }

   @Test
   @DisplayName("RTF-FR-06.1, RTF-FR-06.2: POST done returnerar 422 när tillstånd finns men uppgifter fortfarande saknas")
   void post_done_returns_422_when_tillstand_exists_but_data_still_missing()
   {
      // Establish KompletteringTillstand by triggering komplettering via Kafka
      regelKafkaConnector.sendRegelRequest(MISSING_DATA_HANDLAGGNING_ID, responseTopic);
      waitForRequest("/uppgifter", RequestMethod.POST, 1);

      // Call done without patching — handlaggning still has missing data → 422
      given().when()
            .post("/regel/rtf-manuell/{id}/komplettering/done", MISSING_DATA_HANDLAGGNING_ID)
            .then().statusCode(422);
   }

   @Test
   @DisplayName("RTF-FR-06.1, RTF-FR-06.2, RTF-FR-06.3: POST done returnerar 204 efter att PATCH har kompletterat saknade uppgifter")
   void post_done_returns_204_after_patch_supplies_missing_data()
   {
      // Step 1: Kafka trigger → GET #1 (missing data, scenario: Started → AfterKafkaTrigger)
      // checkKomplettering fails → OUL uppgift created + KompletteringTillstand stored in DB
      regelKafkaConnector.sendRegelRequest(MISSING_DATA_HANDLAGGNING_ID, responseTopic);
      waitForRequest("/uppgifter", RequestMethod.POST, 1);

      // Step 2: PATCH → GET #2 (missing data, scenario: AfterKafkaTrigger → Complete) + PUT
      var patchRequest = new RtfKompletteringData("19901010-5555", "FRAN_ARBETE");
      given().contentType(ContentType.JSON).body(patchRequest)
            .when().patch("/regel/rtf-manuell/{id}/komplettering", MISSING_DATA_HANDLAGGNING_ID)
            .then().statusCode(204);

      // Step 3: POST done → GET #3 (complete data, scenario: Complete) → checkKomplettering passes
      // Framework ends OUL and re-runs the rule; returns 204
      given().when()
            .post("/regel/rtf-manuell/{id}/komplettering/done", MISSING_DATA_HANDLAGGNING_ID)
            .then().statusCode(204);
   }
}
