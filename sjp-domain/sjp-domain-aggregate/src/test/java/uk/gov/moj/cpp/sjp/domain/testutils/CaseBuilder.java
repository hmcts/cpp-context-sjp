package uk.gov.moj.cpp.sjp.domain.testutils;

import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class CaseBuilder {

    private static final String URN = "urnValue";
    private static final String ENTERPRISE_ID = "enterpriseIdValue";
    private static BigDecimal COSTS = BigDecimal.valueOf(33.33);
    private static LocalDate POSTING_DATE = LocalDate.of(2015, 12, 3);

    private UUID id;
    private String urn;
    private String enterpriseId;
    private String prosecutingAuthority;
    private Defendant defendant;

    private CaseBuilder() {
        id = DefaultTestData.CASE_ID;
        urn = URN;
        enterpriseId = ENTERPRISE_ID;
        prosecutingAuthority = "TFL";
        defendant = new DefendantBuilder().build();
    }

    public static CaseBuilder aDefaultSjpCase() {
        return new CaseBuilder();
    }

    public CaseBuilder withDefendant(Defendant defendant) {
        this.defendant = defendant;
        return this;
    }

    public CaseBuilder withId(UUID id) {
        this.id = id;
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
