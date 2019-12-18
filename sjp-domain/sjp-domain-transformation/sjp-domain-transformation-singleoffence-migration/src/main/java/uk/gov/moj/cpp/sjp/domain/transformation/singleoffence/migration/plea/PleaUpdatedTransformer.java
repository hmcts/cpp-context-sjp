package uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.plea;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.NO_ACTION;
import static uk.gov.justice.tools.eventsourcing.transformation.api.Action.TRANSFORM;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.tools.eventsourcing.transformation.api.Action;
import uk.gov.justice.tools.eventsourcing.transformation.api.EventTransformation;
import uk.gov.justice.tools.eventsourcing.transformation.api.annotation.Transformation;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.TransformationException;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.service.SjpEventStoreService;
import uk.gov.moj.cpp.sjp.domain.transformation.singleoffence.migration.service.SjpViewStoreService;

import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.google.common.annotations.VisibleForTesting;

@Transformation
public class PleaUpdatedTransformer implements EventTransformation {


    private SjpEventStoreService sjpEventStoreService;
    private SjpViewStoreService sjpViewStoreService;

    // old event
    private static final String PLEA_UPDATED = "sjp.events.plea-updated";

    // new event
    private static final String PLEAS_SET = "sjp.events.pleas-set";
    private static final String PLEADED_GUILTY = "sjp.events.pleaded-guilty";
    private static final String PLEADED_NOT_GUILTY = "sjp.events.pleaded-not-guilty";
    private static final String PLEADED_GUILTY_COURT_HEARING = "sjp.events.pleaded-guilty-court-hearing-requested";

    private static final String NEEDED = "needed";
    private static final String PLEA_TYPE = "pleaType";
    private static final String CASE_ID = "caseId";
    private static final String NAME = "name";
    private static final String DEFENDANT_COURT_OPTIONS = "defendantCourtOptions";
    private static final String INTERPRETER = "interpreter";
    private static final String LANGUAGE = "language";
    private static final String WELSH_HEARING = "welshHearing";
    private static final String PLEAS = "pleas";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String OFFENCE_ID = "offenceId";
    private static final String NOT_GUILTY_BECAUSE = "notGuiltyBecause";
    private static final String MITIGATION = "mitigation";
    private static final String PLEA = "plea";
    private static final String UPDATED_DATE = "updatedDate";
    private static final String PLEA_METHOD = "pleaMethod";
    private static final String GUILTY = "GUILTY";
    private static final String NOT_GUILTY = "NOT_GUILTY";
    private static final String GUILTY_REQUEST_HEARING = "GUILTY_REQUEST_HEARING";
    private static final String ID = "id";
    private static final String METHOD = "method";
    private static final String PLEAD_DATE = "pleadDate";

    public PleaUpdatedTransformer() {
        this(SjpEventStoreService.getInstance(),
                SjpViewStoreService.getInstance());
    }

    @VisibleForTesting
    PleaUpdatedTransformer(final SjpEventStoreService sjpEventStoreService,
                           final SjpViewStoreService sjpViewStoreService) {
        this.sjpEventStoreService = sjpEventStoreService;
        this.sjpViewStoreService = sjpViewStoreService;
    }

    @Override
    public Action actionFor(final JsonEnvelope eventEnvelope) {
        return eventEnvelope.metadata().name().equals(PLEA_UPDATED) ? TRANSFORM : NO_ACTION;
    }

    @Override
    public void setEnveloper(final Enveloper enveloper) {
        // does nothing??
    }

    @Override
    public Stream<JsonEnvelope> apply(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String caseId = payload.getString(CASE_ID);

        final String defendantId = sjpViewStoreService.getDefendantId(caseId)
                .orElseThrow(() -> new TransformationException("DefendantId is null for caseId " + caseId));

        final JsonEnvelope pleasSetEvent = buildPleasSetEvent(envelope, defendantId);
        final JsonEnvelope pleaTypeEvent = buildPleaTypeRelatedEvent(envelope, defendantId);

        return Stream.of(pleasSetEvent, pleaTypeEvent);
    }

    private JsonEnvelope buildPleasSetEvent(final JsonEnvelope envelope,
                                            final String defendantId) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final String caseId = payload.getString(CASE_ID);
        final String metaDataId = envelope.metadata().asJsonObject().getString(ID);

        final JsonObject interpreter = sjpEventStoreService.getInterpreter(caseId, metaDataId);
        final Boolean welshHearing = sjpEventStoreService.getWelshHearing(caseId, metaDataId);

