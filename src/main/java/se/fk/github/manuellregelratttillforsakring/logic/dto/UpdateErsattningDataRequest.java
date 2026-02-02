package se.fk.github.manuellregelratttillforsakring.logic.dto;

import java.util.UUID;

import org.immutables.value.Value;

import jakarta.annotation.Nullable;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;

@Value.Immutable
public interface UpdateErsattningDataRequest
{

   UUID kundbehovsflodeId();

   UUID ersattningId();

   Beslutsutfall beslutsutfall();

   @Nullable
   String avslagsanledning();

}
