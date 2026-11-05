package se.fk.github.manuellregelratttillforsakring;

import io.restassured.http.ContentType;
import jakarta.ws.rs.core.HttpHeaders;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;
import static io.restassured.RestAssured.given;

public class RtfManuellRestMock
{
   /** Matches the identity resolved by the framework's WireMock identity stub, see AbstractRegelManuellTest. */
   private static final String AUTHORIZATION_HEADER_VALUE = "Bearer ID_TYP:ID_VARDE";

   /**
    * Sends a GET request and returns the parsed {@code data} part of the response body. Asserts HTTP 200.
    *
    * @param handlaggningId identifier of the handlaggning to retrieve
    * @return parsed {@link GetDataResponse}
    */
   public static GetDataResponse sendGetRtfManuell(String handlaggningId)
   {
      return given().header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE).when()
            .get("/regel/rtf-manuell/{handlaggningId}", handlaggningId).then().statusCode(200).extract()
            .jsonPath().getObject("data", GetDataResponse.class);
   }

   /**
    * Sends a PATCH request to update ersattningar. Asserts HTTP 204.
    *
    * @param handlaggningId         identifier of the handlaggning to update
    * @param patchErsattningRequest request body containing the updated ersattningar
    */
   public static void sendPatchRtfManuell(String handlaggningId, PatchErsattningRequest patchErsattningRequest)
   {
      given().header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE).contentType(ContentType.JSON)
            .body(patchErsattningRequest).when()
            .patch("/regel/rtf-manuell/{handlaggningId}", handlaggningId)
            .then().statusCode(204);
   }

}
