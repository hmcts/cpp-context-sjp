package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_PENDING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_PENDING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.STAT_DEC;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.json.schemas.domains.sjp.commands.CreateCaseApplication;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.Application;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseApplicationForReopeningRecorded;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRecorded;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRejected;
import uk.gov.moj.cpp.sjp.event.CaseStatDecRecorded;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseApplicationHandlerTest {

    private CaseAggregateState state;
    private static final UUID CASE_ID = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a66");
    private static final UUID APP_ID = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a67");


    private final static LocalDate DATE_RECEIVED = LocalDate.now().minusDays(5);


    private final static CreateCaseApplication UNKNOWN_TYPE_APP = CreateCaseApplication.createCaseApplication()
            .withCourtApplication(CourtApplication.courtApplication()
                    .withId(APP_ID)
                    .withType(CourtApplicationType.courtApplicationType().build())
                    .withApplicationStatus(ApplicationStatus.DRAFT)
                    .withApplicationReceivedDate(DATE_RECEIVED.toString())
                    .build())
            .withApplicationIdExists(false)
            .withCaseId(CASE_ID).build();

    private final static CreateCaseApplication STAT_DEC_TYPE_APP = CreateCaseApplication.createCaseApplication()
            .withCourtApplication(CourtApplication.courtApplication()
                    .withId(APP_ID)
                    .withApplicationStatus(ApplicationStatus.DRAFT)
                    .withApplicationReceivedDate(DATE_RECEIVED.toString())
                    .withType(CourtApplicationType.courtApplicationType()
                            .withId(fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e"))
                            .withType("Appearance to make statutory declaration (SJP case)")
                            .withCode("MC80528")
                            .withAppealFlag(false)
                            .build())
                    .withApplicant(CourtApplicationParty.courtApplicationParty()
                            .withId(fromString("5002d600-af66-11e8-b568-0800200c9a67"))
                            .withSummonsRequired(false)
                            .withNotificationRequired(false)
                            .build())
                    .withSubject(CourtApplicationParty.courtApplicationParty()
                            .withId(fromString("5002d600-af66-11e8-b568-0800200c9a68"))
                            .withSummonsRequired(false)
                            .withNotificationRequired(false)
                            .withMasterDefendant(MasterDefendant.masterDefendant()
                                    .withPersonDefendant(PersonDefendant.personDefendant()
                                            .withPersonDetails(Person.person()
                                                    .withAddress(Address.address()
                                                            .withAddress1("Address One")
                                                            .withAddress2("Address Two").withPostcode("RG1 9KS").build())
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build())
            .withApplicationIdExists(false)
            .withCaseId(CASE_ID).build();

    private final static CreateCaseApplication STAT_DEC_WRONG_TYPE_APP = CreateCaseApplication.createCaseApplication()
            .withCourtApplication(CourtApplication.courtApplication()
                    .withId(APP_ID)
                    .withApplicationStatus(ApplicationStatus.DRAFT)
                    .withApplicationReceivedDate(LocalDate.now().plusDays(3).toString())
                    .withType(CourtApplicationType.courtApplicationType()
                            .withId(fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e"))
                            .withType("Appearance to make statutory declaration (SJP case)")
                            .withCode("MC80528")
                            .withAppealFlag(false)
                            .build())
                    .withApplicant(CourtApplicationParty.courtApplicationParty()
                            .withId(fromString("5002d600-af66-11e8-b568-0800200c9a67"))
                            .withSummonsRequired(false)
                            .withNotificationRequired(false)
                            .build())
                    .withSubject(CourtApplicationParty.courtApplicationParty()
                            .withId(fromString("5002d600-af66-11e8-b568-0800200c9a68"))
                            .withSummonsRequired(false)
                            .withNotificationRequired(false)
                            .build())
                    .build())
            .withApplicationIdExists(false)
            .withCaseId(CASE_ID).build();

    private final static CreateCaseApplication APPLICATION_FOR_REOPENING_TYPE_APP = CreateCaseApplication.createCaseApplication()
            .withCourtApplication(CourtApplication.courtApplication()
                    .withId(APP_ID)
                    .withApplicationStatus(ApplicationStatus.DRAFT)
                    .withApplicationReceivedDate(DATE_RECEIVED.toString())
                    .withType(CourtApplicationType.courtApplicationType()
                            .withId(fromString("44c238d9-3bc2-3cf3-a2eb-a7d1437b8383"))
                            .withType("Application to reopen case")
                            .withCode("MC80524")
                            .withAppealFlag(false)
                            .build())
                    .withApplicant(CourtApplicationParty.courtApplicationParty()
                            .withId(fromString("5002d600-af66-11e8-b568-0800200c9a67"))
                            .withSummonsRequired(false)
                            .withNotificationRequired(false)
                            .build())
                    .withSubject(CourtApplicationParty.courtApplicationParty()
                            .withId(fromString("5002d600-af66-11e8-b568-0800200c9a68"))
                            .withSummonsRequired(false)
                            .withNotificationRequired(false)
                            .build())
                    .build())
            .withApplicationIdExists(false)
            .withCaseId(CASE_ID).build();

    private final static CreateCaseApplication APPEAL_TYPE_APP = CreateCaseApplication.createCaseApplication().withCourtApplication(CourtApplication.courtApplication()
            .withId(APP_ID)
            .withApplicationStatus(ApplicationStatus.DRAFT)
            .withApplicationReceivedDate(DATE_RECEIVED.toString())
            .withType(CourtApplicationType.courtApplicationType()
                    .withId(fromString("beb08419-0a9a-3119-b3ec-038d56c8a718"))
                    .withType("Appeal against Sentence")
                    .withCode("A")
                    .withAppealFlag(true)
                    .build())
            .withApplicant(CourtApplicationParty.courtApplicationParty()
                    .withId(fromString("5002d600-af66-11e8-b568-0800200c9a67"))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .withSubject(CourtApplicationParty.courtApplicationParty()
                    .withId(fromString("5002d600-af66-11e8-b568-0800200c9a68"))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .build())
            .withApplicationIdExists(false)
            .withCaseId(CASE_ID).build();

    private final static CreateCaseApplication STAT_DEC_CODE_DIFFERENT = CreateCaseApplication.createCaseApplication().withCourtApplication(CourtApplication.courtApplication()
            .withId(APP_ID)
            .withApplicationStatus(ApplicationStatus.DRAFT)
            .withApplicationReceivedDate(DATE_RECEIVED.toString())
            .withType(CourtApplicationType.courtApplicationType()
                    .withId(fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e"))
                    .withType("Appearance to make statutory declaration (SJP case)")
                    .withCode("MC80530")
                    .withAppealFlag(false)
                    .build())
            .withApplicant(CourtApplicationParty.courtApplicationParty()
                    .withId(fromString("5002d600-af66-11e8-b568-0800200c9a67"))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .withSubject(CourtApplicationParty.courtApplicationParty()
                    .withId(fromString("5002d600-af66-11e8-b568-0800200c9a68"))
                    .withSummonsRequired(false)
                    .withNotificationRequired(false)
                    .build())
            .build())
            .withApplicationIdExists(false)
            .withCaseId(CASE_ID)
            .build();
    private final static CreateCaseApplication STAT_DEC_APP_ID_ALREADY_EXIST = CreateCaseApplication.createCaseApplication()
            .withCourtApplication(CourtApplication.courtApplication()
                    .withId(APP_ID)
                    .withApplicationStatus(ApplicationStatus.DRAFT)
                    .withApplicationReceivedDate(DATE_RECEIVED.toString())
                    .withType(CourtApplicationType.courtApplicationType()
                            .withId(fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e"))
                            .withType("Appearance to make statutory declaration (SJP case)")
                            .withCode("MC80528")
                            .withAppealFlag(false)
                            .build())
                    .withApplicant(CourtApplicationParty.courtApplicationParty()
                            .withId(fromString("5002d600-af66-11e8-b568-0800200c9a67"))
                            .withSummonsRequired(false)
                            .withNotificationRequired(false)
                            .build())
                    .withSubject(CourtApplicationParty.courtApplicationParty()
                            .withId(fromString("5002d600-af66-11e8-b568-0800200c9a68"))
                            .withSummonsRequired(false)
                            .withNotificationRequired(false)
                            .build())
                    .build())
            .withApplicationIdExists(true)
            .withCaseId(CASE_ID).build();

    @BeforeEach
    public void beforeEachTest() {
        state = new CaseAggregateState();
        state.setCaseId(randomUUID());
        state.setDefendantFirstName("Diane");
        state.setDefendantLastName("Fossey");
        state.setDefendantId(randomUUID());
        state.setManagedByAtcm(true);
        state.setDefendantAddress(new uk.gov.moj.cpp.sjp.domain.Address("Flat 1","10 Oxford Road","","","","RG30 4RF"));
    }

    @Test
    public void shouldRejectApplicationForNotCompletedCase() {
        final Stream<Object> eventStream = CaseApplicationHandler.INSTANCE.createCaseApplication(state, STAT_DEC_TYPE_APP);
        final List<Object> eventList = eventStream.collect(Collectors.toList());
        thenTheApplicationIsRejected(eventList, STAT_DEC_TYPE_APP.getCourtApplication(), "Case is not completed");
    }

    @Test
    public void shouldRejectUnknownApplicationType() {
        state.markCaseCompleted();
        final Stream<Object> eventStream = CaseApplicationHandler.INSTANCE.createCaseApplication(state, UNKNOWN_TYPE_APP);
        final List<Object> eventList = eventStream.collect(Collectors.toList());
        thenTheApplicationIsRejected(eventList, UNKNOWN_TYPE_APP.getCourtApplication(), "Unrecognized application type or code");
    }

    @Test
    public void shouldRejectApplicationWhenACurrentApplicationActive() {
        state.markCaseCompleted();
        state.setCurrentApplication(new Application(null));
        state.getCurrentApplication().setType(STAT_DEC);
        state.getCurrentApplication().setStatus(STATUTORY_DECLARATION_PENDING);
        final Stream<Object> eventStream = CaseApplicationHandler.INSTANCE.createCaseApplication(state, STAT_DEC_TYPE_APP);
        final List<Object> eventList = eventStream.collect(Collectors.toList());
        thenTheApplicationIsRejected(eventList, STAT_DEC_TYPE_APP.getCourtApplication(), "Case has a pending application");
    }

    @Test
    public void shouldRejectApplicationWhenFutureReceivedDate() {
        state.markCaseCompleted();
        final Stream<Object> eventStream = CaseApplicationHandler.INSTANCE.createCaseApplication(state, STAT_DEC_WRONG_TYPE_APP);
        final List<Object> eventList = eventStream.collect(Collectors.toList());
        thenTheApplicationIsRejected(eventList, STAT_DEC_WRONG_TYPE_APP.getCourtApplication(), "Application received data is wrong");
    }

    @Test
    public void shouldRejectApplicationWhenApplicationIdAlreadyExist() {

        state.markCaseCompleted();
        final Stream<Object> eventStream = CaseApplicationHandler.INSTANCE.createCaseApplication(state, STAT_DEC_APP_ID_ALREADY_EXIST);
        final List<Object> eventList = eventStream.collect(Collectors.toList());
        thenTheApplicationIsRejected(eventList, STAT_DEC_APP_ID_ALREADY_EXIST.getCourtApplication(), "Application Id already exist");
    }

    @Test
    public void shouldRecordApplicationForStatDec() {
        state.markCaseCompleted();
        final Stream<Object> eventStream = CaseApplicationHandler.INSTANCE.createCaseApplication(state, STAT_DEC_TYPE_APP);
        final List<Object> eventList = eventStream.collect(Collectors.toList());
        thenApplicationIsAccepted(eventList, STAT_DEC_TYPE_APP.getCourtApplication());
        thenStatDecIsRecorded(eventList, STAT_DEC_TYPE_APP.getCourtApplication());
        thenApplicationStatusChanged(eventList, STATUTORY_DECLARATION_PENDING);
    }

    @Test
    public void shouldRecordApplicationForReopening() {
        state.markCaseCompleted();
        final Stream<Object> eventStream = CaseApplicationHandler.INSTANCE.createCaseApplication(state,
                APPLICATION_FOR_REOPENING_TYPE_APP);
        final List<Object> eventList = eventStream.collect(Collectors.toList());
        thenApplicationIsAccepted(eventList, APPLICATION_FOR_REOPENING_TYPE_APP.getCourtApplication());
        thenCaseApplicationForReopeningIsRecorded(eventList, APPLICATION_FOR_REOPENING_TYPE_APP.getCourtApplication());
        thenApplicationStatusChanged(eventList, REOPENING_PENDING);
    }

    @Test
    public void shouldRejectApplicationForAppeal() {
        state.markCaseCompleted();
        final Stream<Object> eventStream = CaseApplicationHandler.INSTANCE.createCaseApplication(state,
                APPEAL_TYPE_APP);
        final List<Object> eventList = eventStream.collect(Collectors.toList());
        thenTheApplicationIsRejected(eventList, APPEAL_TYPE_APP.getCourtApplication(), "Application received is for appeal");
    }

    @Test
    public void shouldRejectApplicationWithNotRecognizedApplicationCode() {
        state.markCaseCompleted();
        final Stream<Object> eventStream = CaseApplicationHandler.INSTANCE.createCaseApplication(state,
                STAT_DEC_CODE_DIFFERENT);
        final List<Object> eventList = eventStream.collect(Collectors.toList());
        thenTheApplicationIsRejected(eventList, STAT_DEC_CODE_DIFFERENT.getCourtApplication(), "Application code not recognised");
    }

    private void thenApplicationIsAccepted(final List<Object> eventList, final CourtApplication courtApplication) {
        assertThat(eventList, hasItem(allOf(
                isA(CaseApplicationRecorded.class),
                hasProperty("courtApplication", hasProperty("id", is(courtApplication.getId()))),
                hasProperty("courtApplication", notNullValue()),
                hasProperty("courtApplication", hasProperty("applicationStatus", is(courtApplication.getApplicationStatus()))),
                hasProperty("courtApplication", hasProperty("applicationReceivedDate", is(courtApplication.getApplicationReceivedDate()))),
                hasProperty("courtApplication", hasProperty("type", hasProperty("id", is(courtApplication.getType().getId())))),
                hasProperty("courtApplication", hasProperty("type", hasProperty("type", is(courtApplication.getType().getType())))),
                hasProperty("courtApplication", hasProperty("type", hasProperty("code", is(courtApplication.getType().getCode())))),
                hasProperty("courtApplication", hasProperty("type", hasProperty("appealFlag", is(courtApplication.getType().getAppealFlag())))),
                hasProperty("courtApplication", hasProperty("applicant", hasProperty("id", is(courtApplication.getApplicant().getId())))),
                hasProperty("courtApplication", hasProperty("applicant", hasProperty("summonsRequired", is(courtApplication.getApplicant().getSummonsRequired())))),
                hasProperty("courtApplication", hasProperty("applicant", hasProperty("notificationRequired", is(courtApplication.getApplicant().getNotificationRequired())))),
                hasProperty("courtApplication", hasProperty("subject", hasProperty("id", is(courtApplication.getSubject().getId())))),
                hasProperty("courtApplication", hasProperty("subject", hasProperty("summonsRequired", is(courtApplication.getSubject().getSummonsRequired())))),
                hasProperty("courtApplication", hasProperty("subject", hasProperty("notificationRequired", is(courtApplication.getSubject().getNotificationRequired())))),
                hasProperty("courtApplication", hasProperty("applicationReference", notNullValue()))
                )
        ));
    }

    private void thenStatDecIsRecorded(final List<Object> eventList, final CourtApplication courtApplication) {
        assertThat(eventList, hasItem(allOf(
                isA(CaseStatDecRecorded.class),
                hasProperty("applicationId", notNullValue()),
                hasProperty("applicant", hasProperty("id", is(courtApplication.getApplicant().getId()))),
                hasProperty("applicant", hasProperty("summonsRequired", is(courtApplication.getApplicant().getSummonsRequired()))),
                hasProperty("applicant", hasProperty("notificationRequired", is(courtApplication.getApplicant().getNotificationRequired())))
        )));
    }

    private void thenCaseApplicationForReopeningIsRecorded(final List<Object> eventList, final CourtApplication courtApplication) {
        assertThat(eventList, hasItem(allOf(
                isA(CaseApplicationForReopeningRecorded.class),
                hasProperty("applicationId", notNullValue()),
                hasProperty("applicant", hasProperty("id", is(courtApplication.getApplicant().getId()))),
                hasProperty("applicant", hasProperty("summonsRequired", is(courtApplication.getApplicant().getSummonsRequired()))),
                hasProperty("applicant", hasProperty("notificationRequired", is(courtApplication.getApplicant().getNotificationRequired())))
                )
        ));
    }

    private void thenApplicationStatusChanged(final List<Object> eventList,
                                              final uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus status) {
        assertThat(eventList, hasItem(allOf(
                isA(ApplicationStatusChanged.class),
                hasProperty("applicationId", notNullValue()),
                hasProperty("status", equalTo(status))
        )));
    }

    private void thenTheApplicationIsRejected(final List<Object> eventList, CourtApplication courtApplication, final String rejectionReason) {
        assertThat(eventList, hasItem(new CaseApplicationRejected(courtApplication.getId().toString(), state.getCaseId().toString(), rejectionReason)));
    }
}