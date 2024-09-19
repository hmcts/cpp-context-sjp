package uk.gov.moj.cpp.sjp.domain.aggregate;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.moj.cpp.sjp.event.CCApplicationStatusCreated;
import uk.gov.moj.cpp.sjp.event.CCApplicationStatusUpdated;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.APPEALED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.RELISTED;

public class CaseApplicationStatusUpdatedTest extends CaseAggregateBaseTest {

    @BeforeEach
    public void setupCaseApplicationStatusUpdated() {
        super.setUp();
        caseAggregate.getState().markCaseCompleted();

    }

    @Test
    public void shouldRaiseCCApplicationCreatedEventWhenApplicationStatusIsStatutoryDeclarationPending() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.STATUTORY_DECLARATION_PENDING);
        assertThat(events, hasSize(1));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusCreated.class));
        assertCCApplicationCreated((CCApplicationStatusCreated)event1, applicationId, ApplicationStatus.STATUTORY_DECLARATION_PENDING);

    }

    @Test
    public void shouldRaiseCCApplicationCreatedEventWhenApplicationStatusIsReopeningPending() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.REOPENING_PENDING);
        assertThat(events, hasSize(1));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusCreated.class));
        assertCCApplicationCreated((CCApplicationStatusCreated)event1, applicationId, ApplicationStatus.REOPENING_PENDING);

    }

    @Test
    public void shouldRaiseCCApplicationCreatedEventWhenApplicationStatusIsAppealPending() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.APPEAL_PENDING);
        assertThat(events, hasSize(1));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusCreated.class));
        assertCCApplicationCreated((CCApplicationStatusCreated)event1, applicationId, ApplicationStatus.APPEAL_PENDING);

    }

    @Test
    public void shouldRaiseCCApplicationUpdatedEventWhenApplicationStatusIsAppealWithDrawn() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.APPEAL_WITHDRAWN);
        assertThat(events, hasSize(1));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusUpdated.class));
        assertCCApplicationUpdated((CCApplicationStatusUpdated)event1, applicationId, ApplicationStatus.APPEAL_WITHDRAWN);

    }

    @Test
    public void shouldRaiseCCApplicationUpdatedEventWhenApplicationStatusIsAppealDismissed() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.APPEAL_DISMISSED);
        assertThat(events, hasSize(1));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusUpdated.class));
        assertCCApplicationUpdated((CCApplicationStatusUpdated)event1, applicationId, ApplicationStatus.APPEAL_DISMISSED);

    }

    @Test
    public void shouldRaiseCCApplicationUpdatedEventWhenApplicationStatusIsAppealAbandoned() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.APPEAL_ABANDONED);
        assertThat(events, hasSize(1));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusUpdated.class));
        assertCCApplicationUpdated((CCApplicationStatusUpdated)event1, applicationId, ApplicationStatus.APPEAL_ABANDONED);

    }

    @Test
    public void shouldRaiseCCApplicationUpdatedEventWhenApplicationStatusIsApplicationDismissedSentenceVaried() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.APPLICATION_DISMISSED_SENTENCE_VARIED);
        assertThat(events, hasSize(1));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusUpdated.class));
        assertCCApplicationUpdated((CCApplicationStatusUpdated)event1, applicationId, ApplicationStatus.APPLICATION_DISMISSED_SENTENCE_VARIED);

    }

    @Test
    public void shouldRaiseCCApplicationUpdatedEventWhenApplicationStatusIsUnkown() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.APPLICATION_STATUS_NOT_KNOWN);
        assertThat(events, hasSize(1));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusUpdated.class));
        assertCCApplicationUpdated((CCApplicationStatusUpdated)event1, applicationId, ApplicationStatus.APPLICATION_STATUS_NOT_KNOWN);

    }

    @Test
    public void shouldRaiseCCApplicationUpdatedAndCaseChangedEventWhenApplicationStatusIsAppealAllowed() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.APPEAL_ALLOWED);
        assertThat(events, hasSize(2));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusUpdated.class));
        assertCCApplicationUpdated((CCApplicationStatusUpdated)event1, applicationId, ApplicationStatus.APPEAL_ALLOWED);

        final Object event2 = events.get(1);
        assertThat(event2, instanceOf(CaseStatusChanged.class));
        assertThat(((CaseStatusChanged) event2).getCaseStatus(), Matchers.is(APPEALED));

    }

    @Test
    public void shouldRaiseCCApplicationUpdatedAndCaseChangedEventWhenApplicationStatusIsStatutoryDeclarationGranted() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.STATUTORY_DECLARATION_GRANTED);
        assertThat(events, hasSize(2));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusUpdated.class));
        assertCCApplicationUpdated((CCApplicationStatusUpdated)event1, applicationId, ApplicationStatus.STATUTORY_DECLARATION_GRANTED);

        final Object event2 = events.get(1);
        assertThat(event2, instanceOf(CaseStatusChanged.class));
        assertThat(((CaseStatusChanged) event2).getCaseStatus(), Matchers.is(RELISTED));

    }

    @Test
    public void shouldRaiseCCApplicationUpdatedAndCaseChangedEventWhenApplicationStatusIsReopeningGranted() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.REOPENING_GRANTED);
        assertThat(events, hasSize(2));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusUpdated.class));
        assertCCApplicationUpdated((CCApplicationStatusUpdated)event1, applicationId, ApplicationStatus.REOPENING_GRANTED);

        final Object event2 = events.get(1);
        assertThat(event2, instanceOf(CaseStatusChanged.class));
        assertThat(((CaseStatusChanged) event2).getCaseStatus(), Matchers.is(RELISTED));

    }

    @Test
    public void shouldRaiseCCApplicationUpdatedEventWhenApplicationStatusIsReopeningRefused() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.REOPENING_REFUSED);
        assertThat(events, hasSize(1));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusUpdated.class));
        assertCCApplicationUpdated((CCApplicationStatusUpdated)event1, applicationId, ApplicationStatus.REOPENING_REFUSED);

    }

    @Test
    public void shouldRaiseCCApplicationUpdatedEventWhenApplicationStatusIsReopeningWithDrawn() {
        final UUID  applicationId = randomUUID();
        final List<Object> events = caseApplicationStatusUpdated(applicationId, ApplicationStatus.REOPENING_WITHDRAWN);
        assertThat(events, hasSize(1));

        final Object event1 = events.get(0);
        assertThat(event1, instanceOf(CCApplicationStatusUpdated.class));
        assertCCApplicationUpdated((CCApplicationStatusUpdated)event1, applicationId, ApplicationStatus.REOPENING_WITHDRAWN);

    }

    private List<Object> caseApplicationStatusUpdated(final UUID applicationId, final ApplicationStatus applicationStatus) {
        return caseAggregate.updateCaseApplicationStatus(aCase.getId(), applicationId, applicationStatus).collect(Collectors.toList());
    }

    private void assertCCApplicationCreated(final CCApplicationStatusCreated ccApplicationStatusCreated, final UUID applicationId, final ApplicationStatus applicationStatus) {
        assertEquals(aCase.getId(), ccApplicationStatusCreated.getCaseId());
        assertEquals(applicationId, ccApplicationStatusCreated.getApplicationId());
        assertEquals(applicationStatus, ccApplicationStatusCreated.getStatus());
    }

    private void assertCCApplicationUpdated(final CCApplicationStatusUpdated ccApplicationStatusUpdated, final UUID applicationId, final ApplicationStatus applicationStatus) {
        assertEquals(aCase.getId(), ccApplicationStatusUpdated.getCaseId());
        assertEquals(applicationId, ccApplicationStatusUpdated.getApplicationId());
        assertEquals(applicationStatus, ccApplicationStatusUpdated.getStatus());
    }

}
