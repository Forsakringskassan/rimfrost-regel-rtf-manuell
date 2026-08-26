package se.fk.github.manuellregelratttillforsakring.logic;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.UUID;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIndividYrkandeRoll;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableYrkande;
import se.fk.rimfrost.framework.handlaggning.model.IndividYrkandeRoll;
import se.fk.rimfrost.framework.regel.logic.KompletteringSvarServiceInterface;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;

/**
 * Handles reading and registering komplettering svar for RTF Manuell.
 *
 * <p>Reads the current personnummer and avsikt from the handlaggning and applies
 * the handläggare's corrections back onto the yrkande.
 */
@ApplicationScoped
public class RtfKompletteringSvarService
      implements KompletteringSvarServiceInterface<RtfKompletteringData, RtfKompletteringData>
{
   /**
    * Returns the current personnummer and avsikt from the handlaggning for the handläggare to review.
    *
    * @param handlaggning the current handlaggning
    * @return current komplettering data, with null personnummer if no role with typId="personnummer" exists
    */
   @Override
   public RtfKompletteringData readSvarData(Handlaggning handlaggning)
   {
      var yrkande = handlaggning.yrkande();
      var personnummer = yrkande.individYrkandeRoller().stream()
            .filter(r -> "personnummer".equals(r.individ().typId()))
            .map(r -> r.individ().varde())
            .findFirst()
            .orElse(null);

      var data = new RtfKompletteringData();
      data.setPersonnummer(personnummer);
      data.setAvsikt(yrkande.avsikt());
      return data;
   }

   /**
    * Applies the handläggare's svar to the yrkande and returns an update ready for persistence.
    *
    * <p>Replaces the existing personnummer role (preserving its yrkandeRollId) or adds a new one,
    * and updates avsikt on the yrkande.
    *
    * @param handlaggning the current handlaggning
    * @param request      the handläggare's registered svar
    * @return handlaggning update with the corrected yrkande
    */
   @Override
   public HandlaggningUpdate registerSvar(Handlaggning handlaggning, RtfKompletteringData request)
   {
      var yrkande = handlaggning.yrkande();

      var existingRollId = yrkande.individYrkandeRoller().stream()
            .filter(r -> "personnummer".equals(r.individ().typId()))
            .map(IndividYrkandeRoll::yrkandeRollId)
            .findFirst()
            .orElse(UUID.randomUUID().toString());

      var updatedRoller = new ArrayList<IndividYrkandeRoll>(
            yrkande.individYrkandeRoller().stream()
                  .filter(r -> !"personnummer".equals(r.individ().typId()))
                  .toList());

      updatedRoller.add(ImmutableIndividYrkandeRoll.builder()
            .individ(ImmutableIdtyp.builder()
                  .typId("personnummer")
                  .varde(request.getPersonnummer())
                  .build())
            .yrkandeRollId(existingRollId)
            .build());

      var updatedYrkande = ImmutableYrkande.copyOf(yrkande)
            .withAvsikt(request.getAvsikt())
            .withIndividYrkandeRoller(updatedRoller);

      return ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .version(handlaggning.version())
            .yrkande(updatedYrkande)
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .build();
   }
}
