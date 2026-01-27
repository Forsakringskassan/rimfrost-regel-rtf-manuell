package se.fk.github.manuellregelratttillforsakring.presentation.rest;

import java.util.UUID;
import jakarta.ws.rs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import se.fk.github.manuellregelratttillforsakring.logic.RtfService;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableGetRtfDataRequest;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.RtfManuellControllerApi;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.common.jaxrsspec.controllers.generatedsource.RtfDoneControllerApi;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;

@Produces("application/json")
@Consumes("application/json")
@ApplicationScoped
@Path("/regel/rtf-manuell")
public class RtfManuellController implements RtfManuellControllerApi, RtfDoneControllerApi
{

   private static final Logger LOGGER = LoggerFactory.getLogger(RtfManuellController.class);

   @Inject
   RtfService rtfService;

   @Inject
   RtfManuellRestMapper mapper;

   @GET
   @Path("/{kundbehovsflodeId}")
   @Override
   public GetDataResponse getData(
         @PathParam("kundbehovsflodeId") UUID kundbehovsflodeId)
   {

      try
      {
         var request = ImmutableGetRtfDataRequest.builder()
               .kundbehovsflodeId(kundbehovsflodeId)
               .build();
         var response = rtfService.getData(request);
         return mapper.toGetDataResponse(response);
      }
      catch (JsonProcessingException e)
      {
         throw new InternalServerErrorException("Failed to process request");
      }
   }

   @PATCH
   @Path("/{kundbehovsflodeId}/ersattning/{ersattningId}")
   @Override
   public void updateErsattning(
         @PathParam("kundbehovsflodeId") UUID kundbehovsflodeId,
         @PathParam("ersattningId") UUID ersattningId,
         @Valid @NotNull PatchErsattningRequest patchRequest)
   {

      var request = mapper.toUpdateErsattningDataRequest(kundbehovsflodeId, ersattningId, patchRequest);
      rtfService.updateErsattningData(request);
   }

   @POST
   @Path("/{kundbehovsflodeId}/done")
   @Override
   public void markDone(
         @PathParam("kundbehovsflodeId") UUID kundbehovsflodeId)
   {
      rtfService.setUppgiftDone(kundbehovsflodeId);
   }
}
