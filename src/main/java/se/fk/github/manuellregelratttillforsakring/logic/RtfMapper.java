package se.fk.github.manuellregelratttillforsakring.logic;

import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.adapter.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.rimfrost.adapter.folkbokford.dto.FolkbokfordResponse;
import se.fk.rimfrost.adapter.folkbokford.dto.FolkbokfordResponse.Kon;
import se.fk.rimfrost.ersattningdata.ErsattningData;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ProduceratResultat;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Anstallning;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Ersattning;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Kund;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Kund.KonEnum;

@ApplicationScoped
public class RtfMapper
{
   private final Logger LOGGER = LoggerFactory.getLogger(RtfMapper.class);

   public GetDataResponse toGetDataResponse(Handlaggning handlaggning, ArbetsgivareResponse arbetsgivareResponse,
         FolkbokfordResponse folkbokfordResponse, ObjectMapper objectMapper)
   {

      var kund = new Kund();

      if (folkbokfordResponse != null)
      {
         kund.setFornamn(folkbokfordResponse.fornamn());
         kund.setEfternamn(folkbokfordResponse.efternamn());
         kund.setKon(mapKonEnum(folkbokfordResponse.kon()));
      }

      var anstallning = new Anstallning();

      if (arbetsgivareResponse != null)
      {
         anstallning.setAnstallningsdag(arbetsgivareResponse.anstallningsdag());
         anstallning.setArbetstidProcent(arbetsgivareResponse.arbetstidProcent());
         anstallning.setSistaAnstallningsdag(arbetsgivareResponse.sistaAnstallningsdag());
         anstallning.setOrganisationsnamn(arbetsgivareResponse.organisationsnamn());
         anstallning.setOrganisationsnummer(arbetsgivareResponse.organisationsnummer());
      }

      kund.setAnstallning(anstallning);

      var ersattningsList = new ArrayList<Ersattning>();
      var ersattningResult = handlaggning.yrkande().produceradeResultat().stream()
            .filter(pr -> pr.typ().equalsIgnoreCase("ersattning")).toList();

      for (var yrkandeErsattning : ersattningResult)
      {
         ersattningsList.add(toErsattning(yrkandeErsattning, objectMapper));
      }

      var response = new GetDataResponse();
      response.setKund(kund);
      response.handlaggningId(handlaggning.id());
      response.setErsattningar(ersattningsList);

      return response;
   }

   private KonEnum mapKonEnum(Kon kon)
   {
       return switch (kon) {
           case MAN -> KonEnum.MAN;
           case KVINNA -> KonEnum.KVINNA;
       };
   }

   public Ersattning toErsattning(ProduceratResultat produceratResultat, ObjectMapper objectMapper)
   {
      ErsattningData ersattningData;
      try
      {
         ersattningData = ErsattningData.fromJson(produceratResultat.data(), objectMapper);
      }
      catch (RuntimeException e)
      {
         LOGGER.error("Failed to parse json as ErsattningData", e);
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, "Internal server error");
      }

      var ersattning = new Ersattning();
      ersattning.setErsattningId(produceratResultat.id());
      ersattning.setErsattningstyp(ersattningData.getErsattningstyp().id());
      ersattning.setOmfattningProcent(ersattningData.getOmfattningProcent());
      ersattning.setBelopp(ersattningData.getBelopp());
      ersattning.setAvslagsanledning(produceratResultat.avslagsanledning());
      ersattning.setBeslutsutfall(toBeslutsutfall(ersattningData.getBeslutsutfall()));
      ersattning.setFrom(produceratResultat.resultatFrom().toLocalDate());
      ersattning.setTom(produceratResultat.resultatTom().toLocalDate());
      return ersattning;
   }

   private Beslutsutfall toBeslutsutfall(se.fk.rimfrost.ersattningdata.Beslutsutfall beslutsutfall)
   {
      if (beslutsutfall == null)
      {
         return null;
      }
      return switch (beslutsutfall) {
         case JA -> Beslutsutfall.JA;
         case NEJ -> Beslutsutfall.NEJ;
         case FU -> Beslutsutfall.FU;
      };
   }
}
