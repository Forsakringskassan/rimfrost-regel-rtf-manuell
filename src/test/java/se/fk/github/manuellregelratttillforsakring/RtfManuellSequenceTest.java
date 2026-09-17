package se.fk.github.manuellregelratttillforsakring;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.fk.github.manuellregelratttillforsakring.RtfManuellRestMock.*;
import static se.fk.github.manuellregelratttillforsakring.RtfManuellTestData.newPatchErsattningRequest;
import static se.fk.rimfrost.framework.regel.WireMockHandlaggning.waitForHandlaggningRequests;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRtfManuell.class)
})
public class RtfManuellSequenceTest extends AbstractRegelManuellTest
{
   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void full_sequence_should_create_regel_response(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      //
      // Verify GET handläggning requested
      //
      var handlaggningGetRequests = waitForHandlaggningRequests(handlaggningId, RequestMethod.GET, 1);
      assertEquals(1, handlaggningGetRequests.size());
      //
      // mock GET operation requested from portal FE
      //
      sendGetRtfManuell(handlaggningId);
      //
      // mock PATCH operation from portal FE
      //
      sendPatchRtfManuell(handlaggningId, newPatchErsattningRequest(Beslutsutfall.JA));
      //
      // Verify PUT handlaggning
      //
      var handlaggningPutRequests = waitForHandlaggningRequests(handlaggningId, RequestMethod.PUT, 1);
      assertEquals(1, handlaggningPutRequests.size());
      //
      // mock POST operation from portal FE
      //
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      //
      // Verify PUT handlaggning
      //
      handlaggningPutRequests = waitForHandlaggningRequests(handlaggningId, RequestMethod.PUT, 2);
      assertEquals(2, handlaggningPutRequests.size());
      //
      // Verify produced regel response
      //
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      var regelResponseData = regelResponse.getData();
      assertEquals(handlaggningId, regelResponseData.getHandlaggningId());
      assertEquals(Utfall.JA, regelResponseData.getUtfall());
   }

}
