package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import java.util.List;

import static java.util.Collections.unmodifiableList;

public class DecisionCourtExtractView {

    private List<DecisionDetailsView> offencesApplicationsDecisions;
    private  PaymentView payment =null;
    private  boolean hasFinancialImposition ;


    public PaymentView getPayment() {
        return payment;
    }

    public void setPayment(final PaymentView payment) {
        this.payment = payment;
    }

    public void setHasFinancialImposition(final boolean hasFinancialImposition) {
        this.hasFinancialImposition = hasFinancialImposition;
    }

    public List<DecisionDetailsView> getOffencesApplicationsDecisions() {
        return unmodifiableList(offencesApplicationsDecisions);
    }

    public void setOffencesApplicationsDecisions(final List<DecisionDetailsView> offencesApplicationsDecisions) {
        this.offencesApplicationsDecisions = unmodifiableList(offencesApplicationsDecisions);
    }
    public boolean isHasFinancialImposition() {
        return hasFinancialImposition;
    }
}
