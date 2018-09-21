package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.sjp.domain.plea.EmploymentStatus.EMPLOYED;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_ADDRESS;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_CONTACT_DETAILS;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_DOB;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_FIRST_NAME;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_LAST_NAME;
import static uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder.PERSON_NI_NUMBER;

import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.PersonalName;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PersonalDetails;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
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
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.TrialRequested;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
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

    private static final UUID userId = UUID.randomUUID();

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
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);
        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleaUpdated.class,
                DefendantDetailsUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OnlinePleaReceived.class));

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
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleaUpdated.class,
                DefendantDetailsUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                InterpreterUpdatedForDefendant.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                OnlinePleaReceived.class));

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
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleaUpdated.class,
                DefendantDetailsUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OnlinePleaReceived.class));

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
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleaUpdated.class,
                TrialRequested.class,
                DefendantDetailsUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                InterpreterUpdatedForDefendant.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                OnlinePleaReceived.class));

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
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleaUpdated.class,
                TrialRequested.class,
                DefendantDetailsUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                InterpreterUpdatedForDefendant.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                OnlinePleaReceived.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldPleaOnlineSuccessfullyForDefendantWithTitleAndMultipleOffences() {
        //given
        final Object[][] pleaInformationArray = {
                {UUID.randomUUID(), PleaType.NOT_GUILTY, true, PleaType.NOT_GUILTY},
                {UUID.randomUUID(), PleaType.GUILTY, false, PleaType.GUILTY},
                {UUID.randomUUID(), PleaType.GUILTY, true, PleaType.GUILTY_REQUEST_HEARING},
                {UUID.randomUUID(), PleaType.NOT_GUILTY, true, PleaType.NOT_GUILTY},
        };
        final UUID[] extraOffenceIds = Arrays.stream(pleaInformationArray).map(pleaInformation ->
                (UUID) pleaInformation[0]).toArray(UUID[]::new);

        final Case testCase = createTestCase("Mr", extraOffenceIds);
        setup(testCase); // Override the @Before

        final String interpreterLanguage = "French";
        final Boolean speakWelsh = TRUE;

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaForMultipleOffences(
                pleaInformationArray, defendantId, interpreterLanguage, speakWelsh);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleaUpdated.class, PleaUpdated.class, PleaUpdated.class, PleaUpdated.class,
                TrialRequested.class,
                DefendantDetailsUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                InterpreterUpdatedForDefendant.class,
                HearingLanguagePreferenceUpdatedForDefendant.class,
                OnlinePleaReceived.class));

        //asserts expectations for all pleas
        IntStream.range(0, pleaInformationArray.length).forEach(index -> {
            PleaUpdated pleaUpdated = (PleaUpdated) events.get(index);
            PleaType expectedPleaType = PleaType.valueOf(pleaInformationArray[index][3].toString());

            assertThat(caseId, equalTo(pleaUpdated.getCaseId()));
            assertThat(PleaMethod.ONLINE, equalTo(pleaUpdated.getPleaMethod()));
            assertThat(expectedPleaType, equalTo(pleaUpdated.getPlea()));
            assertThat(pleadOnline.getOffences().get(index).getId(), equalTo(pleaUpdated.getOffenceId()));
            assertThat(pleadOnline.getOffences().get(index).getMitigation(), equalTo(pleaUpdated.getMitigation()));
            assertThat(pleadOnline.getOffences().get(index).getNotGuiltyBecause(), equalTo(pleaUpdated.getNotGuiltyBecause()));
        });

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldStoreOnlinePleaAndFailToStoreOnlinePleaBasedOnWhetherPleaSubmittedBeforeOrPleaCancelled() {
        //when plea
        PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);
        List<Object> events = asList(eventStream.toArray());

        //then successful
        assertThat(events, containsEventsOf(
                PleaUpdated.class,
                DefendantDetailsUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OnlinePleaReceived.class));

        assertCommonExpectations(pleadOnline, events, now);

        //then plea second time
        eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);
        events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(CaseUpdateRejected.class));

        //then rejected
        final CaseUpdateRejected caseUpdateRejected = (CaseUpdateRejected) events.get(0);
        assertThat(caseId, equalTo(caseUpdateRejected.getCaseId()));
        assertThat(CaseUpdateRejected.RejectReason.PLEA_ALREADY_SUBMITTED, equalTo(caseUpdateRejected.getReason()));

        //then cancel plea
        caseAggregate.cancelPlea(userId, new CancelPlea(caseId, offenceId), now);

        //then plea again
        pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);
        events = asList(eventStream.toArray());

        //then successful
        assertThat(events, containsEventsOf(
                PleaUpdated.class,
                DefendantDetailsUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                OnlinePleaReceived.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldNotStoreOnlinePleaWhenCaseAssigned() {
        //given
        caseAggregate.assignCase(userId, ZonedDateTime.now(), CaseAssignmentType.MAGISTRATE_DECISION);

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(CaseUpdateRejected.class));

        assertThat(((CaseUpdateRejected) events.get(0)).getReason(),
                is(CaseUpdateRejected.RejectReason.CASE_ASSIGNED));
    }

    @Test
    public void shouldStoreOnlinePleaWhenWithdrawalOffencesRequested() {
        //given
        caseAggregate.requestWithdrawalAllOffences();

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleaUpdated.class,
                DefendantDetailsUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OnlinePleaReceived.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldStoreOnlinePleaWhenWithdrawalOffencesRequestCancelled() {
        //given
        caseAggregate.requestWithdrawalAllOffences();
        caseAggregate.cancelRequestWithdrawalAllOffences();

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, containsEventsOf(
                PleaUpdated.class,
                DefendantDetailsUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OnlinePleaReceived.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldNotStoreOnlinePleaWhenOffenceDoesNotExist() {
        //given
        final UUID offenceId = UUID.randomUUID();

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now).collect(toList());

        //then
        assertThat(events, containsEventsOf(OffenceNotFound.class));
    }

    @Test
    public void shouldNotStoreOnlinePleaWhenDefendantIncorrect() {
        //given
        final UUID defendantId = UUID.randomUUID();

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now).collect(toList());

        //then
        assertThat(events, containsEventsOf(DefendantNotFound.class));
    }

    @Test
    public void shouldWarnNameChanged() {
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId, true, false, false);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now).collect(toList());

        //then
        assertThat(events, containsEventsOf(
                PleaUpdated.class,
                DefendantDetailsUpdated.class,
                DefendantPersonalNameUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OnlinePleaReceived.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldWarnAddressChanged() {
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId, false, true, false);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now).collect(toList());

        //then
        assertThat(events, containsEventsOf(
                PleaUpdated.class,
                DefendantDetailsUpdated.class,
                DefendantAddressUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OnlinePleaReceived.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    @Test
    public void shouldWarnDobChanged() {
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId, false, false, true);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now).collect(toList());

        //then
        assertThat(events, containsEventsOf(
                PleaUpdated.class,
                DefendantDetailsUpdated.class,
                DefendantDateOfBirthUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OnlinePleaReceived.class));

        assertCommonExpectations(pleadOnline, events, now);
    }

    private void assertCommonExpectations(final PleadOnline pleadOnline, final List<Object> events, final ZonedDateTime createDate) {
        // in PleadOnline inner objects the defendantIDs are null
        assertThat(pleadOnline.getDefendantId(), equalTo(defendantId));
        assertThat(pleadOnline.getFinancialMeans().getDefendantId(), nullValue());
        assertThat(pleadOnline.getEmployer().getDefendantId(), nullValue());

        assertThat(events.get(0), instanceOf(PleaUpdated.class));
        final PleaUpdated pleaUpdated = (PleaUpdated) events.get(0);

        boolean updateByOnlinePlea = PleaMethod.ONLINE.equals(pleaUpdated.getPleaMethod());

        assertThat(events, not(emptyCollectionOf(Object.class)));
        events.forEach(e -> {
            if(e instanceof PleaUpdated) {
                assertThat(pleaUpdated.getCaseId(), equalTo(caseId));
                assertThat(pleaUpdated.getPleaMethod(), equalTo(PleaMethod.ONLINE));
                assertThat(pleaUpdated.getUpdatedDate(), equalTo(createDate));

                final uk.gov.moj.cpp.sjp.domain.onlineplea.Offence firstOffence = pleadOnline.getOffences().get(0);
                assertThat(firstOffence.getId(), equalTo(pleaUpdated.getOffenceId()));
                assertThat(firstOffence.getMitigation(), equalTo(pleaUpdated.getMitigation()));
                assertThat(firstOffence.getNotGuiltyBecause(), equalTo(pleaUpdated.getNotGuiltyBecause()));
            } else if(e instanceof FinancialMeansUpdated) {
                final FinancialMeansUpdated financialMeansUpdated = (FinancialMeansUpdated) e;

                assertThat(financialMeansUpdated.getDefendantId(), equalTo(defendantId));
                assertThat(financialMeansUpdated.getIncome(), equalTo(pleadOnline.getFinancialMeans().getIncome()));
                assertThat(financialMeansUpdated.getBenefits(), equalTo(pleadOnline.getFinancialMeans().getBenefits()));
                assertThat(financialMeansUpdated.getEmploymentStatus(), equalTo(pleadOnline.getFinancialMeans().getEmploymentStatus()));
                assertThat(financialMeansUpdated.getOutgoings(), hasSize(pleadOnline.getOutgoings().size()));
                assertThat(financialMeansUpdated.getUpdatedDate(), equalTo(createDate));
                assertThat(financialMeansUpdated.isUpdatedByOnlinePlea(), equalTo(updateByOnlinePlea));
                assertTrue(financialMeansUpdated.isUpdatedByOnlinePlea());
            } else if(e instanceof EmployerUpdated) {
                final EmployerUpdated employerUpdated = (EmployerUpdated) e;

                assertThat(employerUpdated.getDefendantId(), equalTo(defendantId));
                assertThat(employerUpdated.getName(), equalTo(pleadOnline.getEmployer().getName()));
                assertThat(employerUpdated.getEmployeeReference(), equalTo(pleadOnline.getEmployer().getEmployeeReference()));
                assertThat(employerUpdated.getPhone(), equalTo(pleadOnline.getEmployer().getPhone()));
                assertThat(employerUpdated.getAddress(), equalTo(pleadOnline.getEmployer().getAddress()));
                assertThat(employerUpdated.getUpdatedDate(), equalTo(createDate));
                assertThat(employerUpdated.isUpdatedByOnlinePlea(), equalTo(updateByOnlinePlea));
                assertTrue(employerUpdated.isUpdatedByOnlinePlea());
            } else if(e instanceof EmploymentStatusUpdated) {
                final EmploymentStatusUpdated employmentStatusUpdated = (EmploymentStatusUpdated) e;

                assertThat(employmentStatusUpdated.getDefendantId(), equalTo(defendantId));
                assertThat(employmentStatusUpdated.getEmploymentStatus(), equalTo(EMPLOYED.name()));
            } else if(e instanceof InterpreterUpdatedForDefendant) {
                final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant = (InterpreterUpdatedForDefendant) e;

                assertThat(interpreterUpdatedForDefendant.getCaseId(), equalTo(caseId));
                assertThat(interpreterUpdatedForDefendant.getDefendantId(), equalTo(defendantId));
                assertThat(interpreterUpdatedForDefendant.getInterpreter(), equalTo(Interpreter.of(pleadOnline.getInterpreterLanguage())));
                assertThat(interpreterUpdatedForDefendant.getUpdatedDate(), equalTo(createDate));
                assertThat(interpreterUpdatedForDefendant.isUpdatedByOnlinePlea(), equalTo(updateByOnlinePlea));
                assertTrue(interpreterUpdatedForDefendant.isUpdatedByOnlinePlea());

                // if contains InterpreterUpdatedForDefendant then contains also HearingLanguagePreferenceUpdatedForDefendant
                assertThat(events, hasItem(instanceOf(HearingLanguagePreferenceUpdatedForDefendant.class)));
            } else if (e instanceof HearingLanguagePreferenceUpdatedForDefendant){
                final HearingLanguagePreferenceUpdatedForDefendant HearingLanguagePreferenceUpdatedForDefendant = (HearingLanguagePreferenceUpdatedForDefendant) e;

                assertThat(HearingLanguagePreferenceUpdatedForDefendant.getCaseId(), equalTo(caseId));
                assertThat(HearingLanguagePreferenceUpdatedForDefendant.getDefendantId(), equalTo(defendantId));
                assertThat(HearingLanguagePreferenceUpdatedForDefendant.getSpeakWelsh(), equalTo(pleadOnline.getSpeakWelsh()));
                assertThat(HearingLanguagePreferenceUpdatedForDefendant.isUpdatedByOnlinePlea(), equalTo(updateByOnlinePlea));

                // if contains InterpreterUpdatedForDefendant then contains also HearingLanguagePreferenceUpdatedForDefendant
                assertThat(events, hasItem(instanceOf(InterpreterUpdatedForDefendant.class)));
            } else if(e instanceof TrialRequested) {
                final TrialRequested trialRequested = (TrialRequested) e;

                assertThat(caseId, equalTo(trialRequested.getCaseId()));
                assertThat(trialRequested.getUnavailability(), equalTo(pleadOnline.getUnavailability()));
                assertThat(trialRequested.getWitnessDetails(), equalTo(pleadOnline.getWitnessDetails()));
                assertThat(trialRequested.getWitnessDispute(), equalTo(pleadOnline.getWitnessDispute()));
                assertThat(createDate, equalTo(trialRequested.getUpdatedDate()));
            } else if(e instanceof DefendantDetailsUpdated) {
                final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) e;

                final PersonalDetails pleadOnlinePersonalDetails = pleadOnline.getPersonalDetails();
                assertThat(defendantDetailsUpdated.getDefendantId(), equalTo(defendantId));
                assertThat(defendantDetailsUpdated.getTitle(), equalTo(pleadOnlinePersonalDetails.getTitle()));
                assertThat(defendantDetailsUpdated.getFirstName(), equalTo(pleadOnlinePersonalDetails.getFirstName()));
                assertThat(defendantDetailsUpdated.getLastName(), equalTo(pleadOnlinePersonalDetails.getLastName()));
                assertThat(defendantDetailsUpdated.getDateOfBirth(), equalTo(pleadOnlinePersonalDetails.getDateOfBirth()));
                assertThat(defendantDetailsUpdated.getGender(), equalTo(pleadOnlinePersonalDetails.getGender()));
                assertThat(defendantDetailsUpdated.getNationalInsuranceNumber(), equalTo(pleadOnlinePersonalDetails.getNationalInsuranceNumber()));
                assertThat(defendantDetailsUpdated.getAddress(), equalTo(pleadOnlinePersonalDetails.getAddress()));
                assertThat(defendantDetailsUpdated.getContactDetails(), equalTo(pleadOnlinePersonalDetails.getContactDetails()));
            } else if(e instanceof DefendantDateOfBirthUpdated) {
                final DefendantDateOfBirthUpdated defendantDateOfBirthUpdated = (DefendantDateOfBirthUpdated) e;

                assertThat(defendantDateOfBirthUpdated.getCaseId(), equalTo(caseId));
                assertThat(defendantDateOfBirthUpdated.getNewDateOfBirth(), equalTo(pleadOnline.getPersonalDetails().getDateOfBirth()));
                assertThat(defendantDateOfBirthUpdated.getOldDateOfBirth(), not(equalTo(defendantDateOfBirthUpdated.getNewDateOfBirth())));
            } else if(e instanceof DefendantPersonalNameUpdated) {
                final DefendantPersonalNameUpdated defendantPersonalNameUpdated = (DefendantPersonalNameUpdated) e;

                assertThat(defendantPersonalNameUpdated.getCaseId(), equalTo(caseId));
                assertThat(defendantPersonalNameUpdated.getNewPersonalName(), equalTo(
                        new PersonalName(
                                pleadOnline.getPersonalDetails().getTitle(),
                                pleadOnline.getPersonalDetails().getFirstName(),
                                pleadOnline.getPersonalDetails().getLastName()
                        )));
                assertThat(defendantPersonalNameUpdated.getOldPersonalName(), not(equalTo(defendantPersonalNameUpdated.getNewPersonalName())));
            } else if(e instanceof DefendantAddressUpdated) {
                final DefendantAddressUpdated defendantAddressUpdated = (DefendantAddressUpdated) e;

                assertThat(defendantAddressUpdated.getCaseId(), equalTo(caseId));
                assertThat(defendantAddressUpdated.getNewAddress(), equalTo(pleadOnline.getPersonalDetails().getAddress()));
                assertThat(defendantAddressUpdated.getNewAddress(), not(equalTo(defendantAddressUpdated.getOldAddress())));
            } else if(e instanceof OnlinePleaReceived) {
                final OnlinePleaReceived onlinePleaReceived = (OnlinePleaReceived) e;

                assertThat(onlinePleaReceived.getCaseId(), equalTo(caseId));
                assertThat(onlinePleaReceived.getDefendantId(), equalTo(defendantId));
            } else {
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
        final List<Offence> offences = Stream.concat(Stream.of(UUID.randomUUID()), Arrays.stream(extraOffenceIds))
                .map(id -> new Offence(id, 1, null, null,
                        1, null, null, null, null, null))
                .collect(toList());

        return new Case(UUID.randomUUID(), "TFL123456", RandomStringUtils.randomAlphanumeric(12).toUpperCase(),
                ProsecutingAuthority.TFL,  null, null,
                new Defendant(UUID.randomUUID(), title, PERSON_FIRST_NAME, PERSON_LAST_NAME, PERSON_DOB,
                        null, PERSON_NI_NUMBER, null, PERSON_ADDRESS, PERSON_CONTACT_DETAILS, 1, offences, null, null, null));
    }

}
