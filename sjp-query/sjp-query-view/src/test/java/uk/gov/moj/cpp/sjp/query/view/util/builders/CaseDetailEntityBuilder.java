package uk.gov.moj.cpp.sjp.query.view.util.builders;

import static java.math.BigDecimal.valueOf;
import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class CaseDetailEntityBuilder {

    private final UUID id;
    private String urn;
    private final String enterpriseId;
    private String prosecutingAuthority;
    private final Boolean completed;
    private final UUID assigneeId;
    private final ZonedDateTime createdOn;
    private DefendantDetail defendantDetail;
    private final BigDecimal costs;
    private LocalDate postingDate;

    private static final String URN = "AAA";
    private static final String ENTERPRISE_ID = "1";
    private static final String PROSECUTION_AUTHORITY = "TFL";
    private static final UUID ASSIGNEE_ID = randomUUID();
    private static final ZonedDateTime CREATE_ON = ZonedDateTime.now();
    private static final BigDecimal COST = valueOf(100);
    private static final LocalDate POSTING_DATE = LocalDate.now();

    private CaseDetailEntityBuilder(final UUID id,
                                    final String urn,
                                    final String enterpriseId,
                                    final String prosecutingAuthority,
                                    final Boolean completed,
                                    final UUID assigneeId,
                                    final ZonedDateTime createdOn,
                                    final DefendantDetail defendantDetail,
                                    final BigDecimal costs,
                                    final LocalDate postingDate) {
        this.id = id;
        this.urn = urn;
        this.enterpriseId = enterpriseId;
        this.prosecutingAuthority = prosecutingAuthority;
        this.completed = completed;
        this.assigneeId = assigneeId;
        this.createdOn = createdOn;
        this.defendantDetail = defendantDetail;
        this.costs = costs;
        this.postingDate = postingDate;
    }

    public static CaseDetailEntityBuilder withDefaults() {
        return new CaseDetailEntityBuilder(
                randomUUID(),
                URN,
                ENTERPRISE_ID,
                PROSECUTION_AUTHORITY,
                false,
                ASSIGNEE_ID,
                CREATE_ON,
                null,
                COST,
                POSTING_DATE);
    }

    public CaseDetail build() {
        return new CaseDetail(
                id,
                urn,
                enterpriseId,
                prosecutingAuthority,
                completed,
                assigneeId,
                createdOn,
                defendantDetail,
                costs,
                postingDate
        );
    }

    public CaseDetailEntityBuilder withDefendantDetail(final DefendantDetail defendantDetail) {
        this.defendantDetail = defendantDetail;
        return this;
    }
    //Mandatory field
    public CaseDetailEntityBuilder withUrn(final String urn) {
        this.urn = urn;
        return this;
    }
    //Mandatory field
    public CaseDetailEntityBuilder withProsecutionAuthority(final String prosecutingAuthority) {
        this.prosecutingAuthority = prosecutingAuthority;
        return this;
    }
    //mandatoryField
    public CaseDetailEntityBuilder withPostingDate(final LocalDate postingDate) {
        this.postingDate = postingDate;
        return this;
    }
}
