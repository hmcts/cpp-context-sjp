package uk.gov.moj.cpp.sjp.domain;

import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackDuty {

    @JsonProperty("backDuty")
    private final Integer value;

    @JsonProperty("backDutyDateFrom")
    private final LocalDate dateFrom;

    @JsonProperty("backDutyDateTo")
    private final LocalDate dateTo;

    public BackDuty(final Integer value, final LocalDate dateFrom, final LocalDate dateTo) {
        this.value = value;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    public Integer getValue() {
        return value;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final BackDuty that = (BackDuty) o;
        return Objects.equals(value, that.value) &&
                Objects.equals(dateFrom, that.dateFrom) &&
                Objects.equals(dateTo, that.dateTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, dateFrom, dateTo);
    }

}
