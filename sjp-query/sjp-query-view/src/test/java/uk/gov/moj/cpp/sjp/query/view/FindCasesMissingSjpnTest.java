package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.query.view.response.CasesMissingSjpnView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FindCasesMissingSjpnTest {

    private static final String QUERY_NAME = "sjp.query.cases-missing-sjpn";
    private static final int COUNT = 1234;
    private final CasesMissingSjpnView resultView = new CasesMissingSjpnView(Arrays.asList("1", "2"), COUNT);

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private CaseService caseService;

    @InjectMocks
    private SjpQueryView sjpQueryView;

    @Test
    public void shouldFindAllCasesMissingSjpn() {
        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME)).build();

        when(caseService.findCasesMissingSjpn(empty(), empty())).thenReturn(resultView);

        verifyResponse(sjpQueryView.findCasesMissingSjpn(query));
    }

    @Test
    public void shouldFindCasesMissingSjpnWithCasesCountLimit() {
        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME)).withPayloadOf(2, "limit").build();

        when(caseService.findCasesMissingSjpn(Optional.of(2), empty())).thenReturn(resultView);

        verifyResponse(sjpQueryView.findCasesMissingSjpn(query));
    }

    @Test
    public void shouldFindCasesMissingSjpnWithPostingDateLimit() {
        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME)).withPayloadOf(3, "daysSincePosting").build();

        when(caseService.findCasesMissingSjpn(empty(), Optional.of(now().minusDays(3)))).thenReturn(resultView);

        verifyResponse(sjpQueryView.findCasesMissingSjpn(query));
    }

    @Test
    public void shouldFindCasesMissingSjpnWithCaseCountAndPostingDateLimits() {
        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME)).withPayloadOf(2, "limit").withPayloadOf(3, "daysSincePosting").build();

        when(caseService.findCasesMissingSjpn(Optional.of(2), Optional.of(now().minusDays(3)))).thenReturn(resultView);

        verifyResponse(sjpQueryView.findCasesMissingSjpn(query));
    }


    private void verifyResponse(final JsonEnvelope responseEnvelope) {
        assertThat(responseEnvelope, jsonEnvelope(metadata().withName(QUERY_NAME), payload().isJson(allOf(
                withJsonPath("$.count", is(COUNT)),
                withJsonPath("$.ids", containsInAnyOrder("1", "2"))
        ))).thatMatchesSchema());
    }
}
