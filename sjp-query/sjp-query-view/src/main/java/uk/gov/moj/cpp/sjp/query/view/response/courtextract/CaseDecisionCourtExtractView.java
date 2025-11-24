package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDecisionView;

import java.util.Objects;

public class CaseDecisionCourtExtractView extends CaseDecisionView {

    private int group = 0;

    public CaseDecisionCourtExtractView(CaseDecision caseDecisionEntity) {
        super(caseDecisionEntity);
    }

    public CaseDecisionCourtExtractView(CaseApplicationDecision applicationDecisionEntity) {
        super(applicationDecisionEntity);
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CaseDecisionCourtExtractView)) {
            return false;
        }
        final CaseDecisionCourtExtractView that = (CaseDecisionCourtExtractView) o;
        return super.getId().equals(that.getId()) &&
                Objects.equals(super.getSession(), that.getSession()) &&
                super.getSavedAt().equals(that.getSavedAt());
    }
    @Override
    public int hashCode() {
        return Objects.hash(super.getId(), super.getSession(), super.getSavedAt());
    }
}
