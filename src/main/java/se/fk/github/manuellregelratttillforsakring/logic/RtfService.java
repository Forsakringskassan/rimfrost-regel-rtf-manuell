package se.fk.github.manuellregelratttillforsakring.logic;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.ws.rs.core.Response;
import java.util.NoSuchElementException;
import java.util.UUID;
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
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.SakfragaStallningstagandeContainer;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.RegelUtils;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceBase;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
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
   OulUppgiftService oulUppgiftService;

   @Override
   public GetDataResponse readData(Handlaggning handlaggning)
   {
      var rollIYrkande = handlaggning.yrkande().rollerIYrkande().getFirst();

      var folkbokfordRequest = ImmutableFolkbokfordRequest.builder()
            .personnummer(rollIYrkande.individ().varde())
            .build();
      FolkbokfordResponse folkbokfordResponse = null;
      try
      {
         folkbokfordResponse = folkbokfordAdapter.getFolkbokfordInfo(folkbokfordRequest);
      }
      catch (FolkbokfordException e)
      {
         if (e.getErrorType() != FolkbokfordException.ErrorType.NOT_FOUND)
         {
            LOGGER.error("Folkbokford adapter failed for personnummer {} with {}: {}", folkbokfordRequest.personnummer(), e.getErrorType(), e.getMessage());
            throw switch (e.getErrorType())
            {
               case BAD_REQUEST -> new RegelManuellException(Response.Status.BAD_REQUEST, e.getMessage());
               case SERVICE_UNAVAILABLE -> new RegelManuellException(Response.Status.SERVICE_UNAVAILABLE, e.getMessage());
               default -> new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
            };
         }
      }

      var arbetsgivareRequest = ImmutableArbetsgivareRequest.builder()
            .personnummer(rollIYrkande.individ().varde())
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
            case NOT_FOUND -> new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
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
            .map(e -> createUpdatedErsattning(handlaggning, e)).toList();

      var updatedYrkande = RegelUtils.createYrkandeWithUpdatedSakfragorStallningstaganden(handlaggning.yrkande(),
            updatedErsattningar);

      var correlation = oulUppgiftService.getCorrelationData(handlaggning.id());

      var updatedHandlaggning = ImmutableHandlaggning.builder()
            .from(handlaggning)
            .version(handlaggning.version() + 1)
            .yrkande(updatedYrkande)
            .build();

      return ImmutableHandlaggningUpdate.builder()
            .handlaggning(updatedHandlaggning)
            .uppgift(correlation.uppgift())
            .build();
   }

   @Override
   public void done(UUID handlaggningId)
   {
      sendRegelResponse(handlaggningId, Utfall.JA);
   }

   private SakfragaStallningstagandeContainer createUpdatedErsattning(Handlaggning handlaggning,
         UpdateErsattning updateErsattning)
   {
      SakfragaStallningstagandeContainer sakfragaStallningstagande;
      try
      {
         sakfragaStallningstagande = handlaggning.yrkande().sakfragorStallningstaganden().stream()
               .filter(s -> s.objektTypId().equalsIgnoreCase(RtfMapper.ERSATTNING_OBJEKT_TYP_ID))
               .filter(s -> s.id().equals(updateErsattning.getErsattningId())).findFirst().orElseThrow();
      }
      catch (NoSuchElementException e)
      {
         LOGGER.error("Failed to locate SakfragaStallningstagande with id {}", updateErsattning.getErsattningId(), e);

         throw new RegelManuellException(Response.Status.BAD_REQUEST,
               "Failed to locate ersattning with id " + updateErsattning.getErsattningId());
      }

      var ersattning = ErsattningData.fromJson(sakfragaStallningstagande.data(), objectMapper);

      try
      {
         ersattning.setBeslutsutfall(toBeslutsutfall(updateErsattning.getBeslutsutfall()));
      }
      catch (IllegalStateException e)
      {
         LOGGER.error("Failed to parse beslutsutfall {} for ersattningId {}", updateErsattning.getBeslutsutfall(),
               updateErsattning.getErsattningId(), e);
         throw new RegelManuellException(Response.Status.BAD_REQUEST, "Unsupported beslutsutfall value "
               + updateErsattning.getBeslutsutfall() + " provided for ersattning with id " + updateErsattning.getErsattningId());
      }

      ersattning.setAvslagsanledning(updateErsattning.getAvslagsanledning());
      ersattning.setVersion(ersattning.getVersion() + 1);

      try
      {
         return RegelUtils.createSakfragaStallningstagande(sakfragaStallningstagande.id(),
               sakfragaStallningstagande.objektTypId(), ersattning, objectMapper);
      }
      catch (InternalError e)
      {
         LOGGER.error("Failed to serialize updated ersattning", e);
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, "Failed to create updated ersattning");
      }
   }

   private Beslutsutfall toBeslutsutfall(se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Beslutsutfall beslutsutfall)
   {
      if (beslutsutfall == null)
      {
         throw new IllegalStateException("Null value not supported for beslutsutfall");
      }

      return switch (beslutsutfall) {
         case JA -> Beslutsutfall.JA;
         case NEJ ->  Beslutsutfall.NEJ;
         case FU ->  Beslutsutfall.FU;
         default -> throw new IllegalStateException("Unexpected value: " + beslutsutfall);
      };
   }
}
