package se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableFolkbokfordResponse.class)
@JsonDeserialize(as = ImmutableFolkbokfordResponse.class)
public interface FolkbokfordResponse
{
   String fornamn();

   String efternamn();

   Kon kon();

   public enum Kon
   {
      MAN, KVINNA
   }
}
