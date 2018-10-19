package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;

import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.PersonalName;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
import uk.gov.moj.cpp.sjp.domain.command.UpdatePlea;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadOnline;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.testutils.PleaBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.StoreOnlinePleaBuilder;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.event.EmploymentStatusUpdated;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.OffenceNotFound;
import uk.gov.moj.cpp.sjp.event.OnlinePleaReceived;
import uk.gov.moj.cpp.sjp.event.PleaCancelled;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.event.TrialRequestCancelled;
import uk.gov.moj.cpp.sjp.event.TrialRequested;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class UpdatePleaTest extends CaseAggregateBaseTest {

    private UUID offenceId;
    private final ZonedDateTime now = clock.now();
    private UUID userId = UUID.randomUUID();
    private String[] trialFields;
    private Defendant defendant;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        defendant = caseReceivedEvent.getDefendant();
        offenceId = defendant.getOffences().get(0).getId();
        trialFields = new String[] {null, null, null};
    }

    @Test
    public void shouldAddUpdateAndCancelPleaAndHearingRequirements() {
        String interpreterLanguage;
        Boolean speakWelsh;

        // add plea
        interpreterLanguage = null;
        speakWelsh = null;
        updatePleaAndThenVerifyEvents(PleaType.GUILTY_REQUEST_HEARING, interpreterLanguage, speakWelsh, () -> singletonList(
                PleaUpdated.class));

        // add hearing requirements
        interpreterLanguage = "Maori";
        speakWelsh = false;
        updatePleaAndThenVerifyEvents(PleaType.GUILTY_REQUEST_HEARING, interpreterLanguage, speakWelsh, () -> asList(
                PleaUpdated.class,
                InterpreterUpdatedForDefendant.class,
                HearingLanguagePreferenceUpdatedForDefendant.class));

        // update just plea
        updatePleaAndThenVerifyEvents(PleaType.NOT_GUILTY, interpreterLanguage, speakWelsh, () -> asList(
                PleaUpdated.class,
                TrialRequested.class));

        // update just interpreter
        interpreterLanguage = "Welsh";
        updatePleaAndThenVerifyEvents(PleaType.NOT_GUILTY, interpreterLanguage, speakWelsh, () -> asList(
                PleaUpdated.class,
                InterpreterUpdatedForDefendant.class));

        // update just speak welsh
        speakWelsh = true;
        updatePleaAndThenVerifyEvents(PleaType.NOT_GUILTY, interpreterLanguage, speakWelsh, () -> asList(
                PleaUpdated.class,
                HearingLanguagePreferenceUpdatedForDefendant.class));

        // cancel the interpreter
        interpreterLanguage = null;
        updatePleaAndThenVerifyEvents(PleaType.NOT_GUILTY, interpreterLanguage, speakWelsh, () -> asList(
                PleaUpdated.class,
                InterpreterCancelledForDefendant.class));

        // cancel the speak welsh
        speakWelsh = null;
        updatePleaAndThenVerifyEvents(PleaType.NOT_GUILTY, interpreterLanguage, speakWelsh, () -> asList(
                PleaUpdated.class,
                HearingLanguagePreferenceCancelledForDefendant.class));

        // update plea and interpreter
        interpreterLanguage = "Maori";
        speakWelsh = true;
        updatePleaAndThenVerifyEvents(PleaType.GUILTY_REQUEST_HEARING, interpreterLanguage, speakWelsh, () -> asList(
                PleaUpdated.class,
                TrialRequestCancelled.class,
                InterpreterUpdatedForDefendant.class,
                HearingLanguagePreferenceUpdatedForDefendant.class));

        // cancel plea (and hearing requirements)
        cancelPleaAndThenVerifyEvents(
                PleaCancelled.class,
                InterpreterCancelledForDefendant.class,
                HearingLanguagePreferenceCancelledForDefendant.class);

        // add plea
        interpreterLanguage = null;
        speakWelsh = null;
        updatePleaAndThenVerifyEvents(PleaType.GUILTY_REQUEST_HEARING, interpreterLanguage, speakWelsh, () -> singletonList(
                PleaUpdated.class));

        // cancel plea (but not interpreter)
        cancelPleaAndThenVerifyEvents(
                PleaCancelled.class);
    }

    @Test
    public void shouldUpdatePleaWhenWithdrawalOffencesRequested() {
        // given
        caseAggregate.requestWithdrawalAllOffences();
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);

        // when
        updatePleaAndThenVerifyEvents(updatePlea, () -> singletonList(
                PleaUpdated.class)); // then
    }

    @Test
    public void shouldUpdatePleaWhenWithdrawalOffencesRequestCancelled() {
        // given
        caseAggregate.requestWithdrawalAllOffences();
        caseAggregate.cancelRequestWithdrawalAllOffences();

        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);

        // when
        updatePleaAndThenVerifyEvents(updatePlea, () -> singletonList(
                PleaUpdated.class)); // then
    }

    @Test
    public void shouldUpdatePleaWhenSjpnIsNotAdded() {
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(offenceId);

        // when
        updatePleaAndThenVerifyEvents(updatePlea, () -> singletonList(
                PleaUpdated.class)); // then
    }

    @Test
    public void shouldNotCancelPleaWhenOffenceDoesNotExist() {
        final CancelPlea cancelPlea = new CancelPlea(caseId, UUID.randomUUID());

        cancelPleaAndThenVerifyEvents(cancelPlea, () -> singletonList(OffenceNotFound.class));
    }

    @Test
    public void shouldNotUpdatePleaWhenOffenceDoesNotExist() {
        final UpdatePlea updatePlea = PleaBuilder.defaultUpdatePlea(UUID.randomUUID());

        updatePleaAndThenVerifyEvents(updatePlea, () -> singletonList(OffenceNotFound.class));
    }

    @Test
    public void shouldUpdatePleaToGuiltyWhenPleadedNotGuiltyPreviouslyOnline() {
        // given
        sendPleadOnlineWithType(PleaType.NOT_GUILTY);

        // when
        pleadByPostAndThenVerifyEvents(PleaType.GUILTY, () -> asList(
                PleaUpdated.class,
                TrialRequestCancelled.class)); // then
    }

    @Test
    public void shouldUpdatePleaToGuiltyWhenPleadedGuiltyPreviouslyOnline() {
        // given
        final PleaType pleaType = PleaType.GUILTY;
        sendPleadOnlineWithType(pleaType);

        // when
        pleadByPostAndThenVerifyEvents(pleaType, () -> singletonList(
                PleaUpdated.class)); // then
    }

    @Test
    public void shouldUpdatePleaToGuiltyWhenPleadedGuiltyRequestHearingPreviouslyOnline() {
        // given
        sendPleadOnlineWithType(PleaType.GUILTY_REQUEST_HEARING);

        // when
        pleadByPostAndThenVerifyEvents(PleaType.GUILTY, () -> singletonList(
                PleaUpdated.class)); // then
    }

    @Test
    public void shouldUpdatePleaToGuiltyWhenPleadedNotGuiltyPreviouslyByPost() {
        // given
        final PleaType pleaType = PleaType.NOT_GUILTY;
        pleadByPostAndThenVerifyEvents(pleaType, () -> asList(
                PleaUpdated.class,
                TrialRequested.class));

        // when
        pleadByPostAndThenVerifyEvents(pleaType, () -> singletonList(
                PleaUpdated.class));  // then
    }

    @Test
    public void shouldUpdatePleaToNotGuiltyWhenPleadedNotGuiltyPreviouslyOnline() {
        // given
        sendPleadOnlineWithType(PleaType.NOT_GUILTY);

        // when
        pleadByPostAndThenVerifyEvents(PleaType.NOT_GUILTY, () -> singletonList(
                PleaUpdated.class));  // then
    }

    @Test
    public void shouldUpdatePleaToNotGuiltyWhenPleadedGuiltyPreviouslyOnline() {
        // given
        sendPleadOnlineWithType(PleaType.GUILTY);

        // when
        pleadByPostAndThenVerifyEvents(PleaType.NOT_GUILTY, () -> asList(
                PleaUpdated.class,
                TrialRequested.class));  // then
    }

    @Test
    public void shouldUpdatePleaToNotGuiltyWhenPleadedGuiltyRequestHearingPreviouslyOnline() {
        // given
        sendPleadOnlineWithType(PleaType.GUILTY_REQUEST_HEARING);

        // when
        pleadByPostAndThenVerifyEvents(PleaType.NOT_GUILTY, () -> asList(
                PleaUpdated.class,
                TrialRequested.class)); // then
    }

    @Test
    public void shouldUpdatePleaToNotGuiltyWhenPleadedNotGuiltyPreviouslyByPost() {
        // given
        final PleaType pleaType = PleaType.NOT_GUILTY;
        pleadByPostAndThenVerifyEvents(pleaType, () -> asList(
                PleaUpdated.class,
                TrialRequested.class));

        // when
        pleadByPostAndThenVerifyEvents(pleaType, () -> singletonList(
                PleaUpdated.class)); // then
    }

    @Test
    public void shouldUpdatePleaToGuiltyRequestHearingWhenPleadedNotGuiltyPreviouslyOnline() {
        // given
        sendPleadOnlineWithType(PleaType.NOT_GUILTY);

        // when
        pleadByPostAndThenVerifyEvents(PleaType.GUILTY_REQUEST_HEARING, () -> asList(
                PleaUpdated.class,
                TrialRequestCancelled.class)); // then
    }

    @Test
    public void shouldUpdatePleaToGuiltyRequestHearingWhenPleadedGuiltyPreviouslyOnline() {
        // given
        sendPleadOnlineWithType(PleaType.GUILTY);

        // when
        pleadByPostAndThenVerifyEvents(PleaType.GUILTY_REQUEST_HEARING, () -> singletonList(
                PleaUpdated.class)); // then
    }

    @Test
    public void shouldUpdatePleaToGuiltyRequestHearingWhenPleadedGuiltyRequestHearingPreviouslyOnline() {
        // given
        sendPleadOnlineWithType(PleaType.GUILTY_REQUEST_HEARING);

        // when
        pleadByPostAndThenVerifyEvents(PleaType.GUILTY_REQUEST_HEARING, () -> singletonList(
                PleaUpdated.class));  // then
    }

    @Test
    public void shouldUpdatePleaToGuiltyRequestHearingWhenPleadedNotGuiltyPreviouslyByPost() {
        // given
        pleadByPostAndThenVerifyEvents(PleaType.NOT_GUILTY, () -> asList(
                PleaUpdated.class,
                TrialRequested.class));

        // when
        pleadByPostAndThenVerifyEvents(PleaType.GUILTY_REQUEST_HEARING, () -> asList(
                PleaUpdated.class,
                TrialRequestCancelled.class)); // then
    }

    @Test
    public void shouldCancelPleaWhenPleadedNotGuiltyPreviouslyOnline() {
        // given
        sendPleadOnlineWithType(PleaType.NOT_GUILTY);

        // when
        cancelPleaAndThenVerifyEvents(
                PleaCancelled.class,
                TrialRequestCancelled.class); // then
    }

    @Test
    public void shouldCancelPleaWhenPleadedGuiltyPreviouslyOnline() {
        // given
        sendPleadOnlineWithType(PleaType.GUILTY);

        // when
        cancelPleaAndThenVerifyEvents(
                PleaCancelled.class); // then
    }

    @Test
    public void shouldCancelPleaWhenPleadedGuiltyRequestHearingPreviouslyOnline() {
        // given
        sendPleadOnlineWithType(PleaType.GUILTY_REQUEST_HEARING);

        // when
        cancelPleaAndThenVerifyEvents(
                PleaCancelled.class); // then
    }

    @Test
    public void shouldCancelPleaWhenPleadedNotGuiltyPreviouslyByPost() {
        // given
        pleadByPostAndThenVerifyEvents(PleaType.NOT_GUILTY, () -> asList(
                PleaUpdated.class,
                TrialRequested.class));

        // when
        cancelPleaAndThenVerifyEvents(
                PleaCancelled.class,
                TrialRequestCancelled.class); // then
    }

    @Test
    public void shouldToggleTrialEventsWhenUpdateAndCancelPleasRepeatedlyWhenInitialPleaWasOnline() {
        // given pleas online NOT_GUILTY
        sendPleadOnlineWithType(PleaType.NOT_GUILTY);

        // when toggle plea
        toggleTrialEventsByUpdatingAndCancellingPleasRepeatedly();
    }

    @Test
    public void shouldNotToggleTrialEventsWhenUpdateAndCancelPleasRepeatedlyWhenInitialPleaWasByPost() {
        // given plead by post NOT_GUILTY
        pleadByPostAndThenVerifyEvents(PleaType.NOT_GUILTY, () -> asList(
                PleaUpdated.class,
                TrialRequested.class));

        // when toggle plea
        toggleTrialEventsByUpdatingAndCancellingPleasRepeatedly();
    }

    private void updatePleaAndThenVerifyEvents(final PleaType plea, final String expectedInterpreterLanguage,
                                               final Boolean expectedSpeakWelsh, final Supplier<List<Class<?>>> expectedEvents) {
        updatePleaAndThenVerifyEvents(new UpdatePlea(caseId, offenceId, plea, expectedInterpreterLanguage, expectedSpeakWelsh), expectedEvents);
    }

    private void updatePleaAndThenVerifyEvents(final UpdatePlea updatePlea, final Supplier<List<Class<?>>> expectedEvents) {
        // when
        final List<Object> actualEvents = caseAggregate.updatePlea(userId, updatePlea, now)
                .collect(toList());

        // then
        verifyEvents(actualEvents, updatePlea.getPlea(), updatePlea.getInterpreterLanguage(), updatePlea.getSpeakWelsh(), expectedEvents.get());
    }

    private void cancelPleaAndThenVerifyEvents(final Class<?>... expectedEvents) {
        final CancelPlea cancelPlea = new CancelPlea(caseId, offenceId);

        // when
        cancelPleaAndThenVerifyEvents(cancelPlea, () -> asList(expectedEvents));
    }

    private void cancelPleaAndThenVerifyEvents(final CancelPlea cancelPlea, final Supplier<List<Class<?>>> expectedEvents) {
        final List<Object> actualEvents = caseAggregate.cancelPlea(userId, cancelPlea)
                .collect(toList());

        verifyEvents(actualEvents, null, null, null, expectedEvents.get());
    }

    private void sendPleadOnlineWithType(final PleaType pleaType) {
        final List<Class<?>> expectedEventTypes = new ArrayList<>(asList(
                PleaUpdated.class,
                // TrialRequested.class - added programmatically when PleaType.NOT_GUILTY
                DefendantDetailsUpdated.class,
                DefendantDateOfBirthUpdated.class,
                DefendantAddressUpdated.class,
                DefendantPersonalNameUpdated.class,
                FinancialMeansUpdated.class,
                EmployerUpdated.class,
                EmploymentStatusUpdated.class,
                OnlinePleaReceived.class
        ));
        PleadOnline pleadOnline = null;

        if (pleaType.equals(PleaType.NOT_GUILTY)) {
            pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithNotGuiltyPlea(offenceId,
                    defendantId, null, null, true);
            expectedEventTypes.add(1, TrialRequested.class);
        }
        else if (pleaType.equals(PleaType.GUILTY_REQUEST_HEARING)) {
            pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyRequestHearingPlea(offenceId,
                    defendantId, null, null);
        }
        else if (pleaType.equals(PleaType.GUILTY)) {
            pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        }

        trialFields = new String[] {
                pleadOnline.getWitnessDetails(),
                pleadOnline.getWitnessDispute(),
                pleadOnline.getUnavailability()
        };

        // when
        final List<Object> actualEvents = caseAggregate.pleadOnline(caseId, pleadOnline, now)
                .collect(toList());

        // then
        verifyEvents(actualEvents, pleaType, null, null, expectedEventTypes);
    }

    private void verifyEvents(final List<Object> actualEvents, final PleaType expectedPleaType, final String expectedInterpreterLanguage,
                              final Boolean expectedSpeakWelsh, final List<Class<?>> expectedEvents) {
        assertThat(actualEvents, contains(expectedEvents.stream().map(Matchers::instanceOf).collect(toList())));

        actualEvents.forEach(event -> {
            if (event instanceof PleaUpdated) {
                final PleaUpdated pleaUpdated = (PleaUpdated) event;

                assertThat(pleaUpdated.getCaseId(), equalTo(caseId));
                assertThat(pleaUpdated.getOffenceId(), equalTo(offenceId));
                assertThat(pleaUpdated.getPlea(), equalTo(expectedPleaType));
            } else if (event instanceof PleaCancelled) {
                final PleaCancelled pleaCancelled = (PleaCancelled) event;

                assertThat(pleaCancelled.getCaseId(), equalTo(caseId));
                assertThat(pleaCancelled.getOffenceId(), equalTo(offenceId));
            } else if (event instanceof InterpreterUpdatedForDefendant) {
                final InterpreterUpdatedForDefendant interpreterUpdated = (InterpreterUpdatedForDefendant) event;

                assertThat(interpreterUpdated.getCaseId(), equalTo(caseId));
                assertThat(interpreterUpdated.getDefendantId(), equalTo(defendantId));
                assertThat(interpreterUpdated.getInterpreter().getLanguage(), equalTo(expectedInterpreterLanguage));
            } else if (event instanceof InterpreterCancelledForDefendant) {
                final InterpreterCancelledForDefendant interpreterCancelledForDefendant = (InterpreterCancelledForDefendant) event;

                assertThat(interpreterCancelledForDefendant.getCaseId(), equalTo(caseId));
                assertThat(interpreterCancelledForDefendant.getDefendantId(), equalTo(defendantId));
            } else if (event instanceof HearingLanguagePreferenceUpdatedForDefendant) {
                final HearingLanguagePreferenceUpdatedForDefendant HearingLanguagePreferenceUpdatedForDefendant = (HearingLanguagePreferenceUpdatedForDefendant) event;

                assertThat(HearingLanguagePreferenceUpdatedForDefendant.getCaseId(), equalTo(caseId));
                assertThat(HearingLanguagePreferenceUpdatedForDefendant.getDefendantId(), equalTo(defendantId));
                assertThat(HearingLanguagePreferenceUpdatedForDefendant.getSpeakWelsh(), equalTo(expectedSpeakWelsh));
            } else if (event instanceof HearingLanguagePreferenceCancelledForDefendant) {
                final HearingLanguagePreferenceCancelledForDefendant hearingLanguageCancelledForDefendan = (HearingLanguagePreferenceCancelledForDefendant) event;

                assertThat(hearingLanguageCancelledForDefendan.getCaseId(), equalTo(caseId));
                assertThat(hearingLanguageCancelledForDefendan.getDefendantId(), equalTo(defendantId));
            } else if (event instanceof TrialRequested) {
                final TrialRequested trialRequested = (TrialRequested) event;

                assertThat(trialRequested.getCaseId(), equalTo(caseId));
                assertThat(trialRequested.getWitnessDetails(), equalTo(trialFields[0]));
                assertThat(trialRequested.getWitnessDispute(), equalTo(trialFields[1]));
                assertThat(trialRequested.getUnavailability(), equalTo(trialFields[2]));
                assertThat(trialRequested.getUpdatedDate(), equalTo(now));
            } else if (event instanceof TrialRequestCancelled) {
                final TrialRequestCancelled trialRequestCancelled = (TrialRequestCancelled) event;

                assertThat(trialRequestCancelled.getCaseId(), equalTo(caseId));
            } else if (event instanceof EmployerUpdated) {
                final EmployerUpdated employerUpdated = (EmployerUpdated) event;

                assertThat(employerUpdated.getDefendantId(), equalTo(defendantId));
                assertThat(employerUpdated.getUpdatedDate(), equalTo(now));
            } else if (event instanceof EmploymentStatusUpdated) {
                final EmploymentStatusUpdated employmentStatusUpdated = (EmploymentStatusUpdated) event;

                assertThat(employmentStatusUpdated.getDefendantId(), equalTo(defendantId));
                assertThat(employmentStatusUpdated.getEmploymentStatus(), equalTo("EMPLOYED"));
            } else if (event instanceof FinancialMeansUpdated) {
                final FinancialMeansUpdated financialMeansUpdated = (FinancialMeansUpdated) event;

                assertThat(financialMeansUpdated.getDefendantId(), equalTo(defendantId));
                assertThat(financialMeansUpdated.getUpdatedDate(), equalTo(now));
            } else if (event instanceof OffenceNotFound) {
                final OffenceNotFound offenceNotFound = (OffenceNotFound) event;

                assertThat(offenceNotFound.getOffenceId(), not(equalTo(offenceId)));
                assertThat(offenceNotFound.getDescription(), equalTo("Update Plea"));
            } else if (event instanceof DefendantPersonalNameUpdated) {
                final DefendantPersonalNameUpdated defendantPersonalNameUpdated = (DefendantPersonalNameUpdated) event;

                assertThat(defendantPersonalNameUpdated.getCaseId(), equalTo(caseId));
                assertThat(defendantPersonalNameUpdated.getOldPersonalName(), equalTo(new PersonalName(
                        defendant.getTitle(),
                        defendant.getFirstName(),
                        defendant.getLastName())));
                assertThat(defendantPersonalNameUpdated.getNewPersonalName(), equalTo(new PersonalName(
                        null,
                        StoreOnlinePleaBuilder.PERSON_FIRST_NAME,
                        StoreOnlinePleaBuilder.PERSON_LAST_NAME)));
            } else if (event instanceof DefendantAddressUpdated) {
                final DefendantAddressUpdated defendantAddressUpdated = (DefendantAddressUpdated) event;

                assertThat(defendantAddressUpdated.getCaseId(), equalTo(caseId));
                assertThat(defendantAddressUpdated.getOldAddress(), equalTo(defendant.getAddress()));
                assertThat(defendantAddressUpdated.getNewAddress(), equalTo(StoreOnlinePleaBuilder.PERSON_ADDRESS));
            } else if (event instanceof DefendantDateOfBirthUpdated) {
                final DefendantDateOfBirthUpdated defendantDateOfBirthUpdated = (DefendantDateOfBirthUpdated) event;

                assertThat(defendantDateOfBirthUpdated.getCaseId(), equalTo(caseId));
                assertThat(defendantDateOfBirthUpdated.getOldDateOfBirth(), equalTo(defendant.getDateOfBirth()));
                assertThat(defendantDateOfBirthUpdated.getNewDateOfBirth(), equalTo(StoreOnlinePleaBuilder.PERSON_DOB));
            } else if (event instanceof DefendantDetailsUpdated) {
                final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) event;
                final Defendant expectedDefendant = defendant;

                assertThat(defendantDetailsUpdated.getCaseId(), equalTo(caseId));
                assertThat(defendantDetailsUpdated.getDefendantId(), equalTo(defendantId));
                assertThat(defendantDetailsUpdated.getFirstName(), equalTo(StoreOnlinePleaBuilder.PERSON_FIRST_NAME));
                assertThat(defendantDetailsUpdated.getLastName(), equalTo(StoreOnlinePleaBuilder.PERSON_LAST_NAME));
                assertThat(defendantDetailsUpdated.getDateOfBirth(), equalTo(StoreOnlinePleaBuilder.PERSON_DOB));
                assertThat(defendantDetailsUpdated.getContactDetails(), equalTo(StoreOnlinePleaBuilder.PERSON_CONTACT_DETAILS));
                assertThat(defendantDetailsUpdated.getNationalInsuranceNumber(), equalTo(StoreOnlinePleaBuilder.PERSON_NI_NUMBER));
                assertThat(defendantDetailsUpdated.getAddress(), equalTo(StoreOnlinePleaBuilder.PERSON_ADDRESS));

                // not updatable externally
                assertThat(defendantDetailsUpdated.getUpdatedDate(), equalTo(now));
                assertThat(defendantDetailsUpdated.isUpdateByOnlinePlea(), equalTo(true));
                assertThat(defendantDetailsUpdated.getTitle(), allOf(nullValue(), not(equalTo(expectedDefendant.getTitle()))));
                assertThat(defendantDetailsUpdated.getGender(), nullValue());
            } else if (event instanceof OnlinePleaReceived) {
                final OnlinePleaReceived onlinePleaReceived = (OnlinePleaReceived) event;

                assertThat(onlinePleaReceived.getCaseId(), equalTo(caseId));
                assertThat(onlinePleaReceived.getDefendantId(), equalTo(defendantId));
            } else {
                fail("No any assertion to validate new event " + event.getClass() + ". Please update this test.");
            }
        });
    }

    private void pleadByPostAndThenVerifyEvents(final PleaType pleaType, final Supplier<List<Class<?>>> expectedEvents) {
        final UpdatePlea updatePlea;

        switch (pleaType) {
            case GUILTY:
                updatePlea = PleaBuilder.updatePleaGuilty(offenceId);
                break;
            case GUILTY_REQUEST_HEARING:
                updatePlea = PleaBuilder.updatePleaGuiltyRequestHearing(offenceId);
                break;
            case NOT_GUILTY:
                updatePlea = PleaBuilder.updatePleaNotGuilty(offenceId);
                break;
            default: throw new AssertionError("pleaType not mapped in this test");
        }

        updatePleaAndThenVerifyEvents(updatePlea, expectedEvents);
    }

    private void toggleTrialEventsByUpdatingAndCancellingPleasRepeatedly() {
        pleadByPostAndThenVerifyEvents(PleaType.GUILTY, () -> asList(
                PleaUpdated.class,
                TrialRequestCancelled.class));

        pleadByPostAndThenVerifyEvents(PleaType.NOT_GUILTY, () -> asList(
                PleaUpdated.class,
                TrialRequested.class));

        pleadByPostAndThenVerifyEvents(PleaType.NOT_GUILTY, () -> singletonList(
                PleaUpdated.class));

        pleadByPostAndThenVerifyEvents(PleaType.NOT_GUILTY, () -> singletonList(
                PleaUpdated.class));

        cancelPleaAndThenVerifyEvents(
                PleaCancelled.class,
                TrialRequestCancelled.class);

        pleadByPostAndThenVerifyEvents(PleaType.NOT_GUILTY, () -> asList(
                PleaUpdated.class,
                TrialRequested.class));
    }

}
