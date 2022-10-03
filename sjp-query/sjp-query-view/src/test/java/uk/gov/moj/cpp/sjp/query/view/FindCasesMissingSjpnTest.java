package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.Mockito.when;
import static uk.gov.justice.json.schemas.domains.sjp.Gender.MALE;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.query.view.response.CaseSummaryView;
import uk.gov.moj.cpp.sjp.query.view.response.CasesMissingSjpnView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;
import uk.gov.moj.cpp.sjp.query.view.util.builders.CaseDetailEntityBuilder;
import uk.gov.moj.cpp.sjp.query.view.util.builders.DefendantEntityBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private static final String LEGAL_ENTITY_NAME = "Samba Ltd";
    private static final String DEFENDANT_FIRST_NAME = "First name";
    private static final String TITLE = "Mr";
    private static final String DEFENDANT_LAST_NAME = "Last name";
    private static final LocalDate DATE_OF_BIRTH = now().minusYears(39);
    private static final String NATIONAL_INSURANCE_NUMBER = "SZ54321";
    private static final String DRIVER_NUMBER = "MORGA753116SM9IJ";
    private static final String PERSON_URN = "PERSON";
    private static final String COMPANY_URN = "COMPANY";
    private static final String PROSECUTOR_TFL = "TFL";
    private static final LocalDate POSTING_DATE = now();

    private static final int COUNT = 1234;
    private final CasesMissingSjpnView resultView = newResultView();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private CaseService caseService;

    @InjectMocks
    private SjpQueryView sjpQueryView;

    private static CasesMissingSjpnView newResultView() {
        final List<CaseSummaryView> cases = new ArrayList<>();

        cases.add(new CaseSummaryView(CaseDetailEntityBuilder
                .withDefaults()
                .withDefendantDetail(DefendantEntityBuilder.withDefaults()
                        .withPersonalDetails(buildPersonalDetails())
                        .build())
                .withUrn(PERSON_URN)
                .withProsecutionAuthority(PROSECUTOR_TFL)
                .withPostingDate(POSTING_DATE)
                .build()));

        cases.add(new CaseSummaryView(CaseDetailEntityBuilder
                .withDefaults()
                .withDefendantDetail(DefendantEntityBuilder.withDefaults()
                        .withLegalEntityDetails(buildLegalEntityDetails())
                        .build())
                .withUrn(COMPANY_URN)
                .withProsecutionAuthority(PROSECUTOR_TFL)
                .withPostingDate(POSTING_DATE)
                .build()));

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
                withJsonPath("$.cases[0].urn", is(PERSON_URN)),
                withJsonPath("$.cases[0].defendant.firstName", is(DEFENDANT_FIRST_NAME)),
                withJsonPath("$.cases[0].defendant.lastName", is(DEFENDANT_LAST_NAME)),
                withJsonPath("$.cases[0].defendant.dateOfBirth", is(DATE_OF_BIRTH.toString())),
                withJsonPath("$.cases[0].defendant.gender", is(MALE.toString())),
                withJsonPath("$.cases[0].defendant.nationalInsuranceNumber", is(NATIONAL_INSURANCE_NUMBER)),
                withJsonPath("$.cases[0].defendant.driverNumber", is(DRIVER_NUMBER)),
                withJsonPath("$.cases[0].postingDate", is(POSTING_DATE.toString())),
                withJsonPath("$.cases[0].prosecutingAuthority", is(PROSECUTOR_TFL)),
                withJsonPath("$.cases[1].defendant.legalEntityName", is(LEGAL_ENTITY_NAME)),
                withJsonPath("$.cases[1].urn", is(COMPANY_URN)),
                withJsonPath("$.cases[1].postingDate", is(POSTING_DATE.toString())),
                withJsonPath("$.cases[1].prosecutingAuthority", is(PROSECUTOR_TFL))
        ))).thatMatchesSchema());
    }

    private static PersonalDetails buildPersonalDetails() {
        return new PersonalDetails(
                TITLE,
                DEFENDANT_FIRST_NAME,
                DEFENDANT_LAST_NAME,
                DATE_OF_BIRTH,
                MALE,
                NATIONAL_INSURANCE_NUMBER,
                DRIVER_NUMBER,
                null);
    }

    private static LegalEntityDetails buildLegalEntityDetails() {
        return new LegalEntityDetails(
                LEGAL_ENTITY_NAME);
    }
}
