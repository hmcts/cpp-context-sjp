package uk.gov.moj.cpp.sjp.query.view.response;

import java.util.UUID;

public class RegionView {

    private final UUID id;
    private final String name;

    public RegionView(final UUID id, final String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
