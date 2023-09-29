package uk.gov.moj.cpp.sjp.event.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.DefendantJudicialResult;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.event.decision.DecisionResubmitted;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.SjpToHearingConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.core.courts.DefendantJudicialResult.defendantJudicialResult;
import static uk.gov.justice.hearing.courts.HearingResulted.hearingResulted;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JPrompt.LSUM_DATE;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JResultCode.LSUM;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JudicialResultHelper.getResultText;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator.DecisionResultAggregator.OUTGOING_PROMPT_DATE_FORMAT;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseDecisionProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDecisionProcessor.class);
    public static final String PAYMENT_TERMS_INFO = "paymentTermsInfo";
    public static final String DECISION_SAVED = "decisionSaved";
    public static final String RESET_PAY_BY_DATE = "resetPayByDate";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private SjpService sjpService;

    @Inject
    private SjpToHearingConverter sjpToHearingConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private static final String PUBLIC_CASE_DECISION_SAVED_EVENT = "public.sjp.case-decision-saved";
    private static final String PUBLIC_EVENTS_CASE_DECISION__RESUBMITTED = "public.sjp.events.case-decision-resubmitted";
    private static final String PUBLIC_EVENTS_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    public static final String UNDO_RESERVE_CASE_TIMER_COMMAND = "sjp.command.undo-reserve-case";

    @Handles(DecisionSaved.EVENT_NAME)
    public void handleCaseDecisionSaved(final JsonEnvelope caseDecisionSavedEnvelope) {
        final JsonObject savedDecision = caseDecisionSavedEnvelope.payloadAsJsonObject();
        final String caseId = savedDecision.getString(EventProcessorConstants.CASE_ID);
        setAside(caseDecisionSavedEnvelope, savedDecision, caseId);

        sender.sendAsAdmin(envelop(createObjectBuilder().add("caseId", caseId).build())
                .withName(UNDO_RESERVE_CASE_TIMER_COMMAND)
                .withMetadataFrom(caseDecisionSavedEnvelope));

        // DD-14110 When the decision type is refer to court then do not emit hearing resulted event
        final boolean isDecisionReferredToCourt = savedDecision
                .getJsonArray("offenceDecisions")
                .stream()
                .anyMatch(e -> ((JsonObject) e).getString("type").equals(DecisionType.REFER_FOR_COURT_HEARING.toString()));

        if (isDecisionReferredToCourt) {
            return;
        }
        sender.send(envelop(savedDecision)
                .withName(PUBLIC_CASE_DECISION_SAVED_EVENT)
                .withMetadataFrom(caseDecisionSavedEnvelope));

        final PublicHearingResulted publicHearingResulted = sjpToHearingConverter.convertCaseDecision(caseDecisionSavedEnvelope);


        publishHearingEvent(caseDecisionSavedEnvelope, publicHearingResulted);
    }

    @Handles(DecisionResubmitted.EVENT_NAME)
    public void handleDecisionResubmitted(final JsonEnvelope decisionResubmittedEnvelope) {

        sender.send(envelop(decisionResubmittedEnvelope.payloadAsJsonObject())
                .withName(PUBLIC_EVENTS_CASE_DECISION__RESUBMITTED)
                .withMetadataFrom(decisionResubmittedEnvelope));

        final JsonObject payloadJsonObject = decisionResubmittedEnvelope.payloadAsJsonObject();

        final JsonEnvelope decisionSavedEnvelope =
                envelopeFrom(decisionResubmittedEnvelope.metadata(),
                        payloadJsonObject.getJsonObject(DECISION_SAVED));

        final PublicHearingResulted publicHearingResulted = getPublicHearingResulted(decisionSavedEnvelope);

        if (nonNull(payloadJsonObject.getJsonObject(PAYMENT_TERMS_INFO))
                && payloadJsonObject.getJsonObject(PAYMENT_TERMS_INFO).getBoolean(RESET_PAY_BY_DATE, false)) {
            // remove the judicial results and add then again
            final List<DefendantJudicialResult> defendantJudicialResultsWithLSUM =
                    ofNullable(publicHearingResulted
                            .getHearing()
                            .getDefendantJudicialResults())
                            .orElse(new ArrayList<>())
                            .stream()
                            .filter(defendantJudicialResult -> defendantJudicialResult.getJudicialResult().getJudicialResultTypeId().equals(LSUM.getResultDefinitionId()))
                            .map(e -> transformTheLumpSumResult(publicHearingResulted, e))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

            if (!defendantJudicialResultsWithLSUM.isEmpty()) {
                // remove
                publicHearingResulted
                        .getHearing()
                        .getDefendantJudicialResults()
                        .removeIf(e -> e.getJudicialResult().getJudicialResultTypeId().equals(LSUM.getResultDefinitionId()));
                // add
                publicHearingResulted
                        .getHearing()
                        .getDefendantJudicialResults()
                        .addAll(defendantJudicialResultsWithLSUM);
            }
        }

        publishHearingEvent(decisionSavedEnvelope, publicHearingResulted);
    }

    /**
     * Here we have to transform the lump sum date as the common code is calculating the lump sum within date using the original decision saved (as we want to preserve this based on the business input)
     *
     * @param publicHearingResulted
     * @param defendantJudicialResult
     * @return
     */
    private DefendantJudicialResult transformTheLumpSumResult(final PublicHearingResulted publicHearingResulted,
                                                              final DefendantJudicialResult defendantJudicialResult) {

        final JudicialResult judicialResultOld = defendantJudicialResult.getJudicialResult();
        if (defendantJudicialResult.getJudicialResult().getJudicialResultTypeId().equals(LSUM.getResultDefinitionId())) {
            final List<JudicialResultPrompt> newJudicialResultPrompts =
                    judicialResultOld
                            .getJudicialResultPrompts()
                            .stream()
                            .filter(judicialResultPrompt -> LSUM_DATE.getId().equals(judicialResultPrompt.getJudicialResultPromptTypeId()))
                            .map(judicialResultPrompt -> {
                                final long daysBetween = DAYS.between(publicHearingResulted.getSharedTime().toLocalDate(), convertToLocalDate(judicialResultPrompt.getValue()));
                                return JudicialResultPrompt
                                        .judicialResultPrompt()
                                        .withValuesFrom(judicialResultPrompt)
                                        .withValue(LocalDate.now().plusDays(daysBetween).format(DateTimeFormatter.ofPattern(OUTGOING_PROMPT_DATE_FORMAT)))
                                        .build();
                            }).collect(Collectors.toList());
            // remove
            judicialResultOld
                    .getJudicialResultPrompts()
                    .removeIf(judicialResultPrompt -> LSUM_DATE.getId().equals(judicialResultPrompt.getJudicialResultPromptTypeId()));

            // add
            judicialResultOld
                    .getJudicialResultPrompts()
                    .addAll(newJudicialResultPrompts);

            final JudicialResult.Builder judicialResultBuilderNew =
                    JudicialResult
                            .judicialResult()
                            .withValuesFrom(judicialResultOld);

            judicialResultBuilderNew
                    .withResultText(getResultText(judicialResultOld.getJudicialResultPrompts(), judicialResultOld.getLabel()));

            return defendantJudicialResult().withValuesFrom(defendantJudicialResult).withJudicialResult(judicialResultBuilderNew.build()).build();
        }

        return null;
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


    private PublicHearingResulted getPublicHearingResulted(final JsonEnvelope caseDecisionSavedEnvelope) {
        final JsonObject savedDecision = caseDecisionSavedEnvelope.payloadAsJsonObject();
        final String caseId = savedDecision.getString(EventProcessorConstants.CASE_ID);

        LOGGER.info("Received Case decision saved message for caseId {}", caseId);

        return sjpToHearingConverter.convertCaseDecision(caseDecisionSavedEnvelope);
    }

    private void setAside(JsonEnvelope caseDecisionSavedEnvelope, JsonObject savedDecision, String caseId) {
        final boolean isSetAside = savedDecision
                .getJsonArray("offenceDecisions")
                .stream()
                .allMatch(e -> ((JsonObject) e).getString("type").equals(DecisionType.SET_ASIDE.toString()));

        if (isSetAside) {// if set aside then only clear the pleas
            clearPleas(caseDecisionSavedEnvelope, caseId);
        }
    }

    private void publishHearingEvent(final JsonEnvelope caseDecisionSavedEnvelope, final PublicHearingResulted publicHearingResulted) {
        final Envelope<HearingResulted> publicHearingResultedEnvelope = envelop(hearingResulted()
                .withHearing(publicHearingResulted.getHearing())
                .withHearingDay(publicHearingResulted.getSharedTime().format(DATE_FORMAT))
                .withSharedTime(publicHearingResulted.getSharedTime())
                .withIsReshare(false)
                .build())
                .withName(PUBLIC_EVENTS_HEARING_RESULTED)
                .withMetadataFrom(caseDecisionSavedEnvelope);
        sender.send(publicHearingResultedEnvelope);
    }


    @SuppressWarnings("squid:S00112")
    protected LocalDate convertToLocalDate(final String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern(OUTGOING_PROMPT_DATE_FORMAT));
        } catch (DateTimeParseException parseException) {
            throw new RuntimeException(String.format("invalid format for incoming date prompt value: %s", value), parseException);
        }
    }
}
