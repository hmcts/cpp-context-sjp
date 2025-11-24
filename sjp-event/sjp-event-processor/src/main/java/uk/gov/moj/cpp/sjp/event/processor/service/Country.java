package uk.gov.moj.cpp.sjp.event.processor.service;

public enum Country {
    WALES("Wales");

    private String name;

    Country(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
