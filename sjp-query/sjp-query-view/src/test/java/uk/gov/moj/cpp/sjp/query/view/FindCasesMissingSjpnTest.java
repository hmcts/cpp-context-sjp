package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.query.view.response.CaseSummaryView;
import uk.gov.moj.cpp.sjp.query.view.response.CasesMissingSjpnView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private final CasesMissingSjpnView resultView = newResultView();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private CaseService caseService;

    @InjectMocks
    private SjpQueryView sjpQueryView;

    private static CasesMissingSjpnView newResultView(){
        final List<CaseSummaryView> cases = new ArrayList<>();
        cases.add(new CaseSummaryView(new CaseDetail(
                UUID.randomUUID(), "AAA", "1",
                ProsecutingAuthority.TFL, false, null, ZonedDateTime.now(),
                new DefendantDetail(UUID.randomUUID(),
                        new PersonalDetails("Mr", "Defen", "Dant",
                                LocalDate.of(1970, 3,1), Gender.MALE,
                                "54321", null, null, null),
                        null, 2),
                new BigDecimal(100), LocalDate.now())));
        cases.add(new CaseSummaryView(new CaseDetail(
                UUID.randomUUID(), "BBB", "1",
                ProsecutingAuthority.TFL, false, null, ZonedDateTime.now(),
                new DefendantDetail(UUID.randomUUID(),
                        new PersonalDetails("Mr", "Defen", "Dant",
                                LocalDate.of(1970, 2,1), Gender.MALE,
                                "12345", null, null, null),
                        null, 2),
                new BigDecimal(100), LocalDate.now())));
        return new CasesMissingSjpnView(Arrays.asList("1", "2"), cases, COUNT);
    }

    @Test
    public void shouldFindAllCasesMissingSjpn() {
        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME)).build();

        when(caseService.findCasesMissingSjpn(query, empty(), empty())).thenReturn(resultView);

        verifyResponse(sjpQueryView.findCasesMissingSjpn(query));
    }

    @Test
    public void shouldFindCasesMissingSjpnWithCasesCountLimit() {
        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME)).withPayloadOf(2, "limit").build();

        when(caseService.findCasesMissingSjpn(query, Optional.of(2), empty())).thenReturn(resultView);

        verifyResponse(sjpQueryView.findCasesMissingSjpn(query));
    }

    @Test
    public void shouldFindCasesMissingSjpnWithPostingDateLimit() {
        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME)).withPayloadOf(3, "daysSincePosting").build();

        when(caseService.findCasesMissingSjpn(query, empty(), Optional.of(now().minusDays(3)))).thenReturn(resultView);

        verifyResponse(sjpQueryView.findCasesMissingSjpn(query));
    }

    @Test
    public void shouldFindCasesMissingSjpnWithCaseCountAndPostingDateLimits() {
        final JsonEnvelope query = envelope().with(metadataWithRandomUUID(QUERY_NAME)).withPayloadOf(2, "limit").withPayloadOf(3, "daysSincePosting").build();

        when(caseService.findCasesMissingSjpn(query, Optional.of(2), Optional.of(now().minusDays(3)))).thenReturn(resultView);

        verifyResponse(sjpQueryView.findCasesMissingSjpn(query));
    }


    private void verifyResponse(final JsonEnvelope responseEnvelope) {
        assertThat(responseEnvelope, jsonEnvelope(metadata().withName(QUERY_NAME), payload().isJson(allOf(
                withJsonPath("$.count", is(COUNT)),
                withJsonPath("$.ids", containsInAnyOrder("1", "2")),
                withJsonPath("$.cases[0].urn", is("AAA")),
                withJsonPath("$.cases[0].defendant.firstName", is("Defen")),
                withJsonPath("$.cases[1].defendant.firstName", is("Defen")),
                withJsonPath("$.cases[1].urn", is("BBB"))
        ))).thatMatchesSchema());
    }
}
