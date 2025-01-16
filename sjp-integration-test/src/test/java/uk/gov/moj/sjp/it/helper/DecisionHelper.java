package uk.gov.moj.sjp.it.helper;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.isA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.getCase;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollForCase;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_USER;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_USER_ID;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makePostCall;
import static uk.gov.moj.sjp.it.util.JsonHelper.toJsonObject;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaultsUntilResponseIsJson;

import uk.gov.justice.json.schemas.domains.sjp.NoteType;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.json.schemas.domains.sjp.events.CaseNoteAdded;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.NoSeparatePenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredForFutureSJPSession;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredToOpenCourt;
import uk.gov.moj.cpp.sjp.domain.decision.SetAside;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearingV2;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.decision.DecisionRejected;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import uk.gov.moj.sjp.it.command.CreateCase.OffenceBuilder;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.util.builders.DismissBuilder;
import uk.gov.moj.sjp.it.util.matchers.OffenceDecisionMatcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;

public class DecisionHelper {

    private static final String WRITE_URL_PATTERN = "/cases/%s/decision";
    private static final String CHANGE_PAYMENT_TERMS_URL = "/cases/%s";
    private static final String QUERY_READY_CASES_RESOURCE = "/cases/ready-cases";
    private static final String QUERY_READY_CASES = "application/vnd.sjp.query.ready-cases+json";

    public static void saveDefaultDecision(final UUID caseId, final UUID... offenceIds) {
        saveDefaultDecision(caseId, asList(offenceIds));
    }

    public static void saveDefaultDecision(final UUID caseId, final Collection<UUID> offenceIds) {
        ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery();
        final JsonObject readyCase = CaseHelper.pollUntilCaseReady(caseId);
        final SessionType sessionType = SessionType.valueOf(readyCase.getString("sessionType"));
        final ProsecutingAuthority prosecutingAuthority = ProsecutingAuthority.valueOf(readyCase.getString("prosecutingAuthority"));

        final UUID sessionId = startSessionAndRequestAssignment(DEFAULT_USER, sessionType, prosecutingAuthority, caseId);

        saveDefaultDecisionInSession(caseId, sessionId, DEFAULT_USER_ID, offenceIds);
    }

    public static void saveDefaultDecisionInSession(final UUID caseId, final UUID sessionId, final UUID userId, final Collection<UUID> offenceIds) {
        final List<OffenceDecision> offenceDecisions = offenceIds.stream().map(offenceId -> DismissBuilder.withDefaults(offenceId).build()).collect(toList());
        final DecisionCommand decisionCommand = new DecisionCommand(sessionId, caseId, "", new User("John", "Smith", userId), offenceDecisions, null);

        saveDecision(decisionCommand);
        pollForCase(caseId, new Matcher[]{withJsonPath("$.caseDecisions", hasSize(1))});

    }

    public static void saveDecision(final DecisionCommand decision) {
        final UUID userId = decision.getSavedBy().getUserId();
        final String path = format(WRITE_URL_PATTERN, decision.getCaseId());
        final String mediaType = "application/vnd.sjp.save-decision+json";
        final String payload = toJsonObject(decision).toString();

        makePostCall(userId, path, mediaType, payload, ACCEPTED);
    }

    public static void changePaymentTerms(final UUID userId,
                                          final UUID caseId) {

        final String path = format(CHANGE_PAYMENT_TERMS_URL, caseId);
        final String mediaType = "application/vnd.sjp.resubmit-results+json";
        final String payload = createObjectBuilder()
                .add("paymentTermsInfo",
                        createObjectBuilder()
                                .add("numberOfDaysToPostponeBy", 1)
                                .add("resetPayByDate", true)
                                .build())
                .add("accountNote", "PAYMENT TERMS HAVE BEEN RESET")
                .build()
                .toString();

        makePostCall(userId, path, mediaType, payload, ACCEPTED);
    }

    public static CreateCasePayloadBuilder createCase(final UUID caseId,
                                                      final UUID offence1Id,
                                                      final UUID offence2Id,
                                                      final UUID offence3Id,
                                                      final LocalDate postingDate) {
        return createCase(caseId, asList(offence1Id, offence2Id, offence3Id), postingDate);
    }

