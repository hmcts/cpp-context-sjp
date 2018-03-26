package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;

import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseSearchResultHelper;

import org.junit.BeforeClass;
import org.junit.Test;

public class SearchCasesAccessControlIT extends BaseIntegrationTest {

    private final static String ALL_PROSECUTING_AUTHORITY_ACCESS_USER = randomUUID().toString();
    private final static String NO_PROSECUTING_AUTHORITY_ACCESS_USER = randomUUID().toString();
    private final static String PROSECUTING_AUTHORITY_1_ACCESS_USER = randomUUID().toString();

    private static CreateCase.CreateCasePayloadBuilder prosecutor1CasePayloadBuilder, prosecutor2CasePayloadBuilder;
    private static CaseSearchResultHelper caseSearchResultHelper;
    private static String defendantLastName = "LASTNAME" + new StringGenerator().next();

    @BeforeClass
    public static void setupCasesAndUsers() {

        final ProsecutingAuthority PROSECUTING_AUTHORITY_1 = ProsecutingAuthority.TFL;
        final ProsecutingAuthority PROSECUTING_AUTHORITY_2 = ProsecutingAuthority.DVLA;

        prosecutor1CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_1);
        prosecutor2CasePayloadBuilder = createCaseForProsecutingAuthority(PROSECUTING_AUTHORITY_2);

        stubForUserDetails(ALL_PROSECUTING_AUTHORITY_ACCESS_USER, "ALL");
        stubForUserDetails(PROSECUTING_AUTHORITY_1_ACCESS_USER, PROSECUTING_AUTHORITY_1.name());
        stubForUserDetails(NO_PROSECUTING_AUTHORITY_ACCESS_USER);
    }

    private static CreateCase.CreateCasePayloadBuilder createCaseForProsecutingAuthority(final ProsecutingAuthority prosecutingAuthority) {

        final CreateCase.DefendantBuilder defendantBuilder = CreateCase.DefendantBuilder
                .withDefaults()
                .withLastName(defendantLastName);

        CreateCase.CreateCasePayloadBuilder prosecutorCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder
                .withDefaults()
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantBuilder(defendantBuilder);
        CreateCase.createCaseForPayloadBuilder(prosecutorCasePayloadBuilder);

        return prosecutorCasePayloadBuilder;
    }

    @Test
    public void shouldOnlyReturnCasesForTheUsersProsecutingAuthorityWhenSearchingByName() {

        // As s single Prosecuting Authority User
        caseSearchResultHelper = new CaseSearchResultHelper(PROSECUTING_AUTHORITY_1_ACCESS_USER);

        // When I Search by last name

        // Then I should see cases for my Prosecuting Authority in search results
        caseSearchResultHelper.verifyPersonFound(prosecutor1CasePayloadBuilder.getUrn(), defendantLastName);

        // And I should not see cases for any other Prosecuting Authority in search results
        caseSearchResultHelper.verifyPersonNotFound(prosecutor2CasePayloadBuilder.getUrn(), defendantLastName);
    }

    @Test
    public void shouldOnlyReturnCasesForTheUsersProsecutingAuthorityWhenSearchingByUrn() {

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

        // As a All Prosecuting Authorities User
        caseSearchResultHelper = new CaseSearchResultHelper(ALL_PROSECUTING_AUTHORITY_ACCESS_USER);

        // When I Search by last name for a case
        // Then I should see all cases in search results with that last name
        caseSearchResultHelper.verifyPersonFound(prosecutor1CasePayloadBuilder.getUrn(), defendantLastName);
        caseSearchResultHelper.verifyPersonFound(prosecutor2CasePayloadBuilder.getUrn(), defendantLastName);
    }

    @Test
    public void shouldReturnNoCasesForNoProsecutingAuthorityAccessUsersWhenSearchingByURN() {

        // As a No Prosecuting Authorities User
        caseSearchResultHelper = new CaseSearchResultHelper(NO_PROSECUTING_AUTHORITY_ACCESS_USER);

        // When I Search by URN for a case
        // Then I should see no cases in search results
        caseSearchResultHelper.verifyUrnNotFound(prosecutor1CasePayloadBuilder.getUrn());
        caseSearchResultHelper.verifyUrnNotFound(prosecutor2CasePayloadBuilder.getUrn());
    }

    @Test
    public void shouldReturnNoCasesForNoProsecutingAuthorityAccessUsersWhenSearchingByName() {

        // As a No Prosecuting Authorities User
        caseSearchResultHelper = new CaseSearchResultHelper(NO_PROSECUTING_AUTHORITY_ACCESS_USER);

        // When I Search by last name for a case
        // Then I should see no cases in search results with that last name
        caseSearchResultHelper.verifyPersonNotFound(prosecutor1CasePayloadBuilder.getUrn(), defendantLastName);
        caseSearchResultHelper.verifyPersonNotFound(prosecutor2CasePayloadBuilder.getUrn(), defendantLastName);
    }

    @Test
    public void shouldReturnAllCasesForAllProsecutingAuthorityAccessUsersWhenSearchingByUrn() {

        // As a All Prosecuting Authorities User
        caseSearchResultHelper = new CaseSearchResultHelper(ALL_PROSECUTING_AUTHORITY_ACCESS_USER);

        // When I Search by URN for a case
        // Then I should see any case in search results
        caseSearchResultHelper.verifyUrnFound(prosecutor1CasePayloadBuilder.getUrn());
        caseSearchResultHelper.verifyUrnFound(prosecutor2CasePayloadBuilder.getUrn());
    }
}
