package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "offence")
public class OffenceSummary implements Serializable {

    @Column(name = "id")
    @Id
    private UUID id;
    @Column(name = "plea")
    private String plea;
    @Column(name = "plea_date")
    private ZonedDateTime pleaDate;

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getPlea() {
        return plea;
    }

    public void setPlea(final String plea) {
        this.plea = plea;
    }

    public ZonedDateTime getPleaDate() {
        return pleaDate;
    }

    public void setPleaDate(final ZonedDateTime pleaDate) {
        this.pleaDate = pleaDate;
    }

}
