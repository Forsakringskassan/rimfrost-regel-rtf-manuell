package se.fk.github.manuellregelratttillforsakring;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellOulTest;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRtfManuell.class)
})
public class RtfManuellOulTest extends AbstractRegelManuellOulTest
{

}
