package se.fk.github.manuellregelratttillforsakring;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import se.fk.github.manuellregelratttillforsakring.logic.RtfService;
import se.fk.rimfrost.adapter.arbetsgivare.ArbetsgivareAdapter;
import se.fk.rimfrost.adapter.folkbokford.dto.FolkbokfordResponse;
import se.fk.rimfrost.adapter.arbetsgivare.exception.ArbetsgivareErrorCode;
import se.fk.rimfrost.adapter.arbetsgivare.exception.ArbetsgivareException;
import se.fk.rimfrost.adapter.folkbokford.FolkbokfordAdapter;
import se.fk.rimfrost.adapter.folkbokford.FolkbokfordException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableRollIYrkande;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableSakfragaStallningstagandeContainer;
import se.fk.rimfrost.framework.handlaggning.model.SakfragaStallningstagandeContainer;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.regel.rtf.manuell.jaxrsspec.controllers.generatedsource.model.Beslutsutfall;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static se.fk.github.manuellregelratttillforsakring.RtfManuellTestData.ANNAN_SAKFRAGA_DATA;
import static se.fk.github.manuellregelratttillforsakring.RtfManuellTestData.ANNAN_SAKFRAGA_ID;
import static se.fk.github.manuellregelratttillforsakring.RtfManuellTestData.ANNAN_SAKFRAGA_OBJEKT_TYP_ID;
import static se.fk.github.manuellregelratttillforsakring.RtfManuellTestData.ERSATTNINGSID;
import static se.fk.github.manuellregelratttillforsakring.RtfManuellTestData.newPatchErsattningRequest;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRtfManuell.class)
})
class RtfServiceExceptionTest
{
   @InjectMock
   FolkbokfordAdapter folkbokfordAdapter;

   @InjectMock
   ArbetsgivareAdapter arbetsgivareAdapter;

   @Inject
   RtfService rtfService;

   @ParameterizedTest
   @EnumSource(value = FolkbokfordException.ErrorType.class, names =
   {
         "NOT_FOUND"
   }, mode = EnumSource.Mode.EXCLUDE)
   void folkbokford_exception_maps_to_regel_manuell_exception(FolkbokfordException.ErrorType errorType)
         throws FolkbokfordException
   {
      when(folkbokfordAdapter.getFolkbokfordInfo(any()))
            .thenThrow(new FolkbokfordException(errorType, "test"));

      var exception = assertThrows(RegelManuellException.class,
            () -> rtfService.readData(handlaggningMock()));

      assertEquals(expectedStatus(errorType), exception.getStatus());
   }

   @ParameterizedTest
   @EnumSource(ArbetsgivareErrorCode.class)
   void arbetsgivare_exception_maps_to_regel_manuell_exception(ArbetsgivareErrorCode errorCode)
         throws FolkbokfordException, ArbetsgivareException
   {
      when(folkbokfordAdapter.getFolkbokfordInfo(any()))
            .thenReturn(mock(FolkbokfordResponse.class));
      when(arbetsgivareAdapter.getArbetsgivareInfo(any()))
            .thenThrow(new ArbetsgivareException("test", errorCode));

      var exception = assertThrows(RegelManuellException.class,
            () -> rtfService.readData(handlaggningMock()));

      assertEquals(expectedStatus(errorCode), exception.getStatus());
   }

   @Test
   void malformed_ersattning_json_exception_maps_to_regel_manuell_exception() throws FolkbokfordException, ArbetsgivareException
   {
      when(folkbokfordAdapter.getFolkbokfordInfo(any()))
            .thenReturn(null);
      when(arbetsgivareAdapter.getArbetsgivareInfo(any()))
            .thenReturn(null);

      var ersattningContainer = ImmutableSakfragaStallningstagandeContainer.builder()
            .from(createErsattningContainer())
            .data("{?:")
            .build();

      var handlaggning = handlaggningMock();
      when(handlaggning.yrkande().sakfragorStallningstaganden()).thenReturn(List.of(ersattningContainer));

      var exception = assertThrows(RegelManuellException.class,
            () -> rtfService.readData(handlaggning));

      assertEquals(Response.Status.INTERNAL_SERVER_ERROR, exception.getStatus());
   }

   @Test
   void unknown_ersattning_id_exception_maps_to_regel_manuell_exception()
   {
      var patchErsattningRequest = newPatchErsattningRequest(Beslutsutfall.JA);
      var ersattning = patchErsattningRequest.getErsattningar().getFirst();
      ersattning.setErsattningId(UUID.fromString("d2f29b54-4f68-4606-ad75-25db2dd99207"));
      patchErsattningRequest.setErsattningar(List.of(ersattning));

      var exception = assertThrows(RegelManuellException.class,
            () -> rtfService.updateData(handlaggningMock(), patchErsattningRequest));
      assertEquals(Response.Status.BAD_REQUEST, exception.getStatus());
   }

