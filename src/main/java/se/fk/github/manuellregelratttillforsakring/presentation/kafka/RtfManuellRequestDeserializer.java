package se.fk.github.manuellregelratttillforsakring.presentation.kafka;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import se.fk.rimfrost.regel.rtf.manuell.RtfManuellRequestMessagePayload;

public class RtfManuellRequestDeserializer extends ObjectMapperDeserializer<RtfManuellRequestMessagePayload>
{
   public RtfManuellRequestDeserializer()
   {
      super(RtfManuellRequestMessagePayload.class);
   }
}
