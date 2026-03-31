package se.fk.github.manuellregelratttillforsakring.presentation.rest;

import jakarta.ws.rs.*;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.manuell.presentation.rest.RegelManuellController;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;

@Produces("application/json")
@Consumes("application/json")
@ApplicationScoped
@Path("/regel/rtf-manuell")
public class RtfManuellController extends RegelManuellController<GetDataResponse, PatchErsattningRequest>
{
}
