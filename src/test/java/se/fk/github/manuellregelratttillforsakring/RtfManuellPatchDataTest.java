package se.fk.github.manuellregelratttillforsakring;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.fk.github.manuellregelratttillforsakring.RtfManuellRestMock.sendPatchRtfManuell;
import static se.fk.github.manuellregelratttillforsakring.RtfManuellTestData.*;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRtfManuell.class)
})
public class RtfManuellPatchDataTest extends AbstractRegelManuellTest
{

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void patch_data_should_update_handlaggning(String handlaggningId) throws JsonProcessingException
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      waitForRegelManuellReady(handlaggningId);
      //
      // clear wiremock requests
      //
      WireMockRtfManuell.getWireMockServer().resetRequests();
      //
      // mock PATCH operation from portal FE
      //
      sendPatchRtfManuell(handlaggningId, newPatchErsattningRequest(Beslutsutfall.JA));
      //
      // verify PUT handlaggning
      //
      var handlaggningPutUpdate = WireMockRtfManuell.getLastPutHandlaggning(handlaggningId);
      assertEquals(handlaggningId, handlaggningPutUpdate.getHandlaggning().getId().toString());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getVersion());
      assertEquals("1", handlaggningPutUpdate.getHandlaggning().getUppgift().getUppgiftStatus());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getYrkande().getProduceradeResultat().size());
      var produceratResultat = handlaggningPutUpdate.getHandlaggning().getYrkande().getProduceradeResultat().getFirst();
      assertEquals(2, produceratResultat.getVersion());
      assertEquals(ERSATTNINGSID, produceratResultat.getId().toString());
      assertEquals(AVSLAGSANLEDNING, produceratResultat.getAvslagsanledning());
   }

}
