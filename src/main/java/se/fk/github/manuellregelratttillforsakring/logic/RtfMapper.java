package se.fk.github.manuellregelratttillforsakring.logic;

import java.util.ArrayList;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.manuellregelratttillforsakring.integration.kafka.dto.ImmutableRtfManuellResponseRequest;
import se.fk.github.manuellregelratttillforsakring.integration.kafka.dto.RtfManuellResponseRequest;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeErsattning;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeRequest;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableUpdateKundbehovsflodeUnderlag;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.KundbehovsflodeResponse;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.UpdateKundbehovsflodeRequest;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.UpdateKundbehovsflodeUnderlag;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableErsattning;
import se.fk.github.manuellregelratttillforsakring.logic.dto.ImmutableGetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.Beslutsutfall;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse;
import se.fk.github.manuellregelratttillforsakring.logic.dto.GetRtfDataResponse.Ersattning;
import se.fk.github.manuellregelratttillforsakring.logic.entity.CloudEventData;
import se.fk.github.manuellregelratttillforsakring.logic.entity.ErsattningData;
import se.fk.github.manuellregelratttillforsakring.logic.entity.RtfData;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Ersattning.BeslutsutfallEnum;

@ApplicationScoped
public class RtfMapper {

    public GetRtfDataResponse toRtfResponse(KundbehovsflodeResponse kundbehovflodesResponse,
                                            FolkbokfordResponse folkbokfordResponse, ArbetsgivareResponse arbetsgivareResponse, RtfData rtfData) {
        var ersattningsList = new ArrayList<Ersattning>();

        for (var kundbehovErsattning : kundbehovflodesResponse.ersattning()) {
            ErsattningData rtfErsattning = rtfData.ersattningar().stream().filter(e -> e.id().equals(kundbehovErsattning.ersattningsId()))
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

            if (rtfErsattning.beslutsutfall() != null) {
                ersattning.beslutsutfall(rtfErsattning.beslutsutfall());
            }

            ersattningsList.add(ersattning.build());
        }

        var builder = ImmutableGetRtfDataResponse.builder()
                .kundbehovsflodeId(kundbehovflodesResponse.kundbehovsflodeId())
                .ersattning(ersattningsList);

        if (folkbokfordResponse != null) {
            builder
                    .fornamn(folkbokfordResponse.fornamn())
                    .efternamn(folkbokfordResponse.efternamn())
                    .kon(folkbokfordResponse.kon().toString());
        }

        if (arbetsgivareResponse != null) {
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

    public RtfManuellResponseRequest toRtfResponseRequest(RtfData rtfData, CloudEventData cloudevent, boolean rattTillForsakring) {
        return ImmutableRtfManuellResponseRequest.builder()
                .id(cloudevent.id())
                .kundbehovsflodeId(rtfData.kundbehovsflodeId())
                .kogitoparentprociid(cloudevent.kogitoparentprociid())
                .kogitorootprociid(cloudevent.kogitorootprociid())
                .kogitoprocid(cloudevent.kogitoprocid())
                .kogitorootprocid(cloudevent.kogitorootprocid())
                .kogitoprocinstanceid(cloudevent.kogitoprocinstanceid())
                .kogitoprocist(cloudevent.kogitoprocist())
                .kogitoprocversion(cloudevent.kogitoprocversion())
                .rattTillForsakring(rattTillForsakring)
                .build();
    }

    public UpdateKundbehovsflodeRequest toUpdateKundbehovsflodeRequest(RtfData rtfData) {

        var requestBuilder = ImmutableUpdateKundbehovsflodeRequest.builder()
                .kundbehovsflodeId(rtfData.kundbehovsflodeId())
                .underlag(new ArrayList<UpdateKundbehovsflodeUnderlag>())
                .uppgiftId(rtfData.uppgiftId());

        for (ErsattningData rtfErsattning : rtfData.ersattningar()) {
            var ersattning = ImmutableUpdateKundbehovsflodeErsattning.builder()
                    .beslutsutfall(mapBeslutsutfall(rtfErsattning.beslutsutfall()))
                    .id(rtfErsattning.id())
                    .avslagsanledning(rtfErsattning.avslagsanledning())
                    .build();
            requestBuilder.addErsattningar(ersattning);
        }

        for (var rtfUnderlag : rtfData.underlag()) {
            var underlag = ImmutableUpdateKundbehovsflodeUnderlag.builder()
                    .typ(rtfUnderlag.typ())
                    .version(rtfUnderlag.version())
                    .data(rtfUnderlag.data())
                    .build();
            requestBuilder.addUnderlag(underlag);
        }

        return requestBuilder.build();
    }

    private BeslutsutfallEnum mapBeslutsutfall(
            Beslutsutfall beslutsutfall) {
        if (beslutsutfall == null) {
            return BeslutsutfallEnum.FU;
        }

        switch (beslutsutfall) {
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
