package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseDecisionProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDecisionProcessor.class);

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private SjpService sjpService;

    private static final String PUBLIC_CASE_DECISION_SAVED_EVENT = "public.sjp.case-decision-saved";

    @Handles(DecisionSaved.EVENT_NAME)
    public void handleCaseDecisionSaved(final JsonEnvelope jsonEnvelope) {
        final JsonObject savedDecision = jsonEnvelope.payloadAsJsonObject();
        final String caseId = savedDecision.getString(EventProcessorConstants.CASE_ID);

        LOGGER.info("Received Case decision saved message for caseId {}", caseId);

        // call the command to make the pleas as empty
        final boolean isSetAside = savedDecision
                .getJsonArray("offenceDecisions")
                .stream()
                .allMatch(e -> ((JsonObject) e).getString("type").equals(DecisionType.SET_ASIDE.toString()));

        if (isSetAside) {// if set aside then only clear the pleas
            clearPleas(jsonEnvelope, caseId);
        }

        sender.send(envelop(savedDecision)
                .withName(PUBLIC_CASE_DECISION_SAVED_EVENT)
                .withMetadataFrom(jsonEnvelope));
    }

    private void clearPleas(final JsonEnvelope jsonEnvelope, final String caseId) {
        final JsonArrayBuilder pleas = createArrayBuilder();
        final CaseDetails caseDetails = sjpService.getCaseDetails(UUID.fromString(caseId), jsonEnvelope);
        final UUID defendantId = caseDetails.getDefendant().getId();
        caseDetails.getDefendant()
                .getOffences()
                .forEach(o -> pleas.add(createObjectBuilder()
                        .add("offenceId", o.getId().toString())
                        .add("defendantId", defendantId.toString())
                        .add("pleaType", JsonValue.NULL)
                        .build()));

        final JsonObject payload = createObjectBuilder()
                .add("pleas", pleas)
                .add("caseId", caseId)
                .build();

        sender.send(enveloper
                .withMetadataFrom(jsonEnvelope, "sjp.command.set-pleas")
                .apply(payload));
    }
}
