package uk.gov.moj.cpp.sjp.query.view.response;

import java.util.List;

public class RegionsView {

    private final List<RegionView> regions;

    public RegionsView(final List<RegionView> regions) {
        this.regions = regions;
    }

    public List<RegionView> getRegions() {
        return this.regions;
    }
}
