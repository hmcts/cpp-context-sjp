package uk.gov.moj.cpp.sjp.query.view.response;

import java.util.List;

public class DefendantsView {

    private List<DefendantView> defendants;

    public DefendantsView(List<DefendantView> defendants) {
        this.defendants = defendants;
    }

    public List<DefendantView> getDefendants() {
        return defendants;
    }

    public void setDefendants(List<DefendantView> defendants) {
        this.defendants = defendants;
    }


}
