package se.fk.github.manuellregelratttillforsakring.logic.Entity;

import java.util.UUID;

import org.immutables.value.Value;

@Value.Immutable
public interface RtfData
{

   UUID kundebehovsflodeId();

   boolean rattTillForsakring();

}
