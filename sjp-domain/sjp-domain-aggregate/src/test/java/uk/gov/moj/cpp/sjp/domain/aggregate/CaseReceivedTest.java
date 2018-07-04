package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

public class CaseReceivedTest extends CaseAggregateBaseTest {

    @Before
    @Override
    public void setUp() {
        super.setUp();
        caseAggregate = new CaseAggregate();
        assertThat(caseAggregate.getCaseId(), nullValue());
        assertThat(caseAggregate.getUrn(), nullValue());
        assertThat(caseAggregate.getProsecutingAuthority(), nullValue());
        assertThat(caseAggregate.getOffenceIdsByDefendantId(), equalTo(emptyMap()));
        assertThat(caseAggregate.isCaseReceived(), equalTo(false));
    }

    @Test
    public void testCreateCase_whenValidSjpCase_shouldTriggerExpectedCaseCreatedAndStartedEvents() {
        final List<Object> events = caseAggregate.receiveCase(aCase, clock.now()).collect(Collectors.toList());

        final CaseReceived expectedCaseReceived = buildCaseReceived(aCase);
        assertThat(reflectionEquals(events.get(0), expectedCaseReceived, singleton("defendant")), is(true));
        assertThat(reflectionEquals(
                ((CaseReceived) events.get(0)).getDefendant(),
                expectedCaseReceived.getDefendant(),
                singleton("id")), is(true));

        assertThat(caseAggregate.isCaseReceived(), equalTo(true));
        assertThat(caseAggregate.getCaseId(), notNullValue());
        assertThat(caseAggregate.getUrn(), notNullValue());
        assertThat(caseAggregate.getProsecutingAuthority(), notNullValue());
        assertThat(caseAggregate.getOffenceIdsByDefendantId().keySet(), hasSize(1));

        verifyAggregatorApplyCall(events);
    }

    private void verifyAggregatorApplyCall(Collection<Object> events) {
        assertThat(events, not(empty()));
        events.stream().map(CaseReceived.class::cast).forEach(caseReceived -> {
            assertThat("Case id does not match", caseAggregate.getCaseId(), equalTo(caseReceived.getCaseId()));
            assertThat("Case urn does not match", caseAggregate.getUrn(), equalTo(caseReceived.getUrn()));
            assertThat("Case prosecutingAuthority does not match", caseAggregate.getProsecutingAuthority(), equalTo(caseReceived.getProsecutingAuthority()));
            assertThat("Case offenceIdsByDefendantId does not match", caseAggregate.getOffenceIdsByDefendantId(),
                    equalTo(singletonMap(
                            caseReceived.getDefendant().getId(),
                            caseReceived.getDefendant().getOffences().stream()
                                    .map(Offence::getId)
                                    .collect(toSet()))));
        });
    }

    @Test
    public void testApply_whenCaseReceivedEvent() {
        CaseReceived sjpCaseCreated = buildCaseReceived(CaseBuilder.aDefaultSjpCase().build());

        caseAggregate.apply(sjpCaseCreated);

        verifyAggregatorApplyCall(singleton(sjpCaseCreated));
    }

    /**
     * For ensure backward compatibility
     */
    @Test
    public void testApply_whenSjpCaseCreatedEvent() {
        SjpCaseCreated sjpCaseCreated = buildSjpCaseCreated(CaseBuilder.aDefaultSjpCase().build());

        caseAggregate.apply(sjpCaseCreated);

        assertThat("Case id does not match", caseAggregate.getCaseId().toString(), equalTo(sjpCaseCreated.getId()));
        assertThat("Case urn does not match", caseAggregate.getUrn(), equalTo(sjpCaseCreated.getUrn()));
        assertThat("Case prosecutingAuthority does not match", caseAggregate.getProsecutingAuthority(), equalTo(sjpCaseCreated.getProsecutingAuthority()));
        assertThat("Case offenceIdsByDefendantId does not match", caseAggregate.getOffenceIdsByDefendantId(),
                equalTo(singletonMap(
                        sjpCaseCreated.getDefendantId(),
                        sjpCaseCreated.getOffences().stream()
                                .map(Offence::getId)
                                .collect(toSet()))));
    }

    /**
     * To ensure backward compatibility
     */
    @SuppressWarnings("deprecation")
    private SjpCaseCreated buildSjpCaseCreated(Case aCase) {
        return new SjpCaseCreated(aCase.getId().toString(), aCase.getUrn(), null,
                null, null, aCase.getProsecutingAuthority(),
                null, null, null,
                null, aCase.getDefendant().getId(),
                aCase.getDefendant().getNumPreviousConvictions(), aCase.getCosts(), aCase.getPostingDate(),
                aCase.getDefendant().getOffences(), clock.now());
    }
}
