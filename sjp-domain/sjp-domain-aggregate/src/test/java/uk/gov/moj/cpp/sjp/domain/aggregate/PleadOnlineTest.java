package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.plea.EmploymentStatus.EMPLOYED;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.CaseAssignment;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.PleaType;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.OffenceNotFound;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.TrialRequested;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;

public class PleadOnlineTest {

    private CaseAggregate caseAggregate = new CaseAggregate();
    private static final UUID caseId = UUID.randomUUID();
    private static final String urn = "TFL123456";
    private static final String INITIATION_CODE = "J";

    private static final UUID offenceId = UUID.randomUUID();

    private Clock clock = new StoppedClock(ZonedDateTime.now());
    private ZonedDateTime now = clock.now();

    private void assertPleaExpectationsAndCommonExpectations(final PleadOnline pleadOnline, final String defendantId, final PleaType pleaType,
                                                             final PleaUpdated pleaUpdated, final FinancialMeansUpdated financialMeansUpdated, final EmployerUpdated employerUpdated,
                                                             final EmploymentStatusUpdated employmentStatusUpdated, final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant,
                                                             final TrialRequested trialRequested, final ZonedDateTime createDate) {

        assertThat(caseId.toString(), equalTo(pleaUpdated.getCaseId()));
        assertThat(PleaMethod.ONLINE, equalTo(pleaUpdated.getPleaMethod()));
        assertThat(pleaType.toString(), equalTo(pleaUpdated.getPlea()));
        assertThat(pleadOnline.getOffences().get(0).getId(), equalTo(pleaUpdated.getOffenceId()));
        assertThat(pleadOnline.getOffences().get(0).getMitigation(), equalTo(pleaUpdated.getMitigation()));
        assertThat(pleadOnline.getOffences().get(0).getNotGuiltyBecause(), equalTo(pleaUpdated.getNotGuiltyBecause()));

        assertCommonExpectations(pleadOnline, defendantId, financialMeansUpdated, employerUpdated, employmentStatusUpdated,
                interpreterUpdatedForDefendant, trialRequested, createDate);
    }

    public void assertCommonExpectations(final PleadOnline pleadOnline, final String defendantId, final FinancialMeansUpdated financialMeansUpdated,
                                         final EmployerUpdated employerUpdated, final EmploymentStatusUpdated employmentStatusUpdated,
                                         final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant, final TrialRequested trialRequested,
                                         final ZonedDateTime createDate) {
        assertThat(UUID.fromString(defendantId), equalTo(financialMeansUpdated.getDefendantId()));
        assertThat(pleadOnline.getFinancialMeans().getIncome(), equalTo(financialMeansUpdated.getIncome()));
        assertThat(pleadOnline.getFinancialMeans().getBenefits(), equalTo(financialMeansUpdated.getBenefits()));
        assertThat(pleadOnline.getFinancialMeans().getEmploymentStatus(), equalTo(financialMeansUpdated.getEmploymentStatus()));
        assertThat(pleadOnline.getOutgoings().size(), equalTo(financialMeansUpdated.getOutgoings().size()));
        assertThat(createDate, equalTo(financialMeansUpdated.getUpdatedDate()));
        assertTrue(financialMeansUpdated.isUpdatedByOnlinePlea());

        assertThat(UUID.fromString(defendantId), equalTo(employerUpdated.getDefendantId()));
        assertThat(pleadOnline.getEmployer().getName(), equalTo(employerUpdated.getName()));
        assertThat(pleadOnline.getEmployer().getEmployeeReference(), equalTo(employerUpdated.getEmployeeReference()));
        assertThat(pleadOnline.getEmployer().getPhone(), equalTo(employerUpdated.getPhone()));
        assertThat(pleadOnline.getEmployer().getAddress(), equalTo(employerUpdated.getAddress()));
        assertThat(createDate, equalTo(employerUpdated.getUpdatedDate()));
        assertTrue(employerUpdated.isUpdatedByOnlinePlea());

        if (employmentStatusUpdated != null) {
            assertThat(UUID.fromString(defendantId), equalTo(employmentStatusUpdated.getDefendantId()));
            assertThat(EMPLOYED.name(), equalTo(employmentStatusUpdated.getEmploymentStatus()));
        }

        if (interpreterUpdatedForDefendant != null) {
            assertThat(caseId, equalTo(interpreterUpdatedForDefendant.getCaseId()));
            assertThat(UUID.fromString(defendantId), equalTo(interpreterUpdatedForDefendant.getDefendantId()));
            assertThat(pleadOnline.getInterpreterLanguage(), equalTo(interpreterUpdatedForDefendant.getInterpreter().getLanguage()));
            assertThat(true, equalTo(interpreterUpdatedForDefendant.getInterpreter().getNeeded()));
            assertThat(createDate, equalTo(interpreterUpdatedForDefendant.getUpdatedDate()));
            assertTrue(interpreterUpdatedForDefendant.isUpdatedByOnlinePlea());
        }
        if (trialRequested != null) {
            assertThat(caseId, equalTo(trialRequested.getCaseId()));
            assertThat(pleadOnline.getUnavailability(), equalTo(trialRequested.getUnavailability()));
            assertThat(pleadOnline.getWitnessDetails(), equalTo(trialRequested.getWitnessDetails()));
            assertThat(pleadOnline.getWitnessDispute(), equalTo(trialRequested.getWitnessDispute()));
            assertThat(createDate, equalTo(trialRequested.getUpdatedDate()));
        }
    }

