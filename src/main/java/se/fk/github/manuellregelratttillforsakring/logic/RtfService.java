package se.fk.github.manuellregelratttillforsakring.logic;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.ArbetsgivareAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.FolkbokfordAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.KundbehovsflodeAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeRequest;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ImmutableErsattningData;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ImmutableRtfData;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ImmutableRtfData.Builder;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ImmutableUnderlag;
import se.fk.github.manuellregelratttillforsakring.logic.entity.RtfData;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.FSSAinformation;
import se.fk.rimfrost.framework.regel.integration.config.RegelConfigProvider;
import se.fk.rimfrost.framework.regel.integration.kafka.RegelKafkaProducer;
import se.fk.rimfrost.framework.regel.logic.RegelMapper;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableCloudEventData;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;
import se.fk.rimfrost.framework.oul.integration.kafka.OulKafkaProducer;
import se.fk.rimfrost.framework.oul.integration.kafka.dto.ImmutableOulMessageRequest;
import se.fk.rimfrost.framework.oul.logic.dto.OulResponse;
import se.fk.rimfrost.framework.oul.logic.dto.OulStatus;
import se.fk.rimfrost.framework.oul.presentation.kafka.OulHandlerInterface;
import se.fk.rimfrost.framework.regel.presentation.kafka.RegelRequestHandlerInterface;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ErsattningData;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateErsattningDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateStatusRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UppgiftStatus;

@ApplicationScoped
public class RtfService implements RegelRequestHandlerInterface, OulHandlerInterface
{

   @ConfigProperty(name = "application.base-url")
   String applicationBaseUrl;

   @ConfigProperty(name = "kafka.source")
   String kafkaSource;

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @Inject
   RegelConfigProvider regelConfigProvider;

   @Inject
   ObjectMapper objectMapper;

   @Inject
   RegelKafkaProducer regelKafkaProducer;

   @Inject
   OulKafkaProducer oulKafkaProducer;

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

   Map<UUID, CloudEventData> cloudevents = new HashMap<UUID, CloudEventData>();
   Map<UUID, RtfData> rtfDatas = new HashMap<UUID, RtfData>();

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
   public void handleRegelRequest(RegelDataRequest request)
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
            .type(responseTopic)
            .source(kafkaSource)
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
            .skapadTs(OffsetDateTime.now())
            .planeradTs(OffsetDateTime.now())
            .uppgiftStatus(UppgiftStatus.PLANERAD)
            .fssaInformation(FSSAinformation.HANDLAGGNING_PAGAR)
            .underlag(new ArrayList<>())
            .build();

      cloudevents.put(cloudeventData.id(), cloudeventData);
      rtfDatas.put(rtfData.kundbehovsflodeId(), rtfData);

      var regelConfig = regelConfigProvider.getConfig();

      var oulMessageRequest = ImmutableOulMessageRequest.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .kundbehov(kundbehovflodesResponse.formanstyp())
            .regel(regelConfig.getSpecifikation().getNamn())
            .beskrivning(regelConfig.getSpecifikation().getUppgiftbeskrivning())
            .verksamhetslogik(regelConfig.getSpecifikation().getVerksamhetslogik())
            .roll(regelConfig.getSpecifikation().getRoll())
            .url(regelConfig.getUppgift().getPath())
            .build();
      oulKafkaProducer.sendOulRequest(oulMessageRequest);
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

      var updatedRtfDataBuilder = ImmutableRtfData.builder()
            .from(rtfData)
            .ersattningar(updatedList);

      var updatedRtfData = updatedRtfDataBuilder.build();
      rtfDatas.put(updateRequest.kundbehovsflodeId(), updatedRtfData);

      updateKundbehovsflodeInfo(updatedRtfData);

   }

   public void setUppgiftDone(UUID kundbehovsflodeId)
   {
      var rtfData = rtfDatas.get(kundbehovsflodeId);

      var updatedRtfDataBuilder = ImmutableRtfData.builder()
            .from(rtfData);

      updatedRtfDataBuilder.uppgiftStatus(UppgiftStatus.AVSLUTAD);

      var updatedRtfData = updatedRtfDataBuilder.build();
      rtfDatas.put(kundbehovsflodeId, updatedRtfData);

      var utfall = rtfData.ersattningar().stream().allMatch(e -> e.beslutsutfall() == Beslutsutfall.JA) ? Utfall.JA : Utfall.NEJ;
      var cloudevent = cloudevents.get(updatedRtfData.cloudeventId());
      var rtfResponse = regelMapper.toRegelResponse(updatedRtfData.kundbehovsflodeId(), cloudevent, utfall);
      oulKafkaProducer.sendOulStatusUpdate(updatedRtfData.uppgiftId(), Status.AVSLUTAD);
      regelKafkaProducer.sendRegelResponse(rtfResponse);

      updateKundbehovsflodeInfo(updatedRtfData);
   }

   public void updateStatus(UpdateStatusRequest request)
   {
      RtfData rtfData = rtfDatas.values()
            .stream()
            .filter(r -> r.uppgiftId().equals(request.uppgiftId()))
            .findFirst()
            .orElse(rtfDatas.get(request.kundbehovsflodeId()));

      Builder rtfBuilder = ImmutableRtfData.builder()
            .from(rtfData);

      if (request.utforarId() != null)
      {
         rtfBuilder
               .utforarId(request.utforarId())
               .uppgiftStatus(UppgiftStatus.TILLDELAD);
      }
      else
      {
         rtfBuilder
               .uppgiftStatus(UppgiftStatus.PLANERAD);
      }

      var updatedRtfData = rtfBuilder.build();
      rtfDatas.put(rtfData.kundbehovsflodeId(), updatedRtfData);
      updateKundbehovsflodeInfo(updatedRtfData);
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
      var request = mapper.toUpdateKundbehovsflodeRequest(rtfData, regelConfigProvider.getConfig());
      kundbehovsflodeAdapter.updateKundbehovsflodeInfo(request);
   }

   @Override
   public void handleOulResponse(OulResponse oulResponse)
   {
      var rtfData = rtfDatas.get(oulResponse.kundbehovsflodeId());
      var updatedRtfData = ImmutableRtfData.builder()
            .from(rtfData)
            .uppgiftId(oulResponse.uppgiftId())
            .build();
      rtfDatas.put(updatedRtfData.kundbehovsflodeId(), updatedRtfData);
      updateKundbehovsflodeInfo(updatedRtfData);
   }

   @Override
   public void handleOulStatus(OulStatus oulStatus)
   {
      RtfData rtfData = rtfDatas.values()
            .stream()
            .filter(r -> r.uppgiftId().equals(oulStatus.uppgiftId()))
            .findFirst()
            .orElse(rtfDatas.get(oulStatus.kundbehovsflodeId()));
      updateKundbehovsflodeInfo(rtfData);
   }
}
