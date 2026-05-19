package se.fk.github.manuellregelratttillforsakring;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRtfManuell.class)
})
public class RtfManuellPostDataTest extends AbstractRegelManuellTest
{

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void post_data_done_should_update_handlaggning_uppgift_avslutad(String handlaggningId, String uppgiftId)
         throws JsonProcessingException, InterruptedException
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);

      //
      // Send mocked OUL response
      //
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      //
      // clear wiremock requests
      //
      WireMockRtfManuell.getWireMockServer().resetRequests();
      //
      // Delay required to make sure regel service ready
      //
      Thread.sleep(1000);
      //
      // mock POST done operation from portal FE
      //
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      //
      // verify PUT handlaggning
      //
      var handlaggningPutUpdate = WireMockRtfManuell.getLastPutHandlaggning(handlaggningId);
      assertEquals(handlaggningId, handlaggningPutUpdate.getHandlaggning().getId().toString());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getVersion());
      assertEquals(2, handlaggningPutUpdate.getHandlaggning().getUppgift().getVersion());
      assertEquals("3", handlaggningPutUpdate.getHandlaggning().getUppgift().getUppgiftStatus());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void post_data_done_should_update_oul_status(String handlaggningId, String uppgiftId) throws InterruptedException
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      //
      // Send mocked OUL response
      //
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      //
      // Delay required to make sure regel service ready
      //
      Thread.sleep(1000);
      //
      // mock POST done operation from portal FE
      //
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      //
      // verify kafka status message sent to oul
      //
      var oulStatusMessage = oulKafkaConnector.waitForOulStatusMessage();
      assertEquals(uppgiftId, oulStatusMessage.getUppgiftId());
      assertEquals(Status.AVSLUTAD, oulStatusMessage.getStatus());
   }

}
