package se.fk.github.manuellregelratttillforsakring;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.github.manuellregelratttillforsakring.logic.RtfKompletteringSvarService;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIndividYrkandeRoll;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableYrkande;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.RtfKompletteringData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(WireMockRtfManuell.class)
class RtfKompletteringSvarServiceTest
{
   private static final UUID HANDLAGGNING_ID = UUID.fromString("5367f6b8-cc4a-11f0-8de9-199901011234");
   private static final UUID SPEC_ID = UUID.fromString("71234567-89ab-4cde-9012-3456789abcde");
   private static final OffsetDateTime SKAPAD_TS = OffsetDateTime.parse("2026-01-22T13:31:00+01:00");

   @Inject
   RtfKompletteringSvarService svarService;

   @Test
   @DisplayName("RTF-FR-06.3: Kompletteringsgränssnittet exponerar personnummer från individYrkandeRoller")
   void readSvarData_maps_personnummer_from_individYrkandeRoller()
   {
      var result = svarService.readSvarData(handlaggningWith("19901010-1234", "FRAN_ARBETE"));

      assertEquals("19901010-1234", result.getPersonnummer());
   }

   @Test
   @DisplayName("RTF-FR-06.3: Kompletteringsgränssnittet exponerar avsikt från yrkandet")
   void readSvarData_maps_avsikt_from_yrkande()
   {
      var result = svarService.readSvarData(handlaggningWith("19901010-1234", "FRAN_ARBETE"));

      assertEquals("FRAN_ARBETE", result.getAvsikt());
   }

   @Test
   @DisplayName("RTF-FR-06.3: Kompletteringsgränssnittet returnerar null för personnummer när ingen personnummerroll finns")
   void readSvarData_returns_null_personnummer_when_no_personnummer_role()
   {
      var result = svarService.readSvarData(handlaggningWith(null, "FRAN_ARBETE"));

      assertNull(result.getPersonnummer());
   }

   @Test
   @DisplayName("RTF-FR-06.3: Kompletteringsgränssnittet tillåter korrigering av avsikt")
   void registerSvar_updates_avsikt_on_yrkande()
   {
      var request = new RtfKompletteringData("19901010-1234", "NY_AVSIKT");

      var update = svarService.registerSvar(handlaggningWith("19901010-1234", "GAMMAL_AVSIKT"), request);

      assertEquals("NY_AVSIKT", update.yrkande().avsikt());
   }

   @Test
   @DisplayName("RTF-FR-06.3: Kompletteringsgränssnittet tillåter korrigering av personnummer")
   void registerSvar_replaces_existing_personnummer_role()
   {
      var existingRollId = "existing-roll-id";
      var request = new RtfKompletteringData("19901010-9999", "FRAN_ARBETE");

      var update = svarService.registerSvar(handlaggningWithRollId("19901010-1234", existingRollId, "FRAN_ARBETE"), request);

      var roller = update.yrkande().individYrkandeRoller();
      assertEquals(1, roller.size());
      assertEquals("personnummer", roller.getFirst().individ().typId());
      assertEquals("19901010-9999", roller.getFirst().individ().varde());
      assertEquals(existingRollId, roller.getFirst().yrkandeRollId());
   }

   @Test
   @DisplayName("RTF-FR-06.3: Kompletteringsgränssnittet lägger till personnummerroll när ingen finns")
   void registerSvar_adds_personnummer_role_when_none_exists()
   {
      var request = new RtfKompletteringData("19901010-1234", "FRAN_ARBETE");

      var update = svarService.registerSvar(handlaggningWith(null, "FRAN_ARBETE"), request);

      var roller = update.yrkande().individYrkandeRoller();
      assertEquals(1, roller.size());
      assertEquals("personnummer", roller.getFirst().individ().typId());
      assertEquals("19901010-1234", roller.getFirst().individ().varde());
   }

   @Test
   @DisplayName("RTF-FR-06.3: Kompletteringsgränssnittet bevarar övriga individYrkandeRoller vid uppdatering")
   void registerSvar_preserves_other_roller()
   {
      var otherRoll = ImmutableIndividYrkandeRoll.builder()
            .individ(ImmutableIdtyp.builder().typId("annan-typ").varde("value").build())
            .yrkandeRollId("other-roll-id")
            .build();
      var yrkande = yrkandeWith(null, null, "FRAN_ARBETE");
      var yrkandeWithOther = ImmutableYrkande.copyOf(yrkande).withIndividYrkandeRoller(otherRoll);
      var handlaggning = buildHandlaggning(yrkandeWithOther);
      var request = new RtfKompletteringData("19901010-1234", "FRAN_ARBETE");

      var update = svarService.registerSvar(handlaggning, request);

      assertEquals(2, update.yrkande().individYrkandeRoller().size());
   }

   @Test
   @DisplayName("RTF-FR-06.3: Kompletteringssvaret bygger handläggningsuppdatering med korrekt id och version")
   void registerSvar_builds_handlaggning_update_with_correct_id_and_version()
   {
      var request = new RtfKompletteringData("19901010-1234", "FRAN_ARBETE");

      var update = svarService.registerSvar(handlaggningWith("19901010-1234", "FRAN_ARBETE"), request);

      assertEquals(HANDLAGGNING_ID, update.id());
      assertEquals(1, update.version());
   }

   /**
    * Builds a Handlaggning mock with a real ImmutableYrkande.
    * A null personnummer means no personnummer role exists in individYrkandeRoller.
    */
   private Handlaggning handlaggningWith(String personnummer, String avsikt)
   {
      return handlaggningWithRollId(personnummer, UUID.randomUUID().toString(), avsikt);
   }

   private Handlaggning handlaggningWithRollId(String personnummer, String rollId, String avsikt)
   {
      return buildHandlaggning(yrkandeWith(personnummer, rollId, avsikt));
   }

   /**
    * Builds a real ImmutableYrkande so that ImmutableYrkande.copyOf() in registerSvar works
    * without NPEs from unstubbed mock methods.
    */
   private static Yrkande yrkandeWith(String personnummer, String rollId, String avsikt)
   {
      var builder = ImmutableYrkande.builder()
            .id(UUID.randomUUID())
            .version(1)
            .erbjudandeId(UUID.randomUUID().toString())
            .yrkandeDatum(OffsetDateTime.now())
            .yrkandeStatus("YRKAT")
            .yrkandeFrom(OffsetDateTime.now())
            .yrkandeTom(OffsetDateTime.now())
            .avsikt(avsikt);

      if (personnummer != null)
      {
         var idtyp = ImmutableIdtyp.builder().typId("personnummer").varde(personnummer).build();
         builder.addIndividYrkandeRoller(ImmutableIndividYrkandeRoll.builder()
               .individ(idtyp)
               .yrkandeRollId(rollId)
               .build());
      }

      return builder.build();
   }

   private Handlaggning buildHandlaggning(Yrkande yrkande)
   {
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(HANDLAGGNING_ID);
      when(handlaggning.version()).thenReturn(1);
      when(handlaggning.yrkande()).thenReturn(yrkande);
      when(handlaggning.skapadTS()).thenReturn(SKAPAD_TS);
      when(handlaggning.avslutadTS()).thenReturn(null);
      when(handlaggning.handlaggningspecifikationId()).thenReturn(SPEC_ID);
      return handlaggning;
   }
}
