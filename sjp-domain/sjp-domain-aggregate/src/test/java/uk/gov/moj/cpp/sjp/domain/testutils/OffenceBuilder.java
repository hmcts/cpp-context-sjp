package uk.gov.moj.cpp.sjp.domain.testutils;

import uk.gov.moj.cpp.sjp.domain.Offence;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class OffenceBuilder {
    private static BigDecimal COMPENSATION = BigDecimal.valueOf(11.11);

    public static Offence createPressRestrictableOffence(final UUID id, final Boolean pressRestrictable) {
        return new Offence(id, 0, null, null, 0, null, null, null, null, COMPENSATION, pressRestrictable, null, null, null);
    }

    public static List<Offence> createDefaultOffences(final UUID... ids) {
        return Arrays.stream(ids)
                .map(id -> new Offence(id, 0, null, null,
                        0, null, null, null, null, COMPENSATION, false, null, null, null))
                .collect(Collectors.toList());
    }

    public static Offence createOffenceWithAOCPDetails(final UUID id, final Boolean prosecutorOfferAOCP, final Boolean isEligibleAOCP, final BigDecimal aocpStandardPenaltyAmount) {
        return new Offence(id, 0, null, null, 0, null, null, null, null, COMPENSATION, null, prosecutorOfferAOCP, isEligibleAOCP, aocpStandardPenaltyAmount);
    }
}
