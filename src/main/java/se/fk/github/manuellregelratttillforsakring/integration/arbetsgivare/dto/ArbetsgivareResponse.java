package se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto;

import java.time.LocalDate;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import jakarta.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableArbetsgivareResponse.class)
@JsonDeserialize(as = ImmutableArbetsgivareResponse.class)
public interface ArbetsgivareResponse
{

   String organisationsnummer();

   String organisationsnamn();

   int arbetstidProcent();

   int loneSumma();

   LocalDate anstallningsdag();

   @Nullable
   LocalDate sistaAnstallningsdag();

   LocalDate lonFrom();

   @Nullable
   LocalDate lonTom();
}
