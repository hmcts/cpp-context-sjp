package uk.gov.moj.sjp.it.test;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SET_PLEAS;
import static uk.gov.moj.sjp.it.command.AddDatesToAvoid.addDatesToAvoid;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseAssignedToUser;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.pollUntilCaseNotAssignedToUser;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubAddMapping;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;


import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.commandclient.AssignNextCaseClient;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.SessionHelper;
import uk.gov.moj.sjp.it.helper.SetPleasHelper;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

public class FilterSessionByProsecutorIT extends BaseIntegrationTest{

    private static final String DATE_TO_AVOID = "a-date-to-avoid";

    private final EventListener eventListener = new EventListener();

    @BeforeEach
    public void setUp() throws Exception {
        final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
        databaseCleaner.cleanAll();

        stubDefaultCourtByCourtHouseOUCodeQuery();

        stubProsecutorQuery(TFL.name(), TFL.getFullName(), randomUUID());
        stubProsecutorQuery(TVL.name(), TVL.getFullName(), randomUUID());
        stubProsecutorQuery(DVLA.name(), DVLA.getFullName(), randomUUID());

        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        final String defaultPostcodeUsed = CreateCase.CreateCasePayloadBuilder.withDefaults().getDefendantBuilder().getAddressBuilder().getPostcode();
        stubEnforcementAreaByPostcode(defaultPostcodeUsed, "1080", "BedfordShire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");
    }

    @Test
    public void shouldAssignOnlyTFLCasesForProsecutorsForMagistrate(){
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        final CreateCase.CreateCasePayloadBuilder tflPleadedGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(10));
        stubGetEmptyAssignmentsByDomainObjectId(tflPleadedGuiltyCasePayloadBuilder.getId());
        CreateCase.createCaseForPayloadBuilder(tflPleadedGuiltyCasePayloadBuilder);

        // pleaded guilty case
        SetPleasHelper.requestSetPleas(tflPleadedGuiltyCasePayloadBuilder.getId(),
                eventListener,
                true,
                false,
                true,
                null,
                false,
                null,
                singletonList(Triple.of(tflPleadedGuiltyCasePayloadBuilder.getOffenceId(),
                        tflPleadedGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), GUILTY)),
                PUBLIC_EVENT_SET_PLEAS);

        SessionHelper.startSession(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();

        assertThat(pollUntilCaseAssignedToUser(tflPleadedGuiltyCasePayloadBuilder.getId(), userId), Matchers.is(true));
    }

    @Test
    public void shouldNotAssignOnlyTVLCasesForProsecutorsForMagistrate(){
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        final CreateCase.CreateCasePayloadBuilder tvlPleadedGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(10))
                .withProsecutingAuthority(TVL);
        stubGetEmptyAssignmentsByDomainObjectId(tvlPleadedGuiltyCasePayloadBuilder.getId());
        CreateCase.createCaseForPayloadBuilder(tvlPleadedGuiltyCasePayloadBuilder);

        // pleaded guilty case
        SetPleasHelper.requestSetPleas(tvlPleadedGuiltyCasePayloadBuilder.getId(),
                eventListener,
                true,
                false,
                true,
                null,
                false,
                null,
                singletonList(Triple.of(tvlPleadedGuiltyCasePayloadBuilder.getOffenceId(),
                        tvlPleadedGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), GUILTY)),
                PUBLIC_EVENT_SET_PLEAS);

        SessionHelper.startSession(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();

        assertThat(pollUntilCaseNotAssignedToUser(tvlPleadedGuiltyCasePayloadBuilder.getId(), userId), Matchers.is(false));

    }

    @Test
    public void shouldAssignOnlyTFLCasesForProsecutorsForDelegatedPowersSession(){
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        final CreateCase.CreateCasePayloadBuilder tflPleadedNotGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(11));
        stubGetEmptyAssignmentsByDomainObjectId(tflPleadedNotGuiltyCasePayloadBuilder.getId());
        CreateCase.createCaseForPayloadBuilder(tflPleadedNotGuiltyCasePayloadBuilder);
        // pleaded not guilty case
        SetPleasHelper.requestSetPleas(tflPleadedNotGuiltyCasePayloadBuilder.getId(),
                eventListener,
                true,
                false,
                true,
                null,
                false,
                null,
                singletonList(Triple.of(tflPleadedNotGuiltyCasePayloadBuilder.getOffenceId(),
                        tflPleadedNotGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), NOT_GUILTY)),
                PUBLIC_EVENT_SET_PLEAS);
        addDatesToAvoid(tflPleadedNotGuiltyCasePayloadBuilder.getId(), DATE_TO_AVOID);

        SessionHelper.startSession(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, DELEGATED_POWERS);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();

        assertThat(pollUntilCaseAssignedToUser(tflPleadedNotGuiltyCasePayloadBuilder.getId(), userId), Matchers.is(true));
    }

    @Test
    public void shouldNotAssignOnlyTVLCasesForProsecutorsForDelegatedPowersSession(){
        final UUID sessionId = randomUUID();
        final UUID userId = randomUUID();

        final CreateCase.CreateCasePayloadBuilder tvlPleadedNotGuiltyCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withPostingDate(daysAgo(11))
                .withProsecutingAuthority(TVL);
        stubGetEmptyAssignmentsByDomainObjectId(tvlPleadedNotGuiltyCasePayloadBuilder.getId());
        CreateCase.createCaseForPayloadBuilder(tvlPleadedNotGuiltyCasePayloadBuilder);
        // pleaded not guilty case
        SetPleasHelper.requestSetPleas(tvlPleadedNotGuiltyCasePayloadBuilder.getId(),
                eventListener,
                true,
                false,
                true,
                null,
                false,
                null,
                singletonList(Triple.of(tvlPleadedNotGuiltyCasePayloadBuilder.getOffenceId(),
                        tvlPleadedNotGuiltyCasePayloadBuilder.getDefendantBuilder().getId(), NOT_GUILTY)),
                PUBLIC_EVENT_SET_PLEAS);
        addDatesToAvoid(tvlPleadedNotGuiltyCasePayloadBuilder.getId(), DATE_TO_AVOID);

        SessionHelper.startSession(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, DELEGATED_POWERS);

        AssignNextCaseClient assignCase = AssignNextCaseClient.builder().sessionId(sessionId).build();
        assignCase.getExecutor().setExecutingUserId(userId).executeSync();

        assertThat(pollUntilCaseNotAssignedToUser(tvlPleadedNotGuiltyCasePayloadBuilder.getId(), userId), Matchers.is(false));
    }

    private static LocalDate daysAgo(int days) {
        return LocalDate.now().minusDays(days);
    }
}
