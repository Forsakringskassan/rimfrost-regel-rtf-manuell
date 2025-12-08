package se.fk.github.manuellregelratttillforsakring.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.ArbetsgivareAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.FolkbokfordAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.github.manuellregelratttillforsakring.integration.kafka.RtfManuellKafkaProducer;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.KundbehovsflodeAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeRequest;
import se.fk.github.manuellregelratttillforsakring.logic.entity.CloudEventData;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ImmutableCloudEventData;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ImmutableErsattningData;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ImmutableRtfData;
import se.fk.github.manuellregelratttillforsakring.logic.entity.RtfData;
import se.fk.rimfrost.Status;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ErsattningData;
import se.fk.github.manuellregelratttillforsakring.logic.dto.Beslutsutfall;
import se.fk.github.manuellregelratttillforsakring.logic.dto.CreateRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateErsattningDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateStatusRequest;

@ApplicationScoped
public class RtfService
{

   @Inject
   RtfManuellKafkaProducer kafkaProducer;

   @Inject
   RtfMapper mapper;

   @Inject
   FolkbokfordAdapter folkbokfordAdapter;

   @Inject
   ArbetsgivareAdapter arbetsgivareAdapter;

   @Inject
   KundbehovsflodeAdapter kundbehovsflodeAdapter;

   Map<UUID, CloudEventData> cloudevents = new HashMap<UUID, CloudEventData>();
   Map<UUID, RtfData> rtfDatas = new HashMap<UUID, RtfData>();

   public GetRtfDataResponse getData(GetRtfDataRequest request)
   {
      var kundbehovsflodeRequest = ImmutableKundbehovsflodeRequest.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .build();
      var kundbehovflodesResponse = kundbehovsflodeAdapter.getKundbehovsflodeInfo(kundbehovsflodeRequest);

      var folkbokfordRequest = ImmutableFolkbokfordRequest.builder()
            .personnummer(kundbehovflodesResponse.personnummer())
            .build();
      var folkbokfordResponse = folkbokfordAdapter.getFolkbokfordInfo(folkbokfordRequest);

      var arbetsgivareRequest = ImmutableArbetsgivareRequest.builder()
            .personnummer(kundbehovflodesResponse.personnummer())
            .build();
      var arbetsgivareResponse = arbetsgivareAdapter.getArbetsgivareInfo(arbetsgivareRequest);

      var rtfData = rtfDatas.get(request.kundbehovsflodeId());

      updateKundbehovsflodeInfo(rtfData);

      return mapper.toRtfResponse(kundbehovflodesResponse, folkbokfordResponse, arbetsgivareResponse, rtfData);
   }

   public void createRtfData(CreateRtfDataRequest request)
   {
      var kundbehovsflodeRequest = ImmutableKundbehovsflodeRequest.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .build();
      var kundbehovflodesResponse = kundbehovsflodeAdapter.getKundbehovsflodeInfo(kundbehovsflodeRequest);

      var cloudeventData = ImmutableCloudEventData.builder()
            .id(request.id())
            .kogitoparentprociid(request.kogitoparentprociid())
            .kogitoprocid(request.kogitoprocid())
            .kogitoprocinstanceid(request.kogitoprocinstanceid())
            .kogitoprocist(request.kogitoprocist())
            .kogitoprocversion(request.kogitoprocversion())
            .kogitorootprocid(request.kogitorootprocid())
            .kogitorootprociid(request.kogitorootprociid())
            .build();

      var ersattninglist = new ArrayList<ErsattningData>();

      for (var ersattning : kundbehovflodesResponse.ersattning())
      {
         var ersattningData = ImmutableErsattningData.builder()
               .id(ersattning.ersattningsId())
               .build();
         ersattninglist.add(ersattningData);
      }

      var rtfData = ImmutableRtfData.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .cloudeventId(cloudeventData.id())
            .ersattningar(ersattninglist)
            .build();

      cloudevents.put(cloudeventData.id(), cloudeventData);
      rtfDatas.put(rtfData.kundbehovsflodeId(), rtfData);

      kafkaProducer.sendOulRequest(request.kundbehovsflodeId());
   }

   public void updateRtfData(UpdateRtfDataRequest updateRequest)
   {
      var rtfData = rtfDatas.get(updateRequest.kundbehovsflodeId());
      var updatedRtfData = ImmutableRtfData.builder()
            .from(rtfData)
            .uppgiftId(updateRequest.uppgiftId())
            .build();
      rtfDatas.put(updatedRtfData.kundbehovsflodeId(), updatedRtfData);
      updateKundbehovsflodeInfo(updatedRtfData);
   }

   public void updateErsattningData(UpdateErsattningDataRequest updateRequest)
   {
      var rtfData = rtfDatas.get(updateRequest.kundbehovsflodeId());

      var existingErsattning = rtfData.ersattningar().stream()
            .filter(e -> e.id().equals(updateRequest.ersattningId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("ErsattningData not found"));

      var updatedErsattning = ImmutableErsattningData.builder()
            .from(existingErsattning)
            .beslutsutfall(updateRequest.beslutsutfall())
            .avslagsanledning(updateRequest.avslagsanledning())
            .build();

      var updatedList = rtfData.ersattningar().stream()
            .map(e -> e.id().equals(updateRequest.ersattningId()) ? updatedErsattning : e)
            .toList();

      var updatedRtfData = ImmutableRtfData.builder()
            .from(rtfData)
            .ersattningar(updatedList)
            .build();

      rtfDatas.put(updateRequest.kundbehovsflodeId(), updatedRtfData);

      updateKundbehovsflodeInfo(updatedRtfData);

      if (updateRequest.signernad())
      {
         var rattTillForsakring = updatedList.stream().allMatch(e -> e.beslutsutfall() == Beslutsutfall.JA);
         var cloudevent = cloudevents.get(updatedRtfData.cloudeventId());
         var rtfResponse = mapper.toRtfResponseRequest(updatedRtfData, cloudevent, rattTillForsakring);
         kafkaProducer.sendOulStatusUpdate(updatedRtfData.uppgiftId(), Status.AVSLUTAD);
         kafkaProducer.sendRtfManuellResponse(rtfResponse);
      }
   }

   public void updateStatus(UpdateStatusRequest request)
   {
      RtfData rtfData = rtfDatas.values()
            .stream()
            .filter(r -> r.uppgiftId().equals(request.uppgiftId()))
            .findFirst()
            .orElse(rtfDatas.get(request.kundbehovsflodeId()));
      updateKundbehovsflodeInfo(rtfData);
   }

   private void updateKundbehovsflodeInfo(RtfData rtfData)
   {
      var request = mapper.toUpdateKundbehovsflodeRequest(rtfData);
      kundbehovsflodeAdapter.updateKundbehovsflodeInfo(request);
   }
}