    public static CreateCasePayloadBuilder createCase(final UUID caseId,
                                                      final List<UUID> offenceIds,
                                                      final LocalDate postingDate) {
        final CreateCasePayloadBuilder createCasePayloadBuilder = CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withPostingDate(postingDate);
        final List<OffenceBuilder> offenceBuilders = offenceIds.stream()
                .map(offenceId -> OffenceBuilder.withDefaults().withId(offenceId))
                .collect(toList());
        createCasePayloadBuilder.withOffenceBuilders(offenceBuilders);

        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(createCasePayloadBuilder.getId());
        return createCasePayloadBuilder;
    }

    public static CreateCasePayloadBuilder createCaseWithoutDefendantPostcode(
            final UUID caseId,
            final UUID offence1Id,
            final UUID offence2Id,
            final UUID offence3Id,
            final LocalDate postingDate) {
        final CreateCasePayloadBuilder createCasePayloadBuilder = CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceBuilders(
                        OffenceBuilder.withDefaults().withId(offence1Id),
                        OffenceBuilder.withDefaults().withId(offence2Id),
                        OffenceBuilder.withDefaults().withId(offence3Id))
                .withPostingDate(postingDate);

        createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().withPostcode(null);

        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(createCasePayloadBuilder.getId());
        return createCasePayloadBuilder;
    }

    public static void verifyCaseNotReadyInViewStore(final UUID caseId, final UUID userId) {
        pollReadyCasesUntilResponseIsJson(userId, withJsonPath("readyCases.*", not(CoreMatchers.hasItem(
                JsonPathMatchers.isJson(withJsonPath("caseId", equalTo(caseId.toString())))
        ))));
    }

    public static void verifyCaseIsReadyInViewStore(final UUID caseId, final UUID userId) {
        pollReadyCasesUntilResponseIsJson(userId, withJsonPath("readyCases.*", CoreMatchers.hasItems(
                JsonPathMatchers.isJson(withJsonPath("caseId", equalTo(caseId.toString())))
        )));
    }

    public static void verifyCaseIsReadyInViewStoreAndAssignedTo(final UUID caseId, final UUID userId) {
        pollReadyCasesUntilResponseIsJson(userId, withJsonPath("readyCases.*", CoreMatchers.hasItems(
                JsonPathMatchers.isJson(withJsonPath("caseId", equalTo(caseId.toString())))
        )));
    }

    public static void verifyCaseQueryWithWithdrawnDecision(final DecisionCommand decisionCommand,
                                                            final DecisionSaved decisionSaved,
                                                            final JsonObject session,
                                                            final List<Withdraw> withdrawDecisions,
                                                            final String withdrawalReason) {
        final JsonObject decision = queryDecision(decisionSaved);
        verifySession(decision, session, decisionCommand.getSavedBy());
        verifyWithdrawDecision(decision, withdrawDecisions, withdrawalReason);
    }

    public static void verifyCaseQueryWithDisabilityNeeds(final UUID caseId, final String disabilityNeeds) {

        Matcher disabilityNeedsMatcher;
        if (disabilityNeeds != null) {
            disabilityNeedsMatcher = allOf(
                    withJsonPath("$.defendant.disabilityNeeds.needed", is(true)),
                    withJsonPath("$.defendant.disabilityNeeds.disabilityNeeds", equalTo(disabilityNeeds))
            );
        } else {
            disabilityNeedsMatcher = withJsonPath("$.defendant.disabilityNeeds.needed", is(false));
        }

        final JsonObject aCase = getCase(caseId, disabilityNeedsMatcher);
    }

    public static void verifyCaseQueryWithAdjournDecision(final DecisionCommand decisionCommand,
                                                          final DecisionSaved decisionSaved,
                                                          final JsonObject session,
                                                          final Adjourn adjournDecision) {
        final JsonObject decision = queryDecision(decisionSaved);
        verifySession(decision, session, decisionCommand.getSavedBy());
        verifyAdjournDecision(decision, adjournDecision);
    }

    public static void verifyCaseQueryWithReferForCourtHearingDecision(final DecisionCommand decisionCommand,
                                                                       final DecisionSaved decisionSaved,
                                                                       final JsonObject session,
                                                                       final String referralReason,
                                                                       final ReferForCourtHearing referForCourtHearing) {
        final JsonObject decision = queryDecision(decisionSaved);
        verifySession(decision, session, decisionCommand.getSavedBy());
        verifyReferForCourtHearing(decision, referForCourtHearing, referralReason);
    }

    public static void verifyCaseQueryWithDismissDecision(final DecisionCommand decisionCommand,
                                                          final DecisionSaved decisionSaved,
                                                          final JsonObject session,
                                                          final Dismiss dismissDecision) {
        final JsonObject decision = queryDecision(decisionSaved);
        verifySession(decision, session, decisionCommand.getSavedBy());
        verifyDismissDecision(decision, dismissDecision);
    }

