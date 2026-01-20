package se.fk.github.manuellregelratttillforsakring.presentation.kafka;

import java.util.UUID;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableUpdateRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableUpdateStatusRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateStatusRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UppgiftStatus;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.regel.common.RegelRequestMessagePayload;

@ApplicationScoped
public class RtfManuellKafkaMapper
{

   public UpdateRtfDataRequest toUpdateRtfDataRequest(OperativtUppgiftslagerResponseMessage oulResponse)
   {
      return ImmutableUpdateRtfDataRequest.builder()
            .kundbehovsflodeId(UUID.fromString(oulResponse.getKundbehovsflodeId()))
            .uppgiftId(UUID.fromString(oulResponse.getUppgiftId()))
            .build();
   }

   public UpdateStatusRequest toUpdateStatusRequest(OperativtUppgiftslagerStatusMessage statusMessage)
   {

      return ImmutableUpdateStatusRequest.builder()
            .kundbehovsflodeId(UUID.fromString(statusMessage.getKundbehovsflodeId()))
            .uppgiftId(UUID.fromString(statusMessage.getUppgiftId()))
            .uppgiftStatus(mapStatus(statusMessage.getStatus()))
            .build();
   }

   private UppgiftStatus mapStatus(Status status)
   {

      switch (status)
      {
         case NY:
            return UppgiftStatus.NY;
         case TILLDELAD:
            return UppgiftStatus.TILLDELAD;
         case AVSLUTAD:
         default:
            return UppgiftStatus.AVSLUTAD;
      }
   }
}
