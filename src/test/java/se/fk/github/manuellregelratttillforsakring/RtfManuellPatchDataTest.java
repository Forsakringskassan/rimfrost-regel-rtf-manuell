package se.fk.github.manuellregelratttillforsakring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.ersattningdata.ErsattningData;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;
import java.time.LocalDate;
import java.util.UUID;
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
   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @Inject
   ObjectMapper objectMapper;

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void patch_data_should_update_handlaggning(String handlaggningId) throws JsonProcessingException
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
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
      var handlaggningPutUpdate = WireMockRtfManuell.getLastPutHandlaggningUpdate(handlaggningId);
      assertEquals(handlaggningId, handlaggningPutUpdate.getHandlaggning().getId().toString());
      assertEquals(2, handlaggningPutUpdate.getHandlaggning().getVersion());
      assertEquals("PLANERAD", handlaggningPutUpdate.getUppgift().getUppgiftStatus());
      var sakfragor = handlaggningPutUpdate.getHandlaggning().getYrkande().getSakfragorStallningstaganden();
      assertEquals(2, sakfragor.size());
      //
      // verify updated ersattning
      //
      var ersattningContainer = sakfragor.stream()
            .filter(s -> s.getId().toString().equals(ERSATTNINGSID)).findFirst().orElseThrow();
      assertEquals("ersattning", ersattningContainer.getObjektTypId());
      var ersattningData = ErsattningData.fromJson(ersattningContainer.getData(), objectMapper);
      assertEquals(2, ersattningData.getVersion());
      assertEquals(AVSLAGSANLEDNING, ersattningData.getAvslagsanledning());
      assertEquals(se.fk.rimfrost.ersattningdata.Beslutsutfall.JA, ersattningData.getBeslutsutfall());
      //
      // verify untouched ersattning fields survive the round trip
      //
      assertEquals(UUID.fromString(ERSATTNINGSID), ersattningData.getId());
      assertEquals(40000, ersattningData.getBelopp());
      assertEquals(100, ersattningData.getOmfattningProcent());
      assertEquals(100, ersattningData.getBerakningsgrund());
      assertEquals(ERSATTNINGSTYP, ersattningData.getErsattningstyp().id());
      assertEquals("HUNDBIDRAG", ersattningData.getErsattningstyp().namn());
      assertEquals(LocalDate.parse(FIXTURE_FROM), ersattningData.getFrom());
      assertEquals(LocalDate.parse(FIXTURE_TOM), ersattningData.getTom());
      assertEquals("YRKAT", ersattningData.getYrkandeStatus());
      //
      // verify non-ersattning sakfraga is passed through unchanged
      //
      var annanSakfraga = sakfragor.stream()
            .filter(s -> s.getId().toString().equals(ANNAN_SAKFRAGA_ID)).findFirst().orElseThrow();
      assertEquals(ANNAN_SAKFRAGA_OBJEKT_TYP_ID, annanSakfraga.getObjektTypId());
      assertEquals(objectMapper.readTree(ANNAN_SAKFRAGA_DATA), objectMapper.readTree(annanSakfraga.getData()));
   }

}
