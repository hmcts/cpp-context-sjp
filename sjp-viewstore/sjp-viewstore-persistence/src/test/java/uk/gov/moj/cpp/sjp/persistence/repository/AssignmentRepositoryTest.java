package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.DVLA;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TVL;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.StreamStatus;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class AssignmentRepositoryTest extends BaseTransactionalTest {

    private Set<String> NO_EXCLUDED_PROSECUTING_AUTHORITIES = emptySet();
    private int NO_LIMIT = Integer.MAX_VALUE;

    @Inject
    private AssignmentRepository assignmentRepository;

    @Inject
    private EntityManager em;

    private UUID assigneeId;

    @Before
    public void init() {
        assigneeId = UUID.randomUUID();
    }

    @Test
    public void shouldLimitResultsForMagistrateSession() {
        CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(1).plea(GUILTY).save(em);
        CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(2).plea(GUILTY).save(em);

        assertThat(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, 3), hasSize(2));
        assertThat(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, 2), hasSize(2));
        assertThat(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, 1), hasSize(1));
        assertThat(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, 0), hasSize(0));
    }

    @Test
    public void shouldLimitResultsForDelegatePowersSession() {
        CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(1).plea(NOT_GUILTY).save(em);
        CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(2).plea(NOT_GUILTY).save(em);

        assertThat(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, 3), hasSize(2));
        assertThat(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, 2), hasSize(2));
        assertThat(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, 1), hasSize(1));
        assertThat(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, 0), hasSize(0));
    }

    @Test
    public void shouldExcludeProsecutorsForMagistrateSession() {
        final CaseDetail tflCase = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(3).plea(GUILTY).save(em);
        final CaseDetail tvlCase = CaseSaver.prosecutingAuthority(TVL).postedDaysAgo(2).plea(GUILTY).save(em);
        final CaseDetail dvlaCase = CaseSaver.prosecutingAuthority(DVLA).postedDaysAgo(1).plea(GUILTY).save(em);

        List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(TFL), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(tvlCase, dvlaCase)));

        magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(TVL), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(tflCase, dvlaCase)));

        magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(TVL, TFL), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(dvlaCase)));

        magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(TVL, TFL, DVLA), NO_LIMIT);
        assertThat(magistrateSessionCandidates, hasSize(0));
    }

    @Test
    public void shouldExcludeProsecutorsInDelegatedPowersSession() {
        final CaseDetail tflCase = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(3).plea(NOT_GUILTY).save(em);
        final CaseDetail tvlCase = CaseSaver.prosecutingAuthority(TVL).postedDaysAgo(2).plea(NOT_GUILTY).save(em);
        final CaseDetail dvlaCase = CaseSaver.prosecutingAuthority(DVLA).postedDaysAgo(1).plea(NOT_GUILTY).save(em);

        List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(TFL), NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(tvlCase, dvlaCase)));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(TVL), NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(tflCase, dvlaCase)));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(TVL, TFL), NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(dvlaCase)));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(TVL, TFL, DVLA), NO_LIMIT);
        assertThat(delegatedPowersSessionCandidates, hasSize(0));
    }

    @Test
    public void shouldReturnCasesReadyForDecisionBasedOnSessionType() {
        final CaseDetail guiltyPleaded = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(3).plea(GUILTY).save(em);
        final CaseDetail pia = CaseSaver.prosecutingAuthority(TVL).postedDaysAgo(30).save(em);
        final CaseDetail notGuiltyPleaded = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(3).plea(NOT_GUILTY).save(em);
        final CaseDetail guiltyPleadedCourtHearingRequested = CaseSaver.prosecutingAuthority(TVL).postedDaysAgo(2).plea(GUILTY_REQUEST_HEARING).save(em);
        final CaseDetail withdrawalRequested = CaseSaver.prosecutingAuthority(DVLA).postedDaysAgo(1).pendingWithdrawal(true).save(em);

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(guiltyPleaded, pia)));

        final List<AssignmentCandidate> delegatedPowerSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, NO_LIMIT);
        assertThat(getIds(delegatedPowerSessionCandidates), contains(getIds(
                withdrawalRequested,
                notGuiltyPleaded,
                guiltyPleadedCourtHearingRequested
        )));
    }

    @Test
    public void shouldPrioritizeCasesForMagistrateSession() {
        final CaseDetail guiltyPleaded10DaysOld = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(10).plea(GUILTY).save(em);
        final CaseDetail guiltyPleaded20DaysOld = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(20).plea(GUILTY).save(em);
        final CaseDetail pia30DaysOld = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(30).save(em);
        final CaseDetail pia40DaysOld = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(40).save(em);

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(
                guiltyPleaded20DaysOld,
                guiltyPleaded10DaysOld,
                pia40DaysOld,
                pia30DaysOld
        )));
    }

    @Test
    public void shouldPrioritizeCasesForDelegatedPowersSession() {
        final CaseDetail pleadedNotGuilty10DaysOld = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(10).plea(NOT_GUILTY).save(em);
        final CaseDetail pleadedNotGuilty20DaysOld = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(20).plea(NOT_GUILTY).save(em);

        final CaseDetail courtHearingRequested15DaysOld = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(15).plea(GUILTY_REQUEST_HEARING).save(em);
        final CaseDetail courtHearingRequested25DaysOld = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(25).plea(GUILTY_REQUEST_HEARING).save(em);

        final CaseDetail pendingWithdrawal10DaysOld = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(10).pendingWithdrawal(true).save(em);
        final CaseDetail pendingWithdrawal20DaysOld = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(20).pendingWithdrawal(true).save(em);

        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(
                pendingWithdrawal20DaysOld,
                pendingWithdrawal10DaysOld,
                courtHearingRequested25DaysOld,
                pleadedNotGuilty20DaysOld,
                courtHearingRequested15DaysOld,
                pleadedNotGuilty10DaysOld
        )));
    }

    @Test
    public void shouldTopPrioritizeCasesAlreadyAssignedInMagistrateSession() {
        final CaseDetail guiltyPleaded = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(3).plea(GUILTY).save(em);
        final CaseDetail piaAfter40Days = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(40).save(em);
        final CaseDetail assigned = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(30).assigneeId(assigneeId).save(em);

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(
                assigned,
                guiltyPleaded,
                piaAfter40Days
        )));
    }

    @Test
    public void shouldTopPrioritizeCasesAlreadyAssignedInDelegatedPowersSession() {
        final CaseDetail pleadedNotGuiltyAssigned = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(10).plea(NOT_GUILTY).assigneeId(assigneeId).save(em);
        final CaseDetail pleadedNotGuilty = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(10).plea(NOT_GUILTY).save(em);
        final CaseDetail courtHearingRequested = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(11).plea(GUILTY_REQUEST_HEARING).pendingWithdrawal(false).save(em);
        final CaseDetail pendingWithdrawal = CaseSaver.prosecutingAuthority(TFL).postedDaysAgo(11).pendingWithdrawal(true).save(em);

        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, NO_LIMIT);

        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(
                pleadedNotGuiltyAssigned,
                pendingWithdrawal,
                courtHearingRequested,
                pleadedNotGuilty
        )));
    }

    @Test
    public void shouldIgnoreAlreadyCompletedCases() {
        CaseSaver.prosecutingAuthority(TFL).plea(GUILTY).completed(true).save(em);
        final CaseDetail pleadedGuilty = CaseSaver.prosecutingAuthority(TFL).plea(GUILTY).save(em);

        CaseSaver.prosecutingAuthority(TFL).plea(NOT_GUILTY).completed(true).save(em);
        final CaseDetail pleadedNotGuilty = CaseSaver.prosecutingAuthority(TFL).plea(NOT_GUILTY).save(em);

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(pleadedGuilty)));

        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(pleadedNotGuilty)));
    }

    @Test
    public void shouldReturnCaseVersion() {
        CaseSaver.prosecutingAuthority(TFL).plea(GUILTY).version(2).completed(false).save(em);
        CaseSaver.prosecutingAuthority(TFL).plea(NOT_GUILTY).version(3).save(em);

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, NO_LIMIT);
        assertThat(magistrateSessionCandidates, hasSize(1));
        assertThat(magistrateSessionCandidates.get(0).getCaseStreamVersion(), equalTo(2));

        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, NO_LIMIT);
        assertThat(delegatedPowersSessionCandidates.get(0).getCaseStreamVersion(), equalTo(3));
    }

    @Test
    public void shouldReturnRightAssignmentWhenCaseDetailsDatesToAvoidIsNull() {
        // GIVEN - all of them will be returned as datesToAvoid IS NULL
        Function<Integer, CaseDetail> saveCaseDetails = pastDaysFromNow -> CaseSaver.prosecutingAuthority(TFL)
                .plea(NOT_GUILTY)
                .datesToAvoid(null)
                .pendingDatesToAvoid(ZonedDateTime.now(UTC).minusDays(pastDaysFromNow))
                .save(em);

        // dates to avoid is not set -> return only older than 10 days
        AssignmentCandidate[] expectedAssignments = Stream.of(0, 1, 9, 10, 11, 20, 100)
                .map(pastDaysFromNow -> new Pair<>(pastDaysFromNow, saveCaseDetails.apply(pastDaysFromNow).getId()))
                .filter(p -> p.getKey() > 10)
                .map(Pair::getValue)
                .map(caseId -> new AssignmentCandidate(caseId, 1))
                .toArray(AssignmentCandidate[]::new);

        // WHEN
        List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, NO_LIMIT);

        assertThat(delegatedPowersSessionCandidates, not(emptyList()));
        assertThat(delegatedPowersSessionCandidates, containsInAnyOrder(expectedAssignments));
    }

    @Test
    public void shouldReturnRightAssignmentWhenCaseDetailsDatesToAvoidIsNotNull() {

        // GIVEN - all of them will be returned as datesToAvoid IS NOT NULL
        Function<Integer, CaseDetail> saveCaseDetails = (notGuiltyPleaDaysAgo) -> CaseSaver.prosecutingAuthority(TFL)
                .plea(NOT_GUILTY)
                .pendingDatesToAvoid(ZonedDateTime.now(UTC).minusDays(notGuiltyPleaDaysAgo))
                .datesToAvoid("dates-to-avoid" + notGuiltyPleaDaysAgo)
                .save(em);

        // dates to avoid is set -> return all of them
        AssignmentCandidate[] expectedAssignments = Stream.of(9, 10, 11)
                .map(saveCaseDetails)
                .map(CaseDetail::getId)
                .map(caseId -> new AssignmentCandidate(caseId, 1))
                .toArray(AssignmentCandidate[]::new);

        // WHEN
        List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, NO_EXCLUDED_PROSECUTING_AUTHORITIES, NO_LIMIT);

        assertThat(delegatedPowersSessionCandidates, not(emptyList()));
        assertThat(delegatedPowersSessionCandidates, containsInAnyOrder(expectedAssignments));
    }

    private static List<UUID> getIds(final List<AssignmentCandidate> assignmentCandidates) {
        return assignmentCandidates.stream().map(AssignmentCandidate::getCaseId).collect(toList());
    }

    private static UUID[] getIds(final CaseDetail... assignmentCandidates) {
        return Stream.of(assignmentCandidates).map(CaseDetail::getId).toArray(UUID[]::new);
    }

    private static Set<String> excludedProsecutingAuthorities(final ProsecutingAuthority... excludedProsecutingAuthorities) {
        return stream(excludedProsecutingAuthorities)
                .map(ProsecutingAuthority::name)
                .collect(toSet());
    }

    private static class CaseSaver {
        private ProsecutingAuthority prosecutingAuthority;
        private LocalDate postingDate;
        private boolean completed;
        private boolean pendingWithdrawal;
        private UUID assigneeId;
        private PleaType plea;
        private int version = 1;
        private String datesToAvoid;
        private ZonedDateTime datesToAvoidPleaDate;

        static CaseSaver prosecutingAuthority(final ProsecutingAuthority prosecutingAuthority) {
            return new CaseSaver(prosecutingAuthority);
        }

        private CaseSaver(final ProsecutingAuthority prosecutingAuthority) {
            this.prosecutingAuthority = prosecutingAuthority;
        }

        private CaseSaver postedDaysAgo(int daysAgo) {
            this.postingDate = LocalDate.now().minusDays(daysAgo);
            return this;
        }

        private CaseSaver completed(boolean completed) {
            this.completed = completed;
            return this;
        }

        private CaseSaver pendingWithdrawal(boolean pendingWithdrawal) {
            this.pendingWithdrawal = pendingWithdrawal;
            return this;
        }

        private CaseSaver assigneeId(UUID assigneeId) {
            this.assigneeId = assigneeId;
            return this;
        }

        CaseSaver plea(PleaType plea) {
            this.plea = plea;
            return this;
        }

        private CaseSaver version(int version) {
            this.version = version;
            return this;
        }

        CaseSaver datesToAvoid(String datesToAvoid) {
            this.datesToAvoid = datesToAvoid;
            return this;
        }

        CaseSaver pendingDatesToAvoid(ZonedDateTime datesToAvoidPleaDate) {
            this.datesToAvoidPleaDate = datesToAvoidPleaDate;
            return this;
        }

        CaseDetail save(EntityManager em) {
            final UUID caseId = randomUUID();
            final UUID defendantId = randomUUID();
            final UUID offenceId = randomUUID();

            final OffenceDetail offence = new OffenceDetail.OffenceDetailBuilder()
                    .setId(offenceId)
                    .setPendingWithdrawal(pendingWithdrawal)
                    .setPlea(plea)
                    .build();

            final DefendantDetail defendant = new DefendantDetail(defendantId, new PersonalDetails(), singleton(offence), 2);

            final CaseDetail caseDetail = CaseDetailBuilder.aCase()
                    .withCaseId(caseId)
                    .withProsecutingAuthority(prosecutingAuthority)
                    .withPostingDate(postingDate)
                    .withCompleted(completed)
                    .withAssigneeId(assigneeId)
                    .withDatesToAvoid(datesToAvoid)
                    .addDefendantDetail(defendant)
                    .build();

            final StreamStatus caseStreamStatus = new StreamStatus(caseId, version);
            em.persist(caseDetail);
            em.persist(caseStreamStatus);

            //If the dates to avoid are provided, the case shouldn't be added to pending dates to avoid table
            if(datesToAvoidPleaDate != null && StringUtils.isEmpty(datesToAvoid)) {
                em.persist(buildPendingDatesToAvoid(caseDetail, datesToAvoidPleaDate));
            } else {
                assertThat(em.find(PendingDatesToAvoid.class, caseDetail.getId()), nullValue());
            }

            return caseDetail;
        }

        private static PendingDatesToAvoid buildPendingDatesToAvoid(CaseDetail caseDetail, ZonedDateTime datesToAvoidPleaDate) {
            PendingDatesToAvoid pendingDatesToAvoid = new PendingDatesToAvoid(caseDetail);
            pendingDatesToAvoid.setPleaDate(datesToAvoidPleaDate);

            return pendingDatesToAvoid;
        }

    }

}