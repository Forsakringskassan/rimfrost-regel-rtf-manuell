package se.fk.github.manuellregelratttillforsakring.logic.entity;

import java.util.List;
import java.util.UUID;

import org.immutables.value.Value;

@Value.Immutable
public interface RtfData
{

   UUID kundebehovsflodeId();

   UUID cloudeventId();

   UUID uppgiftId();

   List<ErsattningData> ersattningar();

}
