package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidReceived;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class AddDatesToAvoidTest {

    private static final String datesToAvoid = "12th July 2018";

    private CaseAggregate caseAggregate;
    private UUID caseId;

    @Before
    public void initialiseCase() {
        caseAggregate = new CaseAggregate();

        CaseReceived caseReceived = receiveCase();
        caseId = caseReceived.getCaseId();
    }

    @Test
    public void shouldCreateDatesToAvoidReceivedEvent() {
        List<Object> events = caseAggregate.addDatesToAvoid(datesToAvoid).collect(toList());

        assertThat(events, hasSize(1));

        DatesToAvoidReceived datesToAvoidReceived = (DatesToAvoidReceived) events.get(0);

        assertThat(datesToAvoidReceived.getCaseId(), equalTo(caseId));
        assertThat(datesToAvoidReceived.getDatesToAvoid(), equalTo(datesToAvoid));
    }

    private CaseReceived receiveCase() {
        Case sjpCase = CaseBuilder.aDefaultSjpCase().build();

        return caseAggregate.receiveCase(sjpCase, ZonedDateTime.now())
                .filter(CaseReceived.class::isInstance)
                .map(CaseReceived.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError(format("No event of type %s found.", CaseReceived.class.getSimpleName())));
    }
}
