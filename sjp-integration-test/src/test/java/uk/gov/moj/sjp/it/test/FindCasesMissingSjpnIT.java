package uk.gov.moj.sjp.it.test;


import static java.util.Collections.disjoint;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.sjp.it.helper.CasesMissingSjpnHelper.getCasesMissingSjpn;
import static uk.gov.moj.sjp.it.helper.CasesMissingSjpnHelper.getCasesMissingSjpnPostedDaysAgo;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonString;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FindCasesMissingSjpnIT extends BaseIntegrationTest {
    private List<CreateCase.CreateCasePayloadBuilder> sjpCases;
    private List<CreateCase.CreateCasePayloadBuilder> sjpCasesWithoutSjpn;
    private List<CreateCase.CreateCasePayloadBuilder> sjpCasesWithSjpn;
    private List<CaseDocumentHelper> sjpnDocuments;

    private List<CreateCase.CreateCasePayloadBuilder> sjpCasesYoungerThan3Days;
    private List<CreateCase.CreateCasePayloadBuilder> sjpCasesOlderThan3Days;
    private static final String NATIONAL_COURT_CODE = "1080";

    // TODO: close Helpers

    @Before
    public void init() {
        final LocalDate now = LocalDate.now();
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults();
        sjpCasesYoungerThan3Days = Arrays.asList(
                createCasePayloadBuilder.withPostingDate(now.minusDays(1)),
                CreateCase.CreateCasePayloadBuilder.withDefaults().withPostingDate(now.minusDays(2)),
                CreateCase.CreateCasePayloadBuilder.withDefaults().withPostingDate(now.minusDays(3))
        );
        sjpCasesOlderThan3Days = Arrays.asList(
                CreateCase.CreateCasePayloadBuilder.withDefaults().withPostingDate(now.minusDays(4)),
                CreateCase.CreateCasePayloadBuilder.withDefaults().withPostingDate(now.minusDays(5)),
                CreateCase.CreateCasePayloadBuilder.withDefaults().withPostingDate(now.minusDays(6))
        );

        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "TestRegion");

        sjpCases = Stream.concat(sjpCasesYoungerThan3Days.stream(), sjpCasesOlderThan3Days.stream()).collect(toList());

        sjpCasesWithSjpn = sjpCases.subList(0, 2);
        sjpCasesWithoutSjpn = sjpCases.subList(2, sjpCases.size());

        sjpnDocuments = sjpCasesWithSjpn.stream()
                .map(CreateCase.CreateCasePayloadBuilder::getId)
                .map(CaseDocumentHelper::new)
                .collect(toList());
    }

    @After
    public void tearDown() {
        sjpnDocuments.forEach(CaseDocumentHelper::close);
    }

    @Test
    public void findCasesMissingSjpn() {
        int casesMissingSjpnCount = getCasesMissingSjpn(USER_ID).getInt("count");
        int casesOlderThan3DaysMissingSjpnCount = getCasesMissingSjpnPostedDaysAgo(USER_ID, 3).getInt("count");
        int expectedCasesMissingSjpnCount = casesMissingSjpnCount + sjpCasesWithoutSjpn.size();
        int expectedCasesOlderThan3DaysMissingSjpnCount = casesOlderThan3DaysMissingSjpnCount + sjpCasesOlderThan3Days.size();

        createCasesAndDocuments();

        final JsonObject casesWithIds = getCasesMissingSjpn(USER_ID);
        final JsonObject casesWithoutIds = getCasesMissingSjpn(USER_ID, 0);
        final JsonObject casesOlderThan3DaysWithId = getCasesMissingSjpnPostedDaysAgo(USER_ID, 3);
        final JsonObject casesOlderThan3DaysWithoutId = getCasesMissingSjpnPostedDaysAgo(USER_ID, 3, 0);
        final List<UUID> actualCaseIds = extractCaseIds(casesWithIds);
        final List<UUID> actualCasesOlderThan3DaysIds = extractCaseIds(casesOlderThan3DaysWithId);

        assertTrue(actualCaseIds.containsAll(extractCaseIds(sjpCasesWithoutSjpn)));
        assertTrue(disjoint(actualCaseIds, extractCaseIds(sjpCasesWithSjpn)));
        assertThat(casesWithoutIds.getJsonArray("ids"), empty());
        assertThat(casesWithIds.getInt("count"), equalTo(expectedCasesMissingSjpnCount));
        assertThat(casesWithoutIds.getInt("count"), equalTo(expectedCasesMissingSjpnCount));

        assertTrue(actualCasesOlderThan3DaysIds.containsAll(extractCaseIds(sjpCasesOlderThan3Days)));
        assertTrue(disjoint(actualCasesOlderThan3DaysIds, extractCaseIds(sjpCasesWithSjpn)));
        assertTrue(disjoint(actualCasesOlderThan3DaysIds, extractCaseIds(sjpCasesYoungerThan3Days)));
        assertThat(casesOlderThan3DaysWithoutId.getJsonArray("ids"), empty());
        assertThat(casesOlderThan3DaysWithId.getInt("count"), equalTo(expectedCasesOlderThan3DaysMissingSjpnCount));
        assertThat(casesOlderThan3DaysWithoutId.getInt("count"), equalTo(expectedCasesOlderThan3DaysMissingSjpnCount));
    }

    private void createCasesAndDocuments() {
        sjpCases.forEach(CreateCase::createCaseForPayloadBuilder);
        sjpnDocuments.forEach(CaseDocumentHelper::addDocumentAndVerifyAdded);
    }


    private List<UUID> extractCaseIds(List<CreateCase.CreateCasePayloadBuilder> cases) {
        return cases.stream()
                .map(CreateCase.CreateCasePayloadBuilder::getId)
                .collect(toList());
    }

    private List<UUID> extractCaseIds(final JsonObject cases) {
        return cases.getJsonArray("ids").getValuesAs(JsonString.class).stream().map(JsonString::getString).map(UUID::fromString).collect(toList());
    }
}