    @Test
    public void shoulPleaOnlineSuccessfullyForGuiltyPlea() {
        //given
        final ZonedDateTime now = clock.now();
        final CaseReceived sjpCase = caseAggregate.receiveCase(createTestCase(), clock.now())
                .map(c -> (CaseReceived) c)
                .collect(toList()).get(0);

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, sjpCase.getDefendant().getId().toString());
        final Stream<Object> eventStream = caseAggregate.pleaOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(4));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, sjpCase.getDefendant().getId().toString(), PleaType.GUILTY, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(1),
                (EmployerUpdated) events.get(2), (EmploymentStatusUpdated) events.get(3), null, null, now);
    }

    @Test
    public void shoulPleaOnlineSuccessfullyForGuiltyRequestHearingPlea() {
        //given
        final CaseReceived sjpCase = caseAggregate.receiveCase(createTestCase(), clock.now())
                .map(c -> (CaseReceived) c)
                .collect(toList()).get(0);
        final String interpreterLanguage = "French";

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyRequestHearingPlea(offenceId,
                sjpCase.getDefendant().getId().toString(), interpreterLanguage);
        final Stream<Object> eventStream = caseAggregate.pleaOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(5));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has InterpreterUpdatedForDefendant event", events, hasItem(isA(InterpreterUpdatedForDefendant.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, sjpCase.getDefendant().getId().toString(), PleaType.GUILTY_REQUEST_HEARING, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(1),
                (EmployerUpdated) events.get(2), (EmploymentStatusUpdated) events.get(3), (InterpreterUpdatedForDefendant) events.get(4), null, now);
    }

    @Test
    public void shoulPleaOnlineSuccessfullyForGuiltyRequestHearingPleaWithoutInterpreterLanguage() {
        //given
        final CaseReceived sjpCase = caseAggregate.receiveCase(createTestCase(), clock.now())
                .map(c -> (CaseReceived) c)
                .collect(toList()).get(0);
        final String interpreterLanguage = null;

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyRequestHearingPlea(offenceId,
                sjpCase.getDefendant().getId().toString(), interpreterLanguage);
        final Stream<Object> eventStream = caseAggregate.pleaOnline(caseId, pleadOnline, now);

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(4));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, sjpCase.getDefendant().getId().toString(), PleaType.GUILTY_REQUEST_HEARING, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(1),
                (EmployerUpdated) events.get(2), (EmploymentStatusUpdated) events.get(3), null, null, now);
    }

    @Test
    public void shoulPleaOnlineSuccessfullyForNotGuiltyPlea() {
        //given
        final CaseReceived sjpCase = caseAggregate.receiveCase(createTestCase(), clock.now())
                .map(c -> (CaseReceived) c)
                .collect(toList()).get(0);
        final String interpreterLanguage = "French";
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithNotGuiltyPlea(offenceId,
                sjpCase.getDefendant().getId().toString(), interpreterLanguage, true);
        final Stream<Object> eventStream = caseAggregate.pleaOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(6));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has InterpreterUpdatedForDefendant event", events, hasItem(isA(InterpreterUpdatedForDefendant.class)));
        assertThat("Has TrialRequested event", events, hasItem(isA(TrialRequested.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, sjpCase.getDefendant().getId().toString(), PleaType.NOT_GUILTY, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(2),
                (EmployerUpdated) events.get(3), (EmploymentStatusUpdated) events.get(4), (InterpreterUpdatedForDefendant) events.get(5), (TrialRequested) events.get(1), now);
    }

    @Test
    public void shoulPleaOnlineSuccessfullyForNotGuiltyPleaWithoutTrialRequestedEvent() {
        //given
        final CaseReceived sjpCase = caseAggregate.receiveCase(createTestCase(), clock.now())
                .map(c -> (CaseReceived) c)
                .collect(toList()).get(0);
        final String interpreterLanguage = "French";

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithNotGuiltyPlea(offenceId,
                sjpCase.getDefendant().getId().toString(), interpreterLanguage, false);
        final Stream<Object> eventStream = caseAggregate.pleaOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(5));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has InterpreterUpdatedForDefendant event", events, hasItem(isA(InterpreterUpdatedForDefendant.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, sjpCase.getDefendant().getId().toString(), PleaType.NOT_GUILTY, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(1),
                (EmployerUpdated) events.get(2), (EmploymentStatusUpdated) events.get(3), (InterpreterUpdatedForDefendant) events.get(4), null, now);
    }

    @Test
    public void shoulPleaOnlineSuccessfullyForMultipleOffences() {
        //given
        final Object[][] pleaInformationArray = {
                { offenceId, PleaType.NOT_GUILTY, true, PleaType.NOT_GUILTY },
                { UUID.randomUUID(), PleaType.GUILTY, false, PleaType.GUILTY },
                { UUID.randomUUID(), PleaType.GUILTY, true, PleaType.GUILTY_REQUEST_HEARING },
                { UUID.randomUUID(), PleaType.NOT_GUILTY, true, PleaType.NOT_GUILTY },
        };
        final Case caseWithMultipleOffences = createTestCaseWithExtraOffences(
                Arrays.stream(pleaInformationArray)
                        .filter(pleaInformation -> !pleaInformation[0].equals(offenceId))
                        .map(pleaInformation -> (UUID) pleaInformation[0]).collect(toList())
        );
        final CaseReceived sjpCase = caseAggregate.receiveCase(caseWithMultipleOffences, clock.now())
                .map(c -> (CaseReceived) c)
                .collect(toList()).get(0);
        final String interpreterLanguage = "French";

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaForMultipleOffences(pleaInformationArray,
                sjpCase.getDefendant().getId().toString(), interpreterLanguage);
        final Stream<Object> eventStream = caseAggregate.pleaOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(9));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has InterpreterUpdatedForDefendant event", events, hasItem(isA(InterpreterUpdatedForDefendant.class)));
        assertThat("Has TrialRequested event", events, hasItem(isA(TrialRequested.class)));

        //asserts expectations for all pleas
        IntStream.range(0, pleaInformationArray.length).forEach(index -> {
            PleaUpdated pleaUpdated = (PleaUpdated) events.get(index);
            PleaType expectedPleaType = (PleaType) pleaInformationArray[index][3];

            assertThat(caseId.toString(), equalTo(pleaUpdated.getCaseId()));
            assertThat(PleaMethod.ONLINE, equalTo(pleaUpdated.getPleaMethod()));
            assertThat(expectedPleaType.toString(), equalTo(pleaUpdated.getPlea()));
            assertThat(pleadOnline.getOffences().get(index).getId(), equalTo(pleaUpdated.getOffenceId()));
            assertThat(pleadOnline.getOffences().get(index).getMitigation(), equalTo(pleaUpdated.getMitigation()));
            assertThat(pleadOnline.getOffences().get(index).getNotGuiltyBecause(), equalTo(pleaUpdated.getNotGuiltyBecause()));
        });
        assertCommonExpectations(pleadOnline, sjpCase.getDefendant().getId().toString(), (FinancialMeansUpdated) events.get(5),
                (EmployerUpdated) events.get(6), (EmploymentStatusUpdated) events.get(7), (InterpreterUpdatedForDefendant) events.get(8), (TrialRequested) events.get(4), now);
    }

    @Test
    public void shouldStoreOnlinePleaAndFailToStoreOnlinePleaBasedOnWhetherPleaSubmittedBeforeOrPleaCancelled() {
        //given
        final CaseReceived sjpCase = caseAggregate.receiveCase(createTestCase(), clock.now())
                .map(c -> (CaseReceived) c)
                .collect(toList()).get(0);

        //when plea
        PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, sjpCase.getDefendant().getId().toString());
        Stream<Object> eventStream = caseAggregate.pleaOnline(caseId, pleadOnline, now);
        List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(4));

        //then successful
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, sjpCase.getDefendant().getId().toString(), PleaType.GUILTY, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(1),
                (EmployerUpdated) events.get(2), (EmploymentStatusUpdated) events.get(3), null, null, now);

        //then plea second time
        eventStream = caseAggregate.pleaOnline(caseId, pleadOnline, now);
        events = asList(eventStream.toArray());
        assertThat(events, hasSize(1));
        assertThat("Has CaseUpdateRejected event", events, hasItem(isA(CaseUpdateRejected.class)));

        //then rejected
        final CaseUpdateRejected caseUpdateRejected = (CaseUpdateRejected) events.get(0);
        assertThat(caseId, equalTo(caseUpdateRejected.getCaseId()));
        assertThat(CaseUpdateRejected.RejectReason.PLEA_ALREADY_SUBMITTED, equalTo(caseUpdateRejected.getReason()));

        //then cancel plea
        caseAggregate.cancelPlea(new CancelPlea(caseId, offenceId));

        //then plea again
        pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, sjpCase.getDefendant().getId().toString());
        eventStream = caseAggregate.pleaOnline(caseId, pleadOnline, now);
        events = asList(eventStream.toArray());

        //then successful
        assertThat(events, hasSize(3));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, sjpCase.getDefendant().getId().toString(), PleaType.GUILTY, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(1),
                (EmployerUpdated) events.get(2), null, null, null, now);
    }

    @Test
    public void shouldNotStoreOnlinePleaWhenCaseAssigned() {
        //given
        final CaseReceived sjpCase = caseAggregate.receiveCase(createTestCase(), clock.now())
                .map(c -> (CaseReceived) c)
                .collect(toList()).get(0);
        caseAggregate.caseAssignmentCreated(new CaseAssignment(caseId.toString(), "for-magistrate-decision"));

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, sjpCase.getDefendant().getId().toString());
        final Stream<Object> eventStream = caseAggregate.pleaOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat("Has CaseUpdateRejected event", events, hasItem(isA(CaseUpdateRejected.class)));
        assertThat(((CaseUpdateRejected) events.get(0)).getReason(),
                is(CaseUpdateRejected.RejectReason.CASE_ASSIGNED));
    }

    @Test
    public void shouldNotStoreOnlinePleaWhenWithdrawalOffencesRequested() {
        //given
        final CaseReceived sjpCase = caseAggregate.receiveCase(createTestCase(), clock.now())
                .map(c -> (CaseReceived) c)
                .collect(toList()).get(0);
        caseAggregate.requestWithdrawalAllOffences(caseId.toString());

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, sjpCase.getDefendant().getId().toString());
        final Stream<Object> eventStream = caseAggregate.pleaOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat("Has CaseUpdateRejected event", events, hasItem(isA(CaseUpdateRejected.class)));
        assertThat(((CaseUpdateRejected) events.get(0)).getReason(),
                        is(CaseUpdateRejected.RejectReason.WITHDRAWAL_PENDING));
    }

    @Test
    public void shouldStoreOnlinePleaWhenWithdrawalOffencesRequestCancelled() {
        //given
        final CaseReceived sjpCase = caseAggregate.receiveCase(createTestCase(), clock.now())
                .map(c -> (CaseReceived) c)
                .collect(toList()).get(0);
        caseAggregate.requestWithdrawalAllOffences(caseId.toString());
        caseAggregate.cancelRequestWithdrawalAllOffences(caseId.toString());

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, sjpCase.getDefendant().getId().toString());
        final Stream<Object> eventStream = caseAggregate.pleaOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
    }

    @Test
    public void shouldNotStoreOnlinePleaWhenOffenceDoesNotExist() {
        final CaseReceived sjpCase = caseAggregate.receiveCase(createTestCase(), clock.now())
                .map(c -> (CaseReceived) c)
                .collect(toList()).get(0);

        final UUID offenceId = UUID.randomUUID();
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, sjpCase.getDefendant().getId().toString());
        final List<Object> events = caseAggregate.pleaOnline(caseId, pleadOnline, now).collect(toList());

        assertThat(events.size(), is(1));

        final Object object = events.get(0);
        assertThat(object.getClass() , is(CoreMatchers.equalTo(OffenceNotFound.class)));
    }

    @Test
    public void shouldNotStoreOnlinePleaWhenDefendantIncorrect() {
        caseAggregate.receiveCase(createTestCase(), clock.now());

        final String defendantId = UUID.randomUUID().toString();
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final List<Object> events = caseAggregate.pleaOnline(caseId, pleadOnline, now).collect(toList());

        assertThat(events.size(), is(1));

        final Object object = events.get(0);
        assertThat(object.getClass() , is(CoreMatchers.equalTo(DefendantNotFound.class)));
    }

    private Case createTestCase() {
        final Offence offence = new Offence(
                offenceId,
                1, null, null, 1, null, null, null, null, null
        );

        return new Case(
                caseId,
                urn,
                null, null, INITIATION_CODE, null, null, null, null,
                null, null, null,
                new Defendant(UUID.randomUUID(), null, null, null, null, null, null, 1, new ArrayList<Offence>() {{ add(offence); }})
        );
    }

    private Case createTestCaseWithExtraOffences(List<UUID> offenceIds) {
        final Case caseWithExtraOffences = createTestCase();
        offenceIds.stream().forEach(offenceId -> {
            Offence sjpOffence = new Offence(offenceId,
                    1, null, null, 1, null, null, null, null, null
            );
            caseWithExtraOffences.getDefendant().getOffences().add(sjpOffence);
        });
        return caseWithExtraOffences;
    }
}