    private static void verifySession(final JsonObject decision, final JsonObject expectedSession, final User user) {
        final JsonObject session = decision.getJsonObject("session");
        final JsonObject legalAdviser = session.getJsonObject("legalAdviser");

        assertThat(session.getString("sessionId"), is(expectedSession.getString("sessionId")));
        assertThat(session.getString("courtHouseCode"), is(expectedSession.getString("courtHouseCode")));
        assertThat(session.getString("courtHouseName"), is(expectedSession.getString("courtHouseName")));
        assertThat(session.getString("localJusticeAreaNationalCourtCode"), is(expectedSession.getString("localJusticeAreaNationalCourtCode")));
        assertThat(session.getString("startedAt"), is(expectedSession.getString("startedAt")));

        assertThat(legalAdviser.getString("firstName"), is(user.getFirstName()));
        assertThat(legalAdviser.getString("lastName"), is(user.getLastName()));
    }

    public static void verifyWithdrawDecision(final JsonObject decision, final List<Withdraw> expectedWithdrawDecisions, final String withdrawalReason) {
        for (final Withdraw withdraw : expectedWithdrawDecisions) {
            final Optional<JsonObject> offenceDecision = getOffenceDecision(decision, withdraw.getOffenceDecisionInformation().getOffenceId());
            assertThat(offenceDecision.isPresent(), is(true));
            assertThat(offenceDecision.get().getString("decisionType"), is(withdraw.getType().name()));
            assertThat(offenceDecision.get().getString("withdrawalReasonId"), is(withdraw.getWithdrawalReasonId().toString()));
            assertThat(offenceDecision.get().getString("withdrawalReason"), is(withdrawalReason));
        }
    }

    public static void verifyAdjournDecision(final JsonObject decision, final Adjourn expectedAdjournDecisions) {
        final List<UUID> offenceIds = expectedAdjournDecisions.offenceDecisionInformationAsList()
                .stream()
                .map(OffenceDecisionInformation::getOffenceId).collect(toList());

        for (final UUID offenceId : offenceIds) {
            final Optional<JsonObject> offenceDecision = getOffenceDecision(decision, offenceId);

            assertThat(offenceDecision.isPresent(), is(true));
            assertThat(offenceDecision.get().getString("decisionType"), is(expectedAdjournDecisions.getType().name()));
            assertThat(offenceDecision.get().containsKey("reason"), is(false));
            assertThat(offenceDecision.get().getString("adjournedTo"), is(expectedAdjournDecisions.getAdjournTo().toString()));
        }
    }

    public static void verifyReferForCourtHearing(final JsonObject decision, final ReferForCourtHearing expectedReferForCourtDecisions, final String referralReason) {
        final List<UUID> offenceIds = expectedReferForCourtDecisions.offenceDecisionInformationAsList()
                .stream()
                .map(OffenceDecisionInformation::getOffenceId)
                .collect(toList());
        for (final UUID offenceId : offenceIds) {
            final Optional<JsonObject> offenceDecision = getOffenceDecision(decision, offenceId);

            assertThat(offenceDecision.isPresent(), is(true));
            assertThat(offenceDecision.get().getString("decisionType"), is(expectedReferForCourtDecisions.getType().name()));
            assertThat(offenceDecision.get().getString("referralReasonId"), is(expectedReferForCourtDecisions.getReferralReasonId().toString()));
            assertThat(offenceDecision.get().getString("referralReason"), is(referralReason));
            assertThat(offenceDecision.get().containsKey("listingNotes"), is(false));
            assertThat(offenceDecision.get().getInt("estimatedHearingDuration"), is(expectedReferForCourtDecisions.getEstimatedHearingDuration()));
        }
    }

    public static void verifyDismissDecision(final JsonObject decision, final Dismiss expectedDismissDecisions) {

        OffenceDecisionInformation offenceDecisionInformation = expectedDismissDecisions.getOffenceDecisionInformation();
        final Optional<JsonObject> offenceDecision = getOffenceDecision(decision, offenceDecisionInformation.getOffenceId());
        assertThat(offenceDecision.isPresent(), is(true));
        assertThat(offenceDecision.get().getString("decisionType"), is(expectedDismissDecisions.getType().name()));
        assertThat(offenceDecision.get().getString("verdict"), is(offenceDecisionInformation.getVerdict().name()));

    }

