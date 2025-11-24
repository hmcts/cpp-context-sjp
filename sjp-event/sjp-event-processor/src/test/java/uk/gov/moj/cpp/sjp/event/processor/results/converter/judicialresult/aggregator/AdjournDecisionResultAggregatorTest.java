package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.CourtCentreConverter;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AdjournDecisionResultAggregatorTest extends BaseDecisionResultAggregatorTest {

    private static final String ADJOURN_REASON = "Not enough documents present for decision, waiting for document";
    private AdjournDecisionResultAggregator aggregator;

    @Mock
    private CourtCentreConverter courtCentreConverter;

    private CourtCentre courtCentre = courtCentre().build();

    @BeforeEach
    public void setUp() {
        super.setUp();
        aggregator = new AdjournDecisionResultAggregator(jCachedReferenceData);
        setField(aggregator, "courtCentreConverter", courtCentreConverter);

        when(courtCentreConverter.convertByOffenceId(any(), any())).thenReturn(Optional.of(courtCentre));

    }

    @Test
    public void shouldPopulateResultWithRightPrompts() {
        final LocalDate adjournTo = LocalDate.parse("2021-02-07").plusDays(10);
        final Adjourn offenceDecision
                = new Adjourn(null, Collections.singletonList(createOffenceDecisionInformation(offence1Id, NO_VERDICT)), ADJOURN_REASON, adjournTo);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn);
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)),Matchers.is(nullValue()));
        assertThat(resultsAggregate.getResults(offence1Id),
                allOf(hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("f7784e82-20b5-4d2c-b174-6fd57ebf8d7c"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Adjourn to a later SJP hearing session\nAdjourn to date 17/02/2021")),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("185e6a04-8b44-430d-8073-d8d12f69733a"))),
                                hasProperty("value", Matchers.is("17/02/2021")))
                        )))))));
    }

}
