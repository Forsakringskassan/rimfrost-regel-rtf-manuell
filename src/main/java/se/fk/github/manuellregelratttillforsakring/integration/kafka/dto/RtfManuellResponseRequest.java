package se.fk.github.manuellregelratttillforsakring.integration.kafka.dto;

import java.util.UUID;

import org.immutables.value.Value;

@Value.Immutable
public interface RtfManuellResponseRequest
{

   UUID id();

   UUID kundbehovsflodeId();

   String kogitorootprocid();

   UUID kogitorootprociid();

   UUID kogitoparentprociid();

   String kogitoprocid();

   UUID kogitoprocinstanceid();

   String kogitoprocist();

   String kogitoprocversion();

   boolean rattTillForsakring();

}
