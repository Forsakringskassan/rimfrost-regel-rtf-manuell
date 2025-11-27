package se.fk.github.manuellregelratttillforsakring.presentation.kafka;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.manuellregelratttillforsakring.logic.dto.CreateRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableCreateRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableUpdateRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateRtfDataRequest;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.regel.rtf.manuell.RtfManuellRequestMessagePayload;

@ApplicationScoped
public class RtfManuellKafkaMapper
{
   public CreateRtfDataRequest toCreateRtfDataRequest(RtfManuellRequestMessagePayload rtfRequest)
   {
      return ImmutableCreateRtfDataRequest.builder()
            .id(UUID.fromString(rtfRequest.getId()))
            .kogitorootprociid(UUID.fromString(rtfRequest.getKogitorootprociid()))
            .kogitorootprocid(rtfRequest.getKogitorootprocid())
            .kogitoparentprociid(UUID.fromString(rtfRequest.getKogitoparentprociid()))
            .kogitoprocid(rtfRequest.getKogitoprocid())
            .kogitoprocinstanceid(UUID.fromString(rtfRequest.getKogitoprocinstanceid()))
            .kogitoprocist(rtfRequest.getKogitoprocist())
            .kundbehovsflodeId(UUID.fromString(rtfRequest.getData().getKundbehovsflodeId()))
            .build();
   }

   public UpdateRtfDataRequest toUpdateRtfDataRequest(OperativtUppgiftslagerResponseMessage oulResponse)
   {
      return ImmutableUpdateRtfDataRequest.builder()
            .kundbehovsflodeId(UUID.fromString(oulResponse.getKundbehovsflodeId()))
            .uppgiftId(UUID.fromString(oulResponse.getUppgiftId()))
            .build();
   }
}
