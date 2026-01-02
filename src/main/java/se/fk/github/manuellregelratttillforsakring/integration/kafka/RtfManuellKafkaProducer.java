package se.fk.github.manuellregelratttillforsakring.integration.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;
import se.fk.github.manuellregelratttillforsakring.integration.kafka.dto.RtfManuellResponseRequest;
import se.fk.rimfrost.OperativtUppgiftslagerRequestMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.regel.rtf.manuell.RtfManuellResponseMessagePayload;
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

   @Inject
   @Channel("rtf-manuell-responses")
   @OnOverflow(value = OnOverflow.Strategy.BUFFER, bufferSize = 1024)
   Emitter<RtfManuellResponseMessagePayload> rtfManuellResponseEmitter;

   public void sendOulRequest(UUID kundbehovsflodeId)
   {
      var request = new OperativtUppgiftslagerRequestMessage();
      request.setRegeltyp("rtf-manuell");
      request.setKundbehovsflodeId(kundbehovsflodeId.toString());
      oulRequestEmitter.send(request);
   }

   public void sendOulStatusUpdate(UUID uppgiftId, Status status)
   {
      var message = new OperativtUppgiftslagerStatusMessage();
      message.setUppgiftId(uppgiftId.toString());
      message.setStatus(status);
      oulStatusEmitter.send(message);
   }

   public void sendRtfManuellResponse(RtfManuellResponseRequest rtfResponseRequest)
   {
      var response = mapper.toRtfManuellResponse(rtfResponseRequest);
      rtfManuellResponseEmitter.send(response);
   }
}