    public static void verifyCaseUnmarkedReady(final UUID caseId, final Adjourn adjourn, final CaseUnmarkedReadyForDecision caseUnmarkedReadyForDecision) {
        assertThat(caseUnmarkedReadyForDecision.getCaseId(), is(caseId));
        assertThat(caseUnmarkedReadyForDecision.getExpectedDateReady(), is(adjourn.getAdjournTo()));
    }

    public static void verifyDecisionRejected(final DecisionCommand decisionCommand, final DecisionRejected decisionRejected, final String... reasons) {
        assertThat(decisionRejected.getDecision().getCaseId(), is(decisionCommand.getCaseId()));
        assertThat(decisionRejected.getDecision().getSessionId(), is(decisionCommand.getSessionId()));
        assertThat(decisionRejected.getDecision().getSavedAt(), notNullValue());
        verifyOffenceDecisions(decisionRejected.getDecision().getOffenceDecisions(), decisionCommand.getOffenceDecisions(), null);
        assertThat(decisionRejected.getRejectionReasons(), containsInAnyOrder(reasons));
    }

    public static void verifyDecisionSaved(final DecisionCommand decisionCommand, final DecisionSaved decisionSaved) {
        assertThat(decisionSaved.getCaseId(), is(decisionCommand.getCaseId()));
        assertThat(decisionSaved.getSessionId(), is(decisionCommand.getSessionId()));
        assertThat(decisionSaved.getSavedAt(), notNullValue());

        List<? extends OffenceDecision> commandOffenceDecisions = decisionCommand.getOffenceDecisions();

        verifyOffenceDecisions(decisionSaved.getOffenceDecisions(), commandOffenceDecisions, decisionSaved);
        if (decisionCommand.getFinancialImposition() != null) {
            assertThat(decisionSaved.getFinancialImposition(), is(decisionCommand.getFinancialImposition()));
        }
    }

