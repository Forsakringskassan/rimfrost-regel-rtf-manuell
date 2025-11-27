package se.fk.github.manuellregelratttillforsakring.presentation.rest;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableUpdateErsattningDataRequest;
import se.fk.github.manuellregelratttillforsakring.logic.dto.UpdateErsattningDataRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Anstallning;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Ersattning;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Kund;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Lon;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.PatchDataRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Kund.KonEnum;

@ApplicationScoped
public class RtfManuellRestMapper
{

   public GetDataResponse toGetDataResponse(GetRtfDataResponse rtfResponse)
   {
      var lon = new Lon();
      lon.setFrom(rtfResponse.lonFrom());
      lon.setTom(rtfResponse.lonTom());
      lon.setLonesumma(rtfResponse.loneSumma());

      var anstallning = new Anstallning();
      anstallning.setAnstallningsdag(rtfResponse.anstallningsdag());
      anstallning.setArbetstidProcent(rtfResponse.arbetstidProcent());
      anstallning.setSistaAnstallningsdag(rtfResponse.sistaAnstallningsdag());
      anstallning.setOrganisationsnamn(rtfResponse.organisationsnamn());
      anstallning.setOrganisationsnummer(rtfResponse.organistaionsnummer());
      anstallning.setLon(lon);

      var kund = new Kund();
      kund.setFornamn(rtfResponse.fornamn());
      kund.setEfternamn(rtfResponse.efternamn());
      kund.setAnstallning(anstallning);
      kund.setKon(mapKonEnum(rtfResponse.kon()));

      var response = new GetDataResponse();
      response.setKund(kund);
      response.kundbehovsflodeId(rtfResponse.kundbehovsflodeId());

      for (var rtfErsattning : rtfResponse.ersattning())
      {
         var ersattning = new Ersattning();
         ersattning.setBelopp(rtfErsattning.belopp());
         ersattning.setBerakningsgrund(rtfErsattning.berakningsgrund());
         ersattning.setErsattningId(rtfErsattning.ersattningsId());
         ersattning.setErsattningstyp(rtfErsattning.ersattningsTyp());
         ersattning.setOmfattningProcent(rtfErsattning.omfattningsProcent());
         ersattning.setFrom(rtfErsattning.from());
         ersattning.setTom(rtfErsattning.tom());
         ersattning.setAvslagsanledning(rtfErsattning.avslagsanledning());
         if (rtfErsattning.beslutsutfall() != null)
         {
            ersattning.setBeslutsutfall(mapBeslutsutfall(rtfErsattning.beslutsutfall()));
         }
         response.addErsattningItem(ersattning);
      }
      return response;
   }

   public UpdateErsattningDataRequest toUpdateErsattningDataRequest(UUID kundbehovsflodeId, PatchDataRequest patchRequest)
   {
      return ImmutableUpdateErsattningDataRequest.builder()
            .kundbehovsflodeId(kundbehovsflodeId)
            .beslutsutfall(mapBeslutsutfall(patchRequest.getBeslutsutfall()))
            .ersattningId(patchRequest.getErsattningId())
            .avslagsanledning(patchRequest.getAvslagsanledning())
            .build();
   }

   private Beslutsutfall mapBeslutsutfall(se.fk.github.manuellregelratttillforsakring.logic.dto.Beslutsutfall beslututfall)
   {
      switch (beslututfall)
      {
         case JA:
            return Beslutsutfall.JA;
         case NEJ:
            return Beslutsutfall.NEJ;
         case FU:
            return Beslutsutfall.FU;
         default:
            return null;
      }
   }

   private se.fk.github.manuellregelratttillforsakring.logic.dto.Beslutsutfall mapBeslutsutfall(Beslutsutfall beslututfall)
   {
      switch (beslututfall)
      {
         case JA:
            return se.fk.github.manuellregelratttillforsakring.logic.dto.Beslutsutfall.JA;
         case NEJ:
            return se.fk.github.manuellregelratttillforsakring.logic.dto.Beslutsutfall.NEJ;
         case FU:
            return se.fk.github.manuellregelratttillforsakring.logic.dto.Beslutsutfall.FU;
         default:
            return null;
      }
   }

   private KonEnum mapKonEnum(String kon)
   {
      switch (kon)
      {
         case "Man":
            return KonEnum.MAN;
         default:
         case "KVINNA":
            return KonEnum.KVINNA;
      }
   }

}
