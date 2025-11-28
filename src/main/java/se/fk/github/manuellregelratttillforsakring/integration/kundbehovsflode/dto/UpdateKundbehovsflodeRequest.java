package se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto;

import java.util.UUID;

import org.immutables.value.Value;

@Value.Immutable
public interface UpdateKundbehovsflodeRequest
{
   UUID kundbehovsflodeId();
}
