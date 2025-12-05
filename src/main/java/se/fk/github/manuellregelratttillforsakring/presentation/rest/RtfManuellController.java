package se.fk.github.manuellregelratttillforsakring.presentation.rest;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Path;
import se.fk.github.manuellregelratttillforsakring.logic.RtfService;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableGetRtfDataRequest;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.RtfManuellControllerApi;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;

@ApplicationScoped
@Path("/regel/rtf-manuell/{kundbehovsflodeId}")
public class RtfManuellController implements RtfManuellControllerApi
{

   private static final Logger LOGGER = LoggerFactory.getLogger(RtfManuellController.class);

   @Inject
   RtfService rtfService;

   @Inject
   RtfManuellRestMapper mapper;

   @Override
   public GetDataResponse getData(UUID kundbehovsflodeId)
   {
      var request = ImmutableGetRtfDataRequest.builder().kundbehovsflodeId(kundbehovsflodeId).build();
      var response = rtfService.getData(request);
      return mapper.toGetDataResponse(response);
   }

   @Override
   public void updateData(UUID kundbehovsflodeId, @Valid @NotNull PatchDataRequest patchRequest)
   {
      LOGGER.info(
            "updateData received with patchrequest: " + patchRequest);
      var request = mapper.toUpdateErsattningDataRequest(kundbehovsflodeId, patchRequest);
      rtfService.updateErsattningData(request);
   }
}
