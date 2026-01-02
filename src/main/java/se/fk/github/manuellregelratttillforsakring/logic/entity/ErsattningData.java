package se.fk.github.manuellregelratttillforsakring.logic.entity;

import java.util.UUID;
import org.immutables.value.Value;
import jakarta.annotation.Nullable;
import se.fk.github.manuellregelratttillforsakring.logic.dto.Beslutsutfall;

@Value.Immutable
public interface ErsattningData
{

   UUID id();

   @Nullable
   Beslutsutfall beslutsutfall();

   @Nullable
   String avslagsanledning();

}
