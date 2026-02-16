package se.fk.github.manuellregelratttillforsakring.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.store.storage.types.StorageManager;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateErsattningDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateStatusRequest;
import se.fk.github.manuellregelratttillforsakring.storage.StorageManagerProvider;
import se.fk.github.manuellregelratttillforsakring.storage.TestDataStorage;
import se.fk.github.manuellregelratttillforsakring.storage.TestDataStorageProvider;
import se.fk.rimfrost.framework.arbetsgivare.adapter.ArbetsgivareAdapter;
import se.fk.rimfrost.framework.arbetsgivare.adapter.dto.ArbetsgivareResponse;
import se.fk.rimfrost.framework.arbetsgivare.adapter.dto.ImmutableArbetsgivareRequest;
import se.fk.rimfrost.framework.folkbokford.adapter.FolkbokfordAdapter;
import se.fk.rimfrost.framework.folkbokford.adapter.dto.FolkbokfordResponse;
import se.fk.rimfrost.framework.folkbokford.adapter.dto.ImmutableFolkbokfordRequest;
import se.fk.rimfrost.framework.kundbehovsflode.adapter.dto.ImmutableKundbehovsflodeRequest;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;
import se.fk.rimfrost.framework.regel.logic.dto.UppgiftStatus;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableErsattningData;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableRegelData;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableUnderlag;
import se.fk.rimfrost.framework.regel.logic.entity.RegelData;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellService;

@ApplicationScoped
@Startup
public class RtfService extends RegelManuellService
{
   Logger logger = LoggerFactory.getLogger(RtfService.class);

   @Inject
   ObjectMapper objectMapper;

   @Inject
   RtfMapper mapper;

   @Inject
   FolkbokfordAdapter folkbokfordAdapter;

   @Inject
   ArbetsgivareAdapter arbetsgivareAdapter;

   @Inject
   StorageManagerProvider storageManagerProvider;

   @Inject
   TestDataStorageProvider testDataStorageProvider;

   @PostConstruct
   public void init()
   {
      TestDataStorage storage = testDataStorageProvider.getDataStorage();
      StorageManager storageManager = storageManagerProvider.getStorageManager();

      logger.info("text: {}", storage.getText());
      storage.setText("bar");

      logger.info("count: {}", storage.getCount());
      storage.setCount(storage.getCount() + 1);
      storageManager.store(storage);

      logger.info("list.size(): {}", storage.getList().size());
      storage.getList().add(new Object());
      storageManager.store(storage.getList());
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

   @Override
   protected Utfall decideUtfall(RegelData regelData)
   {
      return regelData.ersattningar().stream().allMatch(e -> e.beslutsutfall() == Beslutsutfall.JA) ? Utfall.JA : Utfall.NEJ;
   }
}