    private static void verifyOffenceDecisions(final List<? extends OffenceDecision> eventDecisions, final List<? extends OffenceDecision> commandOffenceDecisions, final DecisionSaved decisionSaved) {
        assertEquals(commandOffenceDecisions.size(), eventDecisions.size());

        for (OffenceDecision offenceDecision : commandOffenceDecisions) {

            Matcher decisionTypeSpecificMatcher = null;
            switch (offenceDecision.getType()) {
                case REFER_FOR_COURT_HEARING:
                    ReferForCourtHearing refer = (ReferForCourtHearing) offenceDecision;
                    decisionTypeSpecificMatcher = allOf(
                            isA(ReferForCourtHearing.class),
                            hasProperty("offenceDecisionInformation", containsInAnyOrder(refer.getOffenceDecisionInformation().toArray())),
                            hasProperty("referralReasonId", is(refer.getReferralReasonId())),
                            hasProperty("listingNotes", is(refer.getListingNotes())),
                            hasProperty("estimatedHearingDuration", is(refer.getEstimatedHearingDuration())),
                            hasProperty("defendantCourtOptions", is(refer.getDefendantCourtOptions())),
                            hasProperty("convictionDate", equalTo(decisionSaved != null ? decisionSaved.getSavedAt().toLocalDate() : null))
                    );
                    break;
                case DISMISS:
                    Dismiss dismiss = (Dismiss) offenceDecision;
                    decisionTypeSpecificMatcher = allOf(
                            isA(Dismiss.class),
                            hasProperty("offenceDecisionInformation", is(dismiss.getOffenceDecisionInformation()))
                    );
                    break;
                case ADJOURN:
                    Adjourn adjourn = (Adjourn) offenceDecision;
                    decisionTypeSpecificMatcher = allOf(
                            isA(Adjourn.class),
                            hasProperty("offenceDecisionInformation", containsInAnyOrder(adjourn.getOffenceDecisionInformation().toArray())),
                            hasProperty("reason", is(adjourn.getReason())),
                            hasProperty("adjournTo", is(adjourn.getAdjournTo()))
                    );
                    break;
                case WITHDRAW:
                    Withdraw withdraw = (Withdraw) offenceDecision;
                    decisionTypeSpecificMatcher = allOf(
                            isA(Withdraw.class),
                            hasProperty("offenceDecisionInformation", is(withdraw.getOffenceDecisionInformation())),
                            hasProperty("withdrawalReasonId", is(withdraw.getWithdrawalReasonId()))
                    );
                    break;
                case DISCHARGE:
                    Discharge discharge = (Discharge) offenceDecision;
                    decisionTypeSpecificMatcher = allOf(
                            isA(Discharge.class),
                            hasProperty("offenceDecisionInformation", is(discharge.getOffenceDecisionInformation())),
                            hasProperty("dischargeType", is(discharge.getDischargeType())),
                            hasProperty("dischargedFor", is(discharge.getDischargedFor())),
                            hasProperty("compensation", is(discharge.getCompensation())),
                            hasProperty("noCompensationReason", is(discharge.getNoCompensationReason())),
                            hasProperty("guiltyPleaTakenIntoAccount", is(discharge.getGuiltyPleaTakenIntoAccount())),
                            hasProperty("backDuty", is(discharge.getBackDuty())),
                            hasProperty("licenceEndorsed", is(discharge.getLicenceEndorsed())),
                            hasProperty("penaltyPointsImposed", is(discharge.getPenaltyPointsImposed())),
                            hasProperty("penaltyPointsReason", is(discharge.getPenaltyPointsReason())),
                            hasProperty("additionalPointsReason", is(discharge.getAdditionalPointsReason())),
                            hasProperty("disqualification", is(discharge.getDisqualification())),
                            hasProperty("disqualificationType", is(discharge.getDisqualificationType())),
                            hasProperty("disqualificationPeriod", is(discharge.getDisqualificationPeriod())),
                            hasProperty("notionalPenaltyPoints", is(discharge.getNotionalPenaltyPoints())),
                            hasProperty("convictionDate", equalTo(decisionSaved != null ? decisionSaved.getSavedAt().toLocalDate() : null))
                    );
                    break;
                case FINANCIAL_PENALTY:
                    final FinancialPenalty financialPenalty = (FinancialPenalty) offenceDecision;
                    decisionTypeSpecificMatcher = allOf(
                            isA(FinancialPenalty.class),
                            hasProperty("offenceDecisionInformation", is(financialPenalty.getOffenceDecisionInformation())),
                            hasProperty("fine", is(financialPenalty.getFine())),
                            hasProperty("compensation", is(financialPenalty.getCompensation())),
                            hasProperty("noCompensationReason", is(financialPenalty.getNoCompensationReason())),
                            hasProperty("guiltyPleaTakenIntoAccount", is(financialPenalty.getGuiltyPleaTakenIntoAccount())),
                            hasProperty("backDuty", is(financialPenalty.getBackDuty())),
                            hasProperty("excisePenalty", is(financialPenalty.getExcisePenalty())),
                            hasProperty("licenceEndorsed", is(financialPenalty.getLicenceEndorsed())),
                            hasProperty("penaltyPointsImposed", is(financialPenalty.getPenaltyPointsImposed())),
                            hasProperty("penaltyPointsReason", is(financialPenalty.getPenaltyPointsReason())),
                            hasProperty("additionalPointsReason", is(financialPenalty.getAdditionalPointsReason())),
                            hasProperty("disqualification", is(financialPenalty.getDisqualification())),
                            hasProperty("disqualificationType", is(financialPenalty.getDisqualificationType())),
                            hasProperty("disqualificationPeriod", is(financialPenalty.getDisqualificationPeriod())),
                            hasProperty("notionalPenaltyPoints", is(financialPenalty.getNotionalPenaltyPoints())),
                            hasProperty("convictionDate", equalTo(decisionSaved != null ? decisionSaved.getSavedAt().toLocalDate() : null))
                    );
                    break;
                case NO_SEPARATE_PENALTY:
                    NoSeparatePenalty noSeparatePenalty = (NoSeparatePenalty) offenceDecision;
                    decisionTypeSpecificMatcher = allOf(
                            isA(NoSeparatePenalty.class),
                            hasProperty("offenceDecisionInformation", is(noSeparatePenalty.getOffenceDecisionInformation())),
                            hasProperty("guiltyPleaTakenIntoAccount", is(noSeparatePenalty.getGuiltyPleaTakenIntoAccount())),
                            hasProperty("licenceEndorsed", is(noSeparatePenalty.getLicenceEndorsed())),
                            hasProperty("convictionDate", equalTo(decisionSaved != null ? decisionSaved.getSavedAt().toLocalDate() : null))
                    );
                    break;
                case REFERRED_FOR_FUTURE_SJP_SESSION: // legacy decision type
                    ReferredForFutureSJPSession referredForFutureSJPSessionDecision = (ReferredForFutureSJPSession) offenceDecision;
                    decisionTypeSpecificMatcher = allOf(
                            isA(ReferredForFutureSJPSession.class),
                            hasProperty("offenceDecisionInformation", is(referredForFutureSJPSessionDecision.getOffenceDecisionInformation()))
                    );
                    break;
                case REFERRED_TO_OPEN_COURT: // legacy decision type
                    ReferredToOpenCourt referredToOpenCourt = (ReferredToOpenCourt) offenceDecision;
                    decisionTypeSpecificMatcher = allOf(
                            isA(ReferredForFutureSJPSession.class),
                            hasProperty("offenceDecisionInformation", is(referredToOpenCourt.getOffenceDecisionInformation())),
                            hasProperty("magistratesCourt", is(referredToOpenCourt.getMagistratesCourt())),
                            hasProperty("referredToCourt", is(referredToOpenCourt.getReferredToCourt())),
                            hasProperty("referredToDateTime", is(referredToOpenCourt.getReferredToDateTime())),
                            hasProperty("referredToRoom", is(referredToOpenCourt.getReferredToRoom())),
                            hasProperty("reason", is(referredToOpenCourt.getReason()))
                    );
                    break;
                case SET_ASIDE:
                    SetAside setAside = (SetAside) offenceDecision;
                    decisionTypeSpecificMatcher = allOf(
                            isA(SetAside.class)
                    );
                    break;

            }

            if (isPostConvictionOffenceDecision(offenceDecision)
                    && !DecisionType.SET_ASIDE.equals(offenceDecision.getType())) { // TODO revisit to add more specific assertions
                decisionTypeSpecificMatcher = allOf(decisionTypeSpecificMatcher, hasProperty("convictionDate"));
            }

            assertThat(eventDecisions, hasItem(allOf(
                    OffenceDecisionMatcher.match(offenceDecision),
                    decisionTypeSpecificMatcher)
            ));
        }
    }

