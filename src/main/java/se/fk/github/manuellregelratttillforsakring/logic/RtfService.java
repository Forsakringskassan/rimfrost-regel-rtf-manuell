package se.fk.github.manuellregelratttillforsakring.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.ArbetsgivareAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.FolkbokfordAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableErsattningData;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableRegelData;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableUnderlag;
import se.fk.rimfrost.framework.regel.integration.kundbehovsflode.KundbehovsflodeAdapter;
import se.fk.rimfrost.framework.regel.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateErsattningDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateStatusRequest;
import se.fk.rimfrost.framework.regel.logic.dto.UppgiftStatus;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellService;
import se.fk.rimfrost.framework.regel.logic.entity.RegelData;

@ApplicationScoped
@Startup
public class RtfService extends RegelManuellService
{
   @Inject
   ObjectMapper objectMapper;

   @Inject
   RtfMapper mapper;

   @Inject
   FolkbokfordAdapter folkbokfordAdapter;

   @Inject
   ArbetsgivareAdapter arbetsgivareAdapter;

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

      var regelData = regelDatas.get(request.kundbehovsflodeId());

      updateRtfDataUnderlag(regelData, folkbokfordResponse, arbetsgivareResponse);

      updateKundbehovsflodeInfo(regelData);

      return mapper.toRtfResponse(kundbehovflodesResponse, folkbokfordResponse, arbetsgivareResponse, regelData);
   }

   public void updateErsattningData(UpdateErsattningDataRequest updateRequest)
   {
      var regelData = regelDatas.get(updateRequest.kundbehovsflodeId());

      var existingErsattning = regelData.ersattningar().stream()
            .filter(e -> e.id().equals(updateRequest.ersattningId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("ErsattningData not found"));

      var updatedErsattning = ImmutableErsattningData.builder()
            .from(existingErsattning)
            .beslutsutfall(updateRequest.beslutsutfall())
            .avslagsanledning(updateRequest.avslagsanledning())
            .build();

      var updatedList = regelData.ersattningar().stream()
            .map(e -> e.id().equals(updateRequest.ersattningId()) ? updatedErsattning : e)
            .toList();

      var updatedRegelData = ImmutableRegelData.builder()
            .from(regelData)
            .ersattningar(updatedList)
            .build();

      regelDatas.put(updateRequest.kundbehovsflodeId(), updatedRegelData);

      updateKundbehovsflodeInfo(updatedRegelData);

   }

   public void updateStatus(UpdateStatusRequest request)
   {
      RegelData regelData = regelDatas.values()
            .stream()
            .filter(r -> r.uppgiftId().equals(request.uppgiftId()))
            .findFirst()
            .orElse(regelDatas.get(request.kundbehovsflodeId()));

      var regelDataBuilder = ImmutableRegelData.builder()
            .from(regelData);

      if (request.utforarId() != null)
      {
         regelDataBuilder
               .utforarId(request.utforarId())
               .uppgiftStatus(UppgiftStatus.TILLDELAD);
      }
      else
      {
         regelDataBuilder
               .uppgiftStatus(UppgiftStatus.PLANERAD);
      }

      var updatedRtfData = regelDataBuilder.build();
      regelDatas.put(regelData.kundbehovsflodeId(), updatedRtfData);
      updateKundbehovsflodeInfo(updatedRtfData);
   }

   private void updateRtfDataUnderlag(RegelData regelData, FolkbokfordResponse folkbokfordResponse,
         ArbetsgivareResponse arbetsgivareResponse) throws JsonProcessingException
   {
      var regelDataBuilder = ImmutableRegelData.builder().from(regelData);

      if (folkbokfordResponse != null)
      {
         var folkbokfordUnderlag = ImmutableUnderlag.builder()
               .typ("FolkbokfördUnderlag")
               .version("1.0")
               .data(objectMapper.writeValueAsString(folkbokfordResponse))
               .build();
         regelDataBuilder.addUnderlag(folkbokfordUnderlag);
      }

      if (arbetsgivareResponse != null)
      {
         var arbetsgivareUnderlag = ImmutableUnderlag.builder()
               .typ("ArbetsgivareUnderlag")
               .version("1.0")
               .data(objectMapper.writeValueAsString(arbetsgivareResponse))
               .build();
         regelDataBuilder.addUnderlag(arbetsgivareUnderlag);
      }

      regelDatas.put(regelData.kundbehovsflodeId(), regelDataBuilder.build());
   }
}
