package uk.gov.moj.cpp.sjp.domain.transformation.defendantdetailsmovedfrompeople;

import static java.util.UUID.fromString;
import static java.util.stream.Stream.of;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;

import java.util.stream.Stream;

import javax.json.JsonObject;

import org.slf4j.Logger;

@Transformation
public class DefendantDetailsMovedFromPeopleTransformer implements EventTransformation {
    private static final Logger LOGGER = getLogger(DefendantDetailsMovedFromPeopleTransformer.class);
    public static final String DEFENDANT_DETAILS_MOVED_FROM_PEOPLE = "sjp.events.defendant-details-moved-from-people";
    private static final String DEFENDANT_ID_KEY = "defendantId";

    private final CaseIdDefendantIdCache cache;

    //used in production code
    public DefendantDetailsMovedFromPeopleTransformer() {
        this(CaseIdDefendantIdCache.getInstance());
    }

    //used for unit test
    DefendantDetailsMovedFromPeopleTransformer(final CaseIdDefendantIdCache cache) {
        this.cache = cache;
    }

    @Override
    public Action actionFor(final JsonEnvelope eventEnvelope) {
        if (!isDefendantDetailsMovedFromPeopleEvent(eventEnvelope)) {
            return NO_ACTION;
        }
        if (isDefendantDetailsMovedFromPeopleEvent(eventEnvelope)
                && !eventEnvelope.payloadAsJsonObject().containsKey(DEFENDANT_ID_KEY)) {
            return TRANSFORM;
        }
        return NO_ACTION;
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope eventEnvelope) {
        final JsonObject jsonObject = eventEnvelope.payloadAsJsonObject();
        final String caseId = fromString(jsonObject.getString("caseId")).toString();
        final String defendantId = cache.getDefendantId(caseId);
        if (null == defendantId) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Error condition no defendantId for the case {}", caseId);
            }
            return of(eventEnvelope);
        }

        LOGGER.debug("Transforming event with caseId {}", caseId);
        final JsonObject eventPayload = eventEnvelope.payloadAsJsonObject();
        final JsonObject defendantPayload = JsonObjects.createObjectBuilder(eventPayload)
                .add(DEFENDANT_ID_KEY, defendantId)
                .build();
        final JsonEnvelope transformedEnvelope = JsonEnvelope.envelopeFrom(
                eventEnvelope.metadata(), defendantPayload);

        return of(transformedEnvelope);
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        //Not used
    }

    private boolean isDefendantDetailsMovedFromPeopleEvent(JsonEnvelope eventEnvelope) {
        return DEFENDANT_DETAILS_MOVED_FROM_PEOPLE.equalsIgnoreCase(eventEnvelope.metadata().name());
    }
}