    private static boolean isPostConvictionOffenceDecision(final OffenceDecision offenceDecision) {
        return offenceDecision.offenceDecisionInformationAsList()
                .stream()
                .map(OffenceDecisionInformation::getVerdict)
                .anyMatch(Objects::isNull);
    }

    public static void verifyDecisionSavedPublicEventEmit(final DecisionSaved decisionSaved, final JsonEnvelope decisionSavedEnvelope) {
        final List<Matcher<? super ReadContext>> matchers = new ArrayList<>();

        matchers.add(withJsonPath("caseId", is(decisionSaved.getCaseId().toString())));
        matchers.add(withJsonPath("decisionId", is(decisionSaved.getDecisionId().toString())));
        matchers.add(withJsonPath("sessionId", is(decisionSaved.getSessionId().toString())));
        matchers.add(withJsonPath("defendantId", is(decisionSaved.getDefendantId().toString())));
        matchers.add(withJsonPath("defendantName", is(decisionSaved.getDefendantName())));
        matchers.add(withJsonPath("savedAt", is(ZonedDateTimes.toString(decisionSaved.getSavedAt()))));


        matchers.add(allOf(decisionSaved.getOffenceDecisions().stream()
                .map(offenceDecision -> {
                    final DecisionType decisionType = offenceDecision.getType();
                    final String firstOffenceIdPath = asList(DecisionType.REFER_FOR_COURT_HEARING, DecisionType.ADJOURN).contains(decisionType) ?
                            "offenceDecisionInformation[0].offenceId" : "offenceDecisionInformation.offenceId";
                    return withJsonPath("$.offenceDecisions.*",
                            hasItem(JsonPathMatchers.isJson(
                                    allOf(
                                            withJsonPath("type", is(offenceDecision.getType().toString())),
                                            withJsonPath(firstOffenceIdPath, is(
                                                    offenceDecision.offenceDecisionInformationAsList().get(0).getOffenceId().toString()))
                                    ))));
                }).collect(toList())));

        assertThat(decisionSavedEnvelope, jsonEnvelope(
                metadata().withName("public.sjp.case-decision-saved"),
                payload(JsonValueIsJsonMatcher.isJson(allOf(matchers)))
        ));
    }

    public static void verifyCaseCompleted(final UUID caseId, final CaseCompleted caseCompleted) {
        assertThat(caseCompleted.getCaseId(), is(caseId));
    }

    public static void verifyListingNotesAdded(final DecisionCommand decisionCommand, final DecisionSaved decisionSaved, final ReferForCourtHearing referForCourtHearing, final CaseNoteAdded caseNoteAdded) {
        verifyNoteAdded(decisionCommand, decisionSaved, caseNoteAdded, referForCourtHearing.getListingNotes(), NoteType.LISTING);
    }

