package se.fk.github.manuellregelratttillforsakring.logic;

import java.util.ArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.KundbehovsflodeResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableErsattning;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableGetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse.Ersattning;
import se.fk.github.manuellregelratttillforsakring.logic.entity.RtfData;

@ApplicationScoped
public class RtfMapper
{

   public GetRtfDataResponse toRtfResponse(KundbehovsflodeResponse kundbehovflodesResponse,
         FolkbokfordResponse folkbokfordResponse, ArbetsgivareResponse arbetsgivareResponse, RtfData rtfData)
   {
      var ersattningsList = new ArrayList<Ersattning>();

      for (var kundbehovErsattning : kundbehovflodesResponse.ersattning())
      {
         var rtfErsattning = rtfData.ersattningar().stream().filter(e -> e.id() == kundbehovErsattning.ersattningsId())
               .findFirst()
               .orElseThrow(() -> new IllegalArgumentException("ErsattningData not found"));

         var ersattning = ImmutableErsattning.builder()
               .belopp(kundbehovErsattning.belopp())
               .berakningsgrund(kundbehovErsattning.berakningsgrund())
               .ersattningsId(kundbehovErsattning.ersattningsId())
               .ersattningsTyp(kundbehovErsattning.ersattningsTyp())
               .from(kundbehovErsattning.from())
               .tom(kundbehovErsattning.tom())
               .avslagsanledning(rtfErsattning.avslagsanledning())
               .omfattningsProcent(kundbehovErsattning.omfattningsProcent());

         if (rtfErsattning.beslutsutfall() != null)
         {
            ersattning.beslutsutfall(rtfErsattning.beslutsutfall());
         }

         ersattningsList.add(ersattning.build());
      }

      return ImmutableGetRtfDataResponse.builder()
            .kundbehovsflodeId(kundbehovflodesResponse.kundbehovsflodeId())
            .fornamn(folkbokfordResponse.fornamn())
            .efternamn(folkbokfordResponse.efternamn())
            .kon(folkbokfordResponse.kon().toString())
            .anstallningsdag(arbetsgivareResponse.anstallningsdag())
            .sistaAnstallningsdag(arbetsgivareResponse.sistaAnstallningsdag())
            .arbetstidProcent(arbetsgivareResponse.arbetstidProcent())
            .loneSumma(arbetsgivareResponse.loneSumma())
            .lonFrom(arbetsgivareResponse.lonFrom())
            .lonTom(arbetsgivareResponse.lonTom())
            .organisationsnamn(arbetsgivareResponse.organisationsnamn())
            .organistaionsnummer(arbetsgivareResponse.organisationsnummer())
            .ersattning(ersattningsList)
            .build();
   }

}
