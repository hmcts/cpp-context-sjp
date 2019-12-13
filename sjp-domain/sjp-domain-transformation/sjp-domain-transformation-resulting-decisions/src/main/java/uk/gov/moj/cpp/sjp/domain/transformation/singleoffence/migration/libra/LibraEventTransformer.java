package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.libra;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.CASE_ID;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.ID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;

import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transformation
public class LibraEventTransformer implements EventTransformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibraEventTransformer.class);
    public static final String SJP_EVENTS_CASE_REOPENED_IN_LIBRA = "sjp.events.case-reopened-in-libra";
    public static final String SJP_EVENTS_CASE_REOPENED_IN_LIBRA_UNDONE = "sjp.events.case-reopened-in-libra-undone";
    private static final String NAME = "name";

    @Override
    public void setEnveloper(Enveloper enveloper) {
        // Nothing to do here
    }

    @Override
    public Action actionFor(JsonEnvelope event) {

        final boolean isLibraEvent =
                SJP_EVENTS_CASE_REOPENED_IN_LIBRA.equals(event.metadata().name())
                        || SJP_EVENTS_CASE_REOPENED_IN_LIBRA_UNDONE.equals(event.metadata().name());

        if (!isLibraEvent) {
            return NO_ACTION;
        }

        return TRANSFORM;
    }

    @Override
    @SuppressWarnings("squid:S2139")
    public Stream<JsonEnvelope> apply(JsonEnvelope existingEvent) {
        final String caseId = existingEvent.asJsonObject().getString(CASE_ID);
        try {
            return doApply(existingEvent);
        } catch(TransformationException e) {
            LOGGER.error("Apply failed transformation of caseId {}", caseId, e);
            throw e;
        } catch(RuntimeException e) {
            LOGGER.error("Apply failed with generic error of caseId {}", caseId, e);
            throw e;
        }
    }

    private Stream<JsonEnvelope> doApply(JsonEnvelope currentEnvelope) {
        final String caseId = currentEnvelope.payloadAsJsonObject().getString(CASE_ID);
        LOGGER.info("Creating CASE STATUS event for caseId: {}", caseId);

        final JsonObjectBuilder metadataJsonObjectBuilder =
                createObjectBuilderWithFilter(currentEnvelope.metadata().asJsonObject(),
                        field -> !NAME.equalsIgnoreCase(field));
        metadataJsonObjectBuilder.add(ID, UUID.randomUUID().toString());
        metadataJsonObjectBuilder.add(NAME, CaseStatusChanged.EVENT_NAME);

        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        payloadBuilder.add("caseId", caseId);

        if (SJP_EVENTS_CASE_REOPENED_IN_LIBRA.equals(currentEnvelope.metadata().name())) {
            payloadBuilder.add("caseStatus", "REOPENED_IN_LIBRA");
        } else if (SJP_EVENTS_CASE_REOPENED_IN_LIBRA_UNDONE.equals(currentEnvelope.metadata().name())) {
            payloadBuilder.add("caseStatus", "COMPLETED");
        }

        return Stream.of(currentEnvelope,
                envelopeFrom(metadataFrom(metadataJsonObjectBuilder.build()), payloadBuilder));
    }

}
