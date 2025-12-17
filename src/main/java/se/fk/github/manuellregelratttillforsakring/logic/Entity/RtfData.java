package se.fk.github.manuellregelratttillforsakring.logic.entity;

import java.util.List;
import java.util.UUID;

import org.immutables.value.Value;

import jakarta.annotation.Nullable;

@Value.Immutable
public interface RtfData
{

   UUID kundbehovsflodeId();

   UUID cloudeventId();

   @Nullable
   UUID uppgiftId();

   List<ErsattningData> ersattningar();

   List<Underlag> underlag();

}
