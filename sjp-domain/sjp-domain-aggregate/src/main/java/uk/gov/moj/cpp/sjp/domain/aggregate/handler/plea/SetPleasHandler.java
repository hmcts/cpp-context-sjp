package uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea;

import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea.PleaHandlerUtils.createSetPleasEvents;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaMethod.POSTAL;

import uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseLanguageHandler;
import uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.plea.SetPleas;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Stream;

public class SetPleasHandler {

    public static final SetPleasHandler INSTANCE = new SetPleasHandler();

    private CaseLanguageHandler caseLanguageHandler;

    private SetPleasHandler() {
        caseLanguageHandler = CaseLanguageHandler.INSTANCE;
    }


    public Stream<Object> setPleas(final UUID caseId, final SetPleas pleas, final CaseAggregateState state,
                                   final UUID userId, final ZonedDateTime pleadAt) {
        return HandlerUtils.createRejectionEvents(
                userId,
                state,
                pleas.getPleas(),
                "Set pleas"
        ).orElse(createSetPleasEvents(caseId, pleas, state, userId, pleadAt, caseLanguageHandler, POSTAL));
    }
}