    public static void verifyAdjournmentNoteAdded(final DecisionCommand decisionCommand, final DecisionSaved decisionSaved, final Adjourn adjourn, final CaseNoteAdded caseNoteAdded) {
        verifyNoteAdded(decisionCommand, decisionSaved, caseNoteAdded, adjourn.getReason(), NoteType.ADJOURNMENT);
    }

    public static void verifyNoteAdded(final DecisionCommand decisionCommand, final DecisionSaved decisionSaved, final CaseNoteAdded caseNoteAdded) {
        verifyNoteAdded(decisionCommand, decisionSaved, caseNoteAdded, decisionCommand.getNote(), NoteType.DECISION);
    }

    public static void verifyCaseUnassigned(final UUID caseId, final CaseUnassigned caseUnassigned) {
        assertThat(caseUnassigned.getCaseId(), is(caseId));
    }

    public static void verifyCaseReferredForCourtHearing(final DecisionSaved decisionSaved,
                                                         final ReferForCourtHearing referForCourtHearing,
                                                         final CaseReferredForCourtHearing caseReferredForCourtHearing,
                                                         final List<OffenceDecisionInformation> offenceDecisionInformationList,
                                                         final String referralReason) {
        assertThat(caseReferredForCourtHearing.getCaseId(), is(decisionSaved.getCaseId()));
        assertThat(caseReferredForCourtHearing.getReferralReasonId(), is(referForCourtHearing.getReferralReasonId()));
        assertThat(caseReferredForCourtHearing.getReferralReason(), is(referralReason));
        assertThat(caseReferredForCourtHearing.getEstimatedHearingDuration(), is(referForCourtHearing.getEstimatedHearingDuration()));
        assertThat(caseReferredForCourtHearing.getListingNotes(), is(referForCourtHearing.getListingNotes()));
        assertThat(caseReferredForCourtHearing.getReferredOffences(), is(offenceDecisionInformationList));
        assertThat(caseReferredForCourtHearing.getReferredAt(), is(decisionSaved.getSavedAt()));
        assertThat(caseReferredForCourtHearing.getDefendantCourtOptions(), is(referForCourtHearing.getDefendantCourtOptions()));
    }

    public static void verifyCaseReferredForCourtHearingV2(final DecisionSaved decisionSaved,
                                                           final ReferForCourtHearing referForCourtHearing,
                                                           final CaseReferredForCourtHearingV2 caseReferredForCourtHearing,
                                                           final List<OffenceDecisionInformation> offenceDecisionInformationList,
                                                           final String referralReason) {
        assertThat(caseReferredForCourtHearing.getCaseId(), is(decisionSaved.getCaseId()));
        assertThat(caseReferredForCourtHearing.getReferralReasonId(), is(referForCourtHearing.getReferralReasonId()));
        assertThat(caseReferredForCourtHearing.getReferralReason(), is(referralReason));
        assertThat(caseReferredForCourtHearing.getEstimatedHearingDuration(), is(referForCourtHearing.getEstimatedHearingDuration()));
        assertThat(caseReferredForCourtHearing.getListingNotes(), is(referForCourtHearing.getListingNotes()));
        assertThat(caseReferredForCourtHearing.getReferredOffences(), is(offenceDecisionInformationList));
        assertThat(caseReferredForCourtHearing.getReferredAt(), is(decisionSaved.getSavedAt()));
        assertThat(caseReferredForCourtHearing.getDefendantCourtOptions(), is(referForCourtHearing.getDefendantCourtOptions()));
    }

    public static void verifyCaseAdjourned(final DecisionSaved decisionSaved, final Adjourn adjourn, final CaseAdjournedToLaterSjpHearingRecorded caseAdjourned) {
        assertThat(caseAdjourned.getCaseId(), is(decisionSaved.getCaseId()));
        assertThat(caseAdjourned.getSessionId(), is(decisionSaved.getSessionId()));
        assertThat(caseAdjourned.getAdjournedTo(), is(adjourn.getAdjournTo()));
    }

