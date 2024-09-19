package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.moj.cpp.sjp.domain.decision.ReferredToOpenCourt;

import java.time.ZonedDateTime;
import java.util.Collections;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferredToOpenCourtDecisionResultAggregatorTest extends  BaseDecisionResultAggregatorTest {

    private ReferredToOpenCourtDecisionResultAggregator aggregator;

    @BeforeEach
    public void setUp() {
        super.setUp();
        aggregator = new ReferredToOpenCourtDecisionResultAggregator(jCachedReferenceData);
    }

    @Test
    public void shouldPopulateBasicResultWithRightPrompts() {
        final ReferredToOpenCourt offenceDecision
                = new ReferredToOpenCourt(null,
                Collections.singletonList(createOffenceDecisionInformation(offence1Id, FOUND_GUILTY)),
                "South West London Magistrates' Court",
                25,
                ZonedDateTime.parse("2018-11-02T09:30:00.000Z"),
                "For a case management hearing (no appearance)",
                "Lavender Hill Magistrates' Court");

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(1));


        assertThat(resultsAggregate.getResults(offence1Id),
                allOf(hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("3d2c05b3-fcd6-49c2-b5a9-52855be7f90a"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),


                        hasProperty("resultText", startsWith("Summons on referral to other court\n" +
                                "Date of hearing 02/11/2018\nMagistrates' court Lavender Hill Magistrates' Court\n" +
                                "Time of hearing 09:30 ")),
                        hasProperty("resultText", equalToIgnoringCase("Summons on referral to other court\n" +
                                "Date of hearing 02/11/2018\nMagistrates' court Lavender Hill Magistrates' Court\n" +
                                "Time of hearing 09:30 AM\n" +
                                "Reason for referring to court For a case management hearing (no appearance)")),
                        hasProperty("resultText", endsWith("\n" +
                                "Reason for referring to court For a case management hearing (no appearance)")),
                        hasProperty("judicialResultPrompts", allOf(
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("a528bbfd-54b8-45d6-bad0-26a3a95eb274"))),
                                        hasProperty("value", Matchers.is("02/11/2018")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("f5699b34-f32f-466e-b7d8-40b4173df154"))),
                                        hasProperty("value", Matchers.is("Lavender Hill Magistrates' Court")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("4d125a5a-acbc-461d-a657-ba5643af85a6"))),
                                        hasProperty("value", equalToIgnoringCase("09:30 AM")))
                                ),
                                hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("dbbb47c9-2202-4913-9a0d-db0a048bfd5f"))),
                                        hasProperty("value", Matchers.is("For a case management hearing (no appearance)")))
                                )))))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)), is(true));
    }
}
