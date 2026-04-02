package se.fk.github.manuellregelratttillforsakring.logic;

import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.adapter.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.rimfrost.adapter.folkbokford.dto.FolkbokfordResponse;
import se.fk.rimfrost.adapter.folkbokford.dto.FolkbokfordResponse.Kon;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ProduceratResultat;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Anstallning;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Ersattning;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.GetDataResponse;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Kund;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Kund.KonEnum;

@ApplicationScoped
public class RtfMapper
{

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
      switch (kon)
      {
         case MAN:
            return KonEnum.MAN;
         case KVINNA:
            return KonEnum.KVINNA;
         default:
            throw new InternalError("Could not map enum Kon");
      }
   }

   public Ersattning toErsattning(ProduceratResultat produceratResultat, ObjectMapper objectMapper)
   {

      try
      {
         var ersattning = objectMapper.readValue(produceratResultat.data(), Ersattning.class);
         ersattning.setErsattningId(produceratResultat.id());
         ersattning.setAvslagsanledning(produceratResultat.avslagsanledning());
         ersattning.setFrom(produceratResultat.resultatFrom().toLocalDate());
         ersattning.setTom(produceratResultat.resultatTom().toLocalDate());
         return ersattning;
      }
      catch (JsonProcessingException e)
      {
         throw new RuntimeException("Error while parsing ProduceratResultat.data to Ersattning: " + produceratResultat.data(), e);
      }
   }
}
