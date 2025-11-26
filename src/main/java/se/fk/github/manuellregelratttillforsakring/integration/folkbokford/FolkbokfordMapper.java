package se.fk.github.manuellregelratttillforsakring.integration.folkbokford;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.ImmutableFolkbokfordResponse;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.FolkbokfordResponse.Kon;
import se.fk.rimfrost.api.folkbokforing.jaxrsspec.controllers.generatedsource.model.FolkbokforingPersnrGet200Response;

@ApplicationScoped
public class FolkbokfordMapper
{

   public FolkbokfordResponse toFolkbokfordResponse(FolkbokforingPersnrGet200Response apiResponse)
   {
      //TODO ta data från apiresponse
      return ImmutableFolkbokfordResponse.builder()
            .kon(Kon.KVINNA)
            .fornamn("Lisa")
            .efternamn("Tass")
            .build();
   }

}
