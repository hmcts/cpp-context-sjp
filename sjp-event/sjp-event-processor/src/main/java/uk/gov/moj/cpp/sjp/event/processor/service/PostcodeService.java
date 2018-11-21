package uk.gov.moj.cpp.sjp.event.processor.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

public class PostcodeService {
    //TODO remove this class as part of ATCM-3683
    private static final Logger LOGGER = getLogger(PostcodeService.class);

    @SuppressWarnings("squid:S3457")
    public String getOutwardCode(final String postCode) {
        final String postCodeRegex =  "^([Gg][Ii][Rr] 0[Aa]{2})|((([A-Za-z][0-9]{1,2})|(([A-Za-z][A-Ha-hJ-Yj-y][0-9]{1,2})|(([A-Za-z][0-9][A-Za-z])|([A-Za-z][A-Ha-hJ-Yj-y][0-9][A-Za-z]?))))\\s?[0-9][A-Za-z]{2})$";

        final Pattern postcodePattern = Pattern.compile(postCodeRegex);
        final String trimmedPostCode = postCode.replaceAll("\\s+","");
        final Matcher matcher = postcodePattern.matcher(trimmedPostCode);

        if (!matcher.find()) {
            LOGGER.warn("The postcode {0} does not match the regex {1}", postCode, postCodeRegex);
            return postCode;
        }

        // The 3rd regex group contains the OutwardCode
        return matcher.group(3);
    }
}

