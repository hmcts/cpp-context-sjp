package uk.gov.moj.cpp.sjp.event;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.Outgoing;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
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

    private BigDecimal grossTurnover;

    private BigDecimal  netTurnover;

    private Integer numberOfEmployees;

    private Boolean tradingMoreThanTwelveMonths;

    @JsonCreator
    @SuppressWarnings("unused") // used during deserialization
    private FinancialMeansUpdated(@JsonProperty("defendantId") final UUID defendantId,
                                  @JsonProperty("income") final Income income,
                                  @JsonProperty("benefits") final Benefits benefits,
                                  @JsonProperty("employmentStatus") final String employmentStatus,
                                  @JsonProperty("outgoings") final List<Outgoing> outgoings,
                                  @JsonProperty("updatedByOnlinePlea") final boolean updatedByOnlinePlea,
                                  @JsonProperty("updatedDate") final ZonedDateTime updatedDate,
                                  @JsonProperty("grossTurnover") final BigDecimal grossTurnover,
                                  @JsonProperty("netTurnover") final BigDecimal netTurnover,
                                  @JsonProperty("numberOfEmployees") final Integer numberOfEmployees,
                                  @JsonProperty("tradingMoreThanTwelveMonths") final Boolean tradingMoreThanTwelveMonths) {
        this.defendantId = defendantId;
        this.income = income;
        this.benefits = benefits;
        this.employmentStatus = employmentStatus;
        this.outgoings = outgoings;
        this.updatedByOnlinePlea = updatedByOnlinePlea;
        this.updatedDate = updatedDate;
        this.grossTurnover = grossTurnover;
        this.netTurnover = netTurnover;
        this.numberOfEmployees = numberOfEmployees;
        this.tradingMoreThanTwelveMonths = tradingMoreThanTwelveMonths;
    }

    public static FinancialMeansUpdated createEvent(final UUID defendantId, final Income income, final Benefits benefits,
                                                    final String employmentStatus, final BigDecimal grossTurnover, final BigDecimal netTurnover, final Integer numberOfEmployees, final Boolean tradingMoreThanTwelveMonths) {
        return new FinancialMeansUpdated(defendantId, income, benefits, employmentStatus, emptyList(), false, null, grossTurnover, netTurnover, numberOfEmployees, tradingMoreThanTwelveMonths);
    }

    public static FinancialMeansUpdated createEventForOnlinePlea(final UUID defendantId, final Income income, final Benefits benefits,
                                                                 final String employmentStatus, final List<Outgoing> outgoings,
                                                                 final ZonedDateTime updatedDate,
                                                                 final BigDecimal grossTurnover,
                                                                 final BigDecimal netTurnover,
                                                                 final Integer numberOfEmployees,
                                                                 final Boolean tradingMoreThanTwelveMonths) {
        return new FinancialMeansUpdated(defendantId, income, benefits, employmentStatus, outgoings, true, updatedDate, grossTurnover, netTurnover, numberOfEmployees, tradingMoreThanTwelveMonths);
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public Income getIncome() {
        return ofNullable(income).orElseGet(() -> new Income(null, null));
    }

    public Benefits getBenefits() {
        return ofNullable(benefits).orElseGet(Benefits::new);
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

    public BigDecimal getGrossTurnover() {
        return grossTurnover;
    }

    public BigDecimal getNetTurnover() {
        return netTurnover;
    }

    public Integer getNumberOfEmployees() {
        return numberOfEmployees;
    }

    public Boolean getTradingMoreThanTwelveMonths() {
        return tradingMoreThanTwelveMonths;
    }
}