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
public class OffenceOutOfTimeHelperTest {

    private static LocalDate DATE = LocalDate.of(2019, 01, 01);
    private final OffenceHelper offenceHelper = new OffenceHelper();

    @Parameterized.Parameter(0)
    public LocalDate offenceStartDate;

    @Parameterized.Parameter(1)
    public LocalDate offenceChargeDate;

    @Parameterized.Parameter(2)
    public String prosecutionTimeLimit;

    @Parameterized.Parameter(3)
    public boolean expectedOffenceOutOfTime;

    @Parameterized.Parameters(name = "start date = {0}, charge date = {1}, prosecution time limit = {2}, offence out of time = {3}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {DATE, DATE, "6", false},
                {DATE, DATE.plusMonths(6), "6", false},
                {DATE, DATE.plusMonths(6).plusDays(1), "6", true},
                {DATE, DATE, "0", false},
                {DATE, DATE.plusMonths(10), "6", true},
        });
    }

    @Test
    public void shouldCalculateOffenceOutOfTimeFlag() {
        final JsonObjectBuilder offenceDefinition = createObjectBuilder();
        if (nonNull(prosecutionTimeLimit)) {
            offenceDefinition.add("prosecutionTimeLimit", prosecutionTimeLimit);
        }

        final JsonObjectBuilder offenceInstance = createObjectBuilder()
                .add("startDate", offenceStartDate.toString())
                .add("chargeDate", offenceChargeDate.toString());

        final boolean actualOffenceOutOfTime = offenceHelper.isOffenceOutOfTime(offenceInstance.build(), offenceDefinition.build());
        assertThat(actualOffenceOutOfTime, is(expectedOffenceOutOfTime));
    }
}
