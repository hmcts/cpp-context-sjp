package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.Optional.of;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.core.courts.SummonsTemplateType.NOT_APPLICABLE;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_PENDING;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.command.handler.common.EventNamesHolder.CASE_APPLICATION_CREATED;
import static uk.gov.moj.cpp.sjp.command.handler.common.EventNamesHolder.CASE_MARKED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.command.handler.common.EventNamesHolder.CASE_STAT_DECS;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.APPLICATION_PENDING;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED_APPLICATION_PENDING;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import org.hamcrest.Matchers;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.json.schemas.domains.sjp.commands.CreateCaseApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.command.handler.common.EventNamesHolder;
import uk.gov.moj.cpp.sjp.command.handler.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.ApplicationStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRecorded;
import uk.gov.moj.cpp.sjp.event.CaseApplicationRejected;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseStatDecRecorded;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.JsonObject;

@ExtendWith(MockitoExtension.class)
public class CreateCaseApplicationHandlerTest {

    private static final String CONTACT_EMAIL_ADDRESS_PREFIX = STRING.next();
    private static final String EMAIL_ADDRESS_SUFFIX = "@justice.gov.uk";
    private static final String PROSECUTOR_OU_CODE = randomAlphanumeric(8);
    private static final String PROSECUTOR_MAJOR_CREDITOR_CODE = randomAlphanumeric(12);
    private static final String RESENTENCING_ACTIVATION_CODE = "AJ0001";
    private static final String ORG_OFFENCE_WORDING = "On 12/10/2020 at 10:100am on the corner of the hugh street outside the dog and duck in Croydon you did something wrong";
    private static final String ORG_OFFENCE_WORDING_WELSH = "On 12/10/2020 at 10:100am on the corner of the hugh street outside the";
    private static final String ORG_OFFENCE_CODE = "OFC0001";
    private static final UUID TYPE_ID_FOR_SUSPENDED_SENTENCE_ORDER = fromString("8b1cff00-a456-40da-9ce4-f11c20959084");

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private Requester requester;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CaseApplicationRecorded.class, CaseStatDecRecorded.class, CaseApplicationRejected.class,
            CaseMarkedReadyForDecision.class, CaseStatusChanged.class, ApplicationStatusChanged.class);

    @InjectMocks
    private CreateCaseApplicationHandler createCaseApplicationHandler;

    private final CaseAggregate caseAggregate = new CaseAggregate();

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptor;

    private static final UUID CASE_ID = randomUUID();
    private static final UUID APP_ID = randomUUID();
    private static final UUID TYPE_ID = randomUUID();
    private static final UUID APPLICANT_ID = randomUUID();
    private static final String PRIORITY = "HIGH";


    final Metadata metadata = Envelope
            .metadataBuilder()
            .withName("sjp.command.create-case-application")
            .withId(randomUUID())
            .build();

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        caseAggregate.getState().markCaseCompleted();
        caseAggregate.getState().setCaseId(randomUUID());
        caseAggregate.getState().setManagedByAtcm(true);
        caseAggregate.apply(new CaseApplicationRecorded.Builder()
                .withCourtApplication(new CourtApplication.Builder().build())
                .build());
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new CreateCaseApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("createCaseApplication")
                        .thatHandles("sjp.command.handler.create-case-application")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), any(UUID.class))).thenReturn(empty());
        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication(false)));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldAddProsecutorFromCaseWhenCreateCaseApplication_NoExisting3rdParty() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final CreateCaseApplication createCaseApplication = CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withId(APP_ID) // randomUUID()
                        .withApplicationReference(STRING.next())
                        .withApplicationReceivedDate("2020-09-03")
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(true)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationParty(prosecutor1))
                        .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next())
                                        .withProsecutionAuthorityId(prosecutor2)
                                        .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                        .build())//prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                .withIsSJP(true)
                                .withCaseStatus("ACTIVE")
                                .build())).build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor1))).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor2))).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(subject))).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(respondent))).thenReturn(of(buildProsecutorQueryResult(respondent, "respondent")));
        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(1)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].summonsRequired", is(true)),
                                withJsonPath("$.courtApplication.thirdParties[0].notificationRequired", is(true)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is(prosecutor2AuthCode)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor2 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor2 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor2 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    private JsonObject buildProsecutorQueryResult(final UUID prosecutorId, final String prosecutor) {
        return createObjectBuilder()
                .add("id", prosecutorId.toString())
                .add("fullName", prosecutor + " Name")
                .add("majorCreditorCode", PROSECUTOR_MAJOR_CREDITOR_CODE)
                .add("oucode", PROSECUTOR_OU_CODE)
                .add("nameWelsh", prosecutor + " WelshName")
                .add("address", createObjectBuilder()
                        .add("address1", prosecutor + " Address line 1")
                        .build())
                .add("informantEmailAddress", prosecutor + EMAIL_ADDRESS_SUFFIX)
                .add("contactEmailAddress", CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)
                .build();
    }

    @Test
    public void shouldAddProsecutorFromCaseWhenCourtApplicationProceedingsInitiated_Existing3rdParty() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID prosecutor3 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final CreateCaseApplication createCaseApplication = CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(APP_ID)
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(true)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationParty(prosecutor1))
                        .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor3)))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                        .withProsecutionAuthorityId(prosecutor2)
                                        .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                        .build())
                                .withCaseStatus("ACTIVE")
                                .build()))
                        .build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor1))).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor2))).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor3))).thenReturn(of(buildProsecutorQueryResult(prosecutor3, "prosecutor3")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(subject))).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(respondent))).thenReturn(of(buildProsecutorQueryResult(respondent, "respondent")));

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(2)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor3 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor3 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor3 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.thirdParties[1].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityCode", is(prosecutor2AuthCode)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.name", is("prosecutor2 Name")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.welshName", is("prosecutor2 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.address.address1", is("prosecutor2 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))

                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldNotAddProsecutorFromCaseWhenCourtApplicationProceedingsInitiated_3rdParty() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();

        final CreateCaseApplication createCaseApplication = CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(APP_ID)
                        .withApplicationReceivedDate("2020-09-03")
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(true)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationParty(prosecutor1))
                        .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor2)))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                        .withProsecutionAuthorityId(prosecutor2)
                                        .build())
                                .withCaseStatus("ACTIVE")
                                .build()))
                        .build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor1))).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor2))).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(subject))).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(respondent))).thenReturn(of(buildProsecutorQueryResult(respondent, "respondent")));

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(1)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor2 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor2 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor2 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))

                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldNotAddProsecutorFromCaseWhenCourtApplicationProceedingsInitiated_Applicant() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID subject = randomUUID();

        final CreateCaseApplication createCaseApplication = CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(APP_ID)
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(true)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationParty(prosecutor2))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor1)))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                        .withProsecutionAuthorityId(prosecutor2)
                                        .build())
                                .withCaseStatus("ACTIVE")
                                .build()))
                        .build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor2))).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor1))).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(subject))).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(1)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor1.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor1.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor1 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor1 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor1 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldNotAddProsecutorFromCaseWhenCourtApplicationProceedingsInitiated_Respondent() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID prosecutor3 = randomUUID();
        final UUID subject = randomUUID();

        final CreateCaseApplication createCaseApplication = CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(APP_ID)
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(true)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationParty(prosecutor1))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor3)))
                        .withRespondents(singletonList(buildCourtApplicationParty(prosecutor2)))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                        .withProsecutionAuthorityId(prosecutor2)
                                        .build())
                                .withCaseStatus("ACTIVE")
                                .build()))
                        .build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor1))).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor2))).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor3))).thenReturn(of(buildProsecutorQueryResult(prosecutor3, "prosecutor3")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(subject))).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(1)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor3 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor3 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor3 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldNotAddProsecutorFromCaseWhenCourtApplicationProceedingsInitiated_ProsecutorThirdPartyFlagFalse() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID prosecutor3 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final CreateCaseApplication createCaseApplication = CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(APP_ID)
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(false)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationParty(prosecutor1))
                        .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor3)))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                        .withProsecutionAuthorityId(prosecutor2)
                                        .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                        .build())
                                .withCaseStatus("ACTIVE")
                                .build()))
                        .build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor1))).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor3))).thenReturn(of(buildProsecutorQueryResult(prosecutor3, "prosecutor3")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(subject))).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(respondent))).thenReturn(of(buildProsecutorQueryResult(respondent, "respondent")));

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(1)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor3)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor3 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor3 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor3 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldAddProsecutorFromCourtOrdersWhenCreateCaseApplication_NoExisting3rdParty() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final String prosecutor2AuthCode = STRING.next();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();

        final CreateCaseApplication createCaseApplication = CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(APP_ID)
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(true)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationParty(prosecutor1))
                        .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withCourtOrder(CourtOrder.courtOrder()
                                .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityId(prosecutor2)
                                                .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                                .build())
                                        .withOffence(Offence.offence().withOffenceCode(STRING.next()).build())
                                        .build()))
                                .build())
                        .build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor1))).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor2))).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(subject))).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(respondent))).thenReturn(of(buildProsecutorQueryResult(respondent, "respondent")));

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(1)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].summonsRequired", is(true)),
                                withJsonPath("$.courtApplication.thirdParties[0].notificationRequired", is(true)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is(prosecutor2AuthCode)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor2 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor2 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor2 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldEnrichProsecutorInformationForCourtApplicationParties() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID prosecutor3 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent1 = randomUUID();
        final UUID respondent2 = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final CreateCaseApplication createCaseApplication = CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(APP_ID)
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(true)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationParty(prosecutor1))
                        .withRespondents(asList(buildCourtApplicationParty(respondent1), buildCourtApplicationParty(respondent2)))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor3)))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                        .withProsecutionAuthorityId(prosecutor2)
                                        .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                        .build())
                                .build()))
                        .build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor1))).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor2))).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor3))).thenReturn(of(buildProsecutorQueryResult(prosecutor3, "prosecutor3")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(subject))).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(respondent1))).thenReturn(of(buildProsecutorQueryResult(respondent1, "respondent1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(respondent2))).thenReturn(of(buildProsecutorQueryResult(respondent2, "respondent2")));

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(2)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor3 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor3 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor3 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.thirdParties[1].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityCode", is(prosecutor2AuthCode)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.name", is("prosecutor2 Name")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.welshName", is("prosecutor2 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.address.address1", is("prosecutor2 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.applicant.id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityId", is(prosecutor1.toString())),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor1.toString())),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.name", is("prosecutor1 Name")),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.welshName", is("prosecutor1 WelshName")),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.address.address1", is("prosecutor1 Address line 1")),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.subject.id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.prosecutionAuthorityId", is(subject.toString())),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + subject.toString())),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.name", is("subject Name")),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.welshName", is("subject WelshName")),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.address.address1", is("subject Address line 1")),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.respondents.length()", is(2)),
                                withJsonPath("$.courtApplication.respondents[0].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.prosecutionAuthorityId", is(respondent1.toString())),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + respondent1.toString())),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.name", is("respondent1 Name")),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.welshName", is("respondent1 WelshName")),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.address.address1", is("respondent1 Address line 1")),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.respondents[1].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.prosecutionAuthorityId", is(respondent2.toString())),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + respondent2.toString())),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.name", is("respondent2 Name")),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.welshName", is("respondent2 WelshName")),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.address.address1", is("respondent2 Address line 1")),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldNotEnrichProsecutorInformationForNonStdIndividualProsecutor() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID prosecutor3 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent1 = randomUUID();
        final UUID respondent2 = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final CreateCaseApplication createCaseApplication = CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(APP_ID)
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(true)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationPartyIndividual(prosecutor1, "Non STD Individual Prosecutor"))
                        .withRespondents(asList(buildCourtApplicationParty(respondent1), buildCourtApplicationParty(respondent2)))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor3)))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                        .withProsecutionAuthorityId(prosecutor2)
                                        .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                        .build())
                                .build()))
                        .build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor1))).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor2))).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor3))).thenReturn(of(buildProsecutorQueryResult(prosecutor3, "prosecutor3")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(subject))).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(respondent1))).thenReturn(of(buildProsecutorQueryResult(respondent1, "respondent1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(respondent2))).thenReturn(of(buildProsecutorQueryResult(respondent2, "respondent2")));

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.applicant.id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityId", is(prosecutor1.toString())),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor1.toString())),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.firstName", is("Non STD Individual Prosecutor"))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", notNullValue())
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldNotEnrichProsecutorInformationForNonStdOrganisationProsecutor() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID prosecutor3 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent1 = randomUUID();
        final UUID respondent2 = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final CreateCaseApplication createCaseApplication = CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(APP_ID)
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(true)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationPartyOrganisation(prosecutor1, "Non STD Organisation Prosecutor"))
                        .withRespondents(asList(buildCourtApplicationParty(respondent1), buildCourtApplicationParty(respondent2)))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor3)))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next())
                                        .withProsecutionAuthorityId(prosecutor2)
                                        .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                        .build())
                                .withIsSJP(true)
                                .build()))
                        .build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor1))).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor2))).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor3))).thenReturn(of(buildProsecutorQueryResult(prosecutor3, "prosecutor3")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(subject))).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(respondent1))).thenReturn(of(buildProsecutorQueryResult(respondent1, "respondent1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(respondent2))).thenReturn(of(buildProsecutorQueryResult(respondent2, "respondent2")));

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.applicant.id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityId", is(prosecutor1.toString())),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor1.toString()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", notNullValue())
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    private CourtApplicationParty buildCourtApplicationPartyIndividual(final UUID prosecutionAuthorityId, final String firstName) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                        .withProsecutionAuthorityId(prosecutionAuthorityId)
                        .withProsecutionAuthorityCode("Code_" + prosecutionAuthorityId.toString())
                        .withFirstName(firstName)
                        .build())
                .build();
    }

    private CourtApplicationParty buildCourtApplicationPartyOrganisation(final UUID prosecutionAuthorityId, final String name) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                        .withProsecutionAuthorityId(prosecutionAuthorityId)
                        .withProsecutionAuthorityCode("Code_" + prosecutionAuthorityId.toString())
                        .withName(name)
                        .build())
                .build();
    }

    @Test
    public void shouldUpdateOffenceWordingWhenCourtOrderIsNotSuspendedSentence() throws EventStreamException {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        offenceWordingTestForCourtOrder(randomUUID());
    }

    @Test
    public void shouldUpdateOffenceWordingWhenCourtOrderIsSuspendedSentence() throws EventStreamException {
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        offenceWordingTestForCourtOrder(TYPE_ID_FOR_SUSPENDED_SENTENCE_ORDER);
    }

    private void offenceWordingTestForCourtOrder(final UUID judicialResultTypeId) throws EventStreamException {
        CreateCaseApplication createCaseApplication = buildCreateCaseApplication(judicialResultTypeId, true, false);

        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), any(UUID.class))).thenReturn(empty());

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        final String prefix;
        if (TYPE_ID_FOR_SUSPENDED_SENTENCE_ORDER.equals(judicialResultTypeId)) {
            prefix = "Activation of a suspended sentence order.";
        } else {
            prefix = "Resentenced";
        }
        final String expectedWording = prefix + " Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING;
        final String expectedWordingWelsh = prefix + " Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING_WELSH;
        final CourtOrder courtOrder = createCaseApplication.getCourtApplication().getCourtOrder();
        final UUID offenceId = courtOrder.getCourtOrderOffences().get(0).getOffence().getId();
        final String prosecutor2AuthCode = createCaseApplication.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.offenceCode", is(RESENTENCING_ACTIVATION_CODE)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wording", is(expectedWording)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wordingWelsh", is(expectedWordingWelsh)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.id", is(offenceId.toString())),
                                withJsonPath("$.courtApplication.courtOrder.id", is(courtOrder.getId().toString())),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseIdentifier.prosecutionAuthorityCode", is(prosecutor2AuthCode))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    private CreateCaseApplication buildCreateCaseApplication(final UUID judicialResultTypeId, final boolean withCourtOrder, final boolean withApplicationCases) {
        return buildCreateCaseApplication(judicialResultTypeId, withCourtOrder, withApplicationCases, false);
    }

    private CreateCaseApplication buildCreateCaseApplication(final UUID judicialResultTypeId, final boolean withCourtOrder, final boolean withApplicationCases, final boolean withSummonsApprovalRequired) {
        return buildCreateCaseApplication(judicialResultTypeId, withCourtOrder, withApplicationCases, withSummonsApprovalRequired, true);
    }

    private CreateCaseApplication buildCreateCaseApplication(final UUID judicialResultTypeId, final boolean withCourtOrder, final boolean withApplicationCases, final boolean withSummonsApprovalRequired, final boolean courtHearing) {
        return buildCreateCaseApplication(judicialResultTypeId, withCourtOrder, withApplicationCases, withSummonsApprovalRequired, courtHearing, LinkType.LINKED, "INACTIVE");
    }

    private CreateCaseApplication buildFirstHearingApplicationCourtProceedings() {
        return buildCreateCaseApplication(null, false, true, false, true, LinkType.FIRST_HEARING, "ACTIVE");
    }

    private CreateCaseApplication buildCreateCaseApplication(final UUID judicialResultTypeId, final boolean withCourtOrder, final boolean withApplicationCases, final boolean withSummonsApprovalRequired, final boolean courtHearing, final LinkType linkType, final String caseStatus) {
        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final String prosecutor2AuthCode = STRING.next();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();
        final UUID offenceId = randomUUID();
        final CourtOrder courtOrder;
        if (withCourtOrder) {
            courtOrder = CourtOrder.courtOrder()
                    .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                            .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                    .withProsecutionAuthorityId(prosecutor2)
                                    .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                    .build())
                            .withOffence(Offence.offence()
                                    .withOffenceCode(ORG_OFFENCE_CODE)
                                    .withWording(ORG_OFFENCE_WORDING)
                                    .withWordingWelsh(ORG_OFFENCE_WORDING_WELSH)
                                    .withId(offenceId)
                                    .build())
                            .build()))
                    .withId(randomUUID())
                    .withJudicialResultTypeId(judicialResultTypeId)
                    .build();
        } else {
            courtOrder = null;
        }
        final List<CourtApplicationCase> courtApplicationCases;
        if (withApplicationCases) {
            courtApplicationCases = singletonList(courtApplicationCase()
                    .withIsSJP(false)
                    .withCaseStatus(caseStatus)
                    .withProsecutionCaseId(randomUUID())
                    .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                            .withProsecutionAuthorityId(randomUUID()).build())
                    .withOffences(singletonList(Offence.offence()
                            .withOffenceCode(ORG_OFFENCE_CODE)
                            .withWording(ORG_OFFENCE_WORDING)
                            .withWordingWelsh(ORG_OFFENCE_WORDING_WELSH)
                            .withId(offenceId)
                            .build()))
                    .build());
        } else {
            courtApplicationCases = null;
        }

        return CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(APP_ID)
                        .withType(courtApplicationType()
                                .withLinkType(linkType)
                                .withProsecutorThirdPartyFlag(true)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                // .withSummonsTemplateType(Boolean.TRUE.equals(withSummonsApprovalRequired) ? GENERIC_APPLICATION : NOT_APPLICABLE)
                                .withResentencingActivationCode(RESENTENCING_ACTIVATION_CODE)
                                .withPrefix("Resentenced")
                                .build())
                        .withApplicant(buildCourtApplicationParty(prosecutor1))
                        .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withCourtOrder(courtOrder)
                        .withCourtApplicationCases(courtApplicationCases)
                        .build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();
    }

    @Test
    public void shouldNotUpdateOffenceDetailsForFirstHearingApplication() throws EventStreamException {
        CreateCaseApplication createCaseApplication = buildFirstHearingApplicationCourtProceedings();


        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), any(UUID.class))).thenReturn(empty());

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        final CourtApplicationCase courtApplicationCase = createCaseApplication.getCourtApplication().getCourtApplicationCases().get(0);
        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", is(courtApplicationCase.getProsecutionCaseId().toString())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].id", is(courtApplicationCase.getOffences().get(0).getId().toString())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].offenceCode", is(courtApplicationCase.getOffences().get(0).getOffenceCode())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].wording", is(courtApplicationCase.getOffences().get(0).getWording())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].wordingWelsh", is(courtApplicationCase.getOffences().get(0).getWordingWelsh()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldUpdateOffenceWordingWhenCourtOrderNotExist() throws EventStreamException {
        CreateCaseApplication createCaseApplication = buildCreateCaseApplication(randomUUID(), false, true);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), any(UUID.class))).thenReturn(empty());
        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        final String expectedWording = "Resentenced Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING;
        final String expectedWordingWelsh = "Resentenced Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING_WELSH;
        final UUID offenceId = createCaseApplication.getCourtApplication().getCourtApplicationCases().get(0).getOffences().get(0).getId();
        final UUID prosecutionCaseId = createCaseApplication.getCourtApplication().getCourtApplicationCases().get(0).getProsecutionCaseId();
        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(hasNoJsonPath("$.courtApplication.courtOrders"),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].isSJP", is(false)),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", is(prosecutionCaseId.toString())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].id", is(offenceId.toString())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].offenceCode", is(RESENTENCING_ACTIVATION_CODE)),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].wording", is(expectedWording)),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].wordingWelsh", is(expectedWordingWelsh))
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldNotUpdateFutureSummonsHearingWhenCourtHearingIsNotAvailable() throws EventStreamException {
        CreateCaseApplication createCaseApplication = buildCreateCaseApplication(randomUUID(), false, true, true, false);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), any(UUID.class))).thenReturn(empty());
        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payload().isJson(not(
                                withJsonPath("$.courtApplication.futureSummonsHearing", nullValue())))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldUpdateOffenceWordingWhenCourtApplicationCasesNotExist() throws EventStreamException {
        CreateCaseApplication createCaseApplication = buildCreateCaseApplication(randomUUID(), true, false);
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), any(UUID.class))).thenReturn(empty());
        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        final String expectedWording = "Resentenced Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING;
        final String expectedWordingWelsh = "Resentenced Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING_WELSH;
        final CourtOrder courtOrder = createCaseApplication.getCourtApplication().getCourtOrder();
        final UUID offenceId = courtOrder.getCourtOrderOffences().get(0).getOffence().getId();
        final String prosecutor2AuthCode = createCaseApplication.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getProsecutionCaseIdentifier().getProsecutionAuthorityCode();

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.offenceCode", is(RESENTENCING_ACTIVATION_CODE)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wording", is(expectedWording)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wordingWelsh", is(expectedWordingWelsh)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.id", is(offenceId.toString())),
                                withJsonPath("$.courtApplication.courtOrder.id", is(courtOrder.getId().toString())),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseIdentifier.prosecutionAuthorityCode", is(prosecutor2AuthCode)),
                                hasNoJsonPath("$.courtApplication.courtApplicationCases")
                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldAddProsecutorFromCaseWhenCourtApplicationProceedingsInitiated_Existing3rdPartyWithoutProsecutingAuthority() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final CreateCaseApplication createCaseApplication = CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(APP_ID)
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(true)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationParty(prosecutor1))
                        .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withThirdParties(singletonList(buildCourtApplicationPartyWithoutProsecutingAuthority()))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                        .withProsecutionAuthorityId(prosecutor2)
                                        .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                        .build())
                                .withCaseStatus("ACTIVE")
                                .build()))
                        .build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor1))).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor2))).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(subject))).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(respondent))).thenReturn(of(buildProsecutorQueryResult(respondent, "respondent")));

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(2)),
                                withJsonPath("$.courtApplication.thirdParties[1].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityCode", is(prosecutor2AuthCode)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.name", is("prosecutor2 Name")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.welshName", is("prosecutor2 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.address.address1", is("prosecutor2 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))

                        ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }

    @Test
    public void shouldNotAddProsecutingAuthorityToCourtApplicationPartyWhenRespondentIsDefendant() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final CreateCaseApplication createCaseApplication = CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(APP_ID)
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(true)
                                .withId(TYPE_ID)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationParty(prosecutor1))
                        .withRespondents(singletonList(buildCourtApplicationPartyWithoutProsecutingAuthority()))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withThirdParties(singletonList(buildCourtApplicationPartyWithoutProsecutingAuthority()))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                        .withProsecutionAuthorityId(prosecutor2)
                                        .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                        .build())
                                .withCaseStatus("ACTIVE")
                                .build()))
                        .build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(false)
                .build();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor1))).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(prosecutor2))).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(JsonEnvelope.class), eq(subject))).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));

        createCaseApplicationHandler.createCaseApplication(envelopeFrom(metadata, createCaseApplication));

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), streamContaining(
                jsonEnvelope(metadata().withName(CASE_APPLICATION_CREATED),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.courtApplication.id", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.courtApplication.type.id", CoreMatchers.equalTo(TYPE_ID.toString())),
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.courtApplication", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(2)),
                                withJsonPath("$.courtApplication.thirdParties[1].id", Matchers.notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityCode", is(prosecutor2AuthCode)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.name", is("prosecutor2 Name")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.welshName", is("prosecutor2 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.address.address1", is("prosecutor2 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.respondents.length()", is(1)),
                                withJsonPath("$.courtApplication.respondents[0].id", is(APPLICANT_ID.toString())),
                                        withoutJsonPath("$.courtApplication.respondents[0].prosecutingAuthority")

                                ))),
                jsonEnvelope(metadata().withName(CASE_STAT_DECS),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.applicant.id", CoreMatchers.equalTo(APPLICANT_ID.toString()))
                        ))),
                jsonEnvelope(metadata().withName(ApplicationStatusChanged.EVENT_NAME),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.applicationId", CoreMatchers.equalTo(APP_ID.toString())),
                                withJsonPath("$.status", CoreMatchers.equalTo(STATUTORY_DECLARATION_PENDING.name()))
                        ))),
                jsonEnvelope(metadata().withName(CASE_MARKED_READY_FOR_DECISION),
                        payloadIsJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.reason", equalTo(APPLICATION_PENDING.toString())),
                                withJsonPath("$.priority", equalTo(PRIORITY))
                        ))),
                jsonEnvelope(metadata().withName(EventNamesHolder.CASE_STATUS_CHANGED),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", notNullValue()),
                                withJsonPath("$.caseStatus", equalTo(COMPLETED_APPLICATION_PENDING.toString()))
                        )))
        ));
    }


    private CourtApplicationParty buildCourtApplicationPartyWithoutProsecutingAuthority() {
        return CourtApplicationParty.courtApplicationParty()
                .withId(APPLICANT_ID)
                .withProsecutingAuthority(null)
                .build();
    }

    private CourtApplicationParty buildCourtApplicationParty(final UUID prosecutionAuthorityId) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(APPLICANT_ID)
                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                        .withProsecutionAuthorityId(prosecutionAuthorityId)
                        .withProsecutionAuthorityCode("Code_" + prosecutionAuthorityId.toString())
                        .build())
                .build();
    }

    public CreateCaseApplication createCaseApplication(final boolean isApplicationExist) {

        return CreateCaseApplication.createCaseApplication()
                .withCourtApplication(courtApplication()
                        .withId(APP_ID)
                        .withApplicationReceivedDate("2020-09-03")
                        .withType(courtApplicationType()
                                .withId(TYPE_ID)
                                .withProsecutorThirdPartyFlag(false)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .withType("STANDALONE")
                                .withCode("MC80528")
                                .build())
                        .withApplicant(buildCourtApplicationParty(randomUUID()))
                        .withSubject(buildCourtApplicationParty(randomUUID()))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                .withCaseStatus("ACTIVE")
                                .build())).build())
                .withCaseId(CASE_ID)
                .withApplicationIdExists(isApplicationExist)
                .build();
    }
}
