package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class HearingLanguageUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private OnlinePleaRepository.HearingLanguageOnlinePleaRepository onlinePleaRepository;

    @Handles("sjp.events.hearing-language-preference-for-defendant-updated")
    @Transactional
    public void hearingLanguagePreferenceUpdated(final JsonEnvelope envelope) {
        final HearingLanguagePreferenceUpdatedForDefendant hearingLanguagePreferenceUpdatedForDefendant = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(), HearingLanguagePreferenceUpdatedForDefendant.class);
        updateDefendantSpeakWelsh(hearingLanguagePreferenceUpdatedForDefendant.getCaseId(), hearingLanguagePreferenceUpdatedForDefendant.getSpeakWelsh());

        if (hearingLanguagePreferenceUpdatedForDefendant.isUpdatedByOnlinePlea()) {
            onlinePleaRepository.saveOnlinePlea(new OnlinePlea(hearingLanguagePreferenceUpdatedForDefendant));
        }
    }

    @Handles("sjp.events.hearing-language-preference-for-defendant-cancelled")
    @Transactional
    public void hearingLanguagePreferenceCancelled(final JsonEnvelope envelope) {
        final HearingLanguagePreferenceCancelledForDefendant hearingLanguagePreferenceCancelledForDefendant = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(), HearingLanguagePreferenceCancelledForDefendant.class);
        updateDefendantSpeakWelsh(hearingLanguagePreferenceCancelledForDefendant.getCaseId(), null);
    }

    private void updateDefendantSpeakWelsh(final UUID caseId, final Boolean speakWelsh) {
        caseRepository.findBy(caseId)
                .getDefendant()
                .setSpeakWelsh(speakWelsh);
    }

}
