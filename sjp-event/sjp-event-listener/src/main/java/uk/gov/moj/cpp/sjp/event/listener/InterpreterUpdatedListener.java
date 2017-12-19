package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.listener.converter.OnlinePleaConverter;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class InterpreterUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private OnlinePleaConverter onlinePleaConverter;

    @Inject
    private OnlinePleaRepository.InterpreterLanguageOnlinePleaRepository onlinePleaRepository;

    @Handles("sjp.events.interpreter-for-defendant-updated")
    @Transactional
    public void interpreterUpdated(final JsonEnvelope envelope) {
        final InterpreterUpdatedForDefendant event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(), InterpreterUpdatedForDefendant.class);
        final CaseDetail caseDetail = caseRepository.findBy(event.getCaseId());
        final DefendantDetail defendant = caseDetail.getDefendant();
        // This should not happen (because of cancel). But just in case.
        if (event.getInterpreter() == null) {
            defendant.setInterpreter(null);
        } else {
            defendant.setInterpreter(new InterpreterDetail(event.getInterpreter().getLanguage()));
        }

        if (event.isUpdatedByOnlinePlea()) {
            final OnlinePlea onlinePlea = onlinePleaConverter.convertToOnlinePleaEntity(event.getCaseId(),
                    event.getInterpreter() != null ? event.getInterpreter().getLanguage(): null, event.getUpdatedDate());
            onlinePleaRepository.saveOnlinePlea(onlinePlea);
        }
    }

    @Handles("sjp.events.interpreter-for-defendant-cancelled")
    @Transactional
    public void interpreterCancelled(final JsonEnvelope envelope) {
        final InterpreterCancelledForDefendant event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(), InterpreterCancelledForDefendant.class);
        final CaseDetail caseDetail = caseRepository.findBy(event.getCaseId());
        final DefendantDetail defendant = caseDetail.getDefendant();
        defendant.setInterpreter(null);
    }

}
