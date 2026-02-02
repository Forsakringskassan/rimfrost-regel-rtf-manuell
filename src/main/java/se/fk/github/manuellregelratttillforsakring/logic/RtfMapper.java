package se.fk.github.manuellregelratttillforsakring.logic;

import java.time.ZoneOffset;
import java.util.ArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeErsattning;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeLagrum;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeRegel;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeRequest;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeSpecifikation;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeUnderlag;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeUppgift;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.KundbehovsflodeResponse;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.UpdateKundbehovsflodeRequest;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.UpdateKundbehovsflodeUnderlag;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableErsattning;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableGetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse.Ersattning;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ErsattningData;
import se.fk.github.manuellregelratttillforsakring.logic.entity.RtfData;
import se.fk.rimfrost.framework.regel.logic.config.RegelConfig;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Ersattning.BeslutsutfallEnum;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Roll;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Verksamhetslogik;

@ApplicationScoped
public class RtfMapper
{

   public GetRtfDataResponse toRtfResponse(KundbehovsflodeResponse kundbehovflodesResponse,
         FolkbokfordResponse folkbokfordResponse, ArbetsgivareResponse arbetsgivareResponse, RtfData rtfData)
   {
      var ersattningsList = new ArrayList<Ersattning>();

      for (var kundbehovErsattning : kundbehovflodesResponse.ersattning())
      {
         ErsattningData rtfErsattning = rtfData.ersattningar().stream()
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

   public UpdateKundbehovsflodeRequest toUpdateKundbehovsflodeRequest(RtfData rtfData, RegelConfig regelConfig)
   {

      var lagrum = ImmutableUpdateKundbehovsflodeLagrum.builder()
            .id(regelConfig.getLagrum().getId())
            .version(regelConfig.getLagrum().getVersion())
            .forfattning(regelConfig.getLagrum().getForfattning())
            .giltigFrom(regelConfig.getLagrum().getGiltigFom().toInstant().atOffset(ZoneOffset.UTC))
            .kapitel(regelConfig.getLagrum().getKapitel())
            .paragraf(regelConfig.getLagrum().getParagraf())
            .stycke(regelConfig.getLagrum().getStycke())
            .punkt(regelConfig.getLagrum().getPunkt())
            .build();

      var regel = ImmutableUpdateKundbehovsflodeRegel.builder()
            .id(regelConfig.getRegel().getId())
            .beskrivning(regelConfig.getRegel().getBeskrivning())
            .namn(regelConfig.getRegel().getNamn())
            .version(regelConfig.getRegel().getVersion())
            .lagrum(lagrum)
            .build();

      var specifikation = ImmutableUpdateKundbehovsflodeSpecifikation.builder()
            .id(regelConfig.getSpecifikation().getId())
            .version(regelConfig.getSpecifikation().getVersion())
            .namn(regelConfig.getSpecifikation().getNamn())
            .uppgiftsbeskrivning(regelConfig.getSpecifikation().getUppgiftbeskrivning())
            .verksamhetslogik(Verksamhetslogik.fromString(regelConfig.getSpecifikation().getVerksamhetslogik()))
            .roll(Roll.fromString(regelConfig.getSpecifikation().getRoll()))
            .applikationsId(regelConfig.getSpecifikation().getApplikationsId())
            .applikationsversion(regelConfig.getSpecifikation().getApplikationsversion())
            .url(regelConfig.getUppgift().getPath())
            .regel(regel)
            .build();

      var uppgift = ImmutableUpdateKundbehovsflodeUppgift.builder()
            .id(rtfData.uppgiftId())
            .version(regelConfig.getUppgift().getVersion())
            .skapadTs(rtfData.skapadTs())
            .utfordTs(rtfData.utfordTs())
            .planeradTs(rtfData.planeradTs())
            .utforarId(rtfData.utforarId())
            .uppgiftStatus(rtfData.uppgiftStatus())
            .aktivitet(regelConfig.getUppgift().getAktivitet())
            .fsSAinformation(rtfData.fssaInformation())
            .specifikation(specifikation)
            .build();

      var requestBuilder = ImmutableUpdateKundbehovsflodeRequest.builder()
            .kundbehovsflodeId(rtfData.kundbehovsflodeId())
            .uppgift(uppgift)
            .underlag(new ArrayList<UpdateKundbehovsflodeUnderlag>());

      for (ErsattningData rtfErsattning : rtfData.ersattningar())
      {
         var ersattning = ImmutableUpdateKundbehovsflodeErsattning.builder()
               .beslutsutfall(mapBeslutsutfall(rtfErsattning.beslutsutfall()))
               .id(rtfErsattning.id())
               .avslagsanledning(rtfErsattning.avslagsanledning())
               .build();
         requestBuilder.addErsattningar(ersattning);
      }

      for (var rtfUnderlag : rtfData.underlag())
      {
         var underlag = ImmutableUpdateKundbehovsflodeUnderlag.builder()
               .typ(rtfUnderlag.typ())
               .version(rtfUnderlag.version())
               .data(rtfUnderlag.data())
               .build();
         requestBuilder.addUnderlag(underlag);
      }

      return requestBuilder.build();
   }

   private BeslutsutfallEnum mapBeslutsutfall(Beslutsutfall beslutsutfall)
   {
      if (beslutsutfall == null)
      {
         return BeslutsutfallEnum.FU;
      }

      switch (beslutsutfall)
      {
         case JA:
            return BeslutsutfallEnum.JA;
         case NEJ:
            return BeslutsutfallEnum.NEJ;
         case FU:
         default:
            return BeslutsutfallEnum.FU;
      }
   }
}
