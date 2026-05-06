package se.fk.github.manuellregelratttillforsakring;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
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
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

}
