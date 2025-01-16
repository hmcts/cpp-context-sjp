package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDefaultDecision;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;

import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.MarkCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.UndoCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.helper.CaseReopenedInLibraHelper.UpdateCaseReopenedInLibraHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseReopenedInLibraIT extends BaseIntegrationTest {

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder ;

    @BeforeEach
    public void setUp() throws Exception {
        final ProsecutingAuthority prosecutingAuthority = ProsecutingAuthority.TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();

        cleanViewStore();

        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "DEFENDANT_REGION");

        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(createCasePayloadBuilder.getId());

        saveDefaultDecision(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceIds());
    }

    @Test
    public void shouldMarkCaseReopenedInLibra() {
        try (final CaseReopenedInLibraHelper mark = new MarkCaseReopenedInLibraHelper(createCasePayloadBuilder.getId())) {
            test(mark);
        }
    }

    @Test
    public void shouldUpdateCaseReopenedInLibra() {
        shouldMarkCaseReopenedInLibra();

        try (final CaseReopenedInLibraHelper update = new UpdateCaseReopenedInLibraHelper(createCasePayloadBuilder.getId())) {
            test(update);
        }
    }

    @Test
    public void shouldUndoCaseReopenedInLibra() {
        shouldMarkCaseReopenedInLibra();

        try (final CaseReopenedInLibraHelper undo = new UndoCaseReopenedInLibraHelper(createCasePayloadBuilder.getId())) {
            test(undo);
        }
    }

    private void test(CaseReopenedInLibraHelper caseReopenedInLibraHelper) {
        caseReopenedInLibraHelper.call();
        caseReopenedInLibraHelper.assertCaseReopenedDetailsSet();
        caseReopenedInLibraHelper.verifyEventInPublicTopic();
    }
}
