package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.justice.hearing.courts.HearingResulted;
import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.SjpToHearingConverter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ApplicationDecisionProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationDecisionProcessor.class);
    private static final String PUBLIC_HEARING_RESULTED_EVENT = "public.hearing.resulted";
    private static final String PUBLIC_EVENTS_HEARING_RESULTED = "public.events.hearing.hearing-resulted";

    @Inject
    private Sender sender;

    @Inject
    private SjpToHearingConverter sjpToHearingConverter;

    @Inject
    private FeatureControlGuard featureControlGuard;

    @Handles("sjp.events.application-decision-saved")
    public void handleApplicationDecisionSaved(final Envelope<ApplicationDecisionSaved> applicationDecisionSavedEnvelope) {
        LOGGER.info("Processing decision for application '{}'", applicationDecisionSavedEnvelope.payload().getApplicationId());
        final PublicHearingResulted publicHearingResulted = sjpToHearingConverter.convertApplicationDecision(applicationDecisionSavedEnvelope);

        if (featureControlGuard.isFeatureEnabled("amendReshare")) {
            final Envelope<HearingResulted> publicHearingResultedEnvelope = envelop(HearingResulted
                    .hearingResulted()
                    .withHearing(publicHearingResulted.getHearing())
                    .withHearingDay(publicHearingResulted.getSharedTime().format(DATE_FORMAT))
                    .withSharedTime(publicHearingResulted.getSharedTime())
                    .withIsReshare(false)
                    .build())
                    .withName(PUBLIC_EVENTS_HEARING_RESULTED)
                    .withMetadataFrom(applicationDecisionSavedEnvelope);
            sender.send(publicHearingResultedEnvelope);
        } else {
            final Envelope<PublicHearingResulted> publicHearingResultedEnvelope = envelop(publicHearingResulted)
                    .withName(PUBLIC_HEARING_RESULTED_EVENT)
                    .withMetadataFrom(applicationDecisionSavedEnvelope);

            sender.send(publicHearingResultedEnvelope);
        }
    }
}
