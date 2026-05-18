package se.fk.github.manuellregelratttillforsakring;

import io.restassured.http.ContentType;
import java.util.concurrent.TimeUnit;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

public class RtfManuellRestMock
{
   /**
    * Blocks until the GET endpoint returns 200 for the given handlaggning, up to 5 seconds.
    * Use this after sending a regel request to ensure async Kafka processing and the DB write
    * have completed before making assertions.
    *
    * @param handlaggningId identifier of the handlaggning to wait for
    */
   public static void waitForRtfManuellReady(String handlaggningId)
   {
      await().atMost(5, TimeUnit.SECONDS)
            .until(() -> given().when().get("/regel/rtf-manuell/{handlaggningId}", handlaggningId)
                  .getStatusCode() == 200);
   }

   /**
    * Sends a GET request and returns the parsed response body. Asserts HTTP 200.
    *
    * @param handlaggningId identifier of the handlaggning to retrieve
    * @return parsed {@link GetDataResponse}
    */
   public static GetDataResponse sendGetRtfManuell(String handlaggningId)
   {
      return given().when().get("/regel/rtf-manuell/{handlaggningId}", handlaggningId).then().statusCode(200).extract()
            .as(GetDataResponse.class);
   }

   /**
    * Sends a PATCH request to update ersattningar. Asserts HTTP 204.
    *
    * @param handlaggningId         identifier of the handlaggning to update
    * @param patchErsattningRequest request body containing the updated ersattningar
    */
   public static void sendPatchRtfManuell(String handlaggningId, PatchErsattningRequest patchErsattningRequest)
   {
      given().contentType(ContentType.JSON).body(patchErsattningRequest).when()
            .patch("/regel/rtf-manuell/{handlaggningId}", handlaggningId)
            .then().statusCode(204);
   }

   /**
    * Sends a POST to mark the handlaggning as done. Asserts HTTP 204.
    *
    * @param handlaggningId identifier of the handlaggning to complete
    */
   public static void sendPostRtfManuell(String handlaggningId)
   {
      given().when().post("/regel/rtf-manuell/{handlaggningId}/done", handlaggningId).then().statusCode(204);
   }
}
