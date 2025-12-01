package se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableErsattning;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeResponse;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.KundbehovsflodeResponse;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.UpdateKundbehovsflodeRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Ersattning;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.GetKundbehovsflodeResponse;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Kundbehov;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Kundbehovsflode;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.PutKundbehovsflodeRequest;

@ApplicationScoped
public class KundbehovsflodeMapper
{

   public KundbehovsflodeResponse toKundbehovsflodeResponse(GetKundbehovsflodeResponse apiResponse)
   {
      var responseBuilder = ImmutableKundbehovsflodeResponse.builder()
            .personnummer(apiResponse.getKundbehovsflode().getKundbehov().getKundbehovsroll().getFirst().getIndivid().getId())
            .kundbehovsflodeId(apiResponse.getKundbehovsflode().getId());

      for (var ersattning : apiResponse.getKundbehovsflode().getKundbehov().getErsattning())
      {
         responseBuilder.addErsattning(ImmutableErsattning.builder()
               .belopp(ersattning.getBelopp())
               .berakningsgrund(ersattning.getBerakningsgrund())
               .ersattningsId(ersattning.getErsattningId())
               .ersattningsTyp(ersattning.getErsattningstyp())
               .from(ersattning.getFrom())
               .tom(ersattning.getTom())
               .omfattningsProcent(ersattning.getOmfattningProcent())
               .build());
      }
      return responseBuilder.build();
   }

   public PutKundbehovsflodeRequest toApiRequest(UpdateKundbehovsflodeRequest request)
   {
      var putRequest = new PutKundbehovsflodeRequest();
      var kundbehovflode = new Kundbehovsflode();
      kundbehovflode.setId(request.kundbehovsflodeId());
      putRequest.setKundbehovsflode(kundbehovflode);

      var kundbehov = new Kundbehov();
      for (var ersattning : request.ersattningar())
      {
         var ersattningsItem = new Ersattning();
         ersattningsItem.setErsattningId(ersattning.id());
         ersattningsItem.beslutsutfall(ersattning.beslutsutfall());
         ersattningsItem.avslagsanledning(ersattning.avslagsanledning());
         kundbehov.addErsattningItem(ersattningsItem);
      }

      return putRequest;
   }

}
