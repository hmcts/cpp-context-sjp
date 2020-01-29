package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.getReadUrl;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.UpdateDefendantDetails;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.HttpClientUtil;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.ReadContext;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefendantDetailsIT extends BaseIntegrationTest {

    private static final String DEFENDANT_DETAIL_UPDATES_CONTENT_TYPE = "application/vnd.sjp.query.defendant-details-updates+json";
    private static final String DEFENDANT_DETAILS_UPDATES_ACKNOWLEDGED_PUBLIC_EVENT = "public.sjp.defendant-details-updates-acknowledged";
    private final UUID caseIdOne = randomUUID();
    private final UUID caseIdTwo = randomUUID();
    private final UUID tvlUserUid = randomUUID();
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {

        createCasePayloadBuilder = withDefaults();
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(createCasePayloadBuilder.withId(caseIdOne)));

        new EventListener()
                .subscribe(CaseReceived.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(withDefaults().withId(caseIdTwo)));

        stubForUserDetails(tvlUserUid, ProsecutingAuthority.TVL);
        stubProsecutorQuery(TFL.name(), TFL.getFullName(), randomUUID());
    }

    @Test
    public void shouldUpdateDefendantDetails() {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();

        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseIdOne, UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")), payloadBuilder);

        CasePoller.pollUntilCaseByIdIsOk(caseIdOne, allOf(
                withJsonPath("$.defendant.personalDetails.nameChanged", is(true)),
                withJsonPath("$.defendant.personalDetails.dobChanged", is(true)),
                withJsonPath("$.defendant.personalDetails.addressChanged", is(true))));
    }

    @Test
    public void shouldAllowNonStandardTitle() {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults().withTitle("Mister");

        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseIdOne, UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")), payloadBuilder);

        CasePoller.pollUntilCaseByIdIsOk(caseIdOne, allOf(
                withJsonPath("$.defendant.personalDetails.nameChanged", is(true)),
                withJsonPath("$.defendant.personalDetails.dobChanged", is(true)),
                withJsonPath("$.defendant.personalDetails.addressChanged", is(true)),
                withJsonPath("$.defendant.personalDetails.title", is("Mister"))
        ));
    }

    @Test
    public void shouldAllowEmptyTitle() {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults().withTitle(null);

        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseIdOne, UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")), payloadBuilder);

        CasePoller.pollUntilCaseByIdIsOk(caseIdOne, allOf(
                withJsonPath("$.defendant.personalDetails.nameChanged", is(true)),
                withJsonPath("$.defendant.personalDetails.dobChanged", is(true)),
                withJsonPath("$.defendant.personalDetails.addressChanged", is(true)),
                withoutJsonPath("$.defendant.personalDetails.title")
        ));
    }

    @Test
    public void shouldNotChangeNameOrDobIfThereAreNoUpdates() {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder
                .withDefaults()
                .withLastName("LLOYD")
                .withDateOfBirth(LocalDates.from("1980-07-15"));

        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseIdOne, UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")), payloadBuilder);

        CasePoller.pollUntilCaseByIdIsOk(caseIdOne, allOf(
                withJsonPath("$.defendant.personalDetails.nameChanged", is(false)),
                withJsonPath("$.defendant.personalDetails.dobChanged", is(false)),
                withJsonPath("$.defendant.personalDetails.addressChanged", is(true)),
                withJsonPath("$.defendant.personalDetails.firstName", is(this.createCasePayloadBuilder.getDefendantBuilder().getFirstName())),
                withJsonPath("$.defendant.personalDetails.lastName", is(this.createCasePayloadBuilder.getDefendantBuilder().getLastName())),
                withJsonPath("$.defendant.personalDetails.dateOfBirth", is(LocalDates.to(this.createCasePayloadBuilder.getDefendantBuilder().getDateOfBirth())))
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

    private void shouldSchemaValidationFailWhenDefendantEmailInvalid(final String email) {
        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();
        payloadBuilder.getContactDetailsBuilder().withEmail(email);

        final String response = UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseIdOne, UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")), payloadBuilder, BAD_REQUEST);

        JsonObject responseJson = responseToJsonObject(response);
        JsonValue validationErrors = responseJson.get("validationErrors");
        String validationTrace = validationErrors.toString();

        assertThat(validationTrace, containsString(String.format("#/email: string [%s] does not match pattern", email)));

    }

    @Test
    public void shouldFindUpdatedDefendantDetails() {
        final JsonObject existingUpdatedDefendantDetails = getUpdatedDefendantDetails(USER_ID);
        final int existingUpdatedDefendantDetailsTotal = existingUpdatedDefendantDetails.getInt("total");

        final UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();

        validateDefendantUpdated(existingUpdatedDefendantDetailsTotal, payloadBuilder);
    }

    private void validateDefendantUpdated(final int existingUpdatedDefendantDetailsTotal, final UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder) {
        final UUID defendantId = UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id"));
        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseIdOne, defendantId, payloadBuilder);

        final List<Matcher<? super ReadContext>> matchers = ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$.total", equalTo(existingUpdatedDefendantDetailsTotal + 1)))
                .add(withJsonPath(format("$.defendantDetailsUpdates[{0}].firstName", existingUpdatedDefendantDetailsTotal), equalTo(payloadBuilder.getFirstName())))
                .add(withJsonPath(format("$.defendantDetailsUpdates[{0}].lastName", existingUpdatedDefendantDetailsTotal), equalTo(payloadBuilder.getLastName())))
                .add(withJsonPath(format("$.defendantDetailsUpdates[{0}].defendantId", existingUpdatedDefendantDetailsTotal), equalTo(defendantId.toString())))
                .add(withJsonPath(format("$.defendantDetailsUpdates[{0}].caseUrn", existingUpdatedDefendantDetailsTotal), notNullValue()))
                .add(withJsonPath(format("$.defendantDetailsUpdates[{0}].caseId", existingUpdatedDefendantDetailsTotal), notNullValue()))
                .add(withJsonPath(format("$.defendantDetailsUpdates[{0}].nameUpdated", existingUpdatedDefendantDetailsTotal), is(true)))
                .add(withJsonPath(format("$.defendantDetailsUpdates[{0}].dateOfBirthUpdated", existingUpdatedDefendantDetailsTotal), is(true)))
                .add(withJsonPath(format("$.defendantDetailsUpdates[{0}].addressUpdated", existingUpdatedDefendantDetailsTotal), is(true)))
                .add(withJsonPath(format("$.defendantDetailsUpdates[{0}].updatedOn", existingUpdatedDefendantDetailsTotal), is(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE))))
                .build();

        pollWithDefaults(defendantDetailUpdatesRequestParams(Integer.MAX_VALUE, USER_ID))
                .until(status().is(OK),
                        payload().isJson(allOf(matchers)));
    }

    @Test
    public void shouldNotReturnAcknowledgedUpdates() {
        final JsonObject defendantDetailsUpdatesBeforeUpdate = getUpdatedDefendantDetails(USER_ID);
        final int totalUpdatesBeforeUpdate = defendantDetailsUpdatesBeforeUpdate.getInt("total");

        UpdateDefendantDetails.DefendantDetailsPayloadBuilder payloadBuilder = UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults();
        UUID defendantId = UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id"));
        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(caseIdOne, defendantId, payloadBuilder);

        verifyUpdatedDetailsTotalChanged(totalUpdatesBeforeUpdate + 1);

        final EventListener updatesAcknowledgedListener = new EventListener()
                .subscribe(DEFENDANT_DETAILS_UPDATES_ACKNOWLEDGED_PUBLIC_EVENT)
                .run(() -> UpdateDefendantDetails.acknowledgeDefendantDetailsUpdates(caseIdOne, defendantId));

        verifyUpdatedDetailsTotalChanged(totalUpdatesBeforeUpdate);

        assertThatUpdatesAcknowledgedPublicEventRaised(updatesAcknowledgedListener, caseIdOne, defendantId);
    }

    private void verifyUpdatedDetailsTotalChanged(final int expectedUpdatedDetailsTotal) {
        pollWithDefaults(defendantDetailUpdatesRequestParams(Integer.MAX_VALUE, USER_ID))
                .until(
                        status().is(OK),
                        payload().isJson(
                                withJsonPath(
                                        "$.total",
                                        equalTo(expectedUpdatedDetailsTotal))));
    }

    private void assertThatUpdatesAcknowledgedPublicEventRaised(
            final EventListener eventListener,
            final UUID caseId,
            final UUID defendantId) {


        final Optional<JsonEnvelope> datesToAvoidPublicEvent = eventListener
                .popEvent(DEFENDANT_DETAILS_UPDATES_ACKNOWLEDGED_PUBLIC_EVENT);

        Assert.assertThat(datesToAvoidPublicEvent.isPresent(), is(true));

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
        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(
                caseIdOne,
                UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")),
                UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults());

        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(
                caseIdTwo,
                UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseIdTwo).getString("defendant.id")),
                UpdateDefendantDetails.DefendantDetailsPayloadBuilder.withDefaults());


        List<Matcher<? super ReadContext>> matchers = ImmutableList.<Matcher<? super ReadContext>>builder()
                .add(withJsonPath("$.total", greaterThanOrEqualTo(2)))
                .add(withJsonPath("$.defendantDetailsUpdates.length()", is(1)))
                .build();

        pollWithDefaults(defendantDetailUpdatesRequestParams(1, USER_ID))
                .until(status().is(OK),
                        payload().isJson(allOf(matchers)));
    }


    @Test
    public void shouldReturnUpdatedDetailsBasedOnUserProsecutingAuthority() {
        final JsonObject existingUpdatedDefendantDetails = getUpdatedDefendantDetails(tvlUserUid);

        UpdateDefendantDetails.updateDefendantDetailsForCaseAndPayload(
                caseIdOne,
                UUID.fromString(CasePoller.pollUntilCaseByIdIsOk(caseIdOne).getString("defendant.id")),
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
        final Response response = HttpClientUtil.makeGetCall(
                "/defendant-details-updates?limit=" + Integer.MAX_VALUE,
                DEFENDANT_DETAIL_UPDATES_CONTENT_TYPE,
                userUid);
        assertThat(response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));

        return Json.createReader(new StringReader(response.readEntity(String.class))).readObject();
    }

    private JsonObject responseToJsonObject(String response) {
        return Json.createReader(new StringReader(response)).readObject();
    }
}
