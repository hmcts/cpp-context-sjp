package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import java.util.Optional;
import java.util.UUID;

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
        final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(), InterpreterUpdatedForDefendant.class);

        updateInterpreter(interpreterUpdatedForDefendant.getCaseId(), Optional.ofNullable(interpreterUpdatedForDefendant.getInterpreter())
                .map(Interpreter::getLanguage)
                .map(InterpreterDetail::new)
                .orElse(null));

        //this listener updates two tables for the case where the event is fired via plead-online command
        if (interpreterUpdatedForDefendant.isUpdatedByOnlinePlea()) {
            onlinePleaRepository.saveOnlinePlea(new OnlinePlea(interpreterUpdatedForDefendant));
        }
    }

    @Handles("sjp.events.interpreter-for-defendant-cancelled")
    @Transactional
    public void interpreterCancelled(final JsonEnvelope envelope) {
        final InterpreterCancelledForDefendant interpreterCancelledForDefendant = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(), InterpreterCancelledForDefendant.class);
        updateInterpreter(interpreterCancelledForDefendant.getCaseId(), null);
    }

    private void updateInterpreter(final UUID caseId, final InterpreterDetail interpreter) {
        caseRepository.findBy(caseId)
                .getDefendant()
                .setInterpreter(interpreter);
    }

}
