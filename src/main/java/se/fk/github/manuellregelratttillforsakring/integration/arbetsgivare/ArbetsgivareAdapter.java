package se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.jaxrsclientfactory.JaxrsClientFactory;
import se.fk.github.jaxrsclientfactory.JaxrsClientOptionsBuilders;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ArbetsgivareRequest;
import se.fk.github.manuellregelratttillforsakring.integration.arbetsgivare.dto.ArbetsgivareResponse;
import se.fk.rimfrost.api.arbetsgivare.jaxrsspec.controllers.generatedsource.ArbetsgivareControllerApi;

@ApplicationScoped
public class ArbetsgivareAdapter
{

   @ConfigProperty(name = "arbetsgivare.api.base-url")
   String arbetsgivareApiBaseUrl;

   @Inject
   ArbetsgivareMapper mapper;

   @SuppressWarnings("unused")
   private ArbetsgivareControllerApi arbetsgivareClient;

   @SuppressWarnings("unused")
   @PostConstruct
   void init()
   {
      this.arbetsgivareClient = new JaxrsClientFactory()
            .create(JaxrsClientOptionsBuilders.createClient(arbetsgivareApiBaseUrl, ArbetsgivareControllerApi.class)
                  .build());
   }

   public ArbetsgivareResponse getArbetsgivareInfo(ArbetsgivareRequest arbetsgivareRequest)
   {
      var apiResponse = arbetsgivareClient.getArbetsgivare(arbetsgivareRequest.personnummer());
      return mapper.toArbetsgivareResponse(apiResponse);
   }
}
