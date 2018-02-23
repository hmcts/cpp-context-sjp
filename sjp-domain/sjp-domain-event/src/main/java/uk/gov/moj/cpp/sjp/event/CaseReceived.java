package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Event(CaseReceived.EVENT_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseReceived {

    public static final String EVENT_NAME = "sjp.events.case-received";

    private final UUID caseId;
    private final String urn;
    private final ProsecutingAuthority prosecutingAuthority;
    private final BigDecimal costs;
    private final LocalDate postingDate;
    private final Defendant defendant;
    private final ZonedDateTime createdOn;

    @JsonCreator
    public CaseReceived(@JsonProperty("caseId") UUID caseId,
                        @JsonProperty("urn") String urn,
                        @JsonProperty("prosecutingAuthority") ProsecutingAuthority prosecutingAuthority,
                        @JsonProperty("costs") BigDecimal costs,
                        @JsonProperty("postingDate") LocalDate postingDate,
                        @JsonProperty("defendant") Defendant defendant,
                        @JsonProperty("createdOn") ZonedDateTime createdOn) {
        this.caseId = caseId;
        this.urn = urn;
        this.prosecutingAuthority = prosecutingAuthority;
        this.costs = costs;
        this.postingDate = postingDate;
        this.defendant = defendant;
        this.createdOn = createdOn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

    public ProsecutingAuthority getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public BigDecimal getCosts() {
        return costs;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public ZonedDateTime getCreatedOn() {
        return createdOn;
    }

    public Defendant getDefendant() {
        return defendant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CaseReceived that = (CaseReceived) o;
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(urn, that.urn) &&
                Objects.equals(prosecutingAuthority, that.prosecutingAuthority) &&
                Objects.equals(costs, that.costs) &&
                Objects.equals(postingDate, that.postingDate) &&
                Objects.equals(defendant, that.defendant) &&
                Objects.equals(createdOn, that.createdOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, urn, prosecutingAuthority, costs, postingDate, defendant, createdOn);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
