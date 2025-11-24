package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.time.format.DateTimeFormatter;

public class JCaseResultsConstants {

    public static final String ID = "id";
    public static final String CASE_ID = "caseId";
    public static final String OFFENCE_ID = "offenceId";
    public static final String SESSION_ID = "sessionId";
    public static final String USER_ID = "userId";

    public static final String DISCHARGE_FOR_DAY = "DAY";
    public static final String DISCHARGE_FOR_WEEK = "WEEK";
    public static final String DISCHARGE_FOR_YEAR = "YEAR";

    public static final String CONDITIONAL = "CONDITIONAL";
    public static final String POSTCODE = "postcode";
    public static final String DEFENDANT_ID = "defendantId";
    public static final String ABSOLUTE = "ABSOLUTE";

    public static final String DISCHARGE_FOR_MONTH = "MONTH";
    public static final String DEDUCT_FROM_BENEFITS = "DEDUCT_FROM_BENEFITS";
    public static final Integer FOURTEEN_DAYS = 14;
    public static final Integer TWENTY_EIGHT_DAYS = 28;
    public static final String RESULTS = "results";

    public static final DateTimeFormatter DATE_FORMAT = ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter HOUR_FORMAT = ofPattern("hh:mm a");

    private JCaseResultsConstants() {
    }
}
