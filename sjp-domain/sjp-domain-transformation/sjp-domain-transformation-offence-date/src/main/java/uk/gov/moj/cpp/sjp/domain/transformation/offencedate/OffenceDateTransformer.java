package uk.gov.moj.cpp.sjp.domain.transformation.offencedate;

import static java.util.stream.Stream.of;
import static javax.json.Json.createArrayBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;

@Transformation
public class OffenceDateTransformer implements EventTransformation {

    private static final String CASE_RECEIVED_EVENT_NAME = "sjp.events.case-received";
    private static final String DEFENDANT = "defendant";
    private static final String OFFENCES = "offences";
    private static final String OFFENCE_DATE = "offenceDate";
    private static final String OFFENCE_COMMITTED_DATE = "offenceCommittedDate";
    private static final String CASE_ID_KEY = "caseId";

    private static final Logger LOGGER = getLogger(OffenceDateTransformer.class);

    @Override
    public Action actionFor(final JsonEnvelope eventEnvelope) {
        return needsTransformation(eventEnvelope) ? TRANSFORM : NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope envelope) {

        LOGGER.info("Transforming event with Case Id : {}", envelope.payloadAsJsonObject().getString(CASE_ID_KEY));

        final JsonEnvelope transformedEnvelope = envelopeFrom(envelope.metadata(),
                transformOffenceDateToOffenceCommittedDate(envelope.payloadAsJsonObject()));

        return of(transformedEnvelope);
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        // enveloper is not used
    }

    private boolean needsTransformation(final JsonEnvelope eventEnvelope) {
        return CASE_RECEIVED_EVENT_NAME.equalsIgnoreCase(eventEnvelope.metadata().name())
                && containsOffenceDate(eventEnvelope.payloadAsJsonObject());
    }

    private boolean containsOffenceDate(final JsonObject caseReceivedObj) {
        return JsonObjects.getJsonArray(caseReceivedObj, DEFENDANT, OFFENCES)
                .orElseThrow(() -> new TransformationException("The even does not contain offences"))
                .getValuesAs(JsonObject.class)
                .stream().anyMatch(offence -> offence.containsKey(OFFENCE_DATE));
    }

    private JsonObject transformOffenceDateToOffenceCommittedDate(final JsonObject caseReceivedPayload) {
        final JsonObjectBuilder result = createObjectBuilderWithFilter(caseReceivedPayload, field -> !DEFENDANT.equalsIgnoreCase(field));

        result.add(DEFENDANT, transformDefendant(caseReceivedPayload.getJsonObject(DEFENDANT)));
        return result.build();
    }

    private JsonObjectBuilder transformDefendant(final JsonObject defendantToTransform) {
        final JsonObjectBuilder result = createObjectBuilderWithFilter(defendantToTransform,
                field -> !OFFENCES.equalsIgnoreCase(field));

        result.add(OFFENCES, transformOffences(defendantToTransform.getJsonArray(OFFENCES)));
        return result;
    }

    private JsonArrayBuilder transformOffences(final JsonArray offencesToTransform) {
        final JsonArrayBuilder result = createArrayBuilder();

        offencesToTransform.getValuesAs(JsonObject.class)
                .stream()
                .map(this::transformOffence)
                .forEach(result::add);

        return result;
    }

    private JsonObjectBuilder transformOffence(final JsonObject offence) {
        final JsonObjectBuilder result = createObjectBuilderWithFilter(offence, field -> !OFFENCE_DATE.equalsIgnoreCase(field));

        result.add(OFFENCE_COMMITTED_DATE, offence.getString(OFFENCE_DATE));
        return result;
    }

}
