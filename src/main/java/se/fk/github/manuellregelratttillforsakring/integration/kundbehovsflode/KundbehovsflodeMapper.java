package se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableErsattning;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeResponse;
import se.fk.github.manuellregelratttillforsakring.integration.kundbehovsflode.dto.KundbehovsflodeResponse;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.GetKundbehovsflodeResponse;

@ApplicationScoped
public class KundbehovsflodeMapper
{

   public KundbehovsflodeResponse toKundbehovsflodeResponse(GetKundbehovsflodeResponse apiResponse)
   {
      //TODO use apiResponse

      var ersattning1 = ImmutableErsattning.builder()
            .belopp(40000)
            .berakningsgrund(40000)
            .ersattningsId(UUID.randomUUID())
            .ersattningsTyp("Kronor")
            .from(LocalDate.now().minusDays(5))
            .tom(LocalDate.now().minusDays(5))
            .omfattningsProcent(100)
            .build();

      var ersattning2 = ImmutableErsattning.builder()
            .belopp(40000)
            .berakningsgrund(40000)
            .ersattningsId(UUID.randomUUID())
            .ersattningsTyp("Kronor")
            .from(LocalDate.now().minusDays(4))
            .tom(LocalDate.now().minusDays(4))
            .omfattningsProcent(100)
            .build();

      var ersattning3 = ImmutableErsattning.builder()
            .belopp(40000)
            .berakningsgrund(40000)
            .ersattningsId(UUID.randomUUID())
            .ersattningsTyp("Kronor")
            .from(LocalDate.now().minusDays(3))
            .tom(LocalDate.now().minusDays(3))
            .omfattningsProcent(50)
            .build();

      return ImmutableKundbehovsflodeResponse.builder()
            .personnummer("199007162134")
            .kundbehovsflodeId(apiResponse.getKundbehovsflode().getId())
            .addErsattning(ersattning1, ersattning2, ersattning3)
            .build();
   }

}
