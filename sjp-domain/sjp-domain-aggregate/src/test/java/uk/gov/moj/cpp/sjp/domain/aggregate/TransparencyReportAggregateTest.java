package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportGenerationFailed;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class TransparencyReportAggregateTest {

    private TransparencyReportAggregate aggregate = new TransparencyReportAggregate();

    @Test
    public void transparencyReportFailedShouldCreateEventWithCaseIds() {
        // Given
        final UUID transparencyReportId = randomUUID();
        final ZonedDateTime requestedAt = ZonedDateTime.now();
        aggregate.requestTransparencyReport(transparencyReportId, requestedAt);

        final List<UUID> caseIds = Arrays.asList(randomUUID(), randomUUID(), randomUUID());
        aggregate.startTransparencyReportGeneration(caseIds);

        // When
        final String templateIdentifier = "PendingCasesEnglish";
        final List<Object> results = aggregate.transparencyReportFailed(templateIdentifier).collect(toList());

        // Then
        assertThat(results, containsInAnyOrder(
                new TransparencyReportGenerationFailed(transparencyReportId,templateIdentifier, caseIds, false))
        );

        //When error on second time
        final String templateIdentifierWelsh = "PendingCasesWelsh";
        final List<Object> results2 = aggregate.transparencyReportFailed(templateIdentifierWelsh).collect(toList());

        // Then
        assertThat(results2, containsInAnyOrder(
                new TransparencyReportGenerationFailed(transparencyReportId,templateIdentifierWelsh, caseIds, true))
        );
    }

}