package se.fk.github.manuellregelratttillforsakring.logic;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import se.fk.github.manuellregelratttillforsakring.storage.ManuellRegelCommonDataStorageService;
import se.fk.rimfrost.adapter.arbetsgivare.ArbetsgivareAdapter;
import se.fk.rimfrost.adapter.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.rimfrost.adapter.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.rimfrost.adapter.arbetsgivare.exception.ArbetsgivareException;
import se.fk.rimfrost.adapter.folkbokford.FolkbokfordAdapter;
import se.fk.rimfrost.adapter.folkbokford.FolkbokfordException;
import se.fk.rimfrost.adapter.folkbokford.dto.FolkbokfordResponse;
import se.fk.rimfrost.adapter.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.rimfrost.ersattningdata.ErsattningData;
import se.fk.rimfrost.ersattningdata.Beslutsutfall;
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
   private static final Logger LOGGER = LoggerFactory.getLogger(RtfService.class);

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
      FolkbokfordResponse folkbokfordResponse;
      try
      {
         folkbokfordResponse = folkbokfordAdapter.getFolkbokfordInfo(folkbokfordRequest);
      }
      catch (FolkbokfordException e)
      {
         LOGGER.error("Folkbokford adapter failed for personnummer {} with {}: {}", folkbokfordRequest.personnummer(), e.getErrorType(), e.getMessage());
         throw switch (e.getErrorType())
         {
            case NOT_FOUND -> new RegelManuellException(Response.Status.NOT_FOUND, e.getMessage());
            case BAD_REQUEST -> new RegelManuellException(Response.Status.BAD_REQUEST, e.getMessage());
            case SERVICE_UNAVAILABLE -> new RegelManuellException(Response.Status.SERVICE_UNAVAILABLE, e.getMessage());
            case UNEXPECTED_ERROR -> new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
         };
      }

      var arbetsgivareRequest = ImmutableArbetsgivareRequest.builder()
            .personnummer(indvidyrkandeRoll.individ().varde())
            .build();
      ArbetsgivareResponse arbetsgivareResponse;
      try
      {
         arbetsgivareResponse = arbetsgivareAdapter.getArbetsgivareInfo(arbetsgivareRequest);
      }
      catch (ArbetsgivareException e)
      {
         LOGGER.error("Arbetsgivare adapter failed for personnummer {} with {}: {}", arbetsgivareRequest.personnummer(), e.getErrorCode(), e.getMessage());
         throw switch (e.getErrorCode())
         {
            case NOT_FOUND -> new RegelManuellException(Response.Status.NOT_FOUND, e.getMessage());
            case BAD_REQUEST -> new RegelManuellException(Response.Status.BAD_REQUEST, e.getMessage());
            case SERVICE_UNAVAILABLE -> new RegelManuellException(Response.Status.SERVICE_UNAVAILABLE, e.getMessage());
            case UNEXPECTED_ERROR -> new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
         };
      }

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

   private ProduceratResultat createUpdatedProduceratResultat(Handlaggning handlaggning, UpdateErsattning updateErsattning)
   {
      var produceratResultat = handlaggning.yrkande().produceradeResultat().stream()
            .filter(pr -> pr.id().equals(updateErsattning.getErsattningId())).findFirst().orElseThrow();

      var ersattning = ErsattningData.fromJson(produceratResultat.data(), objectMapper);
      ersattning.setBeslutsutfall(toBeslutsutfall(updateErsattning.getBeslutsutfall()));

      return ImmutableProduceratResultat.builder()
            .from(produceratResultat)
            .version(produceratResultat.version() + 1)
            .avslagsanledning(updateErsattning.getAvslagsanledning())
            .data(RegelUtils.createProduceratResultatData(ersattning, objectMapper))
            .build();
   }

   private Beslutsutfall toBeslutsutfall(se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Beslutsutfall beslutsutfall)
   {
      return switch (beslutsutfall) {
         case JA -> Beslutsutfall.JA;
         case NEJ ->  Beslutsutfall.NEJ;
         case FU ->  Beslutsutfall.FU;
         default -> throw new IllegalStateException("Unexpected value: " + beslutsutfall);
      };
   }
}
