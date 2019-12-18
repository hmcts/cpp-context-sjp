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
public class OffenceNotInEffectHelperTest {

    private static final LocalDate DATE = LocalDate.of(2019, 01, 01);
    private final OffenceHelper offenceHelper = new OffenceHelper();

    @Parameterized.Parameter(0)
    public LocalDate offenceStartDate;

    @Parameterized.Parameter(1)
    public LocalDate offenceDefinitionStartDate;

    @Parameterized.Parameter(2)
    public LocalDate offenceDefinitionEndDate;

    @Parameterized.Parameter(3)
    public boolean expectedOffenceNotInEffect;

    @Parameterized.Parameters(name = "start date = {0}, definition start date = {1}, definition end date = {2}, not in effect = {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {DATE, DATE.minusDays(1), DATE.plusDays(1), false},
                {DATE, DATE.minusDays(1), DATE, false},
                {DATE, DATE, DATE.plusDays(1), false},
                {DATE, DATE, DATE, false},
                {DATE, DATE.plusDays(1), DATE.plusDays(2), true},
                {DATE, DATE.minusDays(2), DATE.minusDays(1), true},
                {DATE, DATE.plusDays(2), DATE.minusDays(1), true},
                {DATE, null, null, false},
                {DATE, null, DATE.plusDays(1), false},
                {DATE, null, DATE, false},
                {DATE, DATE.minusDays(1), null, false},
        });
    }

    @Test
    public void shouldCalculateOffenceNotInEffectFlag() {
        final JsonObjectBuilder offenceDefinition = createObjectBuilder();
        if (nonNull(offenceDefinitionStartDate)) {
            offenceDefinition.add("offenceStartDate", offenceDefinitionStartDate.toString());
        }
        if (nonNull(offenceDefinitionEndDate)) {
            offenceDefinition.add("offenceEndDate", offenceDefinitionEndDate.toString());
        }

        final JsonObjectBuilder offenceInstance = createObjectBuilder()
                .add("startDate", offenceStartDate.toString());

        final boolean actualOffenceNotInEffect = offenceHelper.isOffenceNotInEffect(offenceInstance.build(), offenceDefinition.build());
        assertThat(actualOffenceNotInEffect, is(expectedOffenceNotInEffect));
    }
}
