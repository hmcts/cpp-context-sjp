package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollForCase;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.UpdateHearingRequirementsHelper;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateHearingRequirementsIT extends BaseIntegrationTest {

    private static UpdateHearingRequirementsHelper updateHearingRequirementsHelper;
    private static CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    private static UUID caseId;
    private static String defendantId;

    @BeforeEach
    public void setUpNewCase() {
        updateHearingRequirementsHelper = new UpdateHearingRequirementsHelper();
        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        createCaseForPayloadBuilder(createCasePayloadBuilder);
        caseId = createCasePayloadBuilder.getId();
        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();

        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "NATIONAL_COURT_CODE", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("NATIONAL_COURT_CODE", "TestRegion");

        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        defendantId = pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
    }

    @AfterEach
    public void tearDown() throws Exception {
        updateHearingRequirementsHelper.close();
    }

    @Test
    public void shouldUpdateCaseWithInterpreterAndSpeakWelshAndThanCancelThem() {
        updateHearingRequirementsHelper.updateHearingRequirements(caseId, defendantId, "spanish", false);
        pollForCase(caseId, new Matcher[]{
                withJsonPath("$.defendant.interpreter.language", is("spanish")),
                withJsonPath("$.defendant.interpreter.needed", is(true)),
                withJsonPath("$.defendant.speakWelsh", is(false)),
        });

        updateHearingRequirementsHelper.updateHearingRequirements(caseId, defendantId, null, null);
        pollForCase(caseId, new Matcher[]{
                withoutJsonPath("$.defendant.interpreter.language"),
                withJsonPath("$.defendant.interpreter.needed", is(false)),
                withJsonPath("$.defendant.speakWelsh", is(false)),
        });
    }

    @Test
    public void shouldUpdateCaseWithInterpreterForValidLanguageAndEmptySpeakWelsh() {
        updateHearingRequirementsHelper.updateHearingRequirements(caseId, defendantId, "french", null);
        pollForCase(caseId, new Matcher[]{
                withJsonPath("$.defendant.interpreter.language", is("french")),
                withJsonPath("$.defendant.interpreter.needed", is(true)),
                withJsonPath("$.defendant.speakWelsh", is(false)),
        });

    }

    @Test
    public void shouldUpdateCaseWithSpeakWelshForEmptyInterpreterAndValidSpeakWelsh() {
        updateHearingRequirementsHelper.updateHearingRequirements(caseId, defendantId, null, true);
        pollForCase(caseId, new Matcher[]{
                withoutJsonPath("$.defendant.interpreter.language"),
                withJsonPath("$.defendant.interpreter.needed", is(false)),
                withJsonPath("$.defendant.speakWelsh", is(true)),
        });
    }

}
