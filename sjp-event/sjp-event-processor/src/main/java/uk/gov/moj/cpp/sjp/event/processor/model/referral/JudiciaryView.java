package uk.gov.moj.cpp.sjp.event.processor.model.referral;


import com.google.common.base.Objects;

public class JudiciaryView {

    private final String judicialName;
    private final String judicialRoleTypeSJP;

    public JudiciaryView(final String judicialName,
                         final String judicialRoleTypeSJP) {

        this.judicialName = judicialName;
        this.judicialRoleTypeSJP = judicialRoleTypeSJP;
    }

    public String getJudicialName() {
        return judicialName;
    }

    public String getJudicialRoleTypeSJP() {
        return judicialRoleTypeSJP;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JudiciaryView that = (JudiciaryView) o;

        return Objects.equal(judicialName, that.judicialName) &&
                Objects.equal(judicialRoleTypeSJP, that.judicialRoleTypeSJP);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(judicialName, judicialRoleTypeSJP);
    }
}
