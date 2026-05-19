package se.fk.github.manuellregelratttillforsakring;

import io.restassured.http.ContentType;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;
import static io.restassured.RestAssured.given;

public class RtfManuellRestMock
{
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

}
