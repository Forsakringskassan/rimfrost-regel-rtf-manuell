package se.fk.github.manuellregelratttillforsakring;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.framework.regel.logic.UppgiftStatus;
import se.fk.rimfrost.framework.regel.manuell.RegelManuellTestBase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.fk.github.manuellregelratttillforsakring.RtfManuellRestMock.sendGetRtfManuell;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRtfManuell.class)
})
public class RtfManuellGetDataTest extends RegelManuellTestBase
{

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void get_data_should_contain_handlaggning_id(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var getDataResponse = sendGetRtfManuell(handlaggningId);
      Assertions.assertEquals(handlaggningId, getDataResponse.getHandlaggningId().toString());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void get_data_should_contain_ersattningar(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var getDataResponse = sendGetRtfManuell(handlaggningId);
      Assertions.assertEquals(1, getDataResponse.getErsattningar().size());
      Assertions.assertEquals(RtfManuellTestData.PRODUCERADE_RESULTAT_ID,
            getDataResponse.getErsattningar().getFirst().getErsattningId().toString());
      Assertions.assertEquals(RtfManuellTestData.ERSATTNINGSTYP,
            getDataResponse.getErsattningar().getFirst().getErsattningstyp());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void get_data_should_contain_kund(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var getDataResponse = sendGetRtfManuell(handlaggningId);
      Assertions.assertEquals(RtfManuellTestData.KUND_FORNAMN, getDataResponse.getKund().getFornamn());
      Assertions.assertEquals(RtfManuellTestData.KUND_EFTERNAMN, getDataResponse.getKund().getEfternamn());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void get_data_should_update_handlaggning(String handlaggningId) throws JsonProcessingException
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      //
      // clear wiremock requests
      //
      WireMockRtfManuell.getWireMockServer().resetRequests();
      //
      // Send rtf manuell GET
      //
      sendGetRtfManuell(handlaggningId);
      //
      // verify PUT handlaggning
      //
      var handlaggningPutUpdate = WireMockRtfManuell.getLastPutHandlaggning(handlaggningId);
      assertEquals(handlaggningId, handlaggningPutUpdate.getHandlaggning().getId().toString());
      assertEquals(2, handlaggningPutUpdate.getHandlaggning().getVersion());
      assertEquals(UppgiftStatus.PLANERAD, handlaggningPutUpdate.getHandlaggning().getUppgift().getUppgiftStatus());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getUppgift().getVersion());
   }

}