        final JsonObjectBuilder metadataJsonObjectBuilder =
                createObjectBuilderWithFilter(
                        envelope.metadata().asJsonObject(),
                        field -> !NAME.equalsIgnoreCase(field));
        metadataJsonObjectBuilder.add(NAME, PLEAS_SET);

        final JsonObjectBuilder pleasSetPayloadBuilder = createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(PLEAS, createArrayBuilder().add(
                        buildPleaPayload(defendantId, payload)
                ));

        final JsonObject  defendantCourtDetails = handleDefendantCourtOptions(interpreter, welshHearing);

        if (defendantCourtDetails != null) {
            pleasSetPayloadBuilder.add(DEFENDANT_COURT_OPTIONS, defendantCourtDetails);
        }

        return envelopeFrom(metadataFrom(metadataJsonObjectBuilder.build()), pleasSetPayloadBuilder.build());
    }

    private JsonObjectBuilder buildPleaPayload(final String defendantId, final JsonObject payload) {

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(DEFENDANT_ID, defendantId)
                .add(OFFENCE_ID, payload.getString(OFFENCE_ID))
                .add(PLEA_TYPE, payload.getString(PLEA));

        if (payload.containsKey(NOT_GUILTY_BECAUSE)) {
            jsonObjectBuilder.add(NOT_GUILTY_BECAUSE, payload.getString(NOT_GUILTY_BECAUSE));
        }

        if (payload.containsKey(MITIGATION)) {
            jsonObjectBuilder.add(MITIGATION, payload.getString(MITIGATION));
        }

        return jsonObjectBuilder;
    }

    private JsonEnvelope buildPleaTypeRelatedEvent(final JsonEnvelope envelope,
                                                   final String defendantId) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String caseId = payload.getString(CASE_ID);

        final JsonObjectBuilder metadataJsonObjectBuilder =
                createObjectBuilderWithFilter(
                        envelope.metadata().asJsonObject(),
                        field -> !NAME.equalsIgnoreCase(field));

        final JsonObjectBuilder pleaTypePayloadObjectBuilder = createObjectBuilder();

        if (GUILTY.equals(payload.getString(PLEA))) {
            metadataJsonObjectBuilder.add(NAME, PLEADED_GUILTY);
            if (payload.containsKey(MITIGATION)) {
                pleaTypePayloadObjectBuilder.add(MITIGATION, payload.getString(MITIGATION));
            }
        } else if (NOT_GUILTY.equals(payload.getString(PLEA))) {
            metadataJsonObjectBuilder.add(NAME, PLEADED_NOT_GUILTY);
            if (payload.containsKey(NOT_GUILTY_BECAUSE)) {
                pleaTypePayloadObjectBuilder.add(NOT_GUILTY_BECAUSE, payload.getString(NOT_GUILTY_BECAUSE));
            }
        } else if (GUILTY_REQUEST_HEARING.equals(payload.getString(PLEA))) {
            metadataJsonObjectBuilder.add(NAME, PLEADED_GUILTY_COURT_HEARING);
            if (payload.containsKey(MITIGATION)) {
                pleaTypePayloadObjectBuilder.add(MITIGATION, payload.getString(MITIGATION));
            }
        }

        pleaTypePayloadObjectBuilder
                .add(CASE_ID, caseId)
                .add(DEFENDANT_ID, defendantId)
                .add(OFFENCE_ID, payload.getString(OFFENCE_ID))
                .add(METHOD, payload.getString(PLEA_METHOD))
                .add(PLEAD_DATE, payload.getString(UPDATED_DATE));

        return envelopeFrom(metadataFrom(metadataJsonObjectBuilder.build()), pleaTypePayloadObjectBuilder.build());
    }

    private JsonObject handleDefendantCourtOptions(final JsonObject interpreter,
                                                   final Boolean welshHearing) {
        final JsonObjectBuilder defendantCourtDetailsBuilder = createObjectBuilder();
        if (interpreter != null) {
            defendantCourtDetailsBuilder
                    .add(INTERPRETER, createObjectBuilder()
                            .add(LANGUAGE, interpreter.getString(LANGUAGE))
                            .add(NEEDED, interpreter.getBoolean(LANGUAGE, true)));
        }

        if (welshHearing != null) {
            defendantCourtDetailsBuilder.add(WELSH_HEARING, welshHearing);
        }

        final JsonObject defendantCourtDetails = defendantCourtDetailsBuilder.build();

        if (!defendantCourtDetails.keySet().isEmpty()) {
            return defendantCourtDetails;
        }

        return null;
    }


}
