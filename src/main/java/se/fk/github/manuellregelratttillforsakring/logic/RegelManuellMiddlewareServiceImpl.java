package se.fk.github.manuellregelratttillforsakring.logic;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellMiddlewareService;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;

@ApplicationScoped
public class RegelManuellMiddlewareServiceImpl extends RegelManuellMiddlewareService<GetDataResponse, PatchErsattningRequest>
{

}
