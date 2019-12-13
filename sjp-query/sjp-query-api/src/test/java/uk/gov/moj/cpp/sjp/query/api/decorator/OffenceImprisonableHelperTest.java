package uk.gov.moj.cpp.sjp.query.api.decorator;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class OffenceImprisonableHelperTest {

    private final OffenceHelper offenceHelper = new OffenceHelper();

    @Parameterized.Parameter(0)
    public String offenceCode;

     @Parameterized.Parameter(1)
    public String modeOfTrial;

    @Parameterized.Parameter(2)
    public boolean expectedOffenceIsImprisonable;

    @Parameterized.Parameters(name = "offence code={0}, mode Of Trial = {1}, is imprisonable={2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"CA03013","SNONIMP",false},
                {"RR84227","STRAFF",false},
                {"RT88584B","SIMP",true},
                {"CD98070","EWAY",true}
        });
    }

    @Test
    public void shouldCalculateOffenceOutOfTimeFlag() {
        final JsonObjectBuilder offenceDefinition = createObjectBuilder();
        offenceDefinition.add("modeOfTrial", modeOfTrial);

        final boolean actualOffenceOutOfTime = offenceHelper.isOffenceImprisonable(offenceDefinition.build());
        assertThat(actualOffenceOutOfTime, is(expectedOffenceIsImprisonable));
    }
}
