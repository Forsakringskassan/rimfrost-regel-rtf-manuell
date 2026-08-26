package se.fk.github.manuellregelratttillforsakring.presentation.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;
import se.fk.rimfrost.framework.regel.presentation.rest.KompletteringController;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;

/**
 * Exposes the RTF Manuell komplettering endpoints (GET, PATCH, POST done).
 *
 * <p>All endpoint logic is provided by {@link KompletteringController}. CDI resolves
 * {@link se.fk.github.manuellregelratttillforsakring.logic.RtfKompletteringSvarService} and
 * {@link se.fk.github.manuellregelratttillforsakring.logic.RtfService} automatically.
 */
@Path("/regel/rtf-manuell")
@ApplicationScoped
public class RtfKompletteringController
      extends KompletteringController<RtfKompletteringData, RtfKompletteringData>
{
}
