package uk.gov.moj.cpp.sjp.domain.aggregate.casestatus;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import java.time.LocalDate;
import java.util.UUID;

public class OffenceInformation {

    private final UUID offenceId;
    private final PleaType pleaType;
    private final LocalDate pleaDate;
    private final Boolean pendingWithdrawal;
    private final DecisionType decision;

    private OffenceInformation(final UUID offenceId,
                               final PleaType pleaType,
                               final LocalDate pleaDate,
                               final Boolean pendingWithdrawal,
                               final DecisionType decisionType) {
        this.offenceId = offenceId;
        this.pleaType = pleaType;
        this.pleaDate = pleaDate;
        this.pendingWithdrawal = pendingWithdrawal;
        this.decision = decisionType;
    }

    // todo remove this
    public static OffenceInformation createOffenceInformation(final PleaType pleaType, final LocalDate pleaDate, final Boolean pendingWithdrawal) {
        return new OffenceInformation(null, pleaType, pleaDate, pendingWithdrawal, null);
    }

    public static OffenceInformation createOffenceInformation(final PleaType pleaType, final LocalDate pleaDate, final Boolean pendingWithdrawal, final DecisionType decisionType) {
        return new OffenceInformation(null, pleaType, pleaDate, pendingWithdrawal, decisionType);
    }

    public static OffenceInformation createOffenceInformation(final UUID offenceId,
                                                              final PleaType pleaType,
                                                              final LocalDate pleaDate,
                                                              final Boolean pendingWithdrawal) {
        return new OffenceInformation(offenceId, pleaType, pleaDate, pendingWithdrawal, null);
    }

    public static OffenceInformation createOffenceInformation(final UUID offenceId,
                                                              final PleaType pleaType,
                                                              final LocalDate pleaDate,
                                                              final Boolean pendingWithdrawal,
                                                              final DecisionType decision) {
        return new OffenceInformation(offenceId, pleaType, pleaDate, pendingWithdrawal, decision);
    }

    public PleaType getPleaType() {
        return pleaType;
    }

    public LocalDate getPleaDate() {
        return pleaDate;
    }

    public Boolean getPendingWithdrawal() {
        return pendingWithdrawal;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public boolean hasFinalDecision() {
        return decision != null && decision.isFinal();
    }

    public DecisionType getDecision() {
        return decision;
    }

    @Override
    public String toString() {
        return "OffenceInformation{" +
                "pleaType=" + pleaType +
                ", pleaDate=" + pleaDate +
                ", pendingWithdrawal=" + pendingWithdrawal +
                ", decision=" + decision +
                '}';
    }

}
