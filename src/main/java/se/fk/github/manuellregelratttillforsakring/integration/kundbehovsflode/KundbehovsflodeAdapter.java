package se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.github.jaxrsclientfactory.JaxrsClientFactory;
import se.fk.github.jaxrsclientfactory.JaxrsClientOptionsBuilders;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.KundbehovsflodeRequest;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.KundbehovsflodeResponse;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.KundbehovsflodeControllerApi;

@ApplicationScoped
public class KundbehovsflodeAdapter
{

   @ConfigProperty(name = "kundbehovsflode.api.base-url")
   String kundbehovsflodeBaseUrl;

   @Inject
   KundbehovsflodeMapper mapper;

   private KundbehovsflodeControllerApi kundbehovsClient;

   @PostConstruct
   void init()
   {
      this.kundbehovsClient = new JaxrsClientFactory()
            .create(JaxrsClientOptionsBuilders.createClient(kundbehovsflodeBaseUrl, KundbehovsflodeControllerApi.class)
                  .build());
   }

   public KundbehovsflodeResponse getKundbehovsflodeInfo(KundbehovsflodeRequest kundbehovsflodeRequest)
   {
      var apiResponse = kundbehovsClient.getKundbehovsflode(kundbehovsflodeRequest.kundbehovsflodeId().toString());
      return mapper.toKundbehovsflodeResponse(apiResponse);
   }

}
