package se.fk.github.manuellregelratttillforsakring.logic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.ArbetsgivareAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ImmutableArbetsgivareRequest;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.FolkbokfordAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.ImmutableFolkbokfordRequest;
import se.fk.github.manuellregelratttillforsakring.integration.kafka.RtfManuellKafkaProducer;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.KundbehovsflodeAdapter;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.CreateRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse;

@ApplicationScoped
public class RtfService
{

   @Inject
   RtfManuellKafkaProducer kafkaProducer;

   @Inject
   RtfMapper mapper;

   @Inject
   FolkbokfordAdapter folkbokfordAdapter;

   @Inject
   ArbetsgivareAdapter arbetsgivareAdapter;

   @Inject
   KundbehovsflodeAdapter kundbehovsflodeAdapter;

   public GetRtfDataResponse getData(GetRtfDataRequest request)
   {
      var kundbehovsflodeRequest = ImmutableKundbehovsflodeRequest.builder().kundbehovsflodeId(request.kundbehovsflodeId())
            .build();
      var kundbehovflodesResponse = kundbehovsflodeAdapter.getKundbehovsflodeInfo(kundbehovsflodeRequest);

      var folkbokfordRequest = ImmutableFolkbokfordRequest.builder().personnummer(kundbehovflodesResponse.personnummer()).build();
      var folkbokfordResponse = folkbokfordAdapter.getFolkbokfordInfo(folkbokfordRequest);

      var arbetsgivareRequest = ImmutableArbetsgivareRequest.builder().personnummer(kundbehovflodesResponse.personnummer())
            .build();
      var arbetsgivareResponse = arbetsgivareAdapter.getArbetsgivareInfo(arbetsgivareRequest);

      return mapper.toRtfResponse(kundbehovflodesResponse, folkbokfordResponse, arbetsgivareResponse);
   }

   public void createRtfData(CreateRtfDataRequest request)
   {
      kafkaProducer.sendOulRequest(request.kundbehovsflodeId());
   }

}
