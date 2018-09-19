package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;
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
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.command.CancelPlea;
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
import org.hamcrest.CoreMatchers;
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
                .collect(toList()).get(0);
        caseId = sjpCase.getCaseId();
        defendantId = sjpCase.getDefendant().getId();
        offenceId = sjpCase.getDefendant().getOffences().get(0).getId();
    }

    private void assertPleaExpectationsAndCommonExpectations(final PleadOnline pleadOnline, final UUID defendantId, final PleaType pleaType,
                                                             final PleaUpdated pleaUpdated, final FinancialMeansUpdated financialMeansUpdated, final EmployerUpdated employerUpdated,
                                                             final EmploymentStatusUpdated employmentStatusUpdated, final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant,
                                                             final TrialRequested trialRequested, final DefendantDetailsUpdated defendantDetailsUpdated, final ZonedDateTime createDate) {
        assertThat(caseId, equalTo(pleaUpdated.getCaseId()));
        assertThat(PleaMethod.ONLINE, equalTo(pleaUpdated.getPleaMethod()));
        assertThat(pleaType, equalTo(pleaUpdated.getPlea()));
        assertThat(createDate, equalTo(pleaUpdated.getUpdatedDate()));
        assertThat(pleadOnline.getOffences().get(0).getId(), equalTo(pleaUpdated.getOffenceId()));
        assertThat(pleadOnline.getOffences().get(0).getMitigation(), equalTo(pleaUpdated.getMitigation()));
        assertThat(pleadOnline.getOffences().get(0).getNotGuiltyBecause(), equalTo(pleaUpdated.getNotGuiltyBecause()));

        assertCommonExpectations(pleadOnline, defendantId, financialMeansUpdated, employerUpdated, employmentStatusUpdated,
                interpreterUpdatedForDefendant, trialRequested, defendantDetailsUpdated, createDate);
    }

    private void assertCommonExpectations(final PleadOnline pleadOnline, final UUID defendantId, final FinancialMeansUpdated financialMeansUpdated,
                                          final EmployerUpdated employerUpdated, final EmploymentStatusUpdated employmentStatusUpdated,
                                          final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant, final TrialRequested trialRequested,
                                          final DefendantDetailsUpdated defendantDetailsUpdated, final ZonedDateTime createDate) {
        assertThat(defendantId, equalTo(financialMeansUpdated.getDefendantId()));
        assertThat(pleadOnline.getFinancialMeans().getIncome(), equalTo(financialMeansUpdated.getIncome()));
        assertThat(pleadOnline.getFinancialMeans().getBenefits(), equalTo(financialMeansUpdated.getBenefits()));
        assertThat(pleadOnline.getFinancialMeans().getEmploymentStatus(), equalTo(financialMeansUpdated.getEmploymentStatus()));
        assertThat(pleadOnline.getOutgoings().size(), equalTo(financialMeansUpdated.getOutgoings().size()));
        assertThat(createDate, equalTo(financialMeansUpdated.getUpdatedDate()));
        assertTrue(financialMeansUpdated.isUpdatedByOnlinePlea());

        assertThat(defendantId, equalTo(employerUpdated.getDefendantId()));
        assertThat(pleadOnline.getEmployer().getName(), equalTo(employerUpdated.getName()));
        assertThat(pleadOnline.getEmployer().getEmployeeReference(), equalTo(employerUpdated.getEmployeeReference()));
        assertThat(pleadOnline.getEmployer().getPhone(), equalTo(employerUpdated.getPhone()));
        assertThat(pleadOnline.getEmployer().getAddress(), equalTo(employerUpdated.getAddress()));
        assertThat(createDate, equalTo(employerUpdated.getUpdatedDate()));
        assertTrue(employerUpdated.isUpdatedByOnlinePlea());

        if (employmentStatusUpdated != null) {
            assertThat(defendantId, equalTo(employmentStatusUpdated.getDefendantId()));
            assertThat(EMPLOYED.name(), equalTo(employmentStatusUpdated.getEmploymentStatus()));
        }

        if (interpreterUpdatedForDefendant != null) {
            assertThat(caseId, equalTo(interpreterUpdatedForDefendant.getCaseId()));
            assertThat(defendantId, equalTo(interpreterUpdatedForDefendant.getDefendantId()));
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

        assertThat(pleadOnline.getPersonalDetails().getTitle(), equalTo(defendantDetailsUpdated.getTitle()));
        assertThat(pleadOnline.getPersonalDetails().getFirstName(), equalTo(defendantDetailsUpdated.getFirstName()));
        assertThat(pleadOnline.getPersonalDetails().getLastName(), equalTo(defendantDetailsUpdated.getLastName()));
        assertThat(pleadOnline.getPersonalDetails().getDateOfBirth(), equalTo(defendantDetailsUpdated.getDateOfBirth()));
        assertThat(pleadOnline.getPersonalDetails().getGender(), equalTo(defendantDetailsUpdated.getGender()));
        assertThat(pleadOnline.getPersonalDetails().getNationalInsuranceNumber(), equalTo(defendantDetailsUpdated.getNationalInsuranceNumber()));
        assertThat(pleadOnline.getPersonalDetails().getAddress(), equalTo(defendantDetailsUpdated.getAddress()));
        assertThat(pleadOnline.getPersonalDetails().getContactDetails(), equalTo(defendantDetailsUpdated.getContactDetails()));
        assertTrue(defendantDetailsUpdated.isUpdateByOnlinePlea());
    }

    @Test
    public void shouldPleadOnlineSuccessfullyForGuiltyPlea() {
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);
        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(6));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has DefendantDetailsUpdated event", events, hasItem(isA(DefendantDetailsUpdated.class)));
        assertThat("Has OnlinePleaReceived event", events, hasItem(isA(OnlinePleaReceived.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, defendantId, PleaType.GUILTY, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(2),
                (EmployerUpdated) events.get(3), (EmploymentStatusUpdated) events.get(4), null, null, (DefendantDetailsUpdated) events.get(1), now);
    }

    @Test
    public void shouldPleaOnlineSuccessfullyForGuiltyRequestHearingPlea() {
        //given
        final String interpreterLanguage = "French";

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyRequestHearingPlea(offenceId,
                defendantId, interpreterLanguage);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(7));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has InterpreterUpdatedForDefendant event", events, hasItem(isA(InterpreterUpdatedForDefendant.class)));
        assertThat("Has DefendantDetailsUpdated event", events, hasItem(isA(DefendantDetailsUpdated.class)));
        assertThat("Has OnlinePleaReceived event", events, hasItem(isA(OnlinePleaReceived.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, defendantId, PleaType.GUILTY_REQUEST_HEARING, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(2),
                (EmployerUpdated) events.get(3), (EmploymentStatusUpdated) events.get(4), (InterpreterUpdatedForDefendant) events.get(5), null, (DefendantDetailsUpdated) events.get(1), now);
    }

    @Test
    public void shouldPleaOnlineSuccessfullyForGuiltyRequestHearingPleaWithoutInterpreterLanguage() {
        //given
        final String interpreterLanguage = null;

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyRequestHearingPlea(offenceId,
                defendantId, interpreterLanguage);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(6));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has DefendantDetailsUpdated event", events, hasItem(isA(DefendantDetailsUpdated.class)));
        assertThat("Has OnlinePleaReceived event", events, hasItem(isA(OnlinePleaReceived.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, defendantId, PleaType.GUILTY_REQUEST_HEARING, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(2),
                (EmployerUpdated) events.get(3), (EmploymentStatusUpdated) events.get(4), null, null, (DefendantDetailsUpdated) events.get(1), now);
    }

    @Test
    public void shouldPleaOnlineSuccessfullyForNotGuiltyPlea() {
        //given
        final String interpreterLanguage = "French";
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithNotGuiltyPlea(offenceId,
                defendantId, interpreterLanguage, true);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(8));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has InterpreterUpdatedForDefendant event", events, hasItem(isA(InterpreterUpdatedForDefendant.class)));
        assertThat("Has TrialRequested event", events, hasItem(isA(TrialRequested.class)));
        assertThat("Has DefendantDetailsUpdated event", events, hasItem(isA(DefendantDetailsUpdated.class)));
        assertThat("Has OnlinePleaReceived event", events, hasItem(isA(OnlinePleaReceived.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, defendantId, PleaType.NOT_GUILTY, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(3),
                (EmployerUpdated) events.get(4), (EmploymentStatusUpdated) events.get(5), (InterpreterUpdatedForDefendant) events.get(6), (TrialRequested) events.get(1), (DefendantDetailsUpdated) events.get(2), now);
    }

    @Test
    public void shouldPleaOnlineSuccessfullyForNotGuiltyPleaWithoutTrialRequestedEvent() {
        //given
        final String interpreterLanguage = "French";

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithNotGuiltyPlea(offenceId,
                defendantId, interpreterLanguage, false);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(8));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has InterpreterUpdatedForDefendant event", events, hasItem(isA(InterpreterUpdatedForDefendant.class)));
        assertThat("Has TrialRequested event", events, hasItem(isA(TrialRequested.class)));
        assertThat("Has DefendantDetailsUpdated event", events, hasItem(isA(DefendantDetailsUpdated.class)));
        assertThat("Has OnlinePleaReceived event", events, hasItem(isA(OnlinePleaReceived.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, defendantId, PleaType.NOT_GUILTY, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(3),
                (EmployerUpdated) events.get(4), (EmploymentStatusUpdated) events.get(5), (InterpreterUpdatedForDefendant) events.get(6), (TrialRequested) events.get(1), (DefendantDetailsUpdated) events.get(2), now);
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

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaForMultipleOffences(
                pleaInformationArray, defendantId, interpreterLanguage);
        final Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);

        //then
        final List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(11));
        int pleaUpdatedEventCount = (int) events.stream().map(Object::getClass).filter(PleaUpdated.class::equals).count();
        assertThat("Has PleaUpdated events", pleaUpdatedEventCount, is(pleaInformationArray.length));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has InterpreterUpdatedForDefendant event", events, hasItem(isA(InterpreterUpdatedForDefendant.class)));
        assertThat("Has TrialRequested event", events, hasItem(isA(TrialRequested.class)));
        assertThat("Has DefendantDetailsUpdated event", events, hasItem(isA(DefendantDetailsUpdated.class)));
        assertThat("Has OnlinePleaReceived event", events, hasItem(isA(OnlinePleaReceived.class)));

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

        assertCommonExpectations(pleadOnline, defendantId, (FinancialMeansUpdated) events.get(6),
                (EmployerUpdated) events.get(7), (EmploymentStatusUpdated) events.get(8), (InterpreterUpdatedForDefendant) events.get(9),
                (TrialRequested) events.get(4), (DefendantDetailsUpdated) events.get(5), now);
    }

    @Test
    public void shouldStoreOnlinePleaAndFailToStoreOnlinePleaBasedOnWhetherPleaSubmittedBeforeOrPleaCancelled() {
        //when plea
        PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        Stream<Object> eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);
        List<Object> events = asList(eventStream.toArray());
        assertThat(events, hasSize(6));

        //then successful
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has DefendantDetailsUpdated event", events, hasItem(isA(DefendantDetailsUpdated.class)));
        assertThat("Has OnlinePleaReceived event", events, hasItem(isA(OnlinePleaReceived.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, defendantId, PleaType.GUILTY, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(2),
                (EmployerUpdated) events.get(3), (EmploymentStatusUpdated) events.get(4), null, null, (DefendantDetailsUpdated) events.get(1), now);

        //then plea second time
        eventStream = caseAggregate.pleadOnline(caseId, pleadOnline, now);
        events = asList(eventStream.toArray());
        assertThat(events, hasSize(1));
        assertThat("Has CaseUpdateRejected event", events, hasItem(isA(CaseUpdateRejected.class)));

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
        assertThat(events, hasSize(5));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has DefendantDetailsUpdated event", events, hasItem(isA(DefendantDetailsUpdated.class)));
        assertThat("Has OnlinePleaReceived event", events, hasItem(isA(OnlinePleaReceived.class)));
        assertPleaExpectationsAndCommonExpectations(pleadOnline, defendantId, PleaType.GUILTY, (PleaUpdated) events.get(0), (FinancialMeansUpdated) events.get(2),
                (EmployerUpdated) events.get(3), null, null, null, (DefendantDetailsUpdated) events.get(1), now);
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
        assertThat(events, hasSize(1));
        assertThat("Has CaseUpdateRejected event", events, hasItem(isA(CaseUpdateRejected.class)));
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
        assertThat(events, hasSize(6));
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
        assertThat("Has DefendantDetailsUpdated event", events, hasItem(isA(DefendantDetailsUpdated.class)));
        assertThat("Has FinancialMeansUpdated event", events, hasItem(isA(FinancialMeansUpdated.class)));
        assertThat("Has EmployerUpdated event", events, hasItem(isA(EmployerUpdated.class)));
        assertThat("Has EmploymentStatusUpdated event", events, hasItem(isA(EmploymentStatusUpdated.class)));
        assertThat("Has OnlinePleaReceived event", events, hasItem(isA(OnlinePleaReceived.class)));
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
        assertThat("Has PleaUpdated event", events, hasItem(isA(PleaUpdated.class)));
    }

    @Test
    public void shouldNotStoreOnlinePleaWhenOffenceDoesNotExist() {
        //given
        final UUID offenceId = UUID.randomUUID();

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now).collect(toList());

        //then
        assertThat(events, hasSize(1));

        final Object object = events.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(OffenceNotFound.class)));
    }

    @Test
    public void shouldNotStoreOnlinePleaWhenDefendantIncorrect() {
        //given
        final UUID defendantId = UUID.randomUUID();

        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now).collect(toList());

        //then
        assertThat(events, hasSize(1));

        final Object object = events.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(DefendantNotFound.class)));
    }

    @Test
    public void shouldWarnNameChanged() {
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId, true, false, false);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now).collect(toList());

        //then
        assertThat(events, hasItem(instanceOf(DefendantPersonalNameUpdated.class)));
        assertThat(events, not(hasItem(instanceOf(DefendantAddressUpdated.class))));
        assertThat(events, not(hasItem(instanceOf(DefendantDateOfBirthUpdated.class))));
    }

    @Test
    public void shouldWarnAddressChanged() {
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId, false, true, false);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now).collect(toList());

        //then
        assertThat(events, not(hasItem(instanceOf(DefendantPersonalNameUpdated.class))));
        assertThat(events, hasItem(instanceOf(DefendantAddressUpdated.class)));
        assertThat(events, not(hasItem(instanceOf(DefendantDateOfBirthUpdated.class))));
    }

    @Test
    public void shouldWarnDobChanged() {
        //when
        final PleadOnline pleadOnline = StoreOnlinePleaBuilder.defaultStoreOnlinePleaWithGuiltyPlea(offenceId, defendantId, false, false, true);
        final List<Object> events = caseAggregate.pleadOnline(caseId, pleadOnline, now).collect(toList());

        //then
        assertThat(events, not(hasItem(instanceOf(DefendantPersonalNameUpdated.class))));
        assertThat(events, not(hasItem(instanceOf(DefendantAddressUpdated.class))));
        assertThat(events, hasItem(instanceOf(DefendantDateOfBirthUpdated.class)));
    }

    private static Case createTestCase(final String title, final UUID... extraOffenceIds) {
        final List<Offence> offences = Stream.concat(Stream.of(UUID.randomUUID()), Arrays.stream(extraOffenceIds))
                .map(id -> new Offence(id, 1, null, null,
                        1, null, null, null, null, null))
                .collect(toList());

        return new Case(UUID.randomUUID(), "TFL123456", RandomStringUtils.randomAlphanumeric(12).toUpperCase(),
                ProsecutingAuthority.TFL,  null, null,
                new Defendant(UUID.randomUUID(), title, PERSON_FIRST_NAME, PERSON_LAST_NAME, PERSON_DOB,
                        null, PERSON_NI_NUMBER, PERSON_ADDRESS, PERSON_CONTACT_DETAILS, 1, offences));
    }

}
