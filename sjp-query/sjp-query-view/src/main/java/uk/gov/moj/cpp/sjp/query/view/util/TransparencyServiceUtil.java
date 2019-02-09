package uk.gov.moj.cpp.sjp.query.view.util;

public class TransparencyServiceUtil {

    private static final Integer UNIT_SIZE = 1024;

    private TransparencyServiceUtil() {
    }

    public static String resolveSize(final Integer sizeInBytes) {
        String formattedValue;

        // we cant a have file in GB and over and dont need the floating values.
        if (sizeInBytes / (UNIT_SIZE * UNIT_SIZE) >= 1) {
            formattedValue = (sizeInBytes / (UNIT_SIZE * UNIT_SIZE)) + "MB";
        } else if (sizeInBytes /  UNIT_SIZE >= 1) {
            formattedValue = (sizeInBytes / UNIT_SIZE) + "KB";
        } else {
            formattedValue = sizeInBytes.toString() + "B";
        }

        return formattedValue;
    }

}
