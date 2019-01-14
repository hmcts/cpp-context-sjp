package uk.gov.moj.cpp.sjp.domain.transformation.defendant;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DefendantUpdatedDateTransformerTest {

    private static final String CASE_ID = UUID.randomUUID().toString();
    private static final String DEFENDANT_ID = UUID.randomUUID().toString();

    @InjectMocks
    private DefendantUpdatedDateTransformer transformer;

    @Test
    public void shouldNotProcessEventOtherThanDefendantDetailsUpdated() {
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUID("sjp.events.defendant-personal-name-updated"), JsonValue.NULL);
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(NO_ACTION));
    }

    @Test
    public void shouldProcessDefendantDetailsUpdatedEventIfUpdatedDateNotSet() {
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUID("sjp.events.defendant-details-updated"),
                buildPayloadForDefendantDetailsUpdatedEventWithUpdatedDateSet(null));
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(TRANSFORM));
    }

    @Test
    public void shouldNotProcessDefendantDetailsUpdatedEventIfUpdatedDateIsSet() {
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUID("sjp.events.defendant-details-updated"),
                buildPayloadForDefendantDetailsUpdatedEventWithUpdatedDateSet(true));
        final Action action = whenTransformerActionIsCheckedFor(envelope);
        assertThat(action, is(NO_ACTION));
    }

    @Test
    public void shouldAddDefendantDetailsUpdatedEventWithUpdatedDate(){
        ZonedDateTime updatedDate = ZonedDateTime.now();
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(MetadataBuilderFactory.metadataWithRandomUUID("sjp.events.defendant-details-updated").createdAt(updatedDate),
                buildPayloadForDefendantDetailsUpdatedEventWithUpdatedDateSet(null));
        final Stream<JsonEnvelope> jsonEnvelopeStream = transformer.apply(envelope);
        final List<JsonEnvelope> actual = jsonEnvelopeStream.collect(Collectors.toList());
        assertThat(actual, hasSize(1));
        assertThat(actual.get(0), jsonEnvelope(
                metadata().of(envelope.metadata()),
                payloadIsJson(allOf(
                        withJsonPath("caseId", is(CASE_ID)),
                        withJsonPath("defendantId", is(DEFENDANT_ID)),
                        withJsonPath("title", is("Mr")),
                        withJsonPath("firstName", is("james")),
                        withJsonPath("lastName", is("smith")),
                        withJsonPath("dateOfBirth", is("1983-07-02")),
                        withJsonPath("gender", is("Male")),
                        withJsonPath("nationalInsuranceNumber", is("AA123456C")),
                        withJsonPath("$.contactDetails.email", is("ar@gmail.com")),
                        withJsonPath("$.address.address1", is("Line 1")),
                        withJsonPath("$.address.address2", is("Line 2")),
                        withJsonPath("$.address.address3", is("Line 3")),
                        withJsonPath("$.address.address4", is("Line 4")),
                        withJsonPath("$.address.postcode", is("CR0 1XG")),
                        withJsonPath("updateByOnlinePlea", is(false)),
                        withJsonPath("updatedDate", is(ZonedDateTimes.toString(updatedDate)))
                ))));
    }

    protected Action whenTransformerActionIsCheckedFor(final JsonEnvelope envelope) {
        return transformer.actionFor(envelope);
    }

    private JsonObject buildPayloadForDefendantDetailsUpdatedEventWithUpdatedDateSet(final Boolean updatedDateSet) {
        JsonObjectBuilder contactDetailsBuilder = createObjectBuilder().add("email", "ar@gmail.com");
        JsonObjectBuilder addressBuilder = createObjectBuilder()
                .add("address1", "Line 1")
                .add("address2", "Line 2")
                .add("address3", "Line 3")
                .add("address4", "Line 4")
                .add("postcode", "CR0 1XG");

        JsonObjectBuilder builder = createObjectBuilder()
                .add("caseId", CASE_ID)
                .add("defendantId", DEFENDANT_ID)
                .add("title", "Mr")
                .add("firstName", "james")
                .add("lastName", "smith")
                .add("dateOfBirth", "1983-07-02")
                .add("gender", "Male")
                .add("nationalInsuranceNumber", "AA123456C")
                .add("contactDetails", contactDetailsBuilder)
                .add("address", addressBuilder)
                .add("updateByOnlinePlea", false);

        ofNullable(updatedDateSet).ifPresent(b -> builder.add("updatedDate", ZonedDateTime.now().toString()));
        return builder.build();
    }
}