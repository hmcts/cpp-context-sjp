package uk.gov.moj.cpp.sjp.event.casemanagement;

import static com.google.common.collect.ImmutableList.copyOf;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.common.CaseByManagementStatus;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Event(UpdateCasesManagementStatus.EVENT_NAME)
public class UpdateCasesManagementStatus {
    public static final String EVENT_NAME = "sjp.events.update-cases-management-status";

    private final List<CaseByManagementStatus> caseByManagementStatusList;


    @JsonCreator
    public UpdateCasesManagementStatus(@JsonProperty("cases") final List<CaseByManagementStatus> caseByManagementStatusList) {
        this.caseByManagementStatusList = copyOf(caseByManagementStatusList);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public List<CaseByManagementStatus> getCaseByManagementStatusList() {
        return copyOf(caseByManagementStatusList);
    }
}
