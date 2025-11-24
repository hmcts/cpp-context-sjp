package uk.gov.moj.sjp.it.test;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollCaseUnassigned;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDefaultDecisionInSession;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startMagistrateSessionAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_USER_ID;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.util.UUID;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AssignmentTimeoutIT extends BaseIntegrationTest {

    private static final UUID CASE_ID = randomUUID(), OFFENCE_ID = randomUUID();

    private static CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
            .withPostingDate(now().minusDays(30)).withId(CASE_ID).withOffenceId(OFFENCE_ID);
    private static final String NATIONAL_COURT_CODE = "1080";


    @BeforeEach
    public void setUp() throws Exception {
        cleanViewStore();

        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubGetEmptyAssignmentsByDomainObjectId(CASE_ID);

        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "region");

        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(CASE_ID);
    }

    @Test
    public void shouldCancelAssignmentWhenDecisionSaved() {
        final UUID sessionId = randomUUID();

        startMagistrateSessionAndConfirm(sessionId, DEFAULT_USER_ID, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, "John Smith");

        requestCaseAssignmentAndConfirm(sessionId, DEFAULT_USER_ID, CASE_ID);

        saveDefaultDecisionInSession(CASE_ID, sessionId, DEFAULT_USER_ID, asList(OFFENCE_ID));

        pollCaseUnassigned(CASE_ID);
    }

}
