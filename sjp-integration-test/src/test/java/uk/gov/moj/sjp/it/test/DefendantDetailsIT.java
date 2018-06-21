package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.Is.is;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;

import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.command.UpdateDefendantDetails;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class DefendantDetailsIT extends BaseIntegrationTest {

    private final UUID caseId = randomUUID();

    @Before
    public void setUp() {
        new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(withDefaults().withId(caseId)));
    }

    @Test
    public void shouldUpdateDefendantDetails() {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();

        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseId, UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id")), payloadBuilder);

        CasePoller.pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.defendant.personalDetails.nameChanged", is(true)),
                withJsonPath("$.defendant.personalDetails.dobChanged", is(true)),
                withJsonPath("$.defendant.personalDetails.addressChanged", is(true))
        ));
    }
}