   @Test
   void null_beslutsutfall_value_exception_maps_to_regel_manuell_exception()
   {
      var patchErsattningRequest = newPatchErsattningRequest(Beslutsutfall.JA);
      var ersattning = patchErsattningRequest.getErsattningar().getFirst();
      ersattning.setBeslutsutfall(null);
      patchErsattningRequest.setErsattningar(List.of(ersattning));

      var handlaggning = handlaggningMock();
      when(handlaggning.yrkande().sakfragorStallningstaganden()).thenReturn(List.of(createErsattningContainer()));

      var exception = assertThrows(RegelManuellException.class,
            () -> rtfService.updateData(handlaggning, patchErsattningRequest));
      assertEquals(Response.Status.BAD_REQUEST, exception.getStatus());
   }

   @Test
   void patch_targeting_non_ersattning_sakfraga_maps_to_bad_request()
   {
      var annanSakfraga = ImmutableSakfragaStallningstagandeContainer.builder()
            .id(UUID.fromString(ANNAN_SAKFRAGA_ID))
            .objektTypId(ANNAN_SAKFRAGA_OBJEKT_TYP_ID)
            .data(ANNAN_SAKFRAGA_DATA)
            .build();

      var patchErsattningRequest = newPatchErsattningRequest(Beslutsutfall.JA);
      var ersattning = patchErsattningRequest.getErsattningar().getFirst();
      ersattning.setErsattningId(UUID.fromString(ANNAN_SAKFRAGA_ID));
      patchErsattningRequest.setErsattningar(List.of(ersattning));

      var handlaggning = handlaggningMock();
      when(handlaggning.yrkande().sakfragorStallningstaganden())
            .thenReturn(List.of(createErsattningContainer(), annanSakfraga));

      var exception = assertThrows(RegelManuellException.class,
            () -> rtfService.updateData(handlaggning, patchErsattningRequest));
      assertEquals(Response.Status.BAD_REQUEST, exception.getStatus());
   }

   @Test
   void missing_correlation_data_on_patch_maps_to_internal_server_error()
   {
      var patchErsattningRequest = newPatchErsattningRequest(Beslutsutfall.JA);

      var handlaggning = handlaggningMock();
      // no regel request has been received for this id, so OulUppgiftService has no correlation data
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      var yrkande = handlaggning.yrkande();
      // RegelUtils copies the yrkande via ImmutableYrkande.builder().from(...), which requires non-null values
      var now = OffsetDateTime.now();
      when(yrkande.id()).thenReturn(UUID.randomUUID());
      when(yrkande.ingangtypId()).thenReturn("test-ingangtyp");
      when(yrkande.yrkandeDatum()).thenReturn(now);
      when(yrkande.yrkandeStatus()).thenReturn("YRKAT");
      when(yrkande.yrkandeFrom()).thenReturn(now);
      when(yrkande.yrkandeTom()).thenReturn(now);
      when(yrkande.avsikt()).thenReturn("NY");
      when(yrkande.beslut()).thenReturn(List.of());
      when(yrkande.sakfragorStallningstaganden()).thenReturn(List.of(createErsattningContainer()));

      var exception = assertThrows(RegelManuellException.class,
            () -> rtfService.updateData(handlaggning, patchErsattningRequest));
      assertEquals(Response.Status.INTERNAL_SERVER_ERROR, exception.getStatus());
   }

   private static Response.Status expectedStatus(FolkbokfordException.ErrorType errorType)
   {
      return switch (errorType)
      {
         case NOT_FOUND -> Response.Status.INTERNAL_SERVER_ERROR;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         case UNEXPECTED_ERROR -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }

   private static Response.Status expectedStatus(ArbetsgivareErrorCode errorCode)
   {
      return switch (errorCode)
      {
         case NOT_FOUND -> Response.Status.INTERNAL_SERVER_ERROR;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         case UNEXPECTED_ERROR -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }

   /**
    * RollIYrkande instances are built as real Immutable instances instead of relying on
    * Mockito RETURNS_DEEP_STUBS for the list elements.
    */
   private static Handlaggning handlaggningMock()
   {
      var idtyp = ImmutableIdtyp.builder().typId("personnummer").varde("19901010-1234").build();
      var roll = ImmutableRollIYrkande.builder()
            .id(UUID.randomUUID())
            .individ(idtyp)
            .yrkandeRollId("test-roll")
            .avserYrkande(UUID.randomUUID())
            .build();
      var yrkande = mock(Yrkande.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
      when(yrkande.rollerIYrkande()).thenReturn(List.of(roll));
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.yrkande()).thenReturn(yrkande);
      return handlaggning;
   }

   private static SakfragaStallningstagandeContainer createErsattningContainer()
   {
      return ImmutableSakfragaStallningstagandeContainer.builder()
            .id(UUID.fromString(ERSATTNINGSID))
            .objektTypId("ersattning")
            .data("{\"id\":\"" + ERSATTNINGSID
                  + "\",\"version\":1,\"from\":\"2025-01-10\",\"tom\":\"2025-01-10\",\"yrkandeStatus\":\"abc\",\"belopp\":40000,\"berakningsgrund\":100,\"ersattningstyp\":{\"id\":\"361779e2-cc93-471a-815b-50bad66cd427\",\"namn\":\"HUNDBIDRAG\"},\"omfattningProcent\":100,\"beslutsutfall\":\"FU\"}")
            .build();
   }

}
