package se.fk.github.manuellregelratttillforsakring;

import io.restassured.http.ContentType;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;
import static io.restassured.RestAssured.given;

public class RtfManuellRestMock
{
   public static GetDataResponse sendGetRtfManuell(String handlaggningId)
   {
      return given().when().get("/regel/rtf-manuell/{handlaggningId}", handlaggningId).then().statusCode(200).extract()
            .as(GetDataResponse.class);
   }

   public static void sendPatchRtfManuell(String handlaggningId, PatchErsattningRequest patchErsattningRequest)
   {
      given().contentType(ContentType.JSON).body(patchErsattningRequest).when()
            .patch("/regel/rtf-manuell/{handlaggningId}", handlaggningId)
            .then().statusCode(204);
   }

   public static void sendPostRtfManuell(String handlaggningId)
   {
      given().when().post("/regel/rtf-manuell/{handlaggningId}/done", handlaggningId).then().statusCode(204);
   }
}
