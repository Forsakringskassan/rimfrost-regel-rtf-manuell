package se.fk.github.manuellregelratttillforsakring.integration.folkbokford;

import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.jaxrsclientfactory.JaxrsClientFactory;
import se.fk.github.jaxrsclientfactory.JaxrsClientOptionsBuilders;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.FolkbokfordRequest;
import se.fk.github.manuellregelratttillforsakring.integration.folkbokford.dto.FolkbokfordResponse;
import se.fk.rimfrost.api.folkbokforing.jaxrsspec.controllers.generatedsource.FolkbokforingControllerApi;

@ApplicationScoped
public class FolkbokfordAdapter
{

   @ConfigProperty(name = "folkbokford.api.base-url")
   String folkbokfordBaseUrl;

   @Inject
   FolkbokfordMapper mapper;

   @SuppressWarnings("unused")
   private FolkbokforingControllerApi folkbokfordClient;

   @SuppressWarnings("unused")
   @PostConstruct
   void init()
   {
      this.folkbokfordClient = new JaxrsClientFactory()
            .create(JaxrsClientOptionsBuilders.createClient(folkbokfordBaseUrl, FolkbokforingControllerApi.class)
                  .build());
   }

   public FolkbokfordResponse getFolkbokfordInfo(FolkbokfordRequest request)
   {
      try
      {
         var apiResponse = folkbokfordClient.folkbokforingPersnrGet(request.personnummer());
         return mapper.toFolkbokfordResponse(apiResponse);
      }
      catch (NotFoundException e)
      {
         return null;
      }
   }
}
