package uk.gov.moj.sjp.it.test;


import static java.util.UUID.randomUUID;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;

import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.UpdateHearingRequirementsHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UpdateHearingRequirementsIT extends BaseIntegrationTest {

    private static UpdateHearingRequirementsHelper updateHearingRequirementsHelper;
    private static CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    private static UUID caseId;
    private static String defendantId;

    @Before
    public void setUpNewCase() {
        updateHearingRequirementsHelper = new UpdateHearingRequirementsHelper();
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);
        caseId = createCasePayloadBuilder.getId();
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();

        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "NATIONAL_COURT_CODE", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("NATIONAL_COURT_CODE", "TestRegion");

        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
    }

    @After
    public void tearDown() throws Exception {
        updateHearingRequirementsHelper.close();
    }

    @Test
    public void shouldUpdateCaseWithInterpreterAndSpeakWelshAndThanCancelThem() {

        updateHearingRequirementsHelper.updateHearingRequirements(caseId, defendantId,"spanish", false);

        updateHearingRequirementsHelper.pollForInterpreter(caseId, defendantId, "spanish");
        updateHearingRequirementsHelper.pollForSpeakWelsh(caseId, defendantId, false);

        updateHearingRequirementsHelper.updateHearingRequirements(caseId, defendantId, null, null);
        updateHearingRequirementsHelper.pollForEmptyInterpreter(caseId, defendantId);
        updateHearingRequirementsHelper.pollForSpeakWelsh(caseId, defendantId, false);
    }

    @Test
    public void shouldUpdateCaseWithInterpreterForValidLanguageAndEmptySpeakWelsh() {
        updateHearingRequirementsHelper.updateHearingRequirements(caseId, defendantId, "french",null);

        updateHearingRequirementsHelper.pollForInterpreter(caseId, defendantId, "french");
        updateHearingRequirementsHelper.pollForSpeakWelsh(caseId, defendantId, false);
    }

    @Test
    public void shouldUpdateCaseWithSpeakWelshForEmptyInterpreterAndValidSpeakWelsh() {
        updateHearingRequirementsHelper.updateHearingRequirements(caseId, defendantId, null, true);

        updateHearingRequirementsHelper.pollForEmptyInterpreter(caseId, defendantId);
        updateHearingRequirementsHelper.pollForSpeakWelsh(caseId, defendantId, true);
    }

}
