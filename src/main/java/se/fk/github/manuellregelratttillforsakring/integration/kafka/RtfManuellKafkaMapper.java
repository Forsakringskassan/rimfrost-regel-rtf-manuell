package se.fk.github.manuellregelratttillforsakring.integration.kafka;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.manuellregelratttillforsakring.integration.kafka.dto.RtfManuellResponseRequest;
import se.fk.rimfrost.regel.rtf.manuell.KogitoProcType;
import se.fk.rimfrost.regel.rtf.manuell.RattTillForsakring;
import se.fk.rimfrost.regel.rtf.manuell.RtfManuellResponseMessageData;
import se.fk.rimfrost.regel.rtf.manuell.RtfManuellResponseMessagePayload;
import se.fk.rimfrost.regel.rtf.manuell.SpecVersion;

@ApplicationScoped
public class RtfManuellKafkaMapper
{

   public RtfManuellResponseMessagePayload toRtfManuellResponse(RtfManuellResponseRequest request)
   {
      var data = new RtfManuellResponseMessageData();
      data.setKundbehovsflodeId(request.kundbehovsflodeId().toString());
      data.setRattTillForsakring(request.rattTillForsakring() ? RattTillForsakring.JA : RattTillForsakring.NEJ);

      var response = new RtfManuellResponseMessagePayload();
      response.setId(request.id().toString());
      response.setKogitorootprocid(request.kogitorootprocid());
      response.setKogitorootprociid(request.kogitorootprociid().toString());
      response.setKogitoparentprociid(request.kogitoparentprociid().toString());
      response.setKogitoprocid(request.kogitoprocid());
      response.setKogitoprocinstanceid(request.kogitoprocinstanceid().toString());
      response.setKogitoprocrefid(request.kogitoprocinstanceid().toString());
      response.setKogitoprocist(request.kogitoprocist());
      response.setKogitoprocversion(request.kogitoprocversion());
      response.setSpecversion(SpecVersion.NUMBER_1_DOT_0);
      response.setSource("/regel/rtf-manuell");
      response.setType("vah-rtf-manuell-responses");
      response.setKogitoproctype(KogitoProcType.BPMN);
      response.setData(data);

      return response;
   }
}
