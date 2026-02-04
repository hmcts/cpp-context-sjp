package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.command.UpdateDefendantDetails.acknowledgeDefendantDetailsUpdates;
import static uk.gov.moj.sjp.it.command.UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.UpdateDefendantDetails;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DefendantDetailsIT extends BaseIntegrationTest {

    private static final String DEFENDANT_DETAIL_UPDATES_CONTENT_TYPE = "application/vnd.sjp.query.defendant-details-updates+json";
    private static final String DEFENDANT_DETAILS_UPDATES_ACKNOWLEDGED_PUBLIC_EVENT = "public.sjp.defendant-details-updates-acknowledged";
    private final UUID caseIdOne = randomUUID();
    private final UUID caseIdTwo = randomUUID();
    private final UUID tvlUserUid = randomUUID();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @BeforeEach
    public void setUp() {

        createCasePayloadBuilder = withDefaults();
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        createCaseForPayloadBuilder(createCasePayloadBuilder.withId(caseIdOne));

        createCaseForPayloadBuilder(withDefaults().withId(caseIdTwo));

        stubForUserDetails(tvlUserUid, ProsecutingAuthority.TVL);
        stubProsecutorQuery(TFL.name(), TFL.getFullName(), randomUUID());
    }

    @Test
    public void shouldUpdateDefendantDetails() {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();

        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseIdOne, UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")), payloadBuilder);

        pollUntilCaseByIdIsOk(caseIdOne, allOf(
                        withJsonPath("$.defendant.personalDetails.nameChanged", is(false)),
                        withJsonPath("$.defendant.personalDetails.dobChanged", is(false)),
                        withJsonPath("$.defendant.personalDetails.addressChanged", is(false)),
                        withJsonPath("$.defendant.personalDetails.driverNumber", is("MORGA753116SM9IJ"))
                )
        );
    }

    @Test
    public void shouldAllowNonStandardTitle() {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults().withTitle("Mister");

        updateDefendantDetailsForCaseAndPayload(caseIdOne, UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")), payloadBuilder);

        pollUntilCaseByIdIsOk(caseIdOne, allOf(
                withJsonPath("$.defendant.personalDetails.nameChanged", is(false)),
                withJsonPath("$.defendant.personalDetails.dobChanged", is(false)),
                withJsonPath("$.defendant.personalDetails.addressChanged", is(false)),
                withJsonPath("$.defendant.personalDetails.title", is("Mister")))
        );
    }

    @Test
    public void shouldAllowEmptyTitle() {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults().withTitle(null);

        updateDefendantDetailsForCaseAndPayload(caseIdOne, UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")), payloadBuilder);

        pollUntilCaseByIdIsOk(caseIdOne, allOf(
                withJsonPath("$.defendant.personalDetails.nameChanged", is(false)),
                withJsonPath("$.defendant.personalDetails.dobChanged", is(false)),
                withJsonPath("$.defendant.personalDetails.addressChanged", is(false)),
                withoutJsonPath("$.defendant.personalDetails.title")
        ));
    }

    @Test
    public void shouldNotChangeNameOrDobIfThereAreNoUpdates() {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder
                .withDefaults()
                .withLastName("LLOYD")
                .withDateOfBirth(LocalDates.from("1980-07-15"));

        updateDefendantDetailsForCaseAndPayload(caseIdOne, UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")), payloadBuilder);

        pollUntilCaseByIdIsOk(caseIdOne, allOf(
                withJsonPath("$.defendant.personalDetails.nameChanged", is(false)),
                withJsonPath("$.defendant.personalDetails.dobChanged", is(false)),
                withJsonPath("$.defendant.personalDetails.addressChanged", is(false))
        ));
    }

    @Test
    public void shouldSchemaValidationFailWhenDefendantEmailBlank() {
        shouldSchemaValidationFailWhenDefendantEmailInvalid("   ");
    }

    @Test
    public void shouldSchemaValidationFailWhenDefendantEmailInvalid() {
        shouldSchemaValidationFailWhenDefendantEmailInvalid("@b.co");
    }

    @Test
    public void shouldSchemaValidationFailWhenDefendantDriverNumberInvalid() {
        shouldSchemaValidationFailWhenDefendantDriverNumberInvalid("MORGA753116SM999");
    }

    private void shouldSchemaValidationFailWhenDefendantDriverNumberInvalid(final String driverNumber) {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();
        payloadBuilder.withDriverNumber(driverNumber);

        final String response = updateDefendantDetailsForCaseAndPayload(caseIdOne, UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")), payloadBuilder, BAD_REQUEST);

        JsonObject responseJson = responseToJsonObject(response);
        JsonValue validationErrors = responseJson.get("validationErrors");
        String validationTrace = validationErrors.toString();

        assertThat(validationTrace, containsString(String.format("#/driverNumber: string [%s] does not match pattern", driverNumber)));

    }

    private void shouldSchemaValidationFailWhenDefendantEmailInvalid(final String email) {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();
        payloadBuilder.getContactDetailsBuilder().withEmail(email);

        final String response = updateDefendantDetailsForCaseAndPayload(caseIdOne, UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")), payloadBuilder, BAD_REQUEST);

        JsonObject responseJson = responseToJsonObject(response);
        JsonValue validationErrors = responseJson.get("validationErrors");
        String validationTrace = validationErrors.toString();

        assertThat(validationTrace, containsString(String.format("#/email: string [%s] does not match pattern", email)));

    }

    @Test
    public void shouldNotReturnAcknowledgedUpdates() {
        getUpdatedDefendantDetails(USER_ID);

        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();
        UUID defendantId = UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id"));
        updateDefendantDetailsForCaseAndPayload(caseIdOne, defendantId, payloadBuilder);

        final EventListener updatesAcknowledgedListener = new EventListener()
                .subscribe(DEFENDANT_DETAILS_UPDATES_ACKNOWLEDGED_PUBLIC_EVENT)
                .run(() -> acknowledgeDefendantDetailsUpdates(caseIdOne, defendantId));

        assertThatUpdatesAcknowledgedPublicEventRaised(updatesAcknowledgedListener, caseIdOne, defendantId);
    }

    private void assertThatUpdatesAcknowledgedPublicEventRaised(
            final EventListener eventListener,
            final UUID caseId,
            final UUID defendantId) {


        final Optional<JsonEnvelope> datesToAvoidPublicEvent = eventListener
                .popEvent(DEFENDANT_DETAILS_UPDATES_ACKNOWLEDGED_PUBLIC_EVENT);

        assertThat(datesToAvoidPublicEvent.isPresent(), is(true));

        MatcherAssert.assertThat(datesToAvoidPublicEvent.get(),
                jsonEnvelope(
                        metadata().withName(DEFENDANT_DETAILS_UPDATES_ACKNOWLEDGED_PUBLIC_EVENT),
                        JsonEnvelopePayloadMatcher.payload().isJson(CoreMatchers.allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.defendantId", equalTo(defendantId.toString()))
                        ))));
    }

    @Test
    public void shouldObeyLimit() {
        updateDefendantDetailsForCaseAndPayload(
                caseIdOne,
                UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")),
                UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults());

        updateDefendantDetailsForCaseAndPayload(
                caseIdTwo,
                UUID.fromString(pollUntilCaseByIdIsOk(caseIdTwo).getString("defendant.id")),
                UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults());


        List<Matcher<? super ReadContext>> matchers = ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$.total", greaterThanOrEqualTo(0)))
                .add(withJsonPath("$.defendantDetailsUpdates.length()", is(0)))
                .build();

        pollWithDefaults(defendantDetailUpdatesRequestParams(1, USER_ID))
                .until(status().is(OK),
                        payload().isJson(allOf(matchers)));
    }


    @Test
    public void shouldReturnUpdatedDetailsBasedOnUserProsecutingAuthority() {
        final JsonObject existingUpdatedDefendantDetails = getUpdatedDefendantDetails(tvlUserUid);

        updateDefendantDetailsForCaseAndPayload(
                caseIdOne,
                UUID.fromString(pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")),
                UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults());

        List<Matcher<? super ReadContext>> matchers = ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$.total", equalTo(existingUpdatedDefendantDetails.getInt("total"))))
                .build();

        pollWithDefaults(defendantDetailUpdatesRequestParams(Integer.MAX_VALUE, tvlUserUid))
                .until(status().is(OK),
                        payload().isJson(allOf(matchers)));
    }

    private RequestParamsBuilder defendantDetailUpdatesRequestParams(int limit, UUID userId) {
        return requestParams(
                getReadUrl("/defendant-details-updates?limit=" + limit),
                DEFENDANT_DETAIL_UPDATES_CONTENT_TYPE)
                .withHeader(HeaderConstants.USER_ID, userId.toString());
    }

    private JsonObject getUpdatedDefendantDetails(UUID userUid) {
        final Response response = makeGetCall(
                "/defendant-details-updates?limit=" + Integer.MAX_VALUE,
                DEFENDANT_DETAIL_UPDATES_CONTENT_TYPE,
                userUid);
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        return createReader(new StringReader(response.readEntity(String.class))).readObject();
    }

    private JsonObject responseToJsonObject(String response) {
        return createReader(new StringReader(response)).readObject();
    }
}
