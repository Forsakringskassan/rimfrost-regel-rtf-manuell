package se.fk.github.manuellregelratttillforsakring;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.github.manuellregelratttillforsakring.logic.RtfService;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIndividYrkandeRoll;
import se.fk.rimfrost.framework.handlaggning.model.IndividYrkandeRoll;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.logic.dto.KompletteringUnderlag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(WireMockRtfManuell.class)
class RtfKompletteringCheckTest
{
   @Inject
   RtfService rtfService;

   @Test
   @DisplayName("RTF-FR-06.1, RTF-FR-06.2: Inga kompletteringsbehov returneras när personnummer och avsikt finns")
   void returns_empty_when_personnummer_and_avsikt_present()
   {
      var result = rtfService.checkKomplettering(handlaggningWith("19901010-1234", "FRAN_ARBETE"));

      assertTrue(result.isEmpty());
   }

   @Test
   @DisplayName("RTF-FR-06.1: Kompletteringsbehov returneras när personnummer saknas bland individYrkandeRoller")
   void returns_personnummer_underlag_when_no_personnummer_roll()
   {
      var result = rtfService.checkKomplettering(handlaggningWith(null, "FRAN_ARBETE"));

      assertEquals(1, result.size());
      assertEquals("personnummer", result.getFirst().underlagTyp());
   }

   @Test
   @DisplayName("RTF-FR-06.1: Kompletteringsbehov returneras när personnummer är tomt")
   void returns_personnummer_underlag_when_personnummer_blank()
   {
      var result = rtfService.checkKomplettering(handlaggningWith("", "FRAN_ARBETE"));

      assertEquals(1, result.size());
      assertEquals("personnummer", result.getFirst().underlagTyp());
   }

   @Test
   @DisplayName("RTF-FR-06.2: Kompletteringsbehov returneras när avsikt är tom")
   void returns_avsikt_underlag_when_avsikt_blank()
   {
      var result = rtfService.checkKomplettering(handlaggningWith("19901010-1234", ""));

      assertEquals(1, result.size());
      assertEquals("avsikt", result.getFirst().underlagTyp());
   }

   @Test
   @DisplayName("RTF-FR-06.1, RTF-FR-06.2: Båda kompletteringsbehov returneras när personnummer och avsikt saknas")
   void returns_both_underlag_when_both_missing()
   {
      var result = rtfService.checkKomplettering(handlaggningWith(null, null));

      assertEquals(2, result.size());
      assertEquals("personnummer", result.get(0).underlagTyp());
      assertEquals("avsikt", result.get(1).underlagTyp());
   }

   /**
    * Builds a minimal Handlaggning mock with controllable personnummer and avsikt.
    * A null personnummer means no personnummer role exists in individYrkandeRoller.
    */
   private static Handlaggning handlaggningWith(String personnummer, String avsikt)
   {
      var yrkande = mock(Yrkande.class);
      when(yrkande.avsikt()).thenReturn(avsikt);
      when(yrkande.individYrkandeRoller()).thenReturn(personnummerRoller(personnummer));

      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.yrkande()).thenReturn(yrkande);
      return handlaggning;
   }

   /**
    * Uses ImmutableIdtyp and ImmutableIndividYrkandeRoll builders to avoid Mockito
    * proxy issues when mocking the Yrkande.IndividYrkandeRoll nested interface type.
    */
   private static List<IndividYrkandeRoll> personnummerRoller(String personnummer)
   {
      if (personnummer == null)
      {
         return List.of();
      }
      var idtyp = ImmutableIdtyp.builder().typId("personnummer").varde(personnummer).build();
      return List.of(ImmutableIndividYrkandeRoll.builder().individ(idtyp).yrkandeRollId("test-roll").build());
   }
}
