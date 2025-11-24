package uk.gov.moj.cpp.sjp.event.listener.converter;

import uk.gov.moj.cpp.sjp.domain.Outgoing;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaOutgoingOption;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class OnlinePleaConverter {

    enum OnlinePleaEmploymentStatus {
        EMPLOYED, SELF_EMPLOYED, UNEMPLOYED, OTHER
    }

    public OnlinePlea convertToOnlinePleaEntity(final UUID caseId, final FinancialMeansUpdated financialMeansUpdated) {
        final OnlinePlea.Outgoings outgoings = generateOutgoings(financialMeansUpdated.getOutgoings());
        return generateOnlinePleaWithEmploymentStatus(caseId, financialMeansUpdated, outgoings);
    }

    private OnlinePlea.Outgoings generateOutgoings(final List<Outgoing> outgoingList) {
        final OnlinePlea.Outgoings outgoings = new OnlinePlea.Outgoings();
        if (!CollectionUtils.isEmpty(outgoingList)) {
            outgoingList.forEach(outgoing -> {
                if (!StringUtils.isEmpty(outgoing.getDescription()) && outgoing.getDescription().equalsIgnoreCase(OnlinePleaOutgoingOption.ACCOMMODATION.getDescription())) {
                    outgoings.setAccommodationAmount(outgoing.getAmount());
                } else if (!StringUtils.isEmpty(outgoing.getDescription()) && outgoing.getDescription().equalsIgnoreCase(OnlinePleaOutgoingOption.COUNCIL_TAX.getDescription())) {
                    outgoings.setCouncilTaxAmount(outgoing.getAmount());
                } else if (!StringUtils.isEmpty(outgoing.getDescription()) && outgoing.getDescription().equalsIgnoreCase(OnlinePleaOutgoingOption.HOUSEHOLD_BILLS.getDescription())) {
                    outgoings.setHouseholdBillsAmount(outgoing.getAmount());
                } else if (!StringUtils.isEmpty(outgoing.getDescription()) && outgoing.getDescription().equalsIgnoreCase(OnlinePleaOutgoingOption.TRAVEL_EXPENSES.getDescription())) {
                    outgoings.setTravelExpensesAmount(outgoing.getAmount());
                } else if (!StringUtils.isEmpty(outgoing.getDescription()) && outgoing.getDescription().equalsIgnoreCase(OnlinePleaOutgoingOption.CHILD_MAINTENANCE.getDescription())) {
                    outgoings.setChildMaintenanceAmount(outgoing.getAmount());
                } else if (StringUtils.isEmpty(outgoings.getOtherDescription())) {
                    outgoings.setOtherDescription(outgoing.getDescription());
                    outgoings.setOtherAmount(outgoing.getAmount());
                } else {
                    throw new IllegalStateException(
                            "this exception is thrown to protect us against adding extra outgoings (in the future) and forgetting to add a condition here to handle them"
                    );
                }
            });
        }
        return outgoings;
    }

    private OnlinePlea generateOnlinePleaWithEmploymentStatus(final UUID caseId, final FinancialMeansUpdated financialMeansUpdated, OnlinePlea.Outgoings outgoings) {
        final List<OnlinePleaEmploymentStatus> employmentStatuses = Arrays.asList(OnlinePleaEmploymentStatus.values());
        final Optional<OnlinePleaEmploymentStatus> matchingEmploymentStatuses = employmentStatuses.stream()
                .filter(employmentStatus -> employmentStatus.name().equals(financialMeansUpdated.getEmploymentStatus())).findFirst();
        if (matchingEmploymentStatuses.isPresent()) {
            return new OnlinePlea(caseId, financialMeansUpdated, matchingEmploymentStatuses.get().name(), null, outgoings);
        }
        return new OnlinePlea(caseId, financialMeansUpdated, OnlinePleaEmploymentStatus.OTHER.name(), financialMeansUpdated.getEmploymentStatus(), outgoings);
    }
}
