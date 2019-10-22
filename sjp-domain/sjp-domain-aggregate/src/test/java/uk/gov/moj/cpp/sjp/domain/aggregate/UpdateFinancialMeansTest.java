package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleted;
import uk.gov.moj.cpp.sjp.event.FinancialMeansUpdated;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;

public class UpdateFinancialMeansTest extends CaseAggregateBaseTest {

    private UUID userId = UUID.randomUUID();

    @Test
    public void shouldCreateFinancialMeansUpdatedEventIfDefendantExists() {
        final UUID defendantId = caseReceivedEvent.getDefendant().getId();

        final Income income = new Income(IncomeFrequency.MONTHLY, BigDecimal.valueOf(1000.50));
        final Benefits benefits = new Benefits(false, EMPTY);
        final FinancialMeans financialMeans = new FinancialMeans(defendantId, income, benefits, "EMPLOYED");

        final Stream<Object> eventStream = caseAggregate.updateFinancialMeans(userId, financialMeans);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final FinancialMeansUpdated financialMeansUpdated = (FinancialMeansUpdated) events.get(0);

        assertThat(financialMeansUpdated.getDefendantId(), equalTo(defendantId));
        assertThat(financialMeansUpdated.getBenefits(), equalTo(benefits));
        assertThat(financialMeansUpdated.getIncome(), equalTo(income));
        assertThat(financialMeansUpdated.getEmploymentStatus(), equalTo(financialMeans.getEmploymentStatus()));
        assertTrue(financialMeansUpdated.getOutgoings().isEmpty());
    }

    @Test
    public void shouldCreateDefendantNotFoundEventIfDefendantDoesNotExist() {
        final UUID defendantId = UUID.randomUUID();
        final Income income = new Income(IncomeFrequency.MONTHLY, BigDecimal.valueOf(1000.50));
        final Benefits benefits = new Benefits(false, EMPTY);
        final FinancialMeans financialMeans = new FinancialMeans(defendantId, income, benefits, "EMPLOYED");

        final Stream<Object> eventStream = caseAggregate.updateFinancialMeans(userId, financialMeans);
        final List<Object> events = eventStream.collect(toList());

        assertThat(events, hasSize(1));

        final DefendantNotFound defendantNotFound = (DefendantNotFound) events.get(0);

        assertThat(defendantNotFound.getDefendantId(), equalTo(defendantId));
        assertThat(defendantNotFound.getDescription(), equalTo("Update financial means"));
    }

    @Test
    public void shouldCreateFinancialMeansDeletedEventIfDefendantExists() {

        addCaseDocument("FINANCIAL_MEANS");
        addCaseDocument("xxxxxxxxxx");
        final UUID defendantId = caseReceivedEvent.getDefendant().getId();
        assertThat(caseAggregate.getState().getCaseDocuments().keySet(), hasSize(2));

        final Stream<Object> eventStream = caseAggregate.deleteFinancialMeans(defendantId);
        final List<Object> events = eventStream.collect(toList());
        assertThat(events, hasSize(1));

        final FinancialMeansDeleted financialMeansDeleted = (FinancialMeansDeleted) events.get(0);

        assertThat(financialMeansDeleted.getDefendantId(), equalTo(defendantId));
        assertThat(caseAggregate.getState().getCaseDocuments().keySet(), hasSize(1));
        final List<CaseDocument> caseDocuments = caseAggregate.getState().getCaseDocuments().entrySet().stream()
                .map(uuidCaseDocumentEntry -> uuidCaseDocumentEntry.getValue())
                .filter(caseDocument ->
                        "xxxxxxxxxx".equalsIgnoreCase(caseDocument.getDocumentType()))
                .collect(toList());
        assertThat(caseDocuments.get(0).getDocumentType(), equalTo("xxxxxxxxxx"));
    }


    public void addCaseDocument(String documentType) {
        UUID documentId = UUID.randomUUID();
        CaseDocument caseDocument = new CaseDocument(documentId, UUID.randomUUID(), documentType, ZonedDateTime.now());
        Stream<Object> eventStream = caseAggregate.addCaseDocument(UUID.randomUUID(), caseDocument);

    }
}
