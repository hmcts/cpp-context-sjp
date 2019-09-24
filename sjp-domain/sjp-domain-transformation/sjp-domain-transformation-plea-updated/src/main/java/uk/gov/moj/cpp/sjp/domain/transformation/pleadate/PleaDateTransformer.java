package uk.gov.moj.cpp.sjp.domain.transformation.pleadate;

import static com.google.common.io.Resources.getResource;
import static java.util.stream.Stream.of;
import static org.everit.json.schema.loader.SchemaLoader.load;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.everit.json.schema.Schema;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;

@Transformation
public class PleaDateTransformer implements EventTransformation {

    private static final String PLEA_UPDATED_EVENT_NAME = "sjp.events.plea-updated";
    private static final String UPDATED_DATE_KEY = "updatedDate";
    private static final String CASE_ID_KEY = "caseId";

    private static final Logger LOGGER = getLogger(PleaDateTransformer.class);
    private static final String PLEA_UPDATED_SCHEMA_FILE = "sjp.events.plea-updated.json";

    private Enveloper enveloper;

    @Override
    public Action actionFor(JsonEnvelope eventEnvelope) {

        if (PLEA_UPDATED_EVENT_NAME.equalsIgnoreCase(eventEnvelope.metadata().name())
                && !eventEnvelope.payloadAsJsonObject().containsKey(UPDATED_DATE_KEY)) {
            return TRANSFORM;
        }

        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(JsonEnvelope eventEnvelope) {

        LOGGER.info(
                "Transforming event with Case Id : {}",
                eventEnvelope.payloadAsJsonObject().getString(CASE_ID_KEY));

        final JsonValue transformedEvent = buildTransformedEventPayload(eventEnvelope);

        validateAgainstSchema(PLEA_UPDATED_SCHEMA_FILE, transformedEvent.toString());

        final JsonEnvelope transformedEnvelope = envelopeFrom(
                eventEnvelope.metadata(),
                transformedEvent);

        return of(transformedEnvelope);
    }

    @Override
    public void setEnveloper(Enveloper enveloper) {
        this.enveloper = enveloper;
    }

    private JsonValue buildTransformedEventPayload(final JsonEnvelope event) {
        final JsonObject eventPayload = event.payloadAsJsonObject();

        return JsonObjects.createObjectBuilder(eventPayload)
                .add(UPDATED_DATE_KEY, ZonedDateTimes.toString(
                        event.metadata().createdAt()
                                .orElseThrow(() -> new TransformationException("createdAt is null"))))
                .build();
    }

    private void validateAgainstSchema(final String schemaFileName, final String jsonString) {
        final URL resource = getResource(schemaFileName);

        try (final InputStream inputStream = resource.openStream()) {
            final JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));

            final Schema schema = load(rawSchema);

            schema.validate(new JSONObject(jsonString));
        } catch (IOException e) {
            throw new TransformationException("Error validating payload against schema", e);
        }
    }
}
