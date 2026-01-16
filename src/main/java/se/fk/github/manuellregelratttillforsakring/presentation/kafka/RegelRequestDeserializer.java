package se.fk.github.manuellregelratttillforsakring.presentation.kafka;

import io.quarkus.kafka.client.serialization.ObjectMapperDeserializer;
import se.fk.rimfrost.regel.common.RegelRequestMessagePayload;

public class RegelRequestDeserializer extends ObjectMapperDeserializer<RegelRequestMessagePayload>
{
   public RegelRequestDeserializer()
   {
      super(RegelRequestMessagePayload.class);
   }
}
