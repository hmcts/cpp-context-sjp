package uk.gov.moj.sjp.it.test.ingestor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REOPENED_IN_LIBRA;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDefaultDecision;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.test.ingestor.helper.CasePredicate.caseStatusIs;
import static uk.gov.moj.sjp.it.test.ingestor.helper.ElasticSearchQueryHelper.getCaseFromElasticSearchWithPredicate;

import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.unifiedsearch.test.util.ingest.ElasticSearchIndexRemoverUtil;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.framework.util.ViewStoreCleaner;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.MarkCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.test.BaseIntegrationTest;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseStatusChangedIT extends BaseIntegrationTest {
    private final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
    private static final String NATIONAL_COURT_CODE = "1080";


    @BeforeEach
    public void setUp() throws Exception {
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");

        new ElasticSearchIndexRemoverUtil().deleteAndCreateCaseIndex();

        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder));

        saveDefaultDecision(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceIds());
    }

    @AfterEach
    public void cleanDatabase() {
        viewStoreCleaner.cleanDataInViewStore(createCasePayloadBuilder.getId());
    }

    @Test
    void shouldChangeCaseStatus() {
        final UUID caseId= createCasePayloadBuilder.getId();
        JsonObject outputCase = getCaseFromElasticSearchWithPredicate(caseStatusIs(COMPLETED), caseId.toString());
        assertThat(caseId.toString(), is(outputCase.getString("caseId")));
        assertThat(outputCase.getString("caseStatus"), is(COMPLETED.name()));
        assertThat(outputCase.getString("_case_type"), is("PROSECUTION"));

        try (final CaseReopenedInLibraHelper mark = new MarkCaseReopenedInLibraHelper(caseId)) {
            test(mark);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        outputCase = getCaseFromElasticSearchWithPredicate(caseStatusIs(REOPENED_IN_LIBRA), caseId.toString());
        assertThat(caseId.toString(), is(outputCase.getString("caseId")));
        assertThat(outputCase.getString("caseStatus"), is(REOPENED_IN_LIBRA.name()));
        assertThat(outputCase.getString("_case_type"), is("PROSECUTION"));
    }

    private void test(CaseReopenedInLibraHelper caseReopenedInLibraHelper) {
        caseReopenedInLibraHelper.call();
        caseReopenedInLibraHelper.assertCaseReopenedDetailsSet();
        caseReopenedInLibraHelper.verifyEventInPublicTopic();
    }
}
