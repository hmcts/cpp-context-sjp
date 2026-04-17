package uk.gov.moj.cpp.sjp.persistence.entity;


import java.time.ZonedDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "case_assignment_restriction")
public class CaseAssignmentRestriction {

    @Id
    @Column(name = "prosecuting_authority")
    private String prosecutingAuthority;

    @Column(name = "include_only")
    private String includeOnly;

    @Column(name = "exclude")
    private String exclude;

    @Column(name = "date_time_created")
    private ZonedDateTime dateTimeCreated;

    public String getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public void setProsecutingAuthority(String prosecutingAuthority) {
        this.prosecutingAuthority = prosecutingAuthority;
    }

    public String getIncludeOnly() {
        return includeOnly;
    }

    public void setIncludeOnly(String includeOnly) {
        this.includeOnly = includeOnly;
    }

    public String getExclude() {
        return exclude;
    }

    public void setExclude(String exclude) {
        this.exclude = exclude;
    }

    public ZonedDateTime getDateTimeCreated() {
        return dateTimeCreated;
    }

    public void setDateTimeCreated(ZonedDateTime dateTimeCreated) {
        this.dateTimeCreated = dateTimeCreated;
    }
}
