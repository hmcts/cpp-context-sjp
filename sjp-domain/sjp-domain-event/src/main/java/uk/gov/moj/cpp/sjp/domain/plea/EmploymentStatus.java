package uk.gov.moj.cpp.sjp.domain.plea;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EmploymentStatus {
    EMPLOYED("Employed"),
    EMPLOYED_RECEIVING_BENEFITS("Employed and also receiving benefits"),
    SELF_EMPLOYED("Self employed"),
    SELF_EMPLOYED_RECEIVING_BENEFITS("Self employed and also receiving benefit"),
    RECEIVING_OUT_OF_WORK_BENEFITS("Receiving out of work benefits"),
    OTHER("Other");

    private String name;

    EmploymentStatus(String name) {
        this.name = name;
    }

    public static EmploymentStatus fromString(String s) {
        for (EmploymentStatus status : values()) {
            if (status.name.equals(s)) {
                return status;
            }
        }
        return null;
    }
    @JsonValue
    public String getName() {
        return name;
    }
}