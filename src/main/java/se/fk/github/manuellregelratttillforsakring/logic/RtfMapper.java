package se.fk.github.manuellregelratttillforsakring.logic;

import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.arbetsgivare.adapter.dto.ArbetsgivareResponse;
import se.fk.rimfrost.framework.folkbokford.adapter.dto.FolkbokfordResponse;
import se.fk.rimfrost.framework.folkbokford.adapter.dto.FolkbokfordResponse.Kon;
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

      var anstallning = new Anstallning();
      anstallning.setAnstallningsdag(arbetsgivareResponse.anstallningsdag());
      anstallning.setArbetstidProcent(arbetsgivareResponse.arbetstidProcent());
      anstallning.setSistaAnstallningsdag(arbetsgivareResponse.sistaAnstallningsdag());
      anstallning.setOrganisationsnamn(arbetsgivareResponse.organisationsnamn());
      anstallning.setOrganisationsnummer(arbetsgivareResponse.organisationsnummer());

      var kund = new Kund();
      kund.setFornamn(folkbokfordResponse.fornamn());
      kund.setEfternamn(folkbokfordResponse.efternamn());
      kund.setAnstallning(anstallning);
      kund.setKon(mapKonEnum(folkbokfordResponse.kon()));

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
         var data = objectMapper.readValue(produceratResultat.data(), Ersattning.class);

         var ersattning = new Ersattning();
         ersattning.setErsattningstyp(data.getErsattningstyp());
         ersattning.setOmfattningProcent(data.getOmfattningProcent());
         ersattning.setBelopp(data.getBelopp());
         ersattning.setBerakningsgrund(data.getBerakningsgrund());
         ersattning.setBeslutsutfall(data.getBeslutsutfall());
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
