package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.hearing.courts.HearingResulted.hearingResulted;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourtsV2;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.SjpToHearingConverter;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class CaseListedProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseListedProcessor.class);

    @Inject
    private Sender sender;

    @Inject
    private SjpToHearingConverter sjpToHearingConverter;

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private static final String PUBLIC_HEARING_RESULTED_EVENT = "public.hearing.resulted";
    private static final String PUBLIC_EVENTS_HEARING_RESULTED = "public.events.hearing.hearing-resulted";

    @Handles(CaseListedInCriminalCourtsV2.EVENT_NAME)
    public void handleCaseListedInCCReferToCourt(final JsonEnvelope caseListedInCcForReferToCourtEnvelope) {

        final JsonObject caseListedInCcForReferToCourtEnvelopeObj = caseListedInCcForReferToCourtEnvelope.payloadAsJsonObject();
        final String caseId = caseListedInCcForReferToCourtEnvelopeObj.getString(EventProcessorConstants.CASE_ID);

        LOGGER.info("Received  CaseListedInCcForReferToCourt for caseId {}", caseId);

        final PublicHearingResulted publicHearingResulted = sjpToHearingConverter.convertCaseDecisionInCcForReferToCourt(caseListedInCcForReferToCourtEnvelope);
        if (featureControlGuard.isFeatureEnabled("amendReshare")) {
            final Envelope<HearingResulted> publicHearingResultedEnvelope = envelop(hearingResulted()
                    .withHearing(publicHearingResulted.getHearing())
                    .withHearingDay(publicHearingResulted.getSharedTime().format(DATE_FORMAT))
                    .withSharedTime(publicHearingResulted.getSharedTime())
                    .withIsReshare(false)
                    .build())
                    .withName(PUBLIC_EVENTS_HEARING_RESULTED)
                    .withMetadataFrom(caseListedInCcForReferToCourtEnvelope);
            sender.send(publicHearingResultedEnvelope);
        } else {
            final Envelope<PublicHearingResulted> publicHearingResultedEnvelope = envelop(publicHearingResulted)
                    .withName(PUBLIC_HEARING_RESULTED_EVENT)
                    .withMetadataFrom(caseListedInCcForReferToCourtEnvelope);
            sender.send(publicHearingResultedEnvelope);
        }
    }

}
