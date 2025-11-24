package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.SessionCourt;
import uk.gov.moj.cpp.sjp.domain.decision.ConvictingInformation;
import uk.gov.moj.cpp.sjp.event.decision.ConvictionCourtResolved;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResolveConvictionCourtHandler {

    public static final ResolveConvictionCourtHandler INSTANCE = new ResolveConvictionCourtHandler();

    private ResolveConvictionCourtHandler() {
    }


    public Stream resolveConvictionCourt(final UUID caseId, final CaseAggregateState state, final Map<UUID, Session> sessions) {

        final Stream.Builder<Object> streamBuilder = Stream.builder();
        final List<ConvictingInformation> convictingInformationList = state.getOffencesWithConviction()
                .stream()
                .map(offenceId -> new ConvictingInformation(state.getOffenceConvictionInfo(offenceId).getConvictionDate(),
                        new SessionCourt(sessions.get(offenceId).getCourtHouseCode(), sessions.get(offenceId).getLocalJusticeAreaNationalCourtCode()),
                        state.getOffenceConvictionInfo(offenceId).getSessionId(), offenceId))
                .collect(Collectors.toList());

        streamBuilder.add(new ConvictionCourtResolved(caseId, convictingInformationList));
        return streamBuilder.build();
    }

}
