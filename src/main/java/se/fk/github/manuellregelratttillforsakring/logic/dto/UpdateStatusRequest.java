package se.fk.github.manuellregelratttillforsakring.logic.dto;

import java.util.UUID;

import org.immutables.value.Value;

import jakarta.annotation.Nullable;

@Value.Immutable
public interface UpdateStatusRequest
{
   UUID kundbehovsflodeId();

   UUID uppgiftId();

   @Nullable
   UUID utforarId();

   UppgiftStatus uppgiftStatus();

}
