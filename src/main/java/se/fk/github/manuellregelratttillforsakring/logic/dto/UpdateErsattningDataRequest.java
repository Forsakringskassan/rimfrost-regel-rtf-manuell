package se.fk.github.manuellregelratttillforsakring.logic.dto;

import java.util.UUID;

import org.immutables.value.Value;

import jakarta.annotation.Nullable;

@Value.Immutable
public interface UpdateErsattningDataRequest
{

   UUID kundbehovsflodeId();

   UUID ersattningId();

   Beslutsutfall beslutsutfall();

   @Nullable
   String avslagsanledning();

   boolean signernad();

}
