package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.Month.JULY;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.sjp.domain.CaseAssignmentType.DELEGATED_POWERS_DECISION;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_ALREADY_RESERVED;
import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_ALREADY_UNRESERVED;
import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_RESERVED;
import static uk.gov.moj.sjp.it.Constants.EVENT_CASE_UNRESERVED;
import static uk.gov.moj.sjp.it.Constants.OFFENCE_DATE_CODE_FOR_BETWEEN;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_ALREADY_RESERVED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_ALREADY_UNRESERVED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_RESERVED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_UNRESERVED;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseAssignedToUser;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubFixedLists;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForAllProsecutors;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForVerdictTypes;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static org.hamcrest.Matchers.allOf;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseByIdWithDocumentMetadata;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;


import com.google.common.collect.Sets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.log.Log;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor;
import uk.gov.moj.cpp.sjp.event.session.CaseAssigned;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.commandclient.AssignNextCaseClient;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.ReserveCaseHelper;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.util.ActivitiHelper;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.util.builders.FinancialImpositionBuilder;
import uk.gov.moj.sjp.it.util.builders.FinancialPenaltyBuilder;

public class ReserveCaseIT extends BaseIntegrationTest {

    private static final String NATIONAL_COURT_CODE = "1080";

    private final EventListener eventListener = new EventListener();

    private final UUID caseId = randomUUID();
    private final UUID offenceId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final UUID legalAdviserId = randomUUID();
    private final UUID courtAdminId = randomUUID();
    private final UUID systemAdminId = randomUUID();
    private final LocalDate defendantDateOfBirth = LocalDate.of(1980, JULY, 15);
    private final User user = new User("Integration", "Tester", legalAdviserId);
    private final String urn = generate(TFL);
    private String caseUrn;