    public static void verifyHearingLanguagePreferenceUpdated(final DecisionSaved decisionSaved, final ReferForCourtHearing referForCourtHearing, final HearingLanguagePreferenceUpdatedForDefendant hearingLanguagePreferenceUpdatedForDefendant) {
        assertThat(hearingLanguagePreferenceUpdatedForDefendant.getCaseId(), is(decisionSaved.getCaseId()));
        assertThat(hearingLanguagePreferenceUpdatedForDefendant.isUpdatedByOnlinePlea(), is(false));
        assertThat(hearingLanguagePreferenceUpdatedForDefendant.getSpeakWelsh(), is(referForCourtHearing.getDefendantCourtOptions().getWelshHearing()));
    }

    public static void verifyInterpreterUpdated(final DecisionSaved decisionSaved, final ReferForCourtHearing referForCourtHearing, final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant) {
        assertThat(interpreterUpdatedForDefendant.getCaseId(), is(decisionSaved.getCaseId()));
        assertThat(interpreterUpdatedForDefendant.isUpdatedByOnlinePlea(), is(false));
        assertThat(interpreterUpdatedForDefendant.getInterpreter().getLanguage(), is(referForCourtHearing.getDefendantCourtOptions().getInterpreter().getLanguage()));
    }

    private static void verifyNoteAdded(final DecisionCommand decisionCommand, final DecisionSaved decisionSaved, final CaseNoteAdded caseNoteAdded, final String expectedNote, final NoteType expectedNoteType) {
        assertThat(caseNoteAdded.getCaseId(), is(decisionSaved.getCaseId()));
        assertThat(caseNoteAdded.getAuthor(), is(decisionCommand.getSavedBy()));
        assertThat(caseNoteAdded.getNote().getText(), is(expectedNote));
        assertThat(caseNoteAdded.getNote().getType(), is(expectedNoteType));
        assertThat(caseNoteAdded.getDecisionId(), is(decisionSaved.getDecisionId()));
        assertThat(caseNoteAdded.getNote().getAddedAt(), is(decisionSaved.getSavedAt()));
    }

    //TODO needs changing
    private static Optional<JsonObject> getOffenceDecision(final JsonObject decision, final UUID offenceId) {
        return decision.getJsonArray("offenceDecisions")
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(od -> offenceId.toString().equals(od.getString("offenceId")))
                .findFirst();
    }

    private static JsonObject pollReadyCasesUntilResponseIsJson(final UUID userId, final Matcher<? super ReadContext> matcher) {
        final RequestParamsBuilder requestParams = requestParams(getReadUrl(QUERY_READY_CASES_RESOURCE), QUERY_READY_CASES)
                .withHeader(HeaderConstants.USER_ID, userId);
        return pollWithDefaultsUntilResponseIsJson(requestParams.build(), matcher);
    }

    private static JsonObject queryDecision(final DecisionSaved decisionSaved) {
        final Matcher decisionIdMatcher = withJsonPath("$.caseDecisions[*].id", hasItem(decisionSaved.getDecisionId().toString()));
        final JsonObject aCase = getCase(decisionSaved.getCaseId(), decisionIdMatcher);
        return aCase.getJsonArray("caseDecisions").getValuesAs(JsonObject.class).stream()
                .filter(decision -> decision.getString("id").equals(decisionSaved.getDecisionId().toString()))
                .findFirst()
                .get();
    }

    private static UUID startSessionAndRequestAssignment(final User user, final SessionType sessionType, final ProsecutingAuthority prosecutingAuthority, final UUID caseId) {
        final String courtHouseCode = prosecutingAuthority == ProsecutingAuthority.TFL ? DEFAULT_LONDON_COURT_HOUSE_OU_CODE : DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE;
        final UUID sessionId = randomUUID();
        startSessionAndConfirm(sessionId, user.getUserId(), courtHouseCode, sessionType);
        requestCaseAssignmentAndConfirm(sessionId, user.getUserId(), caseId);
        return sessionId;
    }

    public static void verifyFinancialImposition(final DecisionSaved decisionSaved, final FinancialImposition financialImposition) {
        final JsonObject caseDecision = queryDecision(decisionSaved);
        final JsonObject actualFinancialImposition = caseDecision.getJsonObject("financialImposition");

        assertThat(actualFinancialImposition, isJson(allOf(
                withJsonPath("$.costsAndSurcharge.costs",
                        hasToString(financialImposition.getCostsAndSurcharge().getCosts().toString())),
                withJsonPath("$.payment.totalSum",
                        hasToString(financialImposition.getPayment().getTotalSum().toString())),
                withJsonPath("$.costsAndSurcharge.reasonForReducedVictimSurcharge",
                        hasToString(containsString("reason for reduced victim surcharge"))
                )))
        );
    }
}
