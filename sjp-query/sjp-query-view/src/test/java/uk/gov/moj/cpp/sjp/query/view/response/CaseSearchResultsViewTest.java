package uk.gov.moj.cpp.sjp.query.view.response;

import static java.time.LocalDate.now;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSummary;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceSummary;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

public class CaseSearchResultsViewTest {

    private CaseSearchResult caseSearchResult;

    @Before
    public void setup() {
        caseSearchResult = createCaseSearchResult();
    }

    @Test
    public void shouldCalculateNoOutdatedCasesWhenEmpty() {
        // given
        final List<CaseSearchResult> data = Collections.emptyList();

        // when
        final CaseSearchResultsView caseSearchResultsView = new CaseSearchResultsView(data);

        // then
        assertFalse(caseSearchResultsView.isFoundCasesWithOutdatedDefendantsName());
    }

    @Test
    public void shouldCalculateNoOutdatedCases() {
        // given
        caseSearchResult.setDeprecated(false);

        // when
        final CaseSearchResultsView caseSearchResultsView = new CaseSearchResultsView(Lists.newArrayList(caseSearchResult));

        // then
        assertFalse(caseSearchResultsView.isFoundCasesWithOutdatedDefendantsName());
    }

    @Test
    public void shouldCalculateOutdatedCases() {
        // given
        caseSearchResult.setDeprecated(true);

        // when
        final CaseSearchResultsView caseSearchResultsView = new CaseSearchResultsView(Lists.newArrayList(caseSearchResult));

        // then
        assertTrue(caseSearchResultsView.isFoundCasesWithOutdatedDefendantsName());
    }

    @Test
    public void updatingUnderlyingDataShouldNotInfluenceTheView() {
        // given
        final List<CaseSearchResult> underlyingData = Lists.newArrayList(caseSearchResult);
        final CaseSearchResultsView caseSearchResultsView = new CaseSearchResultsView(underlyingData);

        assertFalse(caseSearchResultsView.isFoundCasesWithOutdatedDefendantsName());
        assertThat(caseSearchResultsView.getResults(), hasSize(1));

        // when
        caseSearchResult.setDeprecated(true);
        underlyingData.add(caseSearchResult);

        // then the view is unaffected
        assertFalse(caseSearchResultsView.isFoundCasesWithOutdatedDefendantsName());
        assertThat(caseSearchResultsView.getResults(), hasSize(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void modifyingTheViewIsNotAllowed() {
        // given
        final CaseSearchResultsView caseSearchResultsView = new CaseSearchResultsView(Lists.newArrayList(caseSearchResult));

        // when
        caseSearchResultsView.getResults().add(new CaseSearchResultsView.CaseSearchResultView(caseSearchResult));

        // then the exception should have been thrown
        fail("Unreachable code");
    }

    @Test
    public void whenMultipleValuesProvidedOneIsSufficientToInfluenceTheView() {
        // given
        caseSearchResult.setDeprecated(true);
        final List<CaseSearchResult> dataWithOutdatedEntry = Lists.newArrayList(caseSearchResult, createCaseSearchResult(), createCaseSearchResult());
        final List<CaseSearchResult> dataWithAllOutdatedEntries = Lists.newArrayList(caseSearchResult, caseSearchResult, caseSearchResult);
        final List<CaseSearchResult> dataWithoutOutdatedEntry = Lists.newArrayList(createCaseSearchResult(), createCaseSearchResult(), createCaseSearchResult());

        // when
        final CaseSearchResultsView viewWithOutdatedEntry = new CaseSearchResultsView(dataWithOutdatedEntry);
        final CaseSearchResultsView viewWithAllOutdatedEntries = new CaseSearchResultsView(dataWithAllOutdatedEntries);
        final CaseSearchResultsView viewWithoutOutdatedEntry = new CaseSearchResultsView(dataWithoutOutdatedEntry);

        // then
        assertTrue(viewWithOutdatedEntry.isFoundCasesWithOutdatedDefendantsName());
        assertTrue(viewWithAllOutdatedEntries.isFoundCasesWithOutdatedDefendantsName());
        assertFalse(viewWithoutOutdatedEntry.isFoundCasesWithOutdatedDefendantsName());
    }

    @Test
    public void shouldReturnOldestPleaDate() {
        // given two offences with different plea dates
        OffenceSummary offenceSummaryWithYesterdayPleaDate = new OffenceSummary();
        final ZonedDateTime yesterdayDateTime = ZonedDateTime.now().minusDays(1);
        offenceSummaryWithYesterdayPleaDate.setPleaDate(yesterdayDateTime);

        OffenceSummary offenceSummaryWithTodayPleaDate = new OffenceSummary();
        final ZonedDateTime now = ZonedDateTime.now();
        offenceSummaryWithTodayPleaDate.setPleaDate(now);

        final CaseSearchResult caseSearchResult = createCaseSearchResultWithOffenceSummaries(offenceSummaryWithTodayPleaDate, offenceSummaryWithYesterdayPleaDate);

        //when case search result view is built
        final CaseSearchResultsView caseSearchResultView = new CaseSearchResultsView(Collections.singletonList(caseSearchResult));
        assertThat(caseSearchResultView.getResults().get(0).getPleaDate(), equalTo(yesterdayDateTime.toLocalDate()));
    }


    private CaseSearchResult createCaseSearchResult() {
        return createCaseSearchResultWithOffenceSummaries(new OffenceSummary());
    }

    private CaseSearchResult createCaseSearchResultWithOffenceSummaries(OffenceSummary... offenceSummaries) {
        final CaseSearchResult caseSearchResult = new CaseSearchResult();
        final CaseSummary caseSummary = new CaseSummary();
        caseSummary.setPostingDate(now());
        caseSearchResult.setCaseSummary(caseSummary);

        addOffencesSummaryToTheSearchResults(caseSearchResult, offenceSummaries);
        return caseSearchResult;

    }


    private void addOffencesSummaryToTheSearchResults(final CaseSearchResult caseSearchResult, OffenceSummary... offenceSummaries) {
        final Set<OffenceSummary> offenceSummariesCollection = new HashSet<>(Arrays.asList(offenceSummaries));
        caseSearchResult.setOffenceSummary(offenceSummariesCollection);

    }
}
