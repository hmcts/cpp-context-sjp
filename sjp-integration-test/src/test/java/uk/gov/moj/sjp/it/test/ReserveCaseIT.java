package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.Month.JULY;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.Constants.OFFENCE_DATE_CODE_FOR_BETWEEN;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_ALREADY_RESERVED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_ALREADY_UNRESERVED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_RESERVED;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_UNRESERVED;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseAssignedToUser;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseStatusCompleted;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubGroupForUser;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.executeTimerJobs;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.pollUntilProcessExists;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseByIdWithDocumentMetadata;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.commandclient.AssignNextCaseClient;
import uk.gov.moj.sjp.it.helper.ReserveCaseHelper;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.util.builders.FinancialImpositionBuilder;
import uk.gov.moj.sjp.it.util.builders.FinancialPenaltyBuilder;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.google.common.collect.Sets;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReserveCaseIT extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ReserveCaseIT.class);

    private static final String NATIONAL_COURT_CODE = "1080";

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

    @BeforeEach
    public void beforeEveryTest() throws SQLException {
        cleanViewStore();

        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubProsecutorQuery(TFL.name(), TFL.getFullName(), randomUUID());
        stubForUserDetails(user, "ALL");

        stubGroupForUser(legalAdviserId, "Legal Advisers");
        stubGroupForUser(courtAdminId, "Court Administrators");
        stubGroupForUser(systemAdminId, "System Users");

        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        final CreateCase.CreateCasePayloadBuilder caseBuilder = withDefaults()
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

        createCaseForPayloadBuilder(caseBuilder);
        pollUntilCaseReady(caseId);
    }

    @Test
    public void shouldReserveCaseThenUndoReserveCaseWithLegalAdviser() throws Exception {
        try (ReserveCaseHelper reserveCaseHelper = new ReserveCaseHelper()) {
            reserveCaseToUser(caseId, legalAdviserId, ACCEPTED);

            final JsonEnvelope eventCaseReservedPublicEvent = reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_RESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", is(caseId.toString())),
                            withJsonPath("$.caseUrn", is(caseUrn)),
                            withJsonPath("$.reservedBy", is(legalAdviserId.toString())),
                            withJsonPath("$.reservedAt", notNullValue())
                    )))));

            reserveCaseToUser(caseId, legalAdviserId, ACCEPTED);

            final JsonEnvelope eventCaseAlreadyReservedPublicEvent = reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseAlreadyReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_ALREADY_RESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", is(caseId.toString()))
                    )))));

            final UUID sessionId = randomUUID();
            verifyCaseAssigned(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, caseId, legalAdviserId, sessionId);

            pollWithDefaults(getCaseByIdWithDocumentMetadata(caseId, USER_ID))
                    .until(
                            status().is(OK),
                            ResponsePayloadMatcher.payload().isJson(CoreMatchers.allOf(
                                    withJsonPath("$.id", is(caseId.toString())),
                                    withJsonPath("$.reservedBy", is(legalAdviserId.toString())),
                                    withJsonPath("$.reservedByName", is("Integration Tester")),
                                    withJsonPath("$.reservedAt", notNullValue()))
                            ));

            undoReserveCaseToUser(caseId, legalAdviserId, ACCEPTED);

            final JsonEnvelope eventCaseUnReservedPublicEvent = reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseUnReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_UNRESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", is(caseId.toString())),
                            withJsonPath("$.caseUrn", is(caseUrn)),
                            withJsonPath("$.reservedBy", is(legalAdviserId.toString()))
                    )))));

            pollWithDefaults(getCaseByIdWithDocumentMetadata(caseId, USER_ID))
                    .until(
                            status().is(OK),
                            ResponsePayloadMatcher.payload().isJson(CoreMatchers.allOf(
                                    withJsonPath("$.id", is(caseId.toString())),
                                    withoutJsonPath("$.reservedBy"),
                                    withoutJsonPath("$.reservedByName"),
                                    withoutJsonPath("$.reservedAt")
                            )));

            undoReserveCaseToUser(caseId, legalAdviserId, ACCEPTED);

            final JsonEnvelope eventCaseAlreadyUnReservedPublicEvent = reserveCaseHelper.getPublicEventFromTopic();

            assertThat(eventCaseAlreadyUnReservedPublicEvent, jsonEnvelope(
                    metadata().withName(PUBLIC_CASE_ALREADY_UNRESERVED),
                    payload(isJson(allOf(
                            withJsonPath("$.caseId", is(caseId.toString()))
                    )))));
        }
    }

    @Test
    public void shouldUndoReserveCaseWhenTimeOut() throws Exception {
        try (ReserveCaseHelper reserveCaseHelper = new ReserveCaseHelper()) {
            reserveCaseToUser(caseId, legalAdviserId, ACCEPTED);

            final UUID sessionId = randomUUID();
            verifyCaseAssigned(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, caseId, legalAdviserId, sessionId);

            pollWithDefaults(getCaseByIdWithDocumentMetadata(caseId, USER_ID))
                    .until(
                            status().is(OK),
                            ResponsePayloadMatcher.payload().isJson(CoreMatchers.allOf(
                                    withJsonPath("$.id", is(caseId.toString())),
                                    withJsonPath("$.reservedBy", is(legalAdviserId.toString())),
                                    withJsonPath("$.reservedByName", is("Integration Tester")),
                                    withJsonPath("$.reservedAt", notNullValue()))
                            ));

            final String pendingProcess = pollUntilProcessExists("timerTimeout", caseId.toString());
            executeTimerJobs(pendingProcess);

            pollWithDefaults(getCaseByIdWithDocumentMetadata(caseId, USER_ID))
                    .until(
                            status().is(OK),
                            ResponsePayloadMatcher.payload().isJson(CoreMatchers.allOf(
                                    withJsonPath("$.id", is(caseId.toString())),
                                    withoutJsonPath("$.reservedBy"),
                                    withoutJsonPath("$.reservedByName"),
                                    withoutJsonPath("$.reservedAt")
                            )));
        }
    }

    @Test
    public void shouldUndoReserveCaseWhenDecided() throws Exception {

        try (ReserveCaseHelper reserveCaseHelper = new ReserveCaseHelper()) {
            reserveCaseToUser(caseId, legalAdviserId, ACCEPTED);

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

            saveDecision(decision);
            pollUntilCaseStatusCompleted(caseId);

            verifyCaseNotFound(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, legalAdviserId);

            pollWithDefaults(getCaseByIdWithDocumentMetadata(caseId, USER_ID))
                    .until(
                            status().is(OK),
                            ResponsePayloadMatcher.payload().isJson(CoreMatchers.allOf(
                                    withJsonPath("$.id", is(caseId.toString())),
                                    withoutJsonPath("$.reservedBy"),
                                    withoutJsonPath("$.reservedByName"),
                                    withoutJsonPath("$.reservedAt")
                            )));

        }
    }

    @Test
    public void shouldNotSeeOtherUsersReservedCase() throws Exception {

        final UUID reservedUser = randomUUID();
        stubGroupForUser(reservedUser, "Legal Advisers");
        try (ReserveCaseHelper reserveCaseHelper = new ReserveCaseHelper()) {
            reserveCaseToUser(caseId, reservedUser, ACCEPTED);

            verifyCaseNotFound(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, legalAdviserId);

            final UUID sessionId = randomUUID();
            verifyCaseAssigned(DEFAULT_LONDON_COURT_HOUSE_OU_CODE, caseId, reservedUser, sessionId);
        }
    }

    private UUID reserveCaseToUser(final UUID caseId,
                                   final UUID callerId,
                                   final Response.Status expectedStatus) {
        final String contentType = "application/vnd.sjp.reserve-case+json";
        final String url = String.format("/cases/%s/reserve-case", caseId);

        final JsonObject payload = createObjectBuilder()
                .build();

        return makePostCall(callerId, url, contentType, payload.toString(), expectedStatus);
    }

    private UUID undoReserveCaseToUser(final UUID caseId,
                                       final UUID callerId,
                                       final Response.Status expectedStatus) {
        final String contentType = "application/vnd.sjp.undo-reserve-case+json";
        final String url = String.format("/cases/%s/reserve-case", caseId);

        final JsonObject payload = createObjectBuilder()
                .build();

        return makePostCall(callerId, url, contentType, payload.toString(), expectedStatus);
    }

    private void verifyCaseAssigned(final String courtHouseOUCode, final UUID caseId, final UUID userId, final UUID sessionId) {

        startSessionAndConfirm(sessionId, userId, courtHouseOUCode, MAGISTRATE);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();

        assignCase.assignedPublicHandler = (envelope) ->
                assertThat((JsonEnvelope) envelope,
                        jsonEnvelope(
                                metadata().withName(AssignmentProcessor.PUBLIC_SJP_CASE_ASSIGNED),
                                payload().isJson(withJsonPath("$.caseId", is(caseId.toString())))));


        assignCase.getExecutor().setExecutingUserId(userId).executeSync();

        pollUntilCaseAssignedToUser(caseId, userId);
    }

    private void verifyCaseNotFound(final String courtHouseOUCode, final UUID userId) {
        final UUID sessionId = randomUUID();

        startSessionAndConfirm(sessionId, userId, courtHouseOUCode, MAGISTRATE);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.notAssignedHandler = (envelope) -> log.info("Case Not Assigned");
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();
    }
}
