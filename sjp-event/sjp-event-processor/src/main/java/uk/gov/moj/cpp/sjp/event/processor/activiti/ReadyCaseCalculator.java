package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class ReadyCaseCalculator {

    private static final Map<PleaType, CaseReadinessReason> READINESS_REASON_BY_PLEA_TYPE = new EnumMap<>(PleaType.class);

    static {
        READINESS_REASON_BY_PLEA_TYPE.put(GUILTY, PLEADED_GUILTY);
        READINESS_REASON_BY_PLEA_TYPE.put(NOT_GUILTY, PLEADED_NOT_GUILTY);
        READINESS_REASON_BY_PLEA_TYPE.put(GUILTY_REQUEST_HEARING, PLEADED_GUILTY_REQUEST_HEARING);

        assert READINESS_REASON_BY_PLEA_TYPE.size() == PleaType.values().length : "not all PleaTypes have been mapped";
    }

    /**
     * Calculate the reason of readiness. PRIORITY: [withdrawal, plea received, proved in absence] +
     * not waiting for dates-to-avoid
     *
     * @param provedInAbsence     notice expired
     * @param withdrawalRequested user has withdraw the case
     * @param pleaReady           specify if the plea is complete - NOT_GUILTY need to wait for dates-to-avoid to be added
     * @param pleaType            null when plea not sent
     * @return                    decision of readiness or empty when case not ready
     */
    public Optional<CaseReadinessReason> getReasonIfReady(
            final boolean provedInAbsence,
            final boolean withdrawalRequested,
            final boolean pleaReady,
            final PleaType pleaType
    ) {
        final CaseReadinessReason caseReadinessReason;

        final boolean isPleaCompleted = pleaReady && pleaType != null;
        final boolean isPleaWaitingForDatesToAvoid = !isPleaCompleted && PleaType.NOT_GUILTY.equals(pleaType);

        if (withdrawalRequested) {
            caseReadinessReason = WITHDRAWAL_REQUESTED;
        } else if (isPleaCompleted) {
            caseReadinessReason = READINESS_REASON_BY_PLEA_TYPE.get(pleaType);
        } else if (provedInAbsence && !isPleaWaitingForDatesToAvoid) {
            caseReadinessReason = PIA;
        } else {
            caseReadinessReason = null;
        }

        return Optional.ofNullable(caseReadinessReason);
    }

}
