package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import java.util.Arrays;
import java.util.Optional;

public enum DecisionTypeCode {
    ADJOURNSJP("ADJOURN"), // enable in future
    AD("DISCHARGE"),
    CD("DISCHARGE"),
    D("DISMISS"),
    FO("FINANCIAL_PENALTY"),
    SUMRCC("REFER_FOR_COURT_HEARING"),
    WDRNNOT("WITHDRAW"),

    RSJP("REFERRED_FOR_FUTURE_SJP_SESSION"),
    SUMRTO("REFERRED_TO_OPEN_COURT");

    private String resultDecision;

    DecisionTypeCode(final String resultDecision) {
        this.resultDecision = resultDecision;
    }

    public String getResultDecision() {
        return resultDecision;
    }

    public static Optional<String> getResultDecision(final String code) {
        Optional<DecisionTypeCode> result = Arrays.stream(DecisionTypeCode.values())
                .filter(e -> e.name().equalsIgnoreCase(code))
                .findFirst();

        if (result.isPresent()) {
            return Optional.ofNullable(result.get().getResultDecision());
        }

        return Optional.empty();
    }

}

