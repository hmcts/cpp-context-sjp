package uk.gov.moj.cpp.sjp.domain.util;


import uk.gov.justice.services.common.converter.LocalDates;

import java.time.LocalDate;
import java.util.UUID;

public class DefaultTestData {

    public static final UUID CASE_ID = UUID.randomUUID();
    public static final String CASE_ID_STR = CASE_ID.toString();

    public static final UUID DEFENDANT_ID = UUID.randomUUID();
    public static final String MATERIAL_ID = UUID.randomUUID().toString();

    public static final String ASN = "ASN value";

    public static final UUID CASE_DOCUMENT_ID = UUID.randomUUID();
    public static final String CASE_DOCUMENT_ID_STR = CASE_DOCUMENT_ID.toString();
    public static final String CASE_DOCUMENT_MATERIAL_ID = UUID.randomUUID().toString();
    public static final String CASE_DOCUMENT_POLICE_NAME = "Richard Prett file";
    public static final String CASE_DOCUMENT_POLICE_MATERIAL_ID = "x1234";
    public static final String CASE_DOCUMENT_TYPE_SJPN = "SJPN";
    public static final String CASE_DOCUMENT_EXTERNAL_LOCATION_URL = "/external/file/location";

    public static final String ATTACHMENT_NAME = "test attachment name";
    public static final String STORAGE_REFERENCE = "test storage reference";

    public static final LocalDate REOPEN_DATE = LocalDates.from("2017-01-01");
    public static final String REOPEN_LIBRA_NUMBER = "LIBRA12345";
    public static final String REOPEN_REASON = "Maybe some reason for reopening";
    public static final LocalDate REOPEN_UPDATE_DATE = LocalDates.from("2016-12-31");
    public static final String REOPEN_UPDATE_LIBRA_NUMBER = "LIBRA9999999";
    public static final String REOPEN_UPDATE_REASON = "No particular reason";
}
