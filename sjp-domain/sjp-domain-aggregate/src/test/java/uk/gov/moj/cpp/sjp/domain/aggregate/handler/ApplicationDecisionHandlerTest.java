package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationDecision.applicationDecision;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_GRANTED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_PENDING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_PENDING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_REFUSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.REOPENING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.STAT_DEC;
import static uk.gov.justice.json.schemas.domains.sjp.commands.SaveApplicationDecision.saveApplicationDecision;
import static uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionRejected.applicationDecisionRejected;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.testutils.builders.DischargeBuilder.withDefaults;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.json.schemas.domains.sjp.commands.SaveApplicationDecision;
import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionRejected;
import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.Application;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.testutils.builders.DismissBuilder;
import uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.VerdictCancelled;
import uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationDecisionHandlerTest {

    private CaseAggregateState caseAggregateState;
    private Application currentApplication;
    private final UUID sessionId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final UUID offenceId1 = randomUUID();
    private final UUID offenceId2 = randomUUID();
    private final UUID offenceId3 = randomUUID();
    private final UUID legalAdviserId = randomUUID();
    private Session session;
    private final String courtHouseCode = "1008";
    private final String courtHouseName = "Test court";
    private final String localJusticeAreaNationalCode = "1009";
    private final User savedBy = new User("John", "Smith", randomUUID());
    private Optional<DelegatedPowers> legalAdviser;

    @Mock
    private CourtApplication courtApplication;

    @Before
    public void onceBeforeEachTest() {
        caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(randomUUID());
        caseAggregateState.setDefendantId(defendantId);
        final Set<UUID> offencesSet = new LinkedHashSet<>();
        offencesSet.addAll(asList(offenceId1, offenceId2, offenceId3));
        caseAggregateState.addOffenceIdsForDefendant(defendantId,offencesSet);
        session = new Session();
        legalAdviser = Optional.of(DelegatedPowers.delegatedPowers().withFirstName("Erica").withLastName("Wilson").withUserId(randomUUID()).build());
        session.startMagistrateSession(sessionId, legalAdviserId, courtHouseCode, courtHouseName,
                localJusticeAreaNationalCode, now(), "magistrate name", legalAdviser);
    }

    private void givenPendingReopening() {
        currentApplication = new Application(courtApplication);
        currentApplication.setType(REOPENING);
        currentApplication.setStatus(REOPENING_PENDING);
        caseAggregateState.setCurrentApplication(currentApplication);
    }

    private void givenPendingStatDec() {
        currentApplication = new Application(courtApplication);
        currentApplication.setType(STAT_DEC);
        currentApplication.setStatus(STATUTORY_DECLARATION_PENDING);
        caseAggregateState.setCurrentApplication(currentApplication);
    }

    @Test
    public void shouldRejectDecisionIfNoCurrentApplication() {
        final SaveApplicationDecision saveApplicationDecision = saveApplicationDecision()
                .withApplicationId(randomUUID())
                .withCaseId(caseAggregateState.getCaseId())
                .withSessionId(sessionId)
                .withGranted(true)
                .withOutOfTime(false)
                .withSavedBy(savedBy)
                .build();

        final Stream<Object> eventStream =
                ApplicationDecisionHandler.INSTANCE.saveApplicationDecision(saveApplicationDecision, caseAggregateState, session);

        final List<String> expectedRejectionReasons = asList(
             "The case doesn't have any pending application"
        );

        thenTheDecisionIsRejected(saveApplicationDecision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void shouldRejectDecisionIfCurrentApplicationIsNotTheSame() {
        givenPendingReopening();
        final UUID applicationId = UUID.randomUUID();
        when(courtApplication.getId()).thenReturn(randomUUID());
        final SaveApplicationDecision saveApplicationDecision = saveApplicationDecision()
                .withApplicationId(applicationId)
                .withCaseId(caseAggregateState.getCaseId())
                .withSessionId(sessionId)
                .withGranted(true)
                .withOutOfTime(false)
                .withSavedBy(savedBy)
                .build();

        final Stream<Object> eventStream =
                ApplicationDecisionHandler.INSTANCE.saveApplicationDecision(saveApplicationDecision, caseAggregateState, session);

        final List<String> expectedRejectionReasons = asList(
                "The current application is not the same as the one in the decision"
        );

        thenTheDecisionIsRejected(saveApplicationDecision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void shouldRejectDecisionIfOutOfTimeReasonIsNotProvided() {
        givenPendingReopening();
        final SaveApplicationDecision saveApplicationDecision = saveApplicationDecision()
                .withApplicationId(currentApplication.getApplicationId())
                .withCaseId(caseAggregateState.getCaseId())
                .withSessionId(sessionId)
                .withGranted(true)
                .withOutOfTime(true)
                .withSavedBy(savedBy)
                .build();

        final Stream<Object> eventStream =
                ApplicationDecisionHandler.INSTANCE.saveApplicationDecision(saveApplicationDecision, caseAggregateState, session);

        final List<String> expectedRejectionReasons = asList(
                "Application decision out of time must have reason"
        );

        thenTheDecisionIsRejected(saveApplicationDecision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void shouldRejectDecisionIfRejectionReasonIsNotProvided() {
        givenPendingReopening();
        final SaveApplicationDecision saveApplicationDecision = saveApplicationDecision()
                .withApplicationId(currentApplication.getApplicationId())
                .withCaseId(caseAggregateState.getCaseId())
                .withSessionId(sessionId)
                .withGranted(false)
                .withOutOfTime(false)
                .withSavedBy(savedBy)
                .build();

        final Stream<Object> eventStream =
                ApplicationDecisionHandler.INSTANCE.saveApplicationDecision(saveApplicationDecision, caseAggregateState, session);

        final List<String> expectedRejectionReasons = asList(
                "Rejected application must have rejection reason"
        );

        thenTheDecisionIsRejected(saveApplicationDecision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void shouldAcceptDecision() {
        givenPendingReopening();
        final SaveApplicationDecision saveApplicationDecision = saveApplicationDecision()
                .withApplicationId(currentApplication.getApplicationId())
                .withCaseId(caseAggregateState.getCaseId())
                .withSessionId(sessionId)
                .withGranted(true)
                .withOutOfTime(false)
                .withSavedBy(savedBy)
                .build();

        final Stream<Object> eventStream =
                ApplicationDecisionHandler.INSTANCE.saveApplicationDecision(saveApplicationDecision, caseAggregateState, session);

        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsSaved(saveApplicationDecision, eventList);
        thenApplicationStatusIsChanged(REOPENING_GRANTED, eventList);
        thenApplicationDecisionSetAside(saveApplicationDecision, eventList);
    }

    @Test
    public void shouldClearPleas() {
        givenPendingReopening();
        final List<Plea> pleas = asList(
            new Plea(defendantId, offenceId1, GUILTY),
            new Plea(defendantId, offenceId2, GUILTY)
        );
        caseAggregateState.setPleas(pleas);

        final SaveApplicationDecision saveApplicationDecision = saveApplicationDecision()
                .withApplicationId(currentApplication.getApplicationId())
                .withCaseId(caseAggregateState.getCaseId())
                .withSessionId(sessionId)
                .withGranted(true)
                .withOutOfTime(false)
                .withSavedBy(savedBy)
                .build();

        final Stream<Object> eventStream =
                ApplicationDecisionHandler.INSTANCE.saveApplicationDecision(saveApplicationDecision, caseAggregateState, session);

        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsSaved(saveApplicationDecision, eventList);
        thenPleaIsCancelled(offenceId1, eventList);
        thenPleaIsCancelled(offenceId2, eventList);
        thenApplicationStatusIsChanged(REOPENING_GRANTED, eventList);
        thenApplicationDecisionSetAside(saveApplicationDecision, eventList);
    }

    @Test
    public void shouldClearPleasAndVerdicts() {
        givenPendingReopening();
        final List<Plea> pleas = asList(
                new Plea(defendantId, offenceId1, GUILTY),
                new Plea(defendantId, offenceId2, GUILTY)
        );
        caseAggregateState.setPleas(pleas);
        final Discharge discharge1 = withDefaults()
                .offenceDecisionInformation(createOffenceDecisionInformation(offenceId1, FOUND_GUILTY))
                .build();
        final Discharge discharge2 = withDefaults()
                .offenceDecisionInformation(createOffenceDecisionInformation(offenceId2, FOUND_GUILTY))
                .build();
        final Dismiss dismiss = DismissBuilder.withDefaults(offenceId3).build();
        final List<OffenceDecision> offenceDecisions = asList(discharge1, discharge2, dismiss);
        caseAggregateState.updateOffenceConvictionDetails(now(), offenceDecisions, null);

        final SaveApplicationDecision saveApplicationDecision = saveApplicationDecision()
                .withApplicationId(currentApplication.getApplicationId())
                .withCaseId(caseAggregateState.getCaseId())
                .withSessionId(sessionId)
                .withGranted(true)
                .withOutOfTime(false)
                .withSavedBy(savedBy)
                .build();

        final Stream<Object> eventStream =
                ApplicationDecisionHandler.INSTANCE.saveApplicationDecision(saveApplicationDecision, caseAggregateState, session);

        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsSaved(saveApplicationDecision, eventList);
        thenPleaIsCancelled(offenceId1, eventList);
        thenPleaIsCancelled(offenceId2, eventList);
        thenVerdictIsCancelled(offenceId1, eventList);
        thenVerdictIsCancelled(offenceId2, eventList);
        thenApplicationStatusIsChanged(REOPENING_GRANTED, eventList);
        thenApplicationDecisionSetAside(saveApplicationDecision, eventList);
    }

    @Test
    public void shouldAcceptRefusedApplication() {
        givenPendingStatDec();

        final SaveApplicationDecision saveApplicationDecision = saveApplicationDecision()
                .withApplicationId(currentApplication.getApplicationId())
                .withCaseId(caseAggregateState.getCaseId())
                .withSessionId(sessionId)
                .withGranted(false)
                .withRejectionReason("insufficient evidence")
                .withSavedBy(savedBy)
                .build();

        final Stream<Object> eventStream =
                ApplicationDecisionHandler.INSTANCE.saveApplicationDecision(saveApplicationDecision, caseAggregateState, session);

        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsSaved(saveApplicationDecision, eventList);
        thenApplicationStatusIsChanged(STATUTORY_DECLARATION_REFUSED, eventList);
        thenCaseIsUnassigned(eventList);
        thenCaseIsCompleted(eventList);
    }

    private void thenCaseIsUnassigned(final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
            instanceOf(CaseUnassigned.class),
            hasProperty("caseId", equalTo(caseAggregateState.getCaseId()))
        )));
    }

    private void thenCaseIsCompleted(final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
                instanceOf(CaseCompleted.class),
                hasProperty("caseId", equalTo(caseAggregateState.getCaseId()))
        )));
    }

    private void thenPleaIsCancelled(final UUID offenceId, final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
                    instanceOf(PleaCancelled.class),
                    hasProperty("caseId", equalTo(caseAggregateState.getCaseId())),
                    hasProperty("defendantId", equalTo(defendantId)),
                    hasProperty("offenceId", equalTo(offenceId))
                )
        ));
    }

    private void thenVerdictIsCancelled(final UUID offenceId, final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
                instanceOf(VerdictCancelled.class),
                hasProperty("offenceId", equalTo(offenceId))
        )));
    }

    private void thenTheDecisionIsSaved(final SaveApplicationDecision saveApplicationDecision, final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
                instanceOf(ApplicationDecisionSaved.class),
                hasProperty("applicationId", equalTo(saveApplicationDecision.getApplicationId())),
                hasProperty("caseId", equalTo(caseAggregateState.getCaseId())),
                hasProperty("sessionId", equalTo(sessionId)),
                hasProperty("decisionId", notNullValue()),
                hasProperty("savedAt", notNullValue()),
                hasProperty("savedBy", equalTo(savedBy)),
                hasProperty("applicationDecision", allOf(
                        hasProperty("granted", equalTo(saveApplicationDecision.getGranted())),
                        hasProperty("outOfTime", equalTo(saveApplicationDecision.getOutOfTime())),
                        hasProperty("outOfTimeReason", equalTo(saveApplicationDecision.getOutOfTimeReason())),
                        hasProperty("rejectionReason", equalTo(saveApplicationDecision.getRejectionReason()))
                ))
        )));
    }

    private void thenApplicationDecisionSetAside(final SaveApplicationDecision saveApplicationDecision, final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
                instanceOf(ApplicationDecisionSetAside.class),
                hasProperty("applicationId", equalTo(saveApplicationDecision.getApplicationId())),
                hasProperty("caseId", equalTo(caseAggregateState.getCaseId()))
        )));
    }

    private void thenApplicationStatusIsChanged(final ApplicationStatus newStatus, final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
                instanceOf(ApplicationStatusChanged.class),
                hasProperty("applicationId", equalTo(currentApplication.getApplicationId())),
                hasProperty("status", equalTo(newStatus))
        )));
    }

    private void thenTheDecisionIsRejected(final SaveApplicationDecision decisionCommand,
                                           final List<String> expectedRejectionReasons,
                                           final Stream<Object> eventStream) {
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(1));

        final ApplicationDecisionRejected applicationDecisionRejected = applicationDecisionRejected()
                .withApplicationId(decisionCommand.getApplicationId())
                .withCaseId(decisionCommand.getCaseId())
                .withSessionId(decisionCommand.getSessionId())
                .withApplicationDecision(applicationDecision()
                        .withGranted(decisionCommand.getGranted())
                        .withOutOfTime(decisionCommand.getOutOfTime())
                        .withRejectionReason(decisionCommand.getRejectionReason())
                        .withOutOfTimeReason(decisionCommand.getOutOfTimeReason())
                        .build())
                .withRejectionReasons(expectedRejectionReasons)
                .build();

        assertThat(eventList, hasItem(applicationDecisionRejected));
    }

}
