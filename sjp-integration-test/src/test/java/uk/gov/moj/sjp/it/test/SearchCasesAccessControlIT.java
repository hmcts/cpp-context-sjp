package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;

import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SearchCasesAccessControlIT extends BaseIntegrationTest {

    private final static UUID ALL_PROSECUTING_AUTHORITY_ACCESS_USER = randomUUID();
    private final static UUID NO_PROSECUTING_AUTHORITY_ACCESS_USER = randomUUID();
    private final static UUID PROSECUTING_AUTHORITY_1_ACCESS_USER = randomUUID();
    private final static ProsecutingAuthority PROSECUTING_AUTHORITY_1 = ProsecutingAuthority.TFL;
    private final static ProsecutingAuthority PROSECUTING_AUTHORITY_2 = ProsecutingAuthority.DVLA;
    private static CreateCase.CreateCasePayloadBuilder prosecutor1CasePayloadBuilder, prosecutor2CasePayloadBuilder;
    private static CaseSearchResultHelper caseSearchResultHelper;
    private static final String defendantLastName = "LAST_NAME_" + new StringGenerator().next();
    private static String companyName = "legalEntityName";

    @BeforeAll
    public static void setupCasesAndUsers() {
        stubForUserDetails(ALL_PROSECUTING_AUTHORITY_ACCESS_USER, "ALL");
        stubForUserDetails(PROSECUTING_AUTHORITY_1_ACCESS_USER, PROSECUTING_AUTHORITY_1.name());
        stubForUserDetails(NO_PROSECUTING_AUTHORITY_ACCESS_USER);

    }

    private static CreateCase.CreateCasePayloadBuilder createCaseForProsecutingAuthority(final ProsecutingAuthority prosecutingAuthority, final boolean companyIsDefendant) {
        final CreateCase.DefendantBuilder defendantBuilder = CreateCase.DefendantBuilder
                .withDefaults()
                .withLastName(defendantLastName);

        if (companyIsDefendant) {
            defendantBuilder.withLegalEntityName(companyName);
        }

        CreateCase.CreateCasePayloadBuilder prosecutorCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantBuilder(defendantBuilder);

        stubEnforcementAreaByPostcode(prosecutorCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        CreateCase.createCaseForPayloadBuilder(prosecutorCasePayloadBuilder);

        return prosecutorCasePayloadBuilder;
    }

    @Test
    public void shouldOnlyReturnCasesForTheUsersProsecutingAuthorityWhenSearchingByName() {
        prosecutor1CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_1, false);
        prosecutor2CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_2, false);
        // As s single Prosecuting Authority User
        caseSearchResultHelper = new CaseSearchResultHelper(PROSECUTING_AUTHORITY_1_ACCESS_USER);

        // When I Search by last name

        // Then I should see cases for my Prosecuting Authority in search results
        prosecutor1CasePayloadBuilder.getDefendantBuilder().withLegalEntityName(null);
        caseSearchResultHelper.verifyPersonFound(prosecutor1CasePayloadBuilder.getUrn(), defendantLastName);

        // And I should not see cases for any other Prosecuting Authority in search results
        caseSearchResultHelper.verifyPersonNotFound(prosecutor2CasePayloadBuilder.getUrn(), defendantLastName);
    }

    @Test
    public void shouldOnlyReturnCasesForTheUsersProsecutingAuthorityWhenSearchingByUrn() {
        prosecutor1CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_1, false);
        prosecutor2CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_2, false);
        // As a single Prosecuting Authority 1 User
        caseSearchResultHelper = new CaseSearchResultHelper(PROSECUTING_AUTHORITY_1_ACCESS_USER);

        // When I Search by URN for a case created by my Prosecuting Authority
        // Then I should see the case in search results
        caseSearchResultHelper.verifyUrnFound(prosecutor1CasePayloadBuilder.getUrn());

        // When I Search by URN a case created by another Prosecuting Authority
        // Then I should see empty search results
        caseSearchResultHelper.verifyUrnNotFound(prosecutor2CasePayloadBuilder.getUrn());
    }

    @Test
    public void shouldReturnAllCasesForAllProsecutingAuthorityAccessUsersWhenSearchingByName() {
        prosecutor1CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_1, false);
        prosecutor2CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_2, false);
        // As a All Prosecuting Authorities User
        caseSearchResultHelper = new CaseSearchResultHelper(ALL_PROSECUTING_AUTHORITY_ACCESS_USER);

        prosecutor1CasePayloadBuilder.getDefendantBuilder().withLegalEntityName(null);
        // When I Search by last name for a case
        // Then I should see all cases in search results with that last name
        caseSearchResultHelper.verifyPersonFound(prosecutor1CasePayloadBuilder.getUrn(), defendantLastName);
        caseSearchResultHelper.verifyPersonFound(prosecutor2CasePayloadBuilder.getUrn(), defendantLastName);
    }

    @Test
    public void shouldReturnNoCasesForNoProsecutingAuthorityAccessUsersWhenSearchingByURN() {
        prosecutor1CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_1, false);
        prosecutor2CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_2, false);
        // As a No Prosecuting Authorities User
        caseSearchResultHelper = new CaseSearchResultHelper(NO_PROSECUTING_AUTHORITY_ACCESS_USER);

        // When I Search by URN for a case
        // Then I should see no cases in search results
        caseSearchResultHelper.verifyUrnNotFound(prosecutor1CasePayloadBuilder.getUrn());
        caseSearchResultHelper.verifyUrnNotFound(prosecutor2CasePayloadBuilder.getUrn());
    }

    @Test
    public void shouldReturnNoCasesForNoProsecutingAuthorityAccessUsersWhenSearchingByName() {
        prosecutor1CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_1, false);
        prosecutor2CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_2, false);
        // As a No Prosecuting Authorities User
        caseSearchResultHelper = new CaseSearchResultHelper(NO_PROSECUTING_AUTHORITY_ACCESS_USER);

        // When I Search by last name for a case
        // Then I should see no cases in search results with that last name
        caseSearchResultHelper.verifyPersonNotFound(prosecutor1CasePayloadBuilder.getUrn(), defendantLastName);
        caseSearchResultHelper.verifyPersonNotFound(prosecutor2CasePayloadBuilder.getUrn(), defendantLastName);
    }

    @Test
    public void shouldReturnAllCasesForAllProsecutingAuthorityAccessUsersWhenSearchingByUrn() {
        prosecutor1CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_1, false);
        prosecutor2CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_2, false);
        // As a All Prosecuting Authorities User
        caseSearchResultHelper = new CaseSearchResultHelper(ALL_PROSECUTING_AUTHORITY_ACCESS_USER);

        // When I Search by URN for a case
        // Then I should see any case in search results
        caseSearchResultHelper.verifyUrnFound(prosecutor1CasePayloadBuilder.getUrn());
        caseSearchResultHelper.verifyUrnFound(prosecutor2CasePayloadBuilder.getUrn());
    }

    @Test
    public void shouldOnlyReturnCasesForTheUsersProsecutingAuthorityWhenSearchingByCompanyName() {
        prosecutor1CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_1, true);
        prosecutor2CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_2, true);
        // As s single Prosecuting Authority User
        caseSearchResultHelper = new CaseSearchResultHelper(ALL_PROSECUTING_AUTHORITY_ACCESS_USER);

        // When I Search by legal entity name
        prosecutor1CasePayloadBuilder.getDefendantBuilder().withLegalEntityName(companyName);
        // Then I should see cases for my Prosecuting Authority in search results
        caseSearchResultHelper.verifyPersonFound(prosecutor1CasePayloadBuilder.getUrn(), companyName);
        companyName = "NotFoundCompany";

        // And I should not see cases for any other Prosecuting Authority in search results
        caseSearchResultHelper.verifyPersonNotFound(prosecutor2CasePayloadBuilder.getUrn(), companyName);
    }
}
