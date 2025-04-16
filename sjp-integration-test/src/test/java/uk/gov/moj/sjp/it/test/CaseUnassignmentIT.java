package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_USER;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.sjp.it.commandclient.AssignNextCaseClient;
import uk.gov.moj.sjp.it.commandclient.CreateCaseClient;
import uk.gov.moj.sjp.it.commandclient.UnassignCaseClient;
import uk.gov.moj.sjp.it.helper.AssignmentHelper;
import uk.gov.moj.sjp.it.model.Defendant;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CaseUnassignmentIT extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(CaseUnassignmentIT.class);

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = randomUUID();
    private static final String COURT_HOUSE_OU_CODE = "B01OK";
    private final UUID[] returnedSessionId = new UUID[1];

    @BeforeEach
    public void setUp() throws SQLException {
        stubCourtByCourtHouseOUCodeQuery(COURT_HOUSE_OU_CODE, "2572");
        cleanViewStore();

        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        final Defendant defendant = Defendant.builder().build();
        CreateCaseClient createCase = CreateCaseClient.builder().id(CASE_ID).defendant(defendant).build();
        final ProsecutingAuthority prosecutingAuthority = createCase.prosecutingAuthority;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(defendant.address.postcode, "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "DEFENDANT_REGION");

        createCase.caseReceivedHandler = envelope -> log.info("Case is created");
        Optional<Response> createCaseResponse = createCase.getExecutor().executeSync();
        assertThat(createCaseResponse.get().getStatus(), equalTo(202));

        startSessionAndConfirm(SESSION_ID, DEFAULT_USER.getUserId(), COURT_HOUSE_OU_CODE, SessionType.MAGISTRATE);
        requestCaseAssignmentAndConfirm(SESSION_ID, DEFAULT_USER.getUserId(), createCase.id);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().build();
        assignCase.sessionId = returnedSessionId[0];

    }

    private static void startSessionAndRequestAssignment(final User user, final UUID caseId) {
        startSessionAndConfirm(SESSION_ID, user.getUserId(), COURT_HOUSE_OU_CODE, SessionType.MAGISTRATE);
        requestCaseAssignmentAndConfirm(SESSION_ID, user.getUserId(), caseId);
    }

    @Test
    @SuppressWarnings("squid:S1607")
    public void unassignCase() {
        UnassignCaseClient unassignCase = new UnassignCaseClient();
        unassignCase.caseId = CASE_ID;
        unassignCase.caseUnassignedHandler = envelope -> {
            final UUID caseId = UUID.fromString(((JsonEnvelope) envelope).payloadAsJsonObject().getString("caseId"));
            assertThat(caseId, equalTo(CASE_ID));

            AssignmentHelper.pollCaseUnassigned(CASE_ID);
        };

        Optional<Response> response = unassignCase.getExecutor().executeSync();
        assertThat(response.get().getStatus(), equalTo(202));
    }

}
