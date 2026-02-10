package se.fk.github.manuellregelratttillforsakring.logic;

import java.util.ArrayList;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableErsattning;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableGetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse.Ersattning;
import se.fk.rimfrost.framework.regel.logic.entity.RegelData;
import se.fk.rimfrost.framework.regel.logic.entity.ErsattningData;
import se.fk.rimfrost.framework.regel.integration.kundbehovsflode.dto.KundbehovsflodeResponse;

@ApplicationScoped
public class RtfMapper
{

   public GetRtfDataResponse toRtfResponse(KundbehovsflodeResponse kundbehovflodesResponse,
         FolkbokfordResponse folkbokfordResponse, ArbetsgivareResponse arbetsgivareResponse, RegelData regelData)
   {
      var ersattningsList = new ArrayList<Ersattning>();

      for (var kundbehovErsattning : kundbehovflodesResponse.ersattning())
      {
         ErsattningData rtfErsattning = regelData.ersattningar().stream()
               .filter(e -> e.id().equals(kundbehovErsattning.ersattningsId()))
               .findFirst()
               .orElseThrow(() -> new IllegalArgumentException("ErsattningData not found"));

         var ersattning = ImmutableErsattning.builder()
               .belopp(kundbehovErsattning.belopp())
               .berakningsgrund(kundbehovErsattning.berakningsgrund())
               .ersattningsId(kundbehovErsattning.ersattningsId())
               .ersattningsTyp(kundbehovErsattning.ersattningsTyp())
               .from(kundbehovErsattning.franOchMed())
               .tom(kundbehovErsattning.tillOchMed())
               .avslagsanledning(rtfErsattning.avslagsanledning())
               .omfattningsProcent(kundbehovErsattning.omfattningsProcent());

         if (rtfErsattning.beslutsutfall() != null)
         {
            ersattning.beslutsutfall(rtfErsattning.beslutsutfall());
         }

         ersattningsList.add(ersattning.build());
      }

      var builder = ImmutableGetRtfDataResponse.builder()
            .kundbehovsflodeId(kundbehovflodesResponse.kundbehovsflodeId())
            .ersattning(ersattningsList);

      if (folkbokfordResponse != null)
      {
         builder
               .fornamn(folkbokfordResponse.fornamn())
               .efternamn(folkbokfordResponse.efternamn())
               .kon(folkbokfordResponse.kon().toString());
      }

      if (arbetsgivareResponse != null)
      {
         builder
               .anstallningsdag(arbetsgivareResponse.anstallningsdag())
               .sistaAnstallningsdag(arbetsgivareResponse.sistaAnstallningsdag())
               .arbetstidProcent(arbetsgivareResponse.arbetstidProcent())
               .loneSumma(arbetsgivareResponse.loneSumma())
               .lonFrom(arbetsgivareResponse.lonFrom())
               .lonTom(arbetsgivareResponse.lonTom())
               .organisationsnamn(arbetsgivareResponse.organisationsnamn())
               .organisationsnummer(arbetsgivareResponse.organisationsnummer());
      }
      return builder.build();
   }
}
