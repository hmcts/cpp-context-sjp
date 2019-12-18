package uk.gov.moj.cpp.sjp.query.api.helper;

import static java.util.Optional.*;
import static java.util.Optional.empty;

import java.util.Optional;

public class SjpQueryHelper {

    public static final SjpQueryHelper INSTANCE = new SjpQueryHelper();

    public enum EmploymentStatus {
        EMPLOYED, SELF_EMPLOYED, UNEMPLOYED, OTHER
    }

    private SjpQueryHelper() {
    }

    public String resolveEffectiveStatus(String employmentStatus) {
        if (employmentStatus != null &&
                !(EmploymentStatus.UNEMPLOYED.name().equals(employmentStatus) ||
                        EmploymentStatus.EMPLOYED.name().equals(employmentStatus) ||
                        EmploymentStatus.SELF_EMPLOYED.name().equals(employmentStatus))) {
            return EmploymentStatus.OTHER.name();
        }
        return employmentStatus;
    }

    public Optional<String> resolveDetail(String employmentStatus) {
        if (EmploymentStatus.OTHER.name().equals(resolveEffectiveStatus(employmentStatus))) {
            return ofNullable(employmentStatus);
        }
        return empty();
    }

}
