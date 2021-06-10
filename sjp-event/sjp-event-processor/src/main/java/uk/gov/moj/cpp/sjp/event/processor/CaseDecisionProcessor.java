package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.hearing.courts.HearingResulted.hearingResulted;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.SjpToHearingConverter;
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

    @Inject
    private SjpToHearingConverter sjpToHearingConverter;

    @Inject
    private FeatureControlGuard featureControlGuard;

    private static final String PUBLIC_CASE_DECISION_SAVED_EVENT = "public.sjp.case-decision-saved";
    private static final String PUBLIC_HEARING_RESULTED_EVENT = "public.hearing.resulted";
    private static final String PUBLIC_EVENTS_HEARING_RESULTED = "public.events.hearing.hearing-resulted";

    @Handles(DecisionSaved.EVENT_NAME)
    public void handleCaseDecisionSaved(final JsonEnvelope caseDecisionSavedEnvelope) {
        final JsonObject savedDecision = caseDecisionSavedEnvelope.payloadAsJsonObject();
        final String caseId = savedDecision.getString(EventProcessorConstants.CASE_ID);

        LOGGER.info("Received Case decision saved message for caseId {}", caseId);

        // call the command to make the pleas as empty
        final boolean isSetAside = savedDecision
                .getJsonArray("offenceDecisions")
                .stream()
                .allMatch(e -> ((JsonObject) e).getString("type").equals(DecisionType.SET_ASIDE.toString()));

        if (isSetAside) {// if set aside then only clear the pleas
            clearPleas(caseDecisionSavedEnvelope, caseId);
        }

        sender.send(envelop(savedDecision)
                .withName(PUBLIC_CASE_DECISION_SAVED_EVENT)
                .withMetadataFrom(caseDecisionSavedEnvelope));

        final PublicHearingResulted publicHearingResulted = sjpToHearingConverter.convertCaseDecision(caseDecisionSavedEnvelope);
        if (featureControlGuard.isFeatureEnabled("amendReshare")) {
            final Envelope<HearingResulted> publicHearingResultedEnvelope = envelop(hearingResulted()
                    .withHearing(publicHearingResulted.getHearing())
                    .withHearingDay(publicHearingResulted.getSharedTime().format(DATE_FORMAT))
                    .withSharedTime(publicHearingResulted.getSharedTime())
                    .withIsReshare(false)
                    .build())
                    .withName(PUBLIC_EVENTS_HEARING_RESULTED)
                    .withMetadataFrom(caseDecisionSavedEnvelope);
            sender.send(publicHearingResultedEnvelope);
        } else {
            final Envelope<PublicHearingResulted> publicHearingResultedEnvelope = envelop(publicHearingResulted)
                    .withName(PUBLIC_HEARING_RESULTED_EVENT)
                    .withMetadataFrom(caseDecisionSavedEnvelope);
            sender.send(publicHearingResultedEnvelope);
        }
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
