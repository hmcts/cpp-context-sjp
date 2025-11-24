package uk.gov.moj.cpp.sjp.query.view.response.onlineplea;

import java.util.List;

public class PleasView {

    private List<Object> pleas;

    public PleasView(List<Object> pleas) {
        this.pleas = pleas;
    }

    public List<Object> getPleas() {
        return pleas;
    }

    public void setPleas(final List<Object> pleas) {
        this.pleas = pleas;
    }
}
