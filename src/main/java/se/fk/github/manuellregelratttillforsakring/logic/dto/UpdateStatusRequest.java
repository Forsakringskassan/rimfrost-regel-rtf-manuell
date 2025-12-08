package se.fk.github.manuellregelratttillforsakring.logic.dto;

import java.util.UUID;

import org.immutables.value.Value;

@Value.Immutable
public interface UpdateStatusRequest
{
   UUID kundbehovsflodeId();

   UUID uppgiftId();

   UppgiftStatus uppgiftStatus();

}
