package uk.gov.moj.sjp.it.test;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static uk.gov.moj.sjp.it.helper.CasesMissingSjpnHelper.getCasesMissingSjpn;
import static uk.gov.moj.sjp.it.helper.CasesMissingSjpnHelper.getCasesMissingSjpnPostedDaysAgo;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;

import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

public class FindCasesMissingSjpnAccessControlIT extends BaseIntegrationTest {

    private final static UUID COURT_ADMIN_USER_ID = randomUUID();
    private final static UUID TFL_USER_ID = randomUUID();

    private static CasesMissingSjpnMetrics beforeTflMetrics, beforeCourtAdminMetrics;

    @BeforeClass
    public static void setupCasesAndUsers() {

        setupUsers();
        collectBeforeMetrics();
        createCases();
    }

    private static void setupUsers() {

        stubForUserDetails(COURT_ADMIN_USER_ID, "ALL");
        stubForUserDetails(TFL_USER_ID, ProsecutingAuthority.TFL.name());
    }

    private static void collectBeforeMetrics() {

        beforeCourtAdminMetrics = collectMetrics(COURT_ADMIN_USER_ID);
        beforeTflMetrics = collectMetrics(TFL_USER_ID);
    }

    private static void createCases() {

        final LocalDate now = LocalDate.now();

        final List<CreateCase.CreateCasePayloadBuilder> allNewCases = Arrays.asList(
                CreateCase.CreateCasePayloadBuilder
                        .withDefaults()
                        .withProsecutingAuthority(ProsecutingAuthority.TFL)
                        .withPostingDate(now),
                CreateCase.CreateCasePayloadBuilder
                        .withDefaults()
                        .withProsecutingAuthority(ProsecutingAuthority.TFL)
                        .withPostingDate(now.minusDays(4)),
                CreateCase.CreateCasePayloadBuilder
                        .withDefaults()
                        .withProsecutingAuthority(ProsecutingAuthority.DVLA)
                        .withPostingDate(now),
                CreateCase.CreateCasePayloadBuilder
                        .withDefaults()
                        .withProsecutingAuthority(ProsecutingAuthority.DVLA)
                        .withPostingDate(now.minusDays(4))
        );

        allNewCases.forEach(CreateCase::createCaseForPayloadBuilder);

        allNewCases.forEach(c -> CasePoller.pollUntilCaseByIdIsOk(c.getId()));
    }

    private static CasesMissingSjpnMetrics collectMetrics(UUID userId) {

        CasesMissingSjpnMetrics casesMissingSjpnMetrics = new CasesMissingSjpnMetrics();

        casesMissingSjpnMetrics.withIdsCount = getCasesMissingSjpn(userId).getInt("count");
        casesMissingSjpnMetrics.withoutIdsCount = getCasesMissingSjpn(userId, 0).getInt("count");
        casesMissingSjpnMetrics.withIdsOlderThan3Days = getCasesMissingSjpnPostedDaysAgo(userId, 3).getInt("count");
        casesMissingSjpnMetrics.withoutIdsOlderThan3Days = getCasesMissingSjpnPostedDaysAgo(userId, 3, 0).getInt("count");

        return casesMissingSjpnMetrics;
    }

    @Test
    public void shouldOnlyReturnCasesMissingSJPNsForOwningProsecutingAuthority() {

        CasesMissingSjpnMetrics tflMetrics = collectMetrics(TFL_USER_ID);

        assertEquals(tflMetrics.withIdsCount, beforeTflMetrics.withIdsCount + 2);
        assertEquals(tflMetrics.withoutIdsCount, beforeTflMetrics.withoutIdsCount + 2);
        assertEquals(tflMetrics.withIdsOlderThan3Days, beforeTflMetrics.withIdsOlderThan3Days + 1);
        assertEquals(tflMetrics.withoutIdsOlderThan3Days, beforeTflMetrics.withoutIdsOlderThan3Days + 1);
    }

    @Test
    public void shouldReturnAllCasesMissingSJPNsForCourtAdminUsers() {

        CasesMissingSjpnMetrics courtAdminMetrics = collectMetrics(COURT_ADMIN_USER_ID);

        assertEquals(courtAdminMetrics.withIdsCount, beforeCourtAdminMetrics.withIdsCount + 4);
        assertEquals(courtAdminMetrics.withoutIdsCount, beforeCourtAdminMetrics.withoutIdsCount + 4);
        assertEquals(courtAdminMetrics.withIdsOlderThan3Days, beforeCourtAdminMetrics.withIdsOlderThan3Days + 2);
        assertEquals(courtAdminMetrics.withoutIdsOlderThan3Days, beforeCourtAdminMetrics.withoutIdsOlderThan3Days + 2);
    }

    private static class CasesMissingSjpnMetrics {
        int withIdsCount;
        int withoutIdsCount;
        int withIdsOlderThan3Days;
        int withoutIdsOlderThan3Days;
    }
}
