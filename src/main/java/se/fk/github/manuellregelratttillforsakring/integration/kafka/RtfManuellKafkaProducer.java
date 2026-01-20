package se.fk.github.manuellregelratttillforsakring.integration.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.UUID;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import se.fk.github.manuellregelratttillforsakring.integration.kafka.dto.OulMessageRequest;
import se.fk.rimfrost.OperativtUppgiftslagerRequestMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.Status;

@ApplicationScoped
public class RtfManuellKafkaProducer
{
   @Inject
   RtfManuellKafkaMapper mapper;

   @Inject
   @Channel("operativt-uppgiftslager-requests")
   @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 1024)
   Emitter<OperativtUppgiftslagerRequestMessage> oulRequestEmitter;

   @Inject
   @Channel("operativt-uppgiftslager-status-control")
   @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 1024)
   Emitter<OperativtUppgiftslagerStatusMessage> oulStatusEmitter;

   public void sendOulRequest(OulMessageRequest messageRequest)
   {
      var request = mapper.toOulRequestMessage(messageRequest);
      oulRequestEmitter.send(request);
   }

   public void sendOulStatusUpdate(UUID uppgiftId, Status status)
   {
      var message = new OperativtUppgiftslagerStatusMessage();
      message.setUppgiftId(uppgiftId.toString());
      message.setStatus(status);
      oulStatusEmitter.send(message);
   }

}
