package se.fk.github.manuellregelratttillforsakring.logic.dto;

import java.util.UUID;

import org.immutables.value.Value;

import jakarta.annotation.Nullable;

import se.fk.rimfrost.framework.regel.logic.dto.UppgiftStatus;

@Value.Immutable
public interface UpdateStatusRequest
{
   UUID kundbehovsflodeId();

   UUID uppgiftId();

   @Nullable
   UUID utforarId();

   UppgiftStatus uppgiftStatus();

}
