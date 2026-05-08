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
import se.fk.rimfrost.framework.handlaggning.model.ImmutableProduceratResultat;
import se.fk.rimfrost.framework.handlaggning.model.ProduceratResultat;
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

      var produceratResultat = ImmutableProduceratResultat.builder()
            .from(createProduceratResultat())
            .data("{?:")
            .build();

      var handlaggning = handlaggningMock();
      when(handlaggning.yrkande().produceradeResultat()).thenReturn(List.of(produceratResultat));

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
      when(handlaggning.yrkande().produceradeResultat()).thenReturn(List.of(createProduceratResultat()));

      var exception = assertThrows(RegelManuellException.class,
            () -> rtfService.updateData(handlaggning, patchErsattningRequest));
      assertEquals(Response.Status.BAD_REQUEST, exception.getStatus());
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

   private static Handlaggning handlaggningMock()
   {
      var handlaggning = mock(Handlaggning.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
      when(handlaggning.yrkande().individYrkandeRoller().getFirst().individ().varde())
            .thenReturn("19901010-1234");
      return handlaggning;
   }

   private static ProduceratResultat createProduceratResultat()
   {
      return ImmutableProduceratResultat.builder()
            .id(UUID.fromString(ERSATTNINGSID))
            .version(1)
            .resultatFrom(OffsetDateTime.now())
            .resultatTom(OffsetDateTime.now())
            .yrkandeStatus("abc")
            .typ("ersattning")
            .data("{\"belopp\":40000,\"berakningsgrund\":100,\"ersattningstyp\":{\"id\":\"361779e2-cc93-471a-815b-50bad66cd427\",\"namn\":\"HUNDBIDRAG\"},\"omfattningProcent\":100,\"beslutsutfall\":\"FU\"}")
            .build();
   }

}
