package se.fk.github.manuellregelratttillforsakring.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.ArbetsgivareAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.github.manuellregelratttillforsakring.integration.config.RegelConfigProvider;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.FolkbokfordAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.github.manuellregelratttillforsakring.integration.kafka.RtfManuellKafkaProducer;
import se.fk.github.manuellregelratttillforsakring.integration.kafka.dto.ImmutableOulMessageRequest;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.KundbehovsflodeAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeRequest;
import se.fk.github.manuellregelratttillforsakring.logic.config.RegelConfig;
import se.fk.github.manuellregelratttillforsakring.logic.dto.*;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ImmutableErsattningData;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ImmutableRtfData;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ImmutableUnderlag;
import se.fk.github.manuellregelratttillforsakring.logic.entity.RtfData;
import se.fk.rimfrost.Status;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ErsattningData;
import se.fk.rimfrost.regel.common.integration.kafka.RegelKafkaProducer;
import se.fk.rimfrost.regel.common.logic.RegelMapper;
import se.fk.rimfrost.regel.common.logic.dto.RegelDataRequest;
import se.fk.rimfrost.regel.common.logic.entity.CloudEventData;
import se.fk.rimfrost.regel.common.logic.entity.ImmutableCloudEventData;
import se.fk.rimfrost.regel.common.presentation.kafka.RegelRequestHandlerInterface;

@ApplicationScoped
@Startup
public class RtfService implements RegelRequestHandlerInterface
{

   @Inject
   RegelConfigProvider regelConfigProvider;

   @Inject
   ObjectMapper objectMapper;

   @Inject
   RtfManuellKafkaProducer rtfManuellKafkaProducer;

   @Inject
   RegelKafkaProducer regelKafkaProducer;

   @Inject
   RtfMapper mapper;

   @Inject
   RegelMapper regelMapper;

   @Inject
   FolkbokfordAdapter folkbokfordAdapter;

   @Inject
   ArbetsgivareAdapter arbetsgivareAdapter;

   @Inject
   KundbehovsflodeAdapter kundbehovsflodeAdapter;

   private RegelConfig regelConfig;

   Map<UUID, CloudEventData> cloudevents = new HashMap<UUID, CloudEventData>();
   Map<UUID, RtfData> rtfDatas = new HashMap<UUID, RtfData>();

   @ConfigProperty(name = "kafka.source")
   String kafkaSource;

   @SuppressWarnings("unused")
   @PostConstruct
   void init()
   {
      this.regelConfig = regelConfigProvider.getConfig();
   }

   public GetRtfDataResponse getData(GetRtfDataRequest request) throws JsonProcessingException
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

      updateRtfDataUnderlag(rtfData, folkbokfordResponse, arbetsgivareResponse);

      updateKundbehovsflodeInfo(rtfData);

      return mapper.toRtfResponse(kundbehovflodesResponse, folkbokfordResponse, arbetsgivareResponse, rtfData);
   }

   @Override
   public void handle(RegelDataRequest request)
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
            .uppgiftId(UUID.randomUUID())
            .cloudeventId(cloudeventData.id())
            .ersattningar(ersattninglist)
            .underlag(new ArrayList<>())
            .build();

      cloudevents.put(cloudeventData.id(), cloudeventData);
      rtfDatas.put(rtfData.kundbehovsflodeId(), rtfData);

      var oulMessageRequest = ImmutableOulMessageRequest.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .kundbehov("Vård av husdjur")
            .regel(regelConfig.getUppgift().getNamn())
            .beskrivning(regelConfig.getUppgift().getBeskrivning())
            .verksamhetslogik(regelConfig.getUppgift().getVerksamhetslogik())
            .roll(regelConfig.getUppgift().getRoll())
            .url("http://localhost:8888" + regelConfig.getUppgift().getPath() + "/" + request.kundbehovsflodeId().toString())
            .build();
      rtfManuellKafkaProducer.sendOulRequest(oulMessageRequest);
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
         var rtfResponse = regelMapper.toRegelResponse(updatedRtfData.kundbehovsflodeId(), cloudevent, rattTillForsakring);
         rtfManuellKafkaProducer.sendOulStatusUpdate(updatedRtfData.uppgiftId(), Status.AVSLUTAD);
         regelKafkaProducer.sendRegelResponse(rtfResponse, kafkaSource);
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

   private void updateRtfDataUnderlag(RtfData rtfData, FolkbokfordResponse folkbokfordResponse,
         ArbetsgivareResponse arbetsgivareResponse) throws JsonProcessingException
   {
      var rtfDataBuilder = ImmutableRtfData.builder().from(rtfData);

      if (folkbokfordResponse != null)
      {
         var folkbokfordUnderlag = ImmutableUnderlag.builder()
               .typ("FolkbokfördUnderlag")
               .version("1.0")
               .data(objectMapper.writeValueAsString(folkbokfordResponse))
               .build();
         rtfDataBuilder.addUnderlag(folkbokfordUnderlag);
      }

      if (arbetsgivareResponse != null)
      {
         var arbetsgivareUnderlag = ImmutableUnderlag.builder()
               .typ("ArbetsgivareUnderlag")
               .version("1.0")
               .data(objectMapper.writeValueAsString(arbetsgivareResponse))
               .build();
         rtfDataBuilder.addUnderlag(arbetsgivareUnderlag);
      }

      rtfDatas.put(rtfData.kundbehovsflodeId(), rtfDataBuilder.build());
   }

   private void updateKundbehovsflodeInfo(RtfData rtfData)
   {
      var request = mapper.toUpdateKundbehovsflodeRequest(rtfData);
      kundbehovsflodeAdapter.updateKundbehovsflodeInfo(request);
   }
}
