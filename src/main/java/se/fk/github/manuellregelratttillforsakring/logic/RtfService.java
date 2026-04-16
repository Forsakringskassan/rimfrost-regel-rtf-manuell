package se.fk.github.manuellregelratttillforsakring.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ErsattningData;
import se.fk.github.manuellregelratttillforsakring.storage.ManuellRegelCommonDataStorageService;
import se.fk.rimfrost.adapter.arbetsgivare.ArbetsgivareAdapter;
import se.fk.rimfrost.adapter.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.rimfrost.adapter.folkbokford.FolkbokfordAdapter;
import se.fk.rimfrost.adapter.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableProduceratResultat;
import se.fk.rimfrost.framework.handlaggning.model.ProduceratResultat;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.RegelUtils;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceBase;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.UpdateErsattning;

@SuppressWarnings("unused")
@ApplicationScoped
@Startup
public class RtfService extends RegelManuellServiceBase
      implements RegelManuellServiceInterface<GetDataResponse, PatchErsattningRequest>
{
   @Inject
   RtfMapper mapper;

   @Inject
   FolkbokfordAdapter folkbokfordAdapter;

   @Inject
   ArbetsgivareAdapter arbetsgivareAdapter;

   @Inject
   HandlaggningAdapter handlaggningAdapter;

   @Inject
   ManuellRegelCommonDataStorageService dataStorage;

   @Override
   public GetDataResponse readData(Handlaggning handlaggning)
   {
      var indvidyrkandeRoll = handlaggning.yrkande().individYrkandeRoller().getFirst();

      var folkbokfordRequest = ImmutableFolkbokfordRequest.builder()
            .personnummer(indvidyrkandeRoll.individ().varde())
            .build();
      var folkbokfordResponse = folkbokfordAdapter.getFolkbokfordInfo(folkbokfordRequest);

      var arbetsgivareRequest = ImmutableArbetsgivareRequest.builder()
            .personnummer(indvidyrkandeRoll.individ().varde())
            .build();
      var arbetsgivareResponse = arbetsgivareAdapter.getArbetsgivareInfo(arbetsgivareRequest);

      return mapper.toGetDataResponse(handlaggning, arbetsgivareResponse, folkbokfordResponse, objectMapper);
   }

   @SuppressWarnings("DataFlowIssue")
   @Override
   public HandlaggningUpdate updateData(Handlaggning handlaggning, PatchErsattningRequest request)
   {
      var updatedErsattningar = request.getErsattningar().stream()
            .map(e -> createUpdatedProduceratResultat(handlaggning, e)).toList();

      var updatedYrkande = RegelUtils.createYrkandeWithUpdatedProduceradeResultat(handlaggning.yrkande(), updatedErsattningar);

      var commonData = dataStorage.getManuellRegelCommonData(handlaggning.id());

      return ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .version(handlaggning.version())
            .yrkande(updatedYrkande)
            .processInstansId(handlaggning.processInstansId())
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .uppgift(commonData.uppgift())
            .build();
   }

   @Override
   public void done(UUID handlaggningId)
   {
      sendRegelResponse(handlaggningId, Utfall.JA);
   }

   private ErsattningData getErsattningData(se.fk.rimfrost.framework.handlaggning.model.ProduceratResultat produceratResultat)
   {
      try
      {
         return objectMapper.readValue(produceratResultat.data(), ErsattningData.class);
      }
      catch (JsonProcessingException e)
      {
         throw new InternalError("Error parsing producerat resultat to ersattning: " + produceratResultat.data(), e);
      }
   }

   private ProduceratResultat createUpdatedProduceratResultat(Handlaggning handlaggning, UpdateErsattning updateErsattning)
   {
      var produceratResultat = handlaggning.yrkande().produceradeResultat().stream()
            .filter(pr -> pr.id().equals(updateErsattning.getErsattningId())).findFirst().orElseThrow();

      var ersattning = getErsattningData(produceratResultat);
      ersattning.setBeslutsutfall(updateErsattning.getBeslutsutfall());

      return ImmutableProduceratResultat.builder()
            .from(produceratResultat)
            .version(produceratResultat.version() + 1)
            .avslagsanledning(updateErsattning.getAvslagsanledning())
            .data(RegelUtils.createProduceratResultatData(ersattning, objectMapper))
            .build();
   }
}
