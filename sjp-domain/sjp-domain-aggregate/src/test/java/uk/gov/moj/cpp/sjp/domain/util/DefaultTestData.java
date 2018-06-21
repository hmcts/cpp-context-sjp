package uk.gov.moj.cpp.sjp.domain.util;


import java.time.LocalDate;
import java.util.UUID;

public class DefaultTestData {

    public static final UUID CASE_ID = UUID.randomUUID();
    public static final String CASE_ID_STR = CASE_ID.toString();

    public static final UUID DEFENDANT_ID = UUID.randomUUID();
    public static final UUID CASE_DOCUMENT_ID = UUID.randomUUID();
    public static final String CASE_DOCUMENT_ID_STR = CASE_DOCUMENT_ID.toString();
    public static final UUID CASE_DOCUMENT_MATERIAL_ID = UUID.randomUUID();
    public static final String CASE_DOCUMENT_MATERIAL_ID_STR = CASE_DOCUMENT_MATERIAL_ID.toString();
    public static final String CASE_DOCUMENT_TYPE_SJPN = "SJPN";
    public static final LocalDate REOPEN_DATE = LocalDate.of(2017, 1, 1);
    public static final String REOPEN_LIBRA_NUMBER = "LIBRA12345";
    public static final String REOPEN_REASON = "Maybe some reason for reopening";
    public static final LocalDate REOPEN_UPDATE_DATE = LocalDate.of(2016, 12, 31);
    public static final String REOPEN_UPDATE_LIBRA_NUMBER = "LIBRA9999999";
    public static final String REOPEN_UPDATE_REASON = "No particular reason";
}
