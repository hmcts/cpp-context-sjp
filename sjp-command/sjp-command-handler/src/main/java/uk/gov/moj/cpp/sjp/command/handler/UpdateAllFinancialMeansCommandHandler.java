package uk.gov.moj.cpp.sjp.command.handler;


import static uk.gov.moj.cpp.sjp.domain.EmploymentStatus.EMPLOYED;
import static uk.gov.moj.cpp.sjp.domain.EmploymentStatus.OTHER;

import uk.gov.justice.json.schemas.domains.sjp.Benefits;
import uk.gov.justice.json.schemas.domains.sjp.Employment;
import uk.gov.justice.json.schemas.domains.sjp.Income;
import uk.gov.justice.json.schemas.domains.sjp.command.UpdateAllFinancialMeans;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.Employer;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;

import java.util.Optional;
import java.util.UUID;


@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateAllFinancialMeansCommandHandler extends CaseCommandHandler {

    @Handles("sjp.command.update-all-financial-means")
    public void updateAllFinancialMeans(final Envelope<UpdateAllFinancialMeans> command) throws EventStreamException {
        final UpdateAllFinancialMeans payload = command.payload();

        final UUID caseId = payload.getCaseId();
        final UUID defendantId = payload.getDefendantId();

        final Income income = payload.getIncome();
        final Benefits benefits = payload.getBenefits();
        final Employer employer = payload.getEmployer();
        final String employmentStatus = Optional
                .ofNullable(payload.getEmployment())
                .map(e -> OTHER.name().equalsIgnoreCase(e.getStatus()) ? e.getDetails() : e.getStatus())
                .orElse( null);

        final uk.gov.moj.cpp.sjp.domain.Income transformedIncome = new uk.gov.moj.cpp.sjp.domain.Income(IncomeFrequency.valueOf(income.getFrequency().toString()), income.getAmount());
        final uk.gov.moj.cpp.sjp.domain.Benefits transformedBenefits = new uk.gov.moj.cpp.sjp.domain.Benefits(benefits.getClaimed(), benefits.getType());
        final uk.gov.moj.cpp.sjp.domain.Employer transformedEmployer = transformEmployer(defendantId, employer, payload.getEmployment());

        applyToCaseAggregate(caseId,
                command,
                aggregate -> aggregate.updateAllFinancialMeans(getUserId(command),
                        defendantId,
                        transformedIncome,
                        transformedBenefits,
                        transformedEmployer,
                        employmentStatus, null, null, null, null));
    }

    private uk.gov.moj.cpp.sjp.domain.Employer transformEmployer(final UUID defendantId,
                                                                 final Employer employer,
                                                                 final Employment employment) {
        uk.gov.moj.cpp.sjp.domain.Employer transformedEmployer = null;
        if (employer != null
                && employment != null
                && EMPLOYED.name().equals(employment.getStatus())) {
            Address address = null;
            if (employer.getAddress() != null) {
                address = new Address(employer.getAddress().getAddress1(),
                        employer.getAddress().getAddress2(),
                        employer.getAddress().getAddress3(),
                        employer.getAddress().getAddress4(),
                        employer.getAddress().getAddress5(),
                        employer.getAddress().getPostcode());
            }
            transformedEmployer = new uk.gov.moj.cpp.sjp.domain.Employer(defendantId,
                    employer.getName(),
                    employer.getEmployeeReference(),
                    employer.getPhone(),
                    address);

        }
        return transformedEmployer;
    }

}
