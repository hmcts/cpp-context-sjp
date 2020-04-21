package uk.gov.moj.cpp.sjp.event.processor.model.referral;


import java.util.UUID;

import com.google.common.base.Objects;

public class ProsecutionCaseIdentifierView {

    private final UUID prosecutionAuthorityId;
    private final String prosecutionAuthorityCode;
    private final String prosecutionAuthorityReference;
    private final String caseURN;

    public ProsecutionCaseIdentifierView(final UUID prosecutionAuthorityId,
                                         final String prosecutionAuthorityCode,
                                         final String prosecutionAuthorityReference,
                                         final String caseURN) {

        this.prosecutionAuthorityId = prosecutionAuthorityId;
        this.prosecutionAuthorityCode = prosecutionAuthorityCode;
        this.prosecutionAuthorityReference = prosecutionAuthorityReference;
        this.caseURN = caseURN;
    }


    public UUID getProsecutionAuthorityId() {
        return prosecutionAuthorityId;
    }

    public String getProsecutionAuthorityCode() {
        return prosecutionAuthorityCode;
    }

    public String getProsecutionAuthorityReference() {
        return prosecutionAuthorityReference;
    }

    public String getCaseURN() {
        return caseURN;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ProsecutionCaseIdentifierView that = (ProsecutionCaseIdentifierView) o;

        return Objects.equal(prosecutionAuthorityId, that.prosecutionAuthorityId) &&
                Objects.equal(prosecutionAuthorityCode, that.prosecutionAuthorityCode) &&
                Objects.equal(prosecutionAuthorityReference, that.prosecutionAuthorityReference) &&
                Objects.equal(caseURN, that.caseURN);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(prosecutionAuthorityId, prosecutionAuthorityCode, prosecutionAuthorityReference, caseURN);
    }
}
