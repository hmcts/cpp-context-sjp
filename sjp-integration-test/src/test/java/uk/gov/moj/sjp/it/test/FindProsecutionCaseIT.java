package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.json.schemas.domains.sjp.Language.W;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.ProsecutionCaseFileServiceStub.stubCaseDetails;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEthnicities;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffencesByCode;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;

import uk.gov.justice.json.schemas.domains.sjp.Language;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.util.UUID;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
@SuppressWarnings({"squid:S1607","squid:S2699"})
public class FindProsecutionCaseIT extends BaseIntegrationTest {

    private SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();

    private final UUID caseId = randomUUID();
    private final UUID offenceId1 = randomUUID();
    private final UUID defendantId = randomUUID();
    private final String caseUrn = generate(TFL);

    private static final String LIBRA_OFFENCE_CODE1 = "PS00001";
    private static final String NATIONAL_INSURANCE_NUMBER = "BB333333B";
    private UUID prosecutorId = randomUUID();

    @Before
    public void setUp() throws Exception {
        databaseCleaner.cleanViewStore();

        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
        stubProsecutorQuery(TFL.name(), TFL.getFullName(), prosecutorId);
        stubQueryOffencesByCode(LIBRA_OFFENCE_CODE1);
        stubCountryNationalities("stub-data/referencedata.query.country-nationality.json");
        stubEthnicities("stub-data/referencedata.query.ethnicities.json");
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details-welsh.json");


        createCaseWithSingleOffence();

    }

    @Test
    @Ignore
    public void shouldFindProsecutionCase() {
        CasePoller.pollUntilProsecutionCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.id", is(caseId.toString())),
                withJsonPath("$.prosecutionCaseIdentifier.prosecutionAuthorityId", is(prosecutorId.toString())),
                withJsonPath("$.prosecutionCaseIdentifier.prosecutionAuthorityCode", is("TFL")),
                withJsonPath("$.prosecutionCaseIdentifier.prosecutionAuthorityReference", is(caseUrn)),
                withJsonPath("$.defendants[0].id", is(defendantId.toString())),
                withJsonPath("$.defendants[0].masterDefendantId", is(defendantId.toString())),
                withJsonPath("$.defendants[0].prosecutionCaseId", is(caseId.toString())),
                withJsonPath("$.defendants[0].personDefendant.personDetails.firstName", is("David")),
                withJsonPath("$.defendants[0].personDefendant.personDetails.lastName", is("LLOYD")),
                withJsonPath("$.defendants[0].personDefendant.personDetails.nationalInsuranceNumber", is(NATIONAL_INSURANCE_NUMBER)),
                withJsonPath("$.defendants[0].offences[0].id", is(offenceId1.toString()))
        ));
    }

    private void createCaseWithSingleOffence() {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(TFL)
                .withOffenceBuilder(CreateCase.OffenceBuilder.withDefaults()
                        .withId(offenceId1)
                        .withLibraOffenceCode(LIBRA_OFFENCE_CODE1))
                .withDefendantBuilder(CreateCase.DefendantBuilder.withDefaults()
                        .withId(defendantId)
                        .withNationalInsuranceNumber(NATIONAL_INSURANCE_NUMBER)
                        .withHearingLanguage(W))
                .withId(caseId)
                .withUrn(caseUrn);
        ;

        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder))
                .popEvent(CaseMarkedReadyForDecision.EVENT_NAME);
    }

}
