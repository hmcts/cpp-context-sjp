package uk.gov.moj.cpp.sjp.query.view.response;

import static java.time.LocalDate.now;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSummary;

import java.util.Collections;
import java.util.List;

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
        List<CaseSearchResult> data = Collections.emptyList();

        // when
        CaseSearchResultsView caseSearchResultsView = new CaseSearchResultsView(data);

        // then
        assertFalse(caseSearchResultsView.isFoundCasesWithOutdatedDefendantsName());
    }

    @Test
    public void shouldCalculateNoOutdatedCases() {
        // given
        caseSearchResult.setDeprecated(false);

        // when
        CaseSearchResultsView caseSearchResultsView = new CaseSearchResultsView(Lists.newArrayList(caseSearchResult));

        // then
        assertFalse(caseSearchResultsView.isFoundCasesWithOutdatedDefendantsName());
    }

    @Test
    public void shouldCalculateOutdatedCases() {
        // given
        caseSearchResult.setDeprecated(true);

        // when
        CaseSearchResultsView caseSearchResultsView = new CaseSearchResultsView(Lists.newArrayList(caseSearchResult));

        // then
        assertTrue(caseSearchResultsView.isFoundCasesWithOutdatedDefendantsName());
    }

    @Test
    public void updatingUnderlyingDataShouldNotInfluenceTheView() {
        // given
        List<CaseSearchResult> underlyingData = Lists.newArrayList(caseSearchResult);
        CaseSearchResultsView caseSearchResultsView = new CaseSearchResultsView(underlyingData);

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
        CaseSearchResultsView caseSearchResultsView = new CaseSearchResultsView(Lists.newArrayList(caseSearchResult));

        // when
        caseSearchResultsView.getResults().add(new CaseSearchResultsView.CaseSearchResultView(caseSearchResult));

        // then the exception should have been thrown
        fail("Unreachable code");
    }

    @Test
    public void whenMultipleValuesProvidedOneIsSufficientToInfluenceTheView() {
        // given
        caseSearchResult.setDeprecated(true);
        List<CaseSearchResult> dataWithOutdatedEntry = Lists.newArrayList(caseSearchResult, createCaseSearchResult(), createCaseSearchResult());
        List<CaseSearchResult> dataWithAllOutdatedEntries = Lists.newArrayList(caseSearchResult, caseSearchResult, caseSearchResult);
        List<CaseSearchResult> dataWithoutOutdatedEntry = Lists.newArrayList(createCaseSearchResult(), createCaseSearchResult(), createCaseSearchResult());

        // when
        CaseSearchResultsView viewWithOutdatedEntry = new CaseSearchResultsView(dataWithOutdatedEntry);
        CaseSearchResultsView viewWithAllOutdatedEntries = new CaseSearchResultsView(dataWithAllOutdatedEntries);
        CaseSearchResultsView viewWithoutOutdatedEntry = new CaseSearchResultsView(dataWithoutOutdatedEntry);

        // then
        assertTrue(viewWithOutdatedEntry.isFoundCasesWithOutdatedDefendantsName());
        assertTrue(viewWithAllOutdatedEntries.isFoundCasesWithOutdatedDefendantsName());
        assertFalse(viewWithoutOutdatedEntry.isFoundCasesWithOutdatedDefendantsName());
    }

    private CaseSearchResult createCaseSearchResult() {
        CaseSearchResult caseSearchResult = new CaseSearchResult();
        final CaseSummary caseSummary = new CaseSummary();
        caseSummary.setPostingDate(now());
        caseSearchResult.setCaseSummary(caseSummary);
        return caseSearchResult;
    }
}
