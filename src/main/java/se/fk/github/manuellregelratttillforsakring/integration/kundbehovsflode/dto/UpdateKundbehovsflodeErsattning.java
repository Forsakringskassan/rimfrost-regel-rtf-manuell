package se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto;

import java.util.UUID;

import org.immutables.value.Value;

import jakarta.annotation.Nullable;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;

@Value.Immutable
public interface UpdateKundbehovsflodeErsattning
{

   UUID id();

   @Nullable
   Beslutsutfall beslutsutfall();

   @Nullable
   String avslagsanledning();

}
