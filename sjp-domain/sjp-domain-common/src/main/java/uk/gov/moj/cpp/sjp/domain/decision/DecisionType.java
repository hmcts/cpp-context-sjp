package uk.gov.moj.cpp.sjp.domain.decision;

public enum DecisionType {

    WITHDRAW(DecisionName.WITHDRAW, true, true),
    DISMISS(DecisionName.DISMISS, true, true),
    ADJOURN(DecisionName.ADJOURN, false, false),
    REFER_FOR_COURT_HEARING(DecisionName.REFER_FOR_COURT_HEARING, true, false),
    DISCHARGE(DecisionName.DISCHARGE, true, true),
    FINANCIAL_PENALTY(DecisionName.FINANCIAL_PENALTY, true, true),
    NO_SEPARATE_PENALTY(DecisionName.NO_SEPARATE_PENALTY, true, true),
    SET_ASIDE(DecisionName.SET_ASIDE, false, false),
    REFERRED_TO_OPEN_COURT(DecisionName.REFERRED_TO_OPEN_COURT, true, false),
    REFERRED_FOR_FUTURE_SJP_SESSION(DecisionName.REFERRED_FOR_FUTURE_SJP_SESSION, true, false),
    OATS(DecisionName.OATS, true, true);

    private final Boolean isFinal;
    // true when the decision disposes of the offence (final result or withdrawn); false when adjourned, set aside or referred
    private final boolean isConcluded;

    DecisionType(final String name, final Boolean isFinal, final boolean isConcluded) {
        if (!name.equals(this.name())) {
            throw new IllegalArgumentException();
        }
        this.isFinal = isFinal;
        this.isConcluded = isConcluded;
    }

    public Boolean isFinal() {
        return isFinal;
    }

    public boolean isConcluded() {
        return isConcluded;
    }

    public static class DecisionName {
        public static final String WITHDRAW = "WITHDRAW";
        public static final String DISMISS = "DISMISS";
        public static final String ADJOURN = "ADJOURN";
        public static final String REFER_FOR_COURT_HEARING = "REFER_FOR_COURT_HEARING";
        public static final String DISCHARGE = "DISCHARGE";
        public static final String FINANCIAL_PENALTY = "FINANCIAL_PENALTY";
        public static final String NO_SEPARATE_PENALTY = "NO_SEPARATE_PENALTY";
        public static final String SET_ASIDE = "SET_ASIDE";
        public static final String REFERRED_TO_OPEN_COURT = "REFERRED_TO_OPEN_COURT";
        public static final String REFERRED_FOR_FUTURE_SJP_SESSION = "REFERRED_FOR_FUTURE_SJP_SESSION";
        public static final String OATS = "OATS";

        private DecisionName() {
        }
    }
}
