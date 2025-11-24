package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.fromString;
import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.domain.common.CaseByManagementStatus;
import uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus;
import uk.gov.moj.cpp.sjp.event.casemanagement.UpdateCasesManagementStatus;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

public class CaseManagementStatusAggregate implements Aggregate {

    private static final long serialVersionUID = 1L;

    public static final UUID CASE_MANAGEMENT_STATUS_STREAM_ID = fromString("ed028c79-031f-4da7-865b-53117af630b9");

    public Stream<Object> updateCaseManagementStatus(final JsonObject caseManagementStatusCommand) {
        final List<CaseByManagementStatus> caseByManagementStatusList = caseManagementStatusCommand.getJsonArray("cases")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(caseJson -> new CaseByManagementStatus(fromString(caseJson.getString("caseId")), CaseManagementStatus.valueOf(caseJson.getString("caseManagementStatus"))))
                .collect(Collectors.toList());

        return apply(of(new UpdateCasesManagementStatus(caseByManagementStatusList)));
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(UpdateCasesManagementStatus.class).apply(e -> doNothing()),
                otherwiseDoNothing());
    }
}
