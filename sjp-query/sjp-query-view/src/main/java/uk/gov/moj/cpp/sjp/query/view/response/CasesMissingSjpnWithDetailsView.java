package uk.gov.moj.cpp.sjp.query.view.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


public class CasesMissingSjpnWithDetailsView {

    private final int count;
    @JsonProperty("cases")
    private List<CaseMissingSjpnWithDetailsView> caseMissingSjpnWithDetailsView;

    public CasesMissingSjpnWithDetailsView(List<CaseMissingSjpnWithDetailsView> caseMissingSjpnWithDetailsView, int count) {
        this.caseMissingSjpnWithDetailsView = caseMissingSjpnWithDetailsView;
        this.count = count;
    }

    public int getCount() {
        return count;
    }


    public List<CaseMissingSjpnWithDetailsView> getCaseMissingSjpnWithDetailsView() {
        return caseMissingSjpnWithDetailsView;
    }

    public void setCaseMissingSjpnWithDetailsView(List<CaseMissingSjpnWithDetailsView> caseMissingSjpnWithDetailsView) {
        this.caseMissingSjpnWithDetailsView = caseMissingSjpnWithDetailsView;
    }


}
