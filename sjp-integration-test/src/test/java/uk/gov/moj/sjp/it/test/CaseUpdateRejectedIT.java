package uk.gov.moj.sjp.it.test;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.jgroups.util.Util.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllReferenceData;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;

import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseUpdateRejected;
import uk.gov.moj.cpp.sjp.event.processor.AssignmentProcessor;
import uk.gov.moj.sjp.it.Constants;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.json.JsonObject;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseUpdateRejectedIT extends BaseIntegrationTest {

    private UUID caseId;
    private UUID offenceId;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @BeforeEach
    public void init() throws SQLException {

        new SjpDatabaseCleaner().cleanViewStore();

        ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery();
        //TODO: remove after ATCM-3219
        stubStartSjpSessionCommand();
        stubAllReferenceData();

        caseId = randomUUID();
        offenceId = randomUUID();

        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        createCasePayloadBuilder = CreateCase
                .CreateCasePayloadBuilder.withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId);

        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "DEFENDANT_REGION");

        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder));
    }

    @Test
    public void shouldRejectWhenCaseAssigned() throws Exception {
        final UUID sessionId = randomUUID();
        final UUID caseId = assignCase(sessionId);
        assertCaseUpdateRejected(caseId, CaseUpdateRejected.RejectReason.CASE_ASSIGNED);
    }

    @Test
    public void shouldRejectWhenCaseCompleted() throws Exception {
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        DecisionHelper.saveDefaultDecision(caseId, offenceId);
        assertCaseUpdateRejected(caseId, CaseUpdateRejected.RejectReason.CASE_COMPLETED);
    }

    private void requestWithdrawalForAllOffences(final UUID caseId) {
        try (final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(USER_ID)) {
            final UUID withdrawalReasonId = randomUUID();
            WithdrawalRequestsStatus offenceStatus = new WithdrawalRequestsStatus(offenceId, withdrawalReasonId);
            offencesWithdrawalRequestHelper.requestWithdrawalOfOffences(caseId, singletonList(offenceStatus));
        } catch (JMSException e) {
            fail("exception requesting offence withdrawal");
        }
    }

    private void assertCaseUpdateRejected(final UUID caseId, final CaseUpdateRejected.RejectReason rejectReason) throws Exception {

        final Optional<JsonEnvelope> jsonEnvelope = new EventListener()
                .subscribe(Constants.PUBLIC_SJP_CASE_UPDATE_REJECTED)
                .run(() -> requestWithdrawalForAllOffences(caseId))
                .popEvent(Constants.PUBLIC_SJP_CASE_UPDATE_REJECTED);

        assertTrue(jsonEnvelope.isPresent());
        final JsonObject payload = jsonEnvelope.get().payloadAsJsonObject();
        assertThat(payload.getString("caseId"), equalTo(caseId.toString()));
        assertThat(payload.getString("reason"), equalTo(rejectReason.name()));

    }

    private UUID assignCase(final UUID sessionId) {

        final UUID userId = randomUUID();

        SessionHelper.startMagistrateSession(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, "Reggie Gates");

        final Optional<JsonEnvelope> jsonEnvelope = AssignmentHelper.requestCaseAssignmentAndWaitForEvent(sessionId, userId, AssignmentProcessor.PUBLIC_SJP_CASE_ASSIGNED);

        return jsonEnvelope.map(envelope ->
                UUID.fromString(envelope.payloadAsJsonObject().getString("caseId")))
                .orElseThrow(IllegalStateException::new);
    }

}