    @Before
    public void beforeEveryTest() throws SQLException {
        final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
        databaseCleaner.cleanViewStore();

        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubResultIds();
        stubFixedLists();
        stubProsecutorQuery(TFL.name(), TFL.getFullName(), randomUUID());
        stubForUserDetails(user, "ALL");
        stubAllResultDefinitions();
        stubQueryForVerdictTypes();
        stubQueryForAllProsecutors();
        stubBailStatuses();
        stubResultIds();

        stubGroupForUser(legalAdviserId, "Legal Advisers");
        stubGroupForUser(courtAdminId, "Court Administrators");
        stubGroupForUser(systemAdminId, "System Users");

        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));


        final CreateCase.CreateCasePayloadBuilder caseBuilder = CreateCase
                .CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(TFL)
                .withDefendantId(defendantId)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withOffenceId(offenceId)
                .withOffenceCode(DEFAULT_OFFENCE_CODE)
                .withLibraOffenceDateCode(OFFENCE_DATE_CODE_FOR_BETWEEN)
                .withUrn(urn);

        caseUrn = caseBuilder.getUrn();

        stubEnforcementAreaByPostcode(caseBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "DEFENDANT_REGION");

        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(caseBuilder))
                .popEvent(CaseMarkedReadyForDecision.EVENT_NAME);
    }

    @Test
    public void shouldReserveCaseThenUnReserveCaseWithLegalAdviser() throws Exception{
        try(ReserveCaseHelper  reserveCaseHelper = new ReserveCaseHelper()){
            reserveCaseToUser(caseId, legalAdviserId, ACCEPTED);

            final JsonEnvelope eventCaseReservedPrivateEvent =  reserveCaseHelper.getEventFromTopic();

            assertThat(eventCaseReservedPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_CASE_RESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo(legalAdviserId.toString())),
                            withJsonPath("$.reservedAt", notNullValue())
                    )))));

            final JsonEnvelope eventCaseReservedPublicEvent =  reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_RESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo(legalAdviserId.toString())),
                            withJsonPath("$.reservedAt", notNullValue())
                    )))));

            reserveCaseToUser(caseId, legalAdviserId, ACCEPTED);

            final JsonEnvelope eventCaseAlreadyReservedPrivateEvent =  reserveCaseHelper.getEventFromTopic();

            assertThat(eventCaseAlreadyReservedPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_CASE_ALREADY_RESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString()))
                    )))));

            final JsonEnvelope eventCaseAlreadyReservedPublicEvent =  reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseAlreadyReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_ALREADY_RESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString()))
                    )))));

            final UUID sessionId = randomUUID();
            verifyCaseAssigned(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, caseId, legalAdviserId, sessionId);

            pollWithDefaults(getCaseByIdWithDocumentMetadata(caseId, USER_ID))
                    .until(
                            status().is(OK),
                            ResponsePayloadMatcher.payload().isJson(CoreMatchers.allOf(
                                    withJsonPath("$.id", Matchers.equalTo(caseId.toString())),
                                    withJsonPath("$.reservedBy", Matchers.equalTo(legalAdviserId.toString())),
                                    withJsonPath("$.reservedByName", Matchers.equalTo("Integration Tester")),
                                    withJsonPath("$.reservedAt", notNullValue()))
                            ));

            unReserveCaseToUser(caseId, legalAdviserId, ACCEPTED);

            final JsonEnvelope eventCaseUnReservedPrivateEvent =  reserveCaseHelper.getEventFromTopic();

            assertThat(eventCaseUnReservedPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_CASE_UNRESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo(legalAdviserId.toString()))
                    )))));

            final JsonEnvelope eventCaseUnReservedPublicEvent =  reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseUnReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_UNRESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo(legalAdviserId.toString()))
                    )))));

            pollWithDefaults(getCaseByIdWithDocumentMetadata(caseId, USER_ID))
                    .until(
                            status().is(OK),
                            ResponsePayloadMatcher.payload().isJson(CoreMatchers.allOf(
                                    withJsonPath("$.id", Matchers.equalTo(caseId.toString())),
                                    withoutJsonPath("$.reservedBy"),
                                    withoutJsonPath("$.reservedByName"),
                                    withoutJsonPath("$.reservedAt")
                            )));

            unReserveCaseToUser(caseId, legalAdviserId, ACCEPTED);

            final JsonEnvelope eventCaseAlreadyUnReservedPrivateEvent =  reserveCaseHelper.getEventFromTopic();

            assertThat(eventCaseAlreadyUnReservedPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_CASE_ALREADY_UNRESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString()))
                    )))));

            final JsonEnvelope eventCaseAlreadyUnReservedPublicEvent =  reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseAlreadyUnReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_ALREADY_UNRESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString()))
                    )))));
        }
    }

    @Test
    public void shouldUndoReserveCaseWhenTimeOut() throws Exception{
        try(ReserveCaseHelper  reserveCaseHelper = new ReserveCaseHelper()) {
            reserveCaseToUser(caseId, legalAdviserId, ACCEPTED);

            final JsonEnvelope eventCaseReservedPrivateEvent =  reserveCaseHelper.getEventFromTopic();

            assertThat(eventCaseReservedPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_CASE_RESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo(legalAdviserId.toString())),
                            withJsonPath("$.reservedAt", notNullValue())
                    )))));

            final JsonEnvelope eventCaseReservedPublicEvent =  reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_RESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo(legalAdviserId.toString())),
                            withJsonPath("$.reservedAt", notNullValue())
                    )))));

            final UUID sessionId = randomUUID();
            verifyCaseAssigned(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, caseId, legalAdviserId, sessionId);

            pollWithDefaults(getCaseByIdWithDocumentMetadata(caseId, USER_ID))
                    .until(
                            status().is(OK),
                            ResponsePayloadMatcher.payload().isJson(CoreMatchers.allOf(
                                    withJsonPath("$.id", Matchers.equalTo(caseId.toString())),
                                    withJsonPath("$.reservedBy", Matchers.equalTo(legalAdviserId.toString())),
                                    withJsonPath("$.reservedByName", Matchers.equalTo("Integration Tester")),
                                    withJsonPath("$.reservedAt", notNullValue()))
                            ));

            final String pendingProscess = ActivitiHelper.pollUntilProcessExists("timerTimeout", caseId.toString());
            ActivitiHelper.executeTimerJobs(pendingProscess);

            final JsonEnvelope eventCaseUnReservedPrivateEvent =  reserveCaseHelper.getEventFromTopic();

            assertThat(eventCaseUnReservedPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_CASE_UNRESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo("1ac91935-4f82-4a4f-bd17-fb50397e42dd"))
                    )))));

            final JsonEnvelope eventCaseUnReservedPublicEvent =  reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseUnReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_UNRESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo("1ac91935-4f82-4a4f-bd17-fb50397e42dd"))
                    )))));

            pollWithDefaults(getCaseByIdWithDocumentMetadata(caseId, USER_ID))
                    .until(
                            status().is(OK),
                            ResponsePayloadMatcher.payload().isJson(CoreMatchers.allOf(
                                    withJsonPath("$.id", Matchers.equalTo(caseId.toString())),
                                    withoutJsonPath("$.reservedBy"),
                                    withoutJsonPath("$.reservedByName"),
                                    withoutJsonPath("$.reservedAt")
                            )));


        }

    }

    @Test
    public void shouldUndoReserveCaseWhenDecided() throws Exception{

        try(ReserveCaseHelper  reserveCaseHelper = new ReserveCaseHelper()) {
            reserveCaseToUser(caseId, legalAdviserId, ACCEPTED);

            final JsonEnvelope eventCaseReservedPrivateEvent = reserveCaseHelper.getEventFromTopic();

            assertThat(eventCaseReservedPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_CASE_RESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo(legalAdviserId.toString())),
                            withJsonPath("$.reservedAt", notNullValue())
                    )))));

            final JsonEnvelope eventCaseReservedPublicEvent = reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_RESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo(legalAdviserId.toString())),
                            withJsonPath("$.reservedAt", notNullValue())
                    )))));

            final UUID sessionId = randomUUID();
            verifyCaseAssigned(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, caseId, legalAdviserId, sessionId);

            // Given
            final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults()
                    .id(offenceId)
                    .disqualificationType(DisqualificationType.DISCRETIONARY)
                    .disqualificationPeriodInMonths(2)
                    .build();
            final FinancialImposition financialImposition = FinancialImpositionBuilder.withDefaults();
            final List<FinancialPenalty> offencesDecisions = singletonList(financialPenalty);
            final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, financialImposition);


            eventListener
                    .subscribe(DecisionSaved.EVENT_NAME)
                    .subscribe(CaseCompleted.EVENT_NAME)
                    .subscribe("public.events.hearing.hearing-resulted")
                    .run(() -> DecisionHelper.saveDecision(decision));


            final Optional<JsonEnvelope> jsonEnvelopePublicHearingResulted = eventListener.popEvent(PUBLIC_EVENTS_HEARING_HEARING_RESULTED);
            assertThat(jsonEnvelopePublicHearingResulted.isPresent(), is(true));

            final JsonEnvelope eventCaseUnReservedPrivateEvent =  reserveCaseHelper.getEventFromTopic();

            assertThat(eventCaseUnReservedPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_CASE_UNRESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo("1ac91935-4f82-4a4f-bd17-fb50397e42dd"))
                    )))));

            final JsonEnvelope eventCaseUnReservedPublicEvent =  reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseUnReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_UNRESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo("1ac91935-4f82-4a4f-bd17-fb50397e42dd"))
                    )))));

            verifyCaseNotFound(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, legalAdviserId);

            pollWithDefaults(getCaseByIdWithDocumentMetadata(caseId, USER_ID))
                    .until(
                            status().is(OK),
                            ResponsePayloadMatcher.payload().isJson(CoreMatchers.allOf(
                                    withJsonPath("$.id", Matchers.equalTo(caseId.toString())),
                                    withoutJsonPath("$.reservedBy"),
                                    withoutJsonPath("$.reservedByName"),
                                    withoutJsonPath("$.reservedAt")
                            )));

        }
    }

    @Test
    public void shouldNotSeeOtherUsersReservedCase() throws Exception{

        final UUID reservedUser = randomUUID();
        stubGroupForUser(reservedUser, "Legal Advisers");
        try(ReserveCaseHelper  reserveCaseHelper = new ReserveCaseHelper()) {
            reserveCaseToUser(caseId, reservedUser, ACCEPTED);

            final JsonEnvelope eventCaseReservedPrivateEvent = reserveCaseHelper.getEventFromTopic();

            assertThat(eventCaseReservedPrivateEvent, jsonEnvelope(
                    metadata().withName(EVENT_CASE_RESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo(reservedUser.toString())),
                            withJsonPath("$.reservedAt", notNullValue())
                    )))));

            final JsonEnvelope eventCaseReservedPublicEvent = reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_RESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", equalTo(caseId.toString())),
                            withJsonPath("$.caseUrn", equalTo(caseUrn)),
                            withJsonPath("$.reservedBy", equalTo(reservedUser.toString())),
                            withJsonPath("$.reservedAt", notNullValue())
                    )))));

            verifyCaseNotFound(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, legalAdviserId);

            final UUID sessionId = randomUUID();
            verifyCaseAssigned(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, caseId, reservedUser, sessionId);
        }
    }


    @Test
    public void shouldNotReserveCaseWithCourtAdmin(){
        reserveCaseToUser(caseId, courtAdminId, FORBIDDEN);
    }

    @Test
    public void shouldNotReserveCaseWithSystemAdmin(){
        unReserveCaseToUser(caseId, systemAdminId, ACCEPTED);
    }

    @Test
    public void shouldNotUnReserveCaseWithCourtAdmin(){
        unReserveCaseToUser(caseId, courtAdminId, FORBIDDEN);
    }

    public static UUID reserveCaseToUser(final UUID caseId,
                                        final UUID callerId,
                                        final Response.Status expectedStatus) {
        final String contentType = "application/vnd.sjp.reserve-case+json";
        final String url = String.format("/cases/%s/reserve-case", caseId);

        final JsonObject payload = createObjectBuilder()
                .build();

        return HttpClientUtil.makePostCall(callerId, url, contentType, payload.toString(), expectedStatus);
    }

    public static UUID unReserveCaseToUser(final UUID caseId,
                                         final UUID callerId,
                                         final Response.Status expectedStatus) {
        final String contentType = "application/vnd.sjp.undo-reserve-case+json";
        final String url = String.format("/cases/%s/reserve-case", caseId);

        final JsonObject payload = createObjectBuilder()
                .build();

        return HttpClientUtil.makePostCall(callerId, url, contentType, payload.toString(), expectedStatus);
    }

    private static void verifyCaseAssigned(final String courtHouseOUCode, final UUID caseId, final UUID userId, final UUID sessionId) {

        SessionHelper.startSession(sessionId, userId, courtHouseOUCode, DELEGATED_POWERS);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.assignedPrivateHandler = (envelope) ->
            assertThat((JsonEnvelope) envelope,
                    jsonEnvelope(
                            metadata().withName(CaseAssigned.EVENT_NAME),
                            payload().isJson(allOf(
                                    withJsonPath("$.caseId", CoreMatchers.equalTo(caseId.toString())),
                                    withJsonPath("$.assigneeId", CoreMatchers.equalTo(userId.toString())),
                                    withJsonPath("$.assignedAt", CoreMatchers.notNullValue()),
                                    withJsonPath("$.caseAssignmentType", CoreMatchers.equalTo( DELEGATED_POWERS_DECISION.toString()))
                            ))));

        assignCase.assignedPublicHandler = (envelope) ->
            assertThat((JsonEnvelope) envelope,
                    jsonEnvelope(
                            metadata().withName(AssignmentProcessor.PUBLIC_SJP_CASE_ASSIGNED),
                            payload().isJson(withJsonPath("$.caseId", CoreMatchers.equalTo(caseId.toString())))));


        assignCase.getExecutor().setExecutingUserId(userId).executeSync();

        pollUntilCaseAssignedToUser(caseId, userId);
    }

    private static void verifyCaseNotFound(final String courtHouseOUCode, final UUID userId) {
        final UUID sessionId = randomUUID();

        SessionHelper.startSession(sessionId, userId, courtHouseOUCode, DELEGATED_POWERS);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.notAssignedHandler = (envelope) -> Log.info("Case Not Assigned");
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();
    }

}
