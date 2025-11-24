package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.persistence.entity.Payment;

public class FinancialImpositionView {

    private CostsAndSurcharge costsAndSurcharge;
    private Payment payment;

    public  FinancialImpositionView (CostsAndSurcharge costsAndSurcharge,Payment payment){
        this.costsAndSurcharge = costsAndSurcharge;
        this.payment = payment;
    }

    public CostsAndSurcharge getCostsAndSurcharge() {
        return costsAndSurcharge;
    }

    public void setCostsAndSurcharge(CostsAndSurcharge costsAndSurcharge) {
        this.costsAndSurcharge = costsAndSurcharge;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

}
