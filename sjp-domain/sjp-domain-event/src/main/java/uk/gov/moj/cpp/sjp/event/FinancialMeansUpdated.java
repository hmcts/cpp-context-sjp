package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.Outgoing;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.financial-means-updated")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FinancialMeansUpdated {

    private UUID defendantId;
    private Income income;
    private Benefits benefits;
    private String employmentStatus;
    private List<Outgoing> outgoings;
    private boolean updatedByOnlinePlea;
    private ZonedDateTime updatedDate;

    @JsonCreator
    private FinancialMeansUpdated(@JsonProperty("defendantId") final UUID defendantId,
                                  @JsonProperty("income") final Income income,
                                  @JsonProperty("benefits") final Benefits benefits,
                                  @JsonProperty("employmentStatus") final String employmentStatus,
                                  @JsonProperty("outgoings") final List<Outgoing> outgoings,
                                  @JsonProperty("updatedByOnlinePlea") final boolean updatedByOnlinePlea,
                                  @JsonProperty("updatedDate") final ZonedDateTime updatedDate) {
        this.defendantId = defendantId;
        this.income = income;
        this.benefits = benefits;
        this.employmentStatus = employmentStatus;
        this.outgoings = outgoings;
        this.updatedByOnlinePlea = updatedByOnlinePlea;
        this.updatedDate = updatedDate;
    }

    public static FinancialMeansUpdated createEvent(final UUID defendantId, final Income income, final Benefits benefits,
                                                                   final String employmentStatus) {
        return new FinancialMeansUpdated(defendantId, income, benefits, employmentStatus, new ArrayList<>(), false, null);
    }

    public static FinancialMeansUpdated createEventForOnlinePlea(final UUID defendantId, final Income income, final Benefits benefits,
                                                                 final String employmentStatus, final List<Outgoing> outgoings,
                                                                 final ZonedDateTime updatedDate) {
        return new FinancialMeansUpdated(defendantId, income, benefits, employmentStatus, outgoings, true, updatedDate);
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public Income getIncome() {
        return income;
    }

    public Benefits getBenefits() {
        return benefits;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public List<Outgoing> getOutgoings() {
        return outgoings;
    }

    public boolean isUpdatedByOnlinePlea() {
        return updatedByOnlinePlea;
    }

    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }
}