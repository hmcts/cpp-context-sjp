package uk.gov.moj.cpp.sjp.domain.testutils;


import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CaseBuilder {

    public static final String URN = "urnValue";
    public static final String ENTERPRISE_ID = "enterpriseIdValue";
    private static BigDecimal COMPENSATION = BigDecimal.valueOf(11.11);
    public static final List<Offence> OFFENCES = Collections.singletonList(
            new Offence(UUID.randomUUID(), 0, null, null, 0, null, null, null, null, COMPENSATION)
    );
    public static final int NUM_PREVIOUS_CONVICTIONS = 3;
    public static BigDecimal COSTS = BigDecimal.valueOf(33.33);
    public static LocalDate POSTING_DATE = LocalDate.of(2015, 12, 3);

    private UUID id;
    private String urn;
    private String enterpriseId;
    private ProsecutingAuthority prosecutingAuthority;
    private Defendant defendant;

    private CaseBuilder() {
        id = DefaultTestData.CASE_ID;
        urn = URN;
        enterpriseId = ENTERPRISE_ID;
        prosecutingAuthority = ProsecutingAuthority.TFL;
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
                enterpriseId,
                prosecutingAuthority,
                COSTS,
                POSTING_DATE,
                defendant);
    }
}
