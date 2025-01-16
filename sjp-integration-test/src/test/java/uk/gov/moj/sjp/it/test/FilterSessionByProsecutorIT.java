package uk.gov.moj.sjp.it.test;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.sjp.it.command.AddDatesToAvoid.addDatesToAvoid;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseAssignedToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseNotAssignedToUser;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.requestSetPleasAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanAll;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.commandclient.AssignNextCaseClient;

import java.time.LocalDate;
import java.util.UUID;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FilterSessionByProsecutorIT extends BaseIntegrationTest {

    private static final String DATE_TO_AVOID = "a-date-to-avoid";

    @BeforeEach
    public void setUp() throws Exception {
        cleanAll();

        stubDefaultCourtByCourtHouseOUCodeQuery();

        stubProsecutorQuery(TFL.name(), TFL.getFullName(), randomUUID());
        stubProsecutorQuery(TVL.name(), TVL.getFullName(), randomUUID());
        stubProsecutorQuery(DVLA.name(), DVLA.getFullName(), randomUUID());

        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        final String defaultPostcodeUsed = withDefaults().getDefendantBuilder().getAddressBuilder().getPostcode();
        stubEnforcementAreaByPostcode(defaultPostcodeUsed, "1080", "BedfordShire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");
    }

    @Test
    public void shouldAssignOnlyTFLCasesForProsecutorsForMagistrate() {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        final CreateCase.CreateCasePayloadBuilder tflPleadedGuiltyCasePayloadBuilder = withDefaults().withPostingDate(daysAgo(10));
        stubGetEmptyAssignmentsByDomainObjectId(tflPleadedGuiltyCasePayloadBuilder.getId());
        createCaseForPayloadBuilder(tflPleadedGuiltyCasePayloadBuilder);

        // pleaded guilty case
        requestSetPleasAndConfirm(tflPleadedGuiltyCasePayloadBuilder.getId(),
                true,
                false,
                true,
                null,
                false,
                null,
                singletonList(Triple.of(tflPleadedGuiltyCasePayloadBuilder.getOffenceId(),
                        tflPleadedGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), GUILTY)));

        startSessionAndConfirm(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();

        assertThat(pollUntilCaseAssignedToUser(tflPleadedGuiltyCasePayloadBuilder.getId(), userId), is(true));
    }

    @Test
    public void shouldNotAssignOnlyTVLCasesForProsecutorsForMagistrate() {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        final CreateCase.CreateCasePayloadBuilder tvlPleadedGuiltyCasePayloadBuilder = withDefaults()
                .withPostingDate(daysAgo(10))
                .withProsecutingAuthority(TVL);
        stubGetEmptyAssignmentsByDomainObjectId(tvlPleadedGuiltyCasePayloadBuilder.getId());
        createCaseForPayloadBuilder(tvlPleadedGuiltyCasePayloadBuilder);

        // pleaded guilty case
        requestSetPleasAndConfirm(tvlPleadedGuiltyCasePayloadBuilder.getId(),
                true,
                false,
                true,
                null,
                false,
                null,
                singletonList(Triple.of(tvlPleadedGuiltyCasePayloadBuilder.getOffenceId(),
                        tvlPleadedGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), GUILTY)));

        startSessionAndConfirm(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();

        assertThat(pollUntilCaseNotAssignedToUser(tvlPleadedGuiltyCasePayloadBuilder.getId(), userId), is(false));

    }

    @Test
    public void shouldAssignOnlyTFLCasesForProsecutorsForDelegatedPowersSession() {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        final CreateCase.CreateCasePayloadBuilder tflPleadedNotGuiltyCasePayloadBuilder = withDefaults()
                .withPostingDate(daysAgo(11));
        stubGetEmptyAssignmentsByDomainObjectId(tflPleadedNotGuiltyCasePayloadBuilder.getId());
        createCaseForPayloadBuilder(tflPleadedNotGuiltyCasePayloadBuilder);
        // pleaded not guilty case
        requestSetPleasAndConfirm(tflPleadedNotGuiltyCasePayloadBuilder.getId(),
                true,
                false,
                true,
                null,
                false,
                null,
                singletonList(Triple.of(tflPleadedNotGuiltyCasePayloadBuilder.getOffenceId(),
                        tflPleadedNotGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), NOT_GUILTY)));
        addDatesToAvoid(tflPleadedNotGuiltyCasePayloadBuilder.getId(), DATE_TO_AVOID);

        startSessionAndConfirm(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, DELEGATED_POWERS);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();

        assertThat(pollUntilCaseAssignedToUser(tflPleadedNotGuiltyCasePayloadBuilder.getId(), userId), is(true));
    }

    @Test
    public void shouldNotAssignOnlyTVLCasesForProsecutorsForDelegatedPowersSession() {
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        final CreateCase.CreateCasePayloadBuilder tvlPleadedNotGuiltyCasePayloadBuilder = withDefaults()
                .withPostingDate(daysAgo(11))
                .withProsecutingAuthority(TVL);
        stubGetEmptyAssignmentsByDomainObjectId(tvlPleadedNotGuiltyCasePayloadBuilder.getId());
        createCaseForPayloadBuilder(tvlPleadedNotGuiltyCasePayloadBuilder);
        // pleaded not guilty case
        requestSetPleasAndConfirm(tvlPleadedNotGuiltyCasePayloadBuilder.getId(),
                true,
                false,
                true,
                null,
                false,
                null,
                singletonList(Triple.of(tvlPleadedNotGuiltyCasePayloadBuilder.getOffenceId(),
                        tvlPleadedNotGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), NOT_GUILTY)));
        addDatesToAvoid(tvlPleadedNotGuiltyCasePayloadBuilder.getId(), DATE_TO_AVOID);

        startSessionAndConfirm(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, DELEGATED_POWERS);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();

        assertThat(pollUntilCaseNotAssignedToUser(tvlPleadedNotGuiltyCasePayloadBuilder.getId(), userId), is(false));
    }

    private LocalDate daysAgo(int days) {
        return LocalDate.now().minusDays(days);
    }
}
