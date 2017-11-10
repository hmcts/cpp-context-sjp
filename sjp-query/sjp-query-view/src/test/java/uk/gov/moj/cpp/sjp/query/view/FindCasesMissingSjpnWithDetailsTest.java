package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
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
import uk.gov.moj.cpp.sjp.query.view.response.CaseMissingSjpnWithDetailsView;
import uk.gov.moj.cpp.sjp.query.view.response.CasesMissingSjpnWithDetailsView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.matchers.JsonPathMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FindCasesMissingSjpnWithDetailsTest {

    private static final String QUERY_NAME = "sjp.query.cases-missing-sjpn-with-details";
    private static final int COUNT = 2;
    private static final CaseMissingSjpnWithDetailsView case1 = new CaseMissingSjpnWithDetailsView(UUID.randomUUID(), "TFL0538921682", LocalDate.now(), "Bob", "Wass");
    private static final CaseMissingSjpnWithDetailsView case2 = new CaseMissingSjpnWithDetailsView(UUID.randomUUID(), "TFL0538215059", LocalDate.now(), "Bob", "Graw");
    private static final List<CaseMissingSjpnWithDetailsView> cases = Arrays.asList(case1, case2);
    private final CasesMissingSjpnWithDetailsView resultView = new CasesMissingSjpnWithDetailsView(cases, COUNT);

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private CaseService caseService;

    @InjectMocks
    private SjpQueryView sjpQueryView;

    @Test
    public void shouldFindAllCasesMissingSjpnWithDetails() {
        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME)).build();
        when(caseService.findCasesMissingSjpnWithDetails(empty())).thenReturn(resultView);
        verifyResponse(sjpQueryView.findCasesMissingSjpnWithDetails(query));
    }

    private void verifyResponse(final JsonEnvelope responseEnvelope) {
        assertThat(responseEnvelope, jsonEnvelope(metadata().withName(QUERY_NAME), payload().isJson(allOf(
                withJsonPath("$.count", is(COUNT)),
                JsonPathMatchers.withJsonPath("$.cases[0].urn", is("TFL0538921682"))
        ))).thatMatchesSchema());
    }


}
