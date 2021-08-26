package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import java.util.Optional;

import static java.util.Comparator.comparing;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 * Representation of the 'caseDetails' fragment in the court extract json payload.
 */
public class CaseDetailsView {

    private String reference;

    private String prosecutor;

    private Address prosecutorAddress;

    private String courtHouseName;

    private String courtHouseCode;

    public CaseDetailsView(final CaseDetail caseDetail) {
        this.reference = caseDetail.getUrn();
        getLastDecision(caseDetail).ifPresent(caseDecision -> {
            this.courtHouseName = caseDecision.getSession().getCourtHouseName();
            this.courtHouseCode = caseDecision.getSession().getLocalJusticeAreaNationalCourtCode();
        });
    }

    private Optional<CaseDecision> getLastDecision(final CaseDetail caseDetail) {
        if (ofNullable(caseDetail.getCaseDecisions()).isPresent()) {
            return caseDetail.getCaseDecisions().stream().
                    max(comparing(CaseDecision::getSavedAt));
        } else {
            return empty();
        }
    }

    public String getReference() {
        return reference;
    }

    public String getProsecutor() {
        return prosecutor;
    }

    public Address getProsecutorAddress() {
        return prosecutorAddress;
    }

    public void setProsecutorAddress(final Address prosecutorAddress) {
        this.prosecutorAddress = prosecutorAddress;
    }

    public void setProsecutor(final String prosecutor) {
        this.prosecutor = prosecutor;
    }

    public String getCourtHouseName() {
        return courtHouseName;
    }

    public String getCourtHouseCode() {
        return courtHouseCode;
    }
}
