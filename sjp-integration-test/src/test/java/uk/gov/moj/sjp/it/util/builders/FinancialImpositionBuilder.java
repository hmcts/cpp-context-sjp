package uk.gov.moj.sjp.it.util.builders;

import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;

import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.LumpSum;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinancialImpositionBuilder {

    public static FinancialImposition withDefaults() {
        return new FinancialImposition(
                costsAndSurcharge(),
                payment()
        );
    }

    private static CostsAndSurcharge costsAndSurcharge() {
        return new CostsAndSurcharge(
                new BigDecimal(40),
                null,
                new BigDecimal(100),
                null,
                "reason for reduced victim surcharge",
                false
        );
    }

    private static Payment payment() {
        return new Payment(
                new BigDecimal(370),
                PAY_TO_COURT,
                "Reason for not attached",
                null,
                paymentTerms(),
                null
        );
    }

    private static PaymentTerms paymentTerms() {
        return new PaymentTerms(
                false,
                new LumpSum(
                        new BigDecimal(370),
                        5,
                        LocalDate.of(2019, 7, 24)
                ),
                new Installments(
                        new BigDecimal(30),
                        InstallmentPeriod.WEEKLY,
                        LocalDate.of(2019, 7, 23)
                )
        );
    }
}
