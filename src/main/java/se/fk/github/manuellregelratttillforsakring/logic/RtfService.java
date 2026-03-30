package se.fk.github.manuellregelratttillforsakring.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import se.fk.github.manuellregelratttillforsakring.logic.entity.Ersattning;
import se.fk.github.manuellregelratttillforsakring.storage.ManuellRegelCommonDataStorageService;
import se.fk.rimfrost.framework.arbetsgivare.adapter.ArbetsgivareAdapter;
import se.fk.rimfrost.framework.arbetsgivare.adapter.dto.ImmutableArbetsgivareRequest;
import se.fk.rimfrost.framework.folkbokford.adapter.FolkbokfordAdapter;
import se.fk.rimfrost.framework.folkbokford.adapter.dto.ImmutableFolkbokfordRequest;
import se.fk.rimfrost.framework.individ.adapter.IndividAdapter;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableProduceratResultat;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableYrkande;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceBase;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.ProduceratResultat;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;

@ApplicationScoped
@Startup
public class RtfService extends RegelManuellServiceBase
      implements RegelManuellServiceInterface<GetDataResponse, PatchErsattningRequest>
{
   @Inject
   RtfMapper mapper;

   @Inject
   IndividAdapter individAdapter;

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

      var individ = individAdapter.getIndivid(indvidyrkandeRoll.individId());

      var folkbokfordRequest = ImmutableFolkbokfordRequest.builder()
            .personnummer(individ.varde())
            .build();
      var folkbokfordResponse = folkbokfordAdapter.getFolkbokfordInfo(folkbokfordRequest);

      var arbetsgivareRequest = ImmutableArbetsgivareRequest.builder()
            .personnummer(individ.varde())
            .build();
      var arbetsgivareResponse = arbetsgivareAdapter.getArbetsgivareInfo(arbetsgivareRequest);

      return mapper.toGetDataResponse(handlaggning, arbetsgivareResponse, folkbokfordResponse, objectMapper);
   }

   @Override
   public HandlaggningUpdate updateData(Handlaggning handlaggning, PatchErsattningRequest request)
   {

      var updatedErsattningar = new ArrayList<se.fk.rimfrost.framework.handlaggning.model.ProduceratResultat>();
      for (var patchErsattning : request.getErsattningar())
      {
         var produceratResultat = handlaggning.yrkande().produceradeResultat().stream()
               .filter(e -> e.id() == patchErsattning.getErsattningId()).findFirst().orElseThrow();
         var ersattning = getErsattning(produceratResultat);
         ersattning.setBeslutsutfall(patchErsattning.getBeslutsutfall());

         try
         {
            var jsonData = objectMapper.writeValueAsString(ersattning);

            var updatedErsattning = ImmutableProduceratResultat.builder()
                  .from(produceratResultat)
                  .version(produceratResultat.version() + 1)
                  .avslagsanledning(patchErsattning.getAvslagsanledning())
                  .data(jsonData)
                  .build();
            updatedErsattningar.add(updatedErsattning);
         }
         catch (JsonProcessingException e)
         {
            throw new InternalError("Error parsing to json: " + ersattning.toString(), e);
         }
      }

      var commonData = dataStorage.getManuellRegelCommonData(handlaggning.id());

      var updatedYrkande = ImmutableYrkande.builder()
            .from(handlaggning.yrkande())
            .addAllProduceradeResultat(updatedErsattningar)
            .build();

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

   private Ersattning getErsattning(se.fk.rimfrost.framework.handlaggning.model.ProduceratResultat produceratResultat)
   {
      try
      {
         return objectMapper.readValue(produceratResultat.data(), Ersattning.class);
      }
      catch (JsonProcessingException e)
      {
         throw new InternalError("Error parsing producerat resultat to ersattning: " + produceratResultat.data(), e);
      }
   }

}
