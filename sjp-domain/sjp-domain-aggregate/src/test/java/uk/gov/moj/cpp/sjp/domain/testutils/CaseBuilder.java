package uk.gov.moj.cpp.sjp.domain.testutils;


import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CaseBuilder {

    public static final String URN = "urnValue";
    public static final String PTI_URN = "ptiUrnValue";
    public static final String INITIATION_CODE = "J";
    public static final String SUMMONS_CODE = "M";
    public static final String LIBRA_ORIGINATING_ORG = "GAFTL00";
    public static final String LIBRA_HEARING_LOCATION = "B01CE03";
    public static final LocalDate DATE_OF_HEARING = LocalDate.of(2016, 1, 1);
    public static final String TIME_OF_HEARING = "11:00";
    private static BigDecimal COMPENSATION = BigDecimal.valueOf(11.11);
    public static final List<Offence> OFFENCES = Collections.singletonList(
            new Offence(UUID.randomUUID(), 0, null, null, 0, null, null, null, null, COMPENSATION)
    );
    public static final int NUM_PREVIOUS_CONVICTIONS = 3;
    public static BigDecimal COSTS = BigDecimal.valueOf(33.33);
    public static LocalDate POSTING_DATE = LocalDate.of(2015, 12, 3);

    private UUID id;
    private String urn;
    private String ptiUrn;
    private String initiationCode;
    private String summonsCode;
    private ProsecutingAuthority prosecutingAuthority;
    private String libraOriginatingOrg;
    private String libraHearingLocation;
    private LocalDate dateOfHearing;
    private String timeOfHearing;
    private Defendant defendant;

    private CaseBuilder() {
        id = DefaultTestData.CASE_ID;
        urn = URN;
        ptiUrn = PTI_URN;
        initiationCode = INITIATION_CODE;
        summonsCode = SUMMONS_CODE;
        prosecutingAuthority = ProsecutingAuthority.TFL;
        libraOriginatingOrg = LIBRA_ORIGINATING_ORG;
        libraHearingLocation = LIBRA_HEARING_LOCATION;
        dateOfHearing = DATE_OF_HEARING;
        timeOfHearing = TIME_OF_HEARING;
        defendant = new Defendant(DefaultTestData.DEFENDANT_ID, null, null, null,
                null, null, null, NUM_PREVIOUS_CONVICTIONS, OFFENCES);
    }

    public static CaseBuilder aDefaultSjpCase() {
        return new CaseBuilder();
    }

    public CaseBuilder withDefendant(Defendant defendant) {
        this.defendant = defendant;
        return this;
    }

    public Case build() {
        return new Case(id,
                urn,
                ptiUrn,
                prosecutingAuthority,
                initiationCode,
                summonsCode,
                libraOriginatingOrg,
                libraHearingLocation,
                dateOfHearing,
                timeOfHearing,
                COSTS,
                POSTING_DATE,
                defendant);
    }
}
