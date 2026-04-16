package se.fk.github.manuellregelratttillforsakring;

import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.PatchErsattningRequest;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.UpdateErsattning;
import java.util.UUID;

public class RtfManuellTestData
{
   // TODO consider refactoring to avoid copy/paste of test data in wiremock mappings

   public static String AVSLAGSANLEDNING = "TestAvslagsAnledning";
   public static String ERSATTNINGSTYP = "361779e2-cc93-471a-815b-50bad66cd427";
   public static String ERSATTNINGSID = "bc6e5150-29be-4758-a2ee-e241f1d7ccf7";
   public static String PRODUCERADE_RESULTAT_ID = "bc6e5150-29be-4758-a2ee-e241f1d7ccf7";
   public static String KUND_FORNAMN = "Lisa";
   public static String KUND_EFTERNAMN = "Tass";

   public static PatchErsattningRequest newPatchErsattningRequest(Beslutsutfall beslutsUtfall)
   {
      PatchErsattningRequest patchDataRequest = new PatchErsattningRequest();
      var updateErsattning = new UpdateErsattning();
      updateErsattning.setAvslagsanledning(AVSLAGSANLEDNING);
      updateErsattning.setBeslutsutfall(beslutsUtfall);
      updateErsattning.setErsattningId(UUID.fromString(ERSATTNINGSID));
      patchDataRequest.addErsattningarItem(updateErsattning);
      return patchDataRequest;
   }
}
