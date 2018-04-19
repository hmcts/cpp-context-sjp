package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.persistence.entity.view.ReadyCasesReasonCount;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;

@SqlResultSetMapping(name = "caseReasonsCountMapping", classes = @ConstructorResult(targetClass = ReadyCasesReasonCount.class, columns = {@ColumnResult(name = "reason"), @ColumnResult(name = "count", type = Long.class)}))
@NamedNativeQuery(name = "readyCases.readyCasesReasonCounts", query = "SELECT reason reason, COUNT(*) count FROM ready_cases GROUP BY reason", resultSetMapping = "caseReasonsCountMapping")
@Entity
@Table(name = "ready_cases")
public class ReadyCase {

    @Id
    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "reason")
    private String reason;

    public ReadyCase() {
        //required for hibernate
    }

    public ReadyCase(UUID caseId, String reason) {
        this.caseId = caseId;
        this.reason = reason;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getReason() {
        return reason;
    }
    
}
