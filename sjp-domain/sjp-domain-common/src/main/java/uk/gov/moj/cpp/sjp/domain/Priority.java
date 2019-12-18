package uk.gov.moj.cpp.sjp.domain;

public enum Priority {
    HIGH,
    MEDIUM,
    LOW;

    public Integer getIntValue() {
        switch (this) {
            case HIGH:
                return 1;
            case MEDIUM:
                return 2;
            case LOW:
                default:
                    return 3;
        }
    }
}
