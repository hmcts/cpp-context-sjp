package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.stream.Stream.*;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.HandlerUtils.createRejectionEvents;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;

import java.util.UUID;
import java.util.stream.Stream;

public class UpdateAllFinancialMeansAggregateHandler {

    public static final UpdateAllFinancialMeansAggregateHandler INSTANCE = new UpdateAllFinancialMeansAggregateHandler();

    private UpdateAllFinancialMeansAggregateHandler() {
    }

    public Stream<Object> updateAllFinancialMeans(final UUID userId,
                                                  final UUID defendantId,
                                                  final Income income,
                                                  final Benefits benefits,
                                                  final Employer employer,
                                                  final String employmentStatus,
                                                  final CaseAggregateState state) {


        final FinancialMeans financialMeans = new FinancialMeans(defendantId,
                income,
                benefits,
                employmentStatus);
        Stream<Object> finalStream = CaseFinancialMeansHandler.INSTANCE.getFinancialMeansEventStream(financialMeans);

        if (employer != null) {
            finalStream = concat(finalStream, CaseEmployerHandler.INSTANCE.getEmployerEventStream(employer, state));
        } else {
            finalStream = concat(finalStream, CaseEmployerHandler.INSTANCE.getDeleteEmployerEventStream(defendantId, state));
        }

        finalStream = concat(finalStream, CaseFinancialMeansHandler.INSTANCE.getAllFinancialMeansUpdatedEventStream(defendantId, income, benefits, employer, employmentStatus));

        return createRejectionEvents(
                userId,
                "Update All Financial Means",
                defendantId,
                state
        ).orElse(finalStream);
    }
}


