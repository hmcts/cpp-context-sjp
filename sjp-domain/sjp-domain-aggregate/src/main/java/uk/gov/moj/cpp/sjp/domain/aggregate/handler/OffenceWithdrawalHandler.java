package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.stream.Collectors.toMap;

import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.moj.cpp.json.schemas.domains.sjp.event.OffencesWithdrawalRequestsStatusSet;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestCancelled;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequestReasonChanged;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class OffenceWithdrawalHandler {

    public static final OffenceWithdrawalHandler INSTANCE = new OffenceWithdrawalHandler();

    private OffenceWithdrawalHandler() {
    }

    public Stream<Object> requestOffenceWithdrawal(final UUID caseId, final UUID setBy, final ZonedDateTime setAt, final List<WithdrawalRequestsStatus> withdrawalRequestsStatus, final CaseAggregateState state, final String prosecutionAuthority) {

        return HandlerUtils.
                createRejectionEvents(null, "Request Offence Withdrawal", null, state, prosecutionAuthority).
                orElse(createWithdrawalEvents(caseId, setBy, setAt, withdrawalRequestsStatus, state));
    }

    private Stream<Object> createWithdrawalEvents(final UUID caseId, final UUID setBy, final ZonedDateTime setAt,
                                                  final List<WithdrawalRequestsStatus> withdrawalRequestsStatus, final CaseAggregateState state) {
        final Stream.Builder<Object> streamBuilder = Stream.builder();

        streamBuilder.add(new OffencesWithdrawalRequestsStatusSet(caseId, setAt, setBy, withdrawalRequestsStatus));

        final Map<UUID, UUID> previousWithdrawnOffences = state.getWithdrawalRequests().stream().collect(toMap(WithdrawalRequestsStatus::getOffenceId, WithdrawalRequestsStatus::getWithdrawalRequestReasonId));
        final Map<UUID, UUID> currentWithdrawnOffences = withdrawalRequestsStatus.stream().collect(toMap(WithdrawalRequestsStatus::getOffenceId, WithdrawalRequestsStatus::getWithdrawalRequestReasonId));

        currentWithdrawnOffences.forEach((offenceId, reason) -> {
            if (!previousWithdrawnOffences.containsKey(offenceId)) {
                streamBuilder.add(new OffenceWithdrawalRequested(caseId, offenceId, reason, setBy, setAt));
            } else if (isReasonChanged(previousWithdrawnOffences, offenceId, reason)) {
                streamBuilder.add(new OffenceWithdrawalRequestReasonChanged(caseId, offenceId, setBy, setAt, reason, previousWithdrawnOffences.get(offenceId)));
            }
        });

        previousWithdrawnOffences.forEach((offenceId, reason) -> {
            if (isWithdrawnRequestCancelled(currentWithdrawnOffences, offenceId)) {
                streamBuilder.add(new OffenceWithdrawalRequestCancelled(caseId, offenceId, setBy, setAt));
            }
        });

        return streamBuilder.build();
    }

    private static boolean isWithdrawnRequestCancelled(final Map<UUID, UUID> currentWithdrawnOffences, final UUID offenceId) {
        return !currentWithdrawnOffences.containsKey(offenceId);
    }

    private static boolean isReasonChanged(final Map<UUID, UUID> previousWithdrawnOffences, final UUID offenceId, final UUID withdrawalRequestReasonId) {
        return !previousWithdrawnOffences.get(offenceId).equals(withdrawalRequestReasonId);
    }
}
