package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class InterpreterUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private OnlinePleaRepository.InterpreterLanguageOnlinePleaRepository onlinePleaRepository;

    @Handles("sjp.events.interpreter-for-defendant-updated")
    @Transactional
    public void interpreterUpdated(final JsonEnvelope envelope) {
        final InterpreterUpdatedForDefendant event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(), InterpreterUpdatedForDefendant.class);

        final InterpreterDetail interpreter = Optional.ofNullable(event.getInterpreter())
                .map(Interpreter::getLanguage)
                .map(InterpreterDetail::new)
                .orElse(null);

        caseRepository.findBy(event.getCaseId())
                .getDefendant()
                .setInterpreter(interpreter);

        //this listener updates two tables for the case where the event is fired via plead-online command
        if (event.isUpdatedByOnlinePlea()) {
            onlinePleaRepository.saveOnlinePlea(new OnlinePlea(event));
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
