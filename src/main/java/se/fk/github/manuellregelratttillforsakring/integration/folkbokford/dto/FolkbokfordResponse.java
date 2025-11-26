package se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto;

import org.immutables.value.Value;

@Value.Immutable
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
