package uk.gov.moj.cpp.sjp.service;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

public class AddressService {

    private AddressService() {
    }

    /**
     * Converts the postcode to uppercase and separates it with space
     *
     * @return normalized postcode String
     */
    public static String normalizePostcode(final String postcode) {
        final String uppercaseTrimmed = postcode.toUpperCase().replaceAll("\\s", EMPTY);

        if (uppercaseTrimmed.length() <= 3) {
            return uppercaseTrimmed;
        }

        return new StringBuilder(uppercaseTrimmed)
                .insert(uppercaseTrimmed.length() - 3, SPACE)
                .toString();
    }

    public static boolean isPostcodeNormalized(final String postcode) {
        return normalizePostcode(postcode).equals(postcode);
    }

}