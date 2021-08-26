package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.sjp.domain.DomainConstants.NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID;
import static uk.gov.moj.cpp.sjp.domain.plea.EmploymentStatus.EMPLOYED;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_ADDRESS;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_CONTACT_DETAILS;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_DOB;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_DRIVER_NUMBER;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_FIRST_NAME;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_LAST_NAME;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_NI_NUMBER;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.WithdrawalRequestsStatus;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.PersonalName;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.DefendantBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder;
import uk.gov.moj.cpp.sjp.event.CaseExpectedDateReadyChanged;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidRequired;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.OffenceNotFound;
import uk.gov.moj.cpp.sjp.event.OnlinePleaReceived;
import uk.gov.moj.cpp.sjp.event.OutstandingFinesUpdated;
import uk.gov.moj.cpp.sjp.event.PleadedGuilty;
import uk.gov.moj.cpp.sjp.event.PleadedGuiltyCourtHearingRequested;
import uk.gov.moj.cpp.sjp.event.PleadedNotGuilty;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.cpp.sjp.event.TrialRequested;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class PleadOnlineTest {

    private CaseAggregate caseAggregate;

    private final ZonedDateTime now = ZonedDateTime.now();

    private UUID caseId;
    private UUID defendantId;
    private UUID offenceId;

    private static final UUID userId = randomUUID();

    @Before
    public void setup() {
        // single offence case
        setup(createTestCase(null));
    }

    private void setup(final Case testCase) {
        caseAggregate = new CaseAggregate();
        final CaseReceived sjpCase = caseAggregate.receiveCase(testCase, ZonedDateTime.now())
                .map(CaseReceived.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Case not Received " + testCase.getId()));
        caseId = sjpCase.getCaseId();
        defendantId = sjpCase.getDefendant().getId();
        offenceId = sjpCase.getDefendant().getOffences().get(0).getId();
    }

    @Test
    public void shouldPleadOnlineSuccessfullyForGuiltyPlea() {
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID());
        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleasSet.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                PleadedGuilty.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OutstandingFinesUpdated.class,
                OnlinePleaReceived.class,
                CaseMarkedReadyForDecision.class,
                CaseStatusChanged.class
        ));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldPleaOnlineSuccessfullyForGuiltyRequestHearingPlea() {
        //given
        final String interpreterLanguage = "French";
        final Boolean speakWelsh = FALSE;

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyRequestHearingPlea(offenceId,
                defendantId, interpreterLanguage, speakWelsh);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID());

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleasSet.class,
                InterpreterUpdatedForDefendant.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                PleadedGuiltyCourtHearingRequested.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OutstandingFinesUpdated.class,
                OnlinePleaReceived.class,
                CaseMarkedReadyForDecision.class,
                CaseStatusChanged.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldPleaOnlineSuccessfullyForGuiltyRequestHearingPleaWithoutInterpreterLanguage() {
        //given
        final String interpreterLanguage = null;
        final Boolean speakWelsh = null;

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyRequestHearingPlea(offenceId,
                defendantId, interpreterLanguage, speakWelsh);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID());

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleasSet.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                PleadedGuiltyCourtHearingRequested.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OutstandingFinesUpdated.class,
                OnlinePleaReceived.class,
                CaseMarkedReadyForDecision.class,
                CaseStatusChanged.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldPleaOnlineSuccessfullyForNotGuiltyPlea() {
        //given
        final String interpreterLanguage = "French";
        final Boolean speakWelsh = TRUE;

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithNotGuiltyPlea(offenceId,
                defendantId, interpreterLanguage, speakWelsh, true);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID());

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleasSet.class,
                InterpreterUpdatedForDefendant.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                PleadedNotGuilty.class,
                DatesToAvoidRequired.class,
                TrialRequested.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OutstandingFinesUpdated.class,
                OnlinePleaReceived.class,
                CaseExpectedDateReadyChanged.class,
                CaseStatusChanged.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldPleaOnlineSuccessfullyForNotGuiltyPleaWithoutTrialRequestedEvent() {
        //given
        final String interpreterLanguage = "French";
        final Boolean speakWelsh = TRUE;

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithNotGuiltyPlea(offenceId,
                defendantId, interpreterLanguage, speakWelsh, false);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID());

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleasSet.class,
                InterpreterUpdatedForDefendant.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                PleadedNotGuilty.class,
                DatesToAvoidRequired.class,
                TrialRequested.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OutstandingFinesUpdated.class,
                OnlinePleaReceived.class,
                CaseExpectedDateReadyChanged.class,
                CaseStatusChanged.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldPleaOnlineSuccessfullyForDefendantWithTitleAndMultipleOffences() {
        //given
        final Object[][] pleaInformationArray = {
                {randomUUID(), PleaType.NOT_GUILTY, PleaType.NOT_GUILTY},
                {randomUUID(), PleaType.GUILTY, PleaType.GUILTY_REQUEST_HEARING},
                {randomUUID(), PleaType.GUILTY, PleaType.GUILTY_REQUEST_HEARING},
                {randomUUID(), PleaType.NOT_GUILTY, PleaType.NOT_GUILTY},
        };
        final UUID[] extraOffenceIds = Arrays.stream(pleaInformationArray).map(pleaInformation ->
                (UUID) pleaInformation[0]).toArray(UUID[]::new);

        final Case testCase = createTestCase("Mr", extraOffenceIds);
        setup(testCase); // Override the @Before

        final String interpreterLanguage = "French";
        final Boolean speakWelsh = TRUE;

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaForMultipleOffences(
                pleaInformationArray, defendantId, interpreterLanguage, speakWelsh, true);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID());

        //then
        final List<Object> events = asList(eventStream.toArray());

        assertThat(events, containsEventsOf(
                PleasSet.class,
                InterpreterUpdatedForDefendant.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                PleadedNotGuilty.class,
                PleadedGuiltyCourtHearingRequested.class,
                PleadedGuiltyCourtHearingRequested.class,
                PleadedNotGuilty.class,
                DatesToAvoidRequired.class,
                TrialRequested.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OutstandingFinesUpdated.class,
                OnlinePleaReceived.class,
                CaseMarkedReadyForDecision.class,
                CaseStatusChanged.class));

        //asserts expectations for all pleas
        IntStream.range(0, pleaInformationArray.length).forEach(index -> {
            if (events.get(index) instanceof PleadedNotGuilty) {
                PleadedNotGuilty pleadedNotGuilty = (PleadedNotGuilty) events.get(index);
                assertThat(caseId, equalTo(pleadedNotGuilty.getCaseId()));
                assertThat(PleaMethod.ONLINE, equalTo(pleadedNotGuilty.getMethod()));
                Optional<uk.gov.moj.cpp.sjp.domain.onlineplea.Offence> actualOffence = pleadOnline.getOffences().stream().filter(offence -> offence.getId().equals(pleadedNotGuilty.getOffenceId())).findAny();
                assertThat(actualOffence.isPresent(), equalTo(TRUE));
                assertThat(actualOffence.get().getNotGuiltyBecause(), equalTo(pleadedNotGuilty.getNotGuiltyBecause()));

            } else if (events.get(index) instanceof PleadedGuilty) {
                PleadedGuilty pleadedGuilty = (PleadedGuilty) events.get(index);
                assertThat(caseId, equalTo(pleadedGuilty.getCaseId()));
                assertThat(PleaMethod.ONLINE, equalTo(pleadedGuilty.getMethod()));
                assertThat(pleadOnline.getOffences().get(index).getId(), equalTo(pleadedGuilty.getOffenceId()));
                assertThat(pleadOnline.getOffences().get(index).getMitigation(), equalTo(pleadedGuilty.getMitigation()));

            } else if (events.get(index) instanceof PleadedGuiltyCourtHearingRequested) {
                PleadedGuiltyCourtHearingRequested pleadedGuiltyCourtHearingRequested = (PleadedGuiltyCourtHearingRequested) events.get(index);
                assertThat(caseId, equalTo(pleadedGuiltyCourtHearingRequested.getCaseId()));
                assertThat(PleaMethod.ONLINE, equalTo(pleadedGuiltyCourtHearingRequested.getMethod()));
                assertThat(pleadOnline.getOffences().get(index).getId(), equalTo(pleadedGuiltyCourtHearingRequested.getOffenceId()));
                assertThat(pleadOnline.getOffences().get(index).getMitigation(), equalTo(pleadedGuiltyCourtHearingRequested.getMitigation()));
            }
        });

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldNotStoreOnlinePleaWhenCaseAssigned() {
        //given
        caseAggregate.assignCase(userId, ZonedDateTime.now(), CaseAssignmentType.MAGISTRATE_DECISION);

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID());

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(CaseUpdateRejected.class));

        assertThat(((CaseUpdateRejected) events.get(0)).getReason(),
                is(CaseUpdateRejected.RejectReason.CASE_ASSIGNED));
    }

    @Test
    public void shouldStoreOnlinePleaWhenWithdrawalOffencesRequested() {
        //given
        final UUID userId = randomUUID();
        final UUID withdrawalRequestReasonId = randomUUID();
        caseAggregate.requestForOffenceWithdrawal(caseId, userId, ZonedDateTime.now(),
                singletonList(new WithdrawalRequestsStatus(offenceId, withdrawalRequestReasonId)), "ALL");

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID());

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleasSet.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                PleadedGuilty.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OutstandingFinesUpdated.class,
                OnlinePleaReceived.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldStoreOnlinePleaWhenWithdrawalOffencesRequestCancelled() {
        //given
        final UUID userId = randomUUID();
        final UUID withdrawalRandomId = randomUUID();
        caseAggregate.requestForOffenceWithdrawal(caseId, userId, ZonedDateTime.now(),
                singletonList(new WithdrawalRequestsStatus(offenceId, withdrawalRandomId)), "ALL");
        //cancel withdrawal request
        caseAggregate.requestForOffenceWithdrawal(caseId, userId, ZonedDateTime.now(), emptyList(), "ALL");

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID());

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleasSet.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                PleadedGuilty.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OutstandingFinesUpdated.class,
                OnlinePleaReceived.class,
                CaseMarkedReadyForDecision.class,
                CaseStatusChanged.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldNotStoreOnlinePleaWhenOffenceDoesNotExist() {
        //given
        final UUID offenceId = randomUUID();

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID()).collect(toList());

        //then
        assertThat(events, containsEventsOf(OffenceNotFound.class));
    }

    @Test
    public void shouldNotStoreOnlinePleaWhenDefendantIncorrect() {
        //given
        final UUID defendantId = randomUUID();

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID()).collect(toList());

        //then
        assertThat(events, containsEventsOf(DefendantNotFound.class));
    }

    @Test
    public void shouldWarnNameChanged() {
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId, true, false, false);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID()).collect(toList());

        //then
        assertThat(events, containsEventsOf(
                PleasSet.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                PleadedGuilty.class,
                DefendantDetailsUpdated.class,
                DefendantPersonalNameUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OutstandingFinesUpdated.class,
                OnlinePleaReceived.class,
                CaseMarkedReadyForDecision.class,
                CaseStatusChanged.class));
        final DefendantDetailsUpdated defendantDetailsUpdated = getDefendantDetailsUpdatedEvent(events);

        assertCommonExpectations(pleadOnline, events, now);
        assertEquals("Norman", defendantDetailsUpdated.getFirstName());
        assertNull(defendantDetailsUpdated.getLastName());
        assertNull(defendantDetailsUpdated.getDateOfBirth());
        assertNull(defendantDetailsUpdated.getAddress());
    }

    @Test
    public void shouldNotRaiseDefendantDetailsUpdatedIfNoPersonalDetailsChange() {
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId, false, false, false);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID()).collect(toList());

        //then
        assertThat(events, containsEventsOf(
                PleasSet.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                PleadedGuilty.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OutstandingFinesUpdated.class,
                OnlinePleaReceived.class,
                CaseMarkedReadyForDecision.class,
                CaseStatusChanged.class));

        assertThat(events, not(containsEventsOf(DefendantDetailsUpdated.class)));

        assertCommonExpectations(pleadOnline, events, now);
    }

    private DefendantDetailsUpdated getDefendantDetailsUpdatedEvent(final List<Object> events) {
        return events.stream()
                .filter(event -> event instanceof DefendantDetailsUpdated)
                .map(event -> (DefendantDetailsUpdated) event)
                .findFirst()
                .get();
    }

    @Test
    public void shouldWarnAddressChanged() {
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId, false, true, false);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID()).collect(toList());

        //then
        assertThat(events, containsEventsOf(
                PleasSet.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                PleadedGuilty.class,
                DefendantDetailsUpdated.class,
                DefendantAddressUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OutstandingFinesUpdated.class,
                OnlinePleaReceived.class,
                CaseMarkedReadyForDecision.class,
                CaseStatusChanged.class));
        final DefendantDetailsUpdated defendantDetailsUpdated = getDefendantDetailsUpdatedEvent(events);

        assertCommonExpectations(pleadOnline, events, now);
        assertEquals(new Address("1 New Amsterdam Rd", "Tulse Hill", "Brixton", "London", "United Kingdom", "SE249HG"),
                defendantDetailsUpdated.getAddress());
        assertNull(defendantDetailsUpdated.getFirstName());
        assertNull(defendantDetailsUpdated.getLastName());
        assertNull(defendantDetailsUpdated.getDateOfBirth());
        assertNull(defendantDetailsUpdated.getNationalInsuranceNumber());
    }

    @Test
    public void shouldWarnDobChanged() {
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId, false, false, true);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now, randomUUID()).collect(toList());

        //then
        assertThat(events, containsEventsOf(
                PleasSet.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                PleadedGuilty.class,
                DefendantDetailsUpdated.class,
                DefendantDateOfBirthUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OutstandingFinesUpdated.class,
                OnlinePleaReceived.class,
                CaseMarkedReadyForDecision.class,
                CaseStatusChanged.class));

        assertCommonExpectations(pleadOnline, events, now);
        final DefendantDetailsUpdated defendantDetailsUpdated = getDefendantDetailsUpdatedEvent(events);

        assertEquals(LocalDate.now().minusYears(18), defendantDetailsUpdated.getDateOfBirth());
        assertNull(defendantDetailsUpdated.getFirstName());
        assertNull(defendantDetailsUpdated.getLastName());
        assertNull(defendantDetailsUpdated.getAddress());
    }

    private void assertCommonExpectations(final PleadOnline pleadOnline,
                                          final List<Object> events,
                                          final ZonedDateTime createDate) {
        // in PleadOnline inner objects the defendantIDs are null
        assertThat(pleadOnline.getDefendantId(), equalTo(defendantId));
        assertThat(pleadOnline.getFinancialMeans().getDefendantId(), nullValue());
        assertThat(pleadOnline.getEmployer().getDefendantId(), nullValue());

        //boolean updateByOnlinePlea = PleaMethod.ONLINE.equals(pleaUpdated.getPleaMethod());
        boolean updateByOnlinePlea = true;

        assertThat(events, not(emptyCollectionOf(Object.class)));
        events.forEach(e -> {
            if (e instanceof PleasSet) {

                PleasSet pleasSet = (PleasSet) e;
                assertThat(pleasSet.getCaseId(), equalTo(caseId));
                assertThat(pleasSet.getPleas().isEmpty(), is(FALSE));

            } else if (e instanceof PleadedGuiltyCourtHearingRequested) {
                PleadedGuiltyCourtHearingRequested pleadedGuiltyCourtHearingRequested = (PleadedGuiltyCourtHearingRequested) e;
                assertThat(pleadedGuiltyCourtHearingRequested.getCaseId(), equalTo(caseId));
                assertThat(pleadedGuiltyCourtHearingRequested.getMethod(), equalTo(pleadedGuiltyCourtHearingRequested.getMethod()));
                assertThat(pleadedGuiltyCourtHearingRequested.getPleadDate(), equalTo(createDate));

                final Optional<uk.gov.moj.cpp.sjp.domain.onlineplea.Offence> firstOffence = pleadOnline.getOffences().stream().filter(o -> o.getId().equals(pleadedGuiltyCourtHearingRequested.getOffenceId())).findFirst();
                assertThat(firstOffence.isPresent(), is(TRUE));
                assertThat(firstOffence.get().getMitigation(), equalTo(pleadedGuiltyCourtHearingRequested.getMitigation()));

            } else if (e instanceof PleadedGuilty) {
                PleadedGuilty pleadedGuilty = (PleadedGuilty) e;
                assertThat(pleadedGuilty.getCaseId(), equalTo(caseId));
                assertThat(pleadedGuilty.getMethod(), equalTo(PleaMethod.ONLINE));
                assertThat(pleadedGuilty.getPleadDate(), equalTo(createDate));

                final Optional<uk.gov.moj.cpp.sjp.domain.onlineplea.Offence> firstOffence = pleadOnline.getOffences().stream().filter(o -> o.getId().equals(pleadedGuilty.getOffenceId())).findFirst();
                assertThat(firstOffence.isPresent(), is(TRUE));
                assertThat(firstOffence.get().getMitigation(), equalTo(pleadedGuilty.getMitigation()));

            } else if (e instanceof PleadedNotGuilty) {
                PleadedNotGuilty pleadedNotGuilty = (PleadedNotGuilty) e;
                assertThat(pleadedNotGuilty.getCaseId(), equalTo(caseId));
                assertThat(pleadedNotGuilty.getMethod(), equalTo(PleaMethod.ONLINE));
                assertThat(pleadedNotGuilty.getPleadDate(), equalTo(createDate));
                final Optional<uk.gov.moj.cpp.sjp.domain.onlineplea.Offence> firstOffence = pleadOnline.getOffences().stream().filter(o -> o.getId().equals(pleadedNotGuilty.getOffenceId())).findFirst();
                assertThat(firstOffence.isPresent(), is(TRUE));
                assertThat(firstOffence.get().getNotGuiltyBecause(), equalTo(pleadedNotGuilty.getNotGuiltyBecause()));
            } else if (e instanceof FinancialMeansUpdated) {
                final FinancialMeansUpdated financialMeansUpdated = (FinancialMeansUpdated) e;
                assertThat(financialMeansUpdated.getDefendantId(), equalTo(defendantId));
                assertThat(financialMeansUpdated.getIncome(), equalTo(pleadOnline.getFinancialMeans().getIncome()));
                assertThat(financialMeansUpdated.getBenefits(), equalTo(pleadOnline.getFinancialMeans().getBenefits()));
                assertThat(financialMeansUpdated.getEmploymentStatus(), equalTo(pleadOnline.getFinancialMeans().getEmploymentStatus()));
                assertThat(financialMeansUpdated.getOutgoings(), hasSize(pleadOnline.getOutgoings().size()));
                assertThat(financialMeansUpdated.getUpdatedDate(), equalTo(createDate));
                assertThat(financialMeansUpdated.isUpdatedByOnlinePlea(), equalTo(updateByOnlinePlea));
                assertTrue(financialMeansUpdated.isUpdatedByOnlinePlea());
            } else if (e instanceof EmployerUpdated) {
                final EmployerUpdated employerUpdated = (EmployerUpdated) e;

                assertThat(employerUpdated.getDefendantId(), equalTo(defendantId));
                assertThat(employerUpdated.getName(), equalTo(pleadOnline.getEmployer().getName()));
                assertThat(employerUpdated.getEmployeeReference(), equalTo(pleadOnline.getEmployer().getEmployeeReference()));
                assertThat(employerUpdated.getPhone(), equalTo(pleadOnline.getEmployer().getPhone()));
                assertThat(employerUpdated.getAddress(), equalTo(pleadOnline.getEmployer().getAddress()));
                assertThat(employerUpdated.getUpdatedDate(), equalTo(createDate));
                assertThat(employerUpdated.isUpdatedByOnlinePlea(), equalTo(updateByOnlinePlea));
                assertTrue(employerUpdated.isUpdatedByOnlinePlea());
            } else if (e instanceof EmploymentStatusUpdated) {
                final EmploymentStatusUpdated employmentStatusUpdated = (EmploymentStatusUpdated) e;
                assertThat(employmentStatusUpdated.getDefendantId(), equalTo(defendantId));
                assertThat(employmentStatusUpdated.getEmploymentStatus(), equalTo(EMPLOYED.name()));
            } else if (e instanceof InterpreterUpdatedForDefendant) {
                final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant = (InterpreterUpdatedForDefendant) e;

                assertThat(interpreterUpdatedForDefendant.getCaseId(), equalTo(caseId));
                assertThat(interpreterUpdatedForDefendant.getDefendantId(), equalTo(defendantId));
                assertThat(interpreterUpdatedForDefendant.getInterpreter(), equalTo(Interpreter.of(pleadOnline.getInterpreterLanguage())));

                assertThat(interpreterUpdatedForDefendant.getUpdatedDate(), equalTo(createDate));
                assertThat(interpreterUpdatedForDefendant.isUpdatedByOnlinePlea(), equalTo(updateByOnlinePlea));
                assertTrue(interpreterUpdatedForDefendant.isUpdatedByOnlinePlea());

                // if contains InterpreterUpdatedForDefendant then contains also HearingLanguagePreferenceUpdatedForDefendant
                assertThat(events, hasItem(instanceOf(InterpreterUpdatedForDefendant.class)));
            } else if (e instanceof HearingLanguagePreferenceUpdatedForDefendant) {
                final HearingLanguagePreferenceUpdatedForDefendant HearingLanguagePreferenceUpdatedForDefendant = (HearingLanguagePreferenceUpdatedForDefendant) e;

                assertThat(HearingLanguagePreferenceUpdatedForDefendant.getCaseId(), equalTo(caseId));
                assertThat(HearingLanguagePreferenceUpdatedForDefendant.getDefendantId(), equalTo(defendantId));
                assertThat(isTrue(HearingLanguagePreferenceUpdatedForDefendant.getSpeakWelsh()), equalTo(isTrue(pleadOnline.getSpeakWelsh())));
                assertThat(HearingLanguagePreferenceUpdatedForDefendant.isUpdatedByOnlinePlea(), equalTo(updateByOnlinePlea));

                // if contains InterpreterUpdatedForDefendant then contains also HearingLanguagePreferenceUpdatedForDefendant
                assertThat(events, hasItem(instanceOf(HearingLanguagePreferenceUpdatedForDefendant.class)));
            } else if (e instanceof TrialRequested) {
                final TrialRequested trialRequested = (TrialRequested) e;

                assertThat(caseId, equalTo(trialRequested.getCaseId()));
                assertThat(trialRequested.getUnavailability(), equalTo(pleadOnline.getUnavailability()));
                assertThat(trialRequested.getWitnessDetails(), equalTo(pleadOnline.getWitnessDetails()));
                assertThat(trialRequested.getWitnessDispute(), equalTo(pleadOnline.getWitnessDispute()));
                assertThat(createDate, equalTo(trialRequested.getUpdatedDate()));
            } else if (e instanceof DefendantDetailsUpdated) {
                final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) e;
                assertTrue(defendantDetailsUpdated.isUpdateByOnlinePlea());
            } else if (e instanceof DefendantDateOfBirthUpdated) {
                final DefendantDateOfBirthUpdated defendantDateOfBirthUpdated = (DefendantDateOfBirthUpdated) e;

                assertThat(defendantDateOfBirthUpdated.getCaseId(), equalTo(caseId));
                assertThat(defendantDateOfBirthUpdated.getNewDateOfBirth(), equalTo(pleadOnline.getPersonalDetails().getDateOfBirth()));
                assertThat(defendantDateOfBirthUpdated.getOldDateOfBirth(), not(equalTo(defendantDateOfBirthUpdated.getNewDateOfBirth())));
            } else if (e instanceof DefendantPersonalNameUpdated) {
                final DefendantPersonalNameUpdated defendantPersonalNameUpdated = (DefendantPersonalNameUpdated) e;

                assertThat(defendantPersonalNameUpdated.getCaseId(), equalTo(caseId));
                assertThat(defendantPersonalNameUpdated.getNewPersonalName(), equalTo(
                        new PersonalName(
                                pleadOnline.getPersonalDetails().getTitle(),
                                pleadOnline.getPersonalDetails().getFirstName(),
                                pleadOnline.getPersonalDetails().getLastName()
                        )));
                assertThat(defendantPersonalNameUpdated.getOldPersonalName(), not(equalTo(defendantPersonalNameUpdated.getNewPersonalName())));
            } else if (e instanceof DefendantAddressUpdated) {
                final DefendantAddressUpdated defendantAddressUpdated = (DefendantAddressUpdated) e;

                assertThat(defendantAddressUpdated.getCaseId(), equalTo(caseId));
                assertThat(defendantAddressUpdated.getNewAddress(), equalTo(pleadOnline.getPersonalDetails().getAddress()));
                assertThat(defendantAddressUpdated.getNewAddress(), not(equalTo(defendantAddressUpdated.getOldAddress())));
            } else if (e instanceof OnlinePleaReceived) {
                final OnlinePleaReceived onlinePleaReceived = (OnlinePleaReceived) e;

                assertThat(onlinePleaReceived.getCaseId(), equalTo(caseId));
                assertThat(onlinePleaReceived.getDefendantId(), equalTo(defendantId));
            } else if (e instanceof CaseStatusChanged) {
                final CaseStatusChanged caseStatusChanged = (CaseStatusChanged) e;
                assertThat(caseStatusChanged.getCaseId(), equalTo(caseId));

            } else if (e instanceof CaseMarkedReadyForDecision) {
                final CaseMarkedReadyForDecision caseMarkedReadyForDecision = (CaseMarkedReadyForDecision) e;
                assertThat(caseMarkedReadyForDecision.getCaseId(), equalTo(caseId));

            } else if (e instanceof DatesToAvoidRequired) {
                final DatesToAvoidRequired datesToAvoidRequired = (DatesToAvoidRequired) e;
                assertThat(caseId, equalTo(datesToAvoidRequired.getCaseId()));
                assertThat(now.toLocalDate().plusDays(NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID), equalTo(datesToAvoidRequired.getDatesToAvoidExpirationDate()));
            }
            else if (e instanceof CaseExpectedDateReadyChanged) {
                final CaseExpectedDateReadyChanged caseExpectedDateReadyChanged = (CaseExpectedDateReadyChanged) e;
                assertThat(caseId, equalTo(caseExpectedDateReadyChanged.getCaseId()));
                assertThat(now.toLocalDate().plusDays(NUMBER_DAYS_WAITING_FOR_DATES_TO_AVOID), equalTo(caseExpectedDateReadyChanged.getNewExpectedDateReady()));
            }
            else if (e instanceof OutstandingFinesUpdated) {
                final OutstandingFinesUpdated outstandingFinesUpdated = (OutstandingFinesUpdated) e;
                assertThat(caseId, equalTo(outstandingFinesUpdated.getCaseId()));
                assertTrue(outstandingFinesUpdated.getOutstandingFines());
            }
            else {
                fail("No any assertion to validate new event " + e.getClass() + ". Please update this test.");
            }
        });
    }

    private static Matcher<Iterable<?>> containsEventsOf(Class<?>... types) {
        return contains(
                Stream.of(types)
                        .map(Matchers::instanceOf)
                        .collect(toList()));
    }

    private static Case createTestCase(final String title, final UUID... extraOffenceIds) {
        final Defendant defendant = new DefendantBuilder()
                .withTitle(title)
                .withFirstName(PERSON_FIRST_NAME)
                .withLastName(PERSON_LAST_NAME)
                .withDateOfBirth(PERSON_DOB)
                .withNationalInsuranceNumber(PERSON_NI_NUMBER)
                .withDriverNumber(PERSON_DRIVER_NUMBER)
                .withAddress(PERSON_ADDRESS)
                .withContactDetails(PERSON_CONTACT_DETAILS)
                .withOffences(extraOffenceIds)
                .addOffence(randomUUID())
                .build();

        return CaseBuilder.aDefaultSjpCase()
                .withDefendant(defendant)
                .build();
    }

}
