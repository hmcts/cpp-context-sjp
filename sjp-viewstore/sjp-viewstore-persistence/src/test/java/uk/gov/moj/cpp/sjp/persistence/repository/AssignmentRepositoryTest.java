package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.StreamStatus;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class AssignmentRepositoryTest extends BaseTransactionalTest {

    private static final String TFL = "TFL", TVL = "TVL", DVLA = "DVLA";
    private static final String GUILTY = "GUILTY", NOT_GUILTY = "NOT_GUILTY", GUILTY_REQUEST_HEARING = "GUILTY_REQUEST_HEARING";

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
        new CaseSaver(TFL).postedDaysAgo(1).plea(GUILTY).save();
        new CaseSaver(TFL).postedDaysAgo(2).plea(GUILTY).save();

        assertThat(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(), 3), hasSize(2));
        assertThat(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(), 2), hasSize(2));
        assertThat(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(), 1), hasSize(1));
        assertThat(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(), 0), hasSize(0));
    }

    @Test
    public void shouldLimitResultsForDelegatePowersSession() {
        new CaseSaver(TFL).postedDaysAgo(1).plea(NOT_GUILTY).save();
        new CaseSaver(TFL).postedDaysAgo(2).plea(NOT_GUILTY).save();

        assertThat(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(), 3), hasSize(2));
        assertThat(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(), 2), hasSize(2));
        assertThat(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(), 1), hasSize(1));
        assertThat(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(), 0), hasSize(0));
    }

    @Test
    public void shouldExcludeProsecutorsForMagistrateSession() {
        final CaseDetail tflCase = new CaseSaver(TFL).postedDaysAgo(3).plea(GUILTY).save();
        final CaseDetail tvlCase = new CaseSaver(TVL).postedDaysAgo(2).plea(GUILTY).save();
        final CaseDetail dvlaCase = new CaseSaver(DVLA).postedDaysAgo(1).plea(GUILTY).save();

        List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(TFL), 10);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(tvlCase, dvlaCase)));

        magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(TVL), 10);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(tflCase, dvlaCase)));

        magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(TVL, TFL), 10);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(dvlaCase)));

        magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(TVL, TFL, DVLA), 10);
        assertThat(magistrateSessionCandidates, hasSize(0));
    }

    @Test
    public void shouldExcludeProsecutorsInDelegatedPowersSession() {
        final CaseDetail tflCase = new CaseSaver(TFL).postedDaysAgo(3).plea(NOT_GUILTY).save();
        final CaseDetail tvlCase = new CaseSaver(TVL).postedDaysAgo(2).plea(NOT_GUILTY).save();
        final CaseDetail dvlaCase = new CaseSaver(DVLA).postedDaysAgo(1).plea(NOT_GUILTY).save();

        List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(TFL), 10);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(tvlCase, dvlaCase)));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(TVL), 10);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(tflCase, dvlaCase)));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(TVL, TFL), 10);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(dvlaCase)));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(TVL, TFL, DVLA), 10);
        assertThat(delegatedPowersSessionCandidates, hasSize(0));
    }

    @Test
    public void shouldReturnCasesReadyForDecisionBasedOnSessionType() {
        final CaseDetail guiltyPleaded = new CaseSaver(TFL).postedDaysAgo(3).plea(GUILTY).save();
        final CaseDetail pia = new CaseSaver(TVL).postedDaysAgo(30).save();
        final CaseDetail notGuiltyPleaded = new CaseSaver(TFL).postedDaysAgo(3).plea(NOT_GUILTY).save();
        final CaseDetail guiltyPleadedCourtHearingRequested = new CaseSaver(TVL).postedDaysAgo(2).plea(GUILTY_REQUEST_HEARING).save();
        final CaseDetail withdrawalRequested = new CaseSaver(DVLA).postedDaysAgo(1).pendingWithdrawal(true).save();

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(), 10);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(
                guiltyPleaded,
                pia
        )));

        final List<AssignmentCandidate> delegatedPowerSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(), 10);
        assertThat(getIds(delegatedPowerSessionCandidates), contains(getIds(
                withdrawalRequested,
                notGuiltyPleaded,
                guiltyPleadedCourtHearingRequested
        )));
    }

    @Test
    public void shouldPrioritizeCasesForMagistrateSession() {
        final CaseDetail guiltyPleaded10DaysOld = new CaseSaver(TFL).postedDaysAgo(10).plea(GUILTY).save();
        final CaseDetail guiltyPleaded20DaysOld = new CaseSaver(TFL).postedDaysAgo(20).plea(GUILTY).save();
        final CaseDetail pia30DaysOld = new CaseSaver(TFL).postedDaysAgo(30).save();
        final CaseDetail pia40DaysOld = new CaseSaver(TFL).postedDaysAgo(40).save();

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(), 10);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(
                guiltyPleaded20DaysOld,
                guiltyPleaded10DaysOld,
                pia40DaysOld,
                pia30DaysOld
        )));
    }

    @Test
    public void shouldPrioritizeCasesForDelegatedPowersSession() {
        final CaseDetail pleadedNotGuilty10DaysOld = new CaseSaver(TFL).postedDaysAgo(10).plea(NOT_GUILTY).save();
        final CaseDetail pleadedNotGuilty20DaysOld = new CaseSaver(TFL).postedDaysAgo(20).plea(NOT_GUILTY).save();

        final CaseDetail courtHearingRequested15DaysOld = new CaseSaver(TFL).postedDaysAgo(15).plea(GUILTY_REQUEST_HEARING).save();
        final CaseDetail courtHearingRequested25DaysOld = new CaseSaver(TFL).postedDaysAgo(25).plea(GUILTY_REQUEST_HEARING).save();

        final CaseDetail pendingWithdrawal10DaysOld = new CaseSaver(TFL).postedDaysAgo(10).pendingWithdrawal(true).save();
        final CaseDetail pendingWithdrawal20DaysOld = new CaseSaver(TFL).postedDaysAgo(20).pendingWithdrawal(true).save();

        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(), 10);
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
        final CaseDetail guiltyPleaded = new CaseSaver(TFL).postedDaysAgo(3).plea(GUILTY).save();
        final CaseDetail piaAfter40Days = new CaseSaver(TFL).postedDaysAgo(40).save();
        final CaseDetail assigned = new CaseSaver(TFL).postedDaysAgo(30).assigneeId(assigneeId).save();

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(), 10);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(
                assigned,
                guiltyPleaded,
                piaAfter40Days
        )));
    }

    @Test
    public void shouldTopPrioritizeCasesAlreadyAssignedInDelegatedPowersSession() {
        final CaseDetail pleadedNotGuiltyAssigned = new CaseSaver(TFL).postedDaysAgo(10).plea(NOT_GUILTY).assigneeId(assigneeId).save();
        final CaseDetail pleadedNotGuilty = new CaseSaver(TFL).postedDaysAgo(10).plea(NOT_GUILTY).save();
        final CaseDetail courtHearingRequested = new CaseSaver(TFL).postedDaysAgo(11).plea(GUILTY_REQUEST_HEARING).save();
        final CaseDetail pendingWithdrawal = new CaseSaver(TFL).postedDaysAgo(11).pendingWithdrawal(true).save();

        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(), 10);

        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(
                pleadedNotGuiltyAssigned,
                pendingWithdrawal,
                courtHearingRequested,
                pleadedNotGuilty
        )));
    }

    @Test
    public void shouldIgnoreAlreadyCompletedCases() {
        final CaseDetail completedPleadedGuilty = new CaseSaver(TFL).plea(GUILTY).completed(true).save();
        final CaseDetail pleadedGuilty = new CaseSaver(TFL).plea(GUILTY).save();

        final CaseDetail completedPleadedNotGuilty = new CaseSaver(TFL).plea(NOT_GUILTY).completed(true).save();
        final CaseDetail pleadedNotGuilty = new CaseSaver(TFL).plea(NOT_GUILTY).save();

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(), 10);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(pleadedGuilty)));

        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(), 10);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(pleadedNotGuilty)));
    }

    @Test
    public void shouldReturnCaseVersion() {
        new CaseSaver(TFL).plea(GUILTY).version(2).save();
        new CaseSaver(TFL).plea(NOT_GUILTY).version(3).save();

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, excludedProsecutingAuthorities(), 10);
        assertThat(magistrateSessionCandidates.get(0).getCaseStreamVersion(), equalTo(2));

        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, excludedProsecutingAuthorities(), 10);
        assertThat(delegatedPowersSessionCandidates.get(0).getCaseStreamVersion(), equalTo(3));
    }

    private static List<UUID> getIds(final List<AssignmentCandidate> assignmentCandidates) {
        return assignmentCandidates.stream().map(AssignmentCandidate::getCaseId).collect(toList());
    }

    private static UUID[] getIds(final CaseDetail... assignmentCandidates) {
        return Stream.of(assignmentCandidates).map(CaseDetail::getId).toArray(UUID[]::new);
    }

    private static Set<String> excludedProsecutingAuthorities(final String... excludedProsecutingAuthorities) {
        return new HashSet<>(asList(excludedProsecutingAuthorities));
    }

    private class CaseSaver {
        private String prosecutingAuthority;
        private LocalDate postingDate;
        private boolean completed;
        private boolean pendingWithdrawal;
        private UUID assigneeId;
        private String plea;
        private int version = 1;

        public CaseSaver(final String prosecutingAuthority) {
            this.prosecutingAuthority = prosecutingAuthority;
        }

        public CaseSaver postedDaysAgo(int daysAgo) {
            this.postingDate = LocalDate.now().minusDays(daysAgo);
            return this;
        }

        public CaseSaver completed(boolean completed) {
            this.completed = completed;
            return this;
        }

        public CaseSaver pendingWithdrawal(boolean pendingWithdrawal) {
            this.pendingWithdrawal = pendingWithdrawal;
            return this;
        }

        public CaseSaver assigneeId(UUID assigneeId) {
            this.assigneeId = assigneeId;
            return this;
        }

        public CaseSaver plea(String plea) {
            this.plea = plea;
            return this;
        }

        public CaseSaver version(int version) {
            this.version = version;
            return this;
        }

        public CaseDetail save() {
            final UUID caseId = randomUUID();
            final UUID defendantId = randomUUID();
            final UUID offenceId = randomUUID();

            final OffenceDetail offence = new OffenceDetail.OffenceDetailBuilder()
                    .setId(offenceId)
                    .setPendingWithdrawal(pendingWithdrawal)
                    .setPlea(plea)
                    .build();

            final DefendantDetail defendant = new DefendantDetail(defendantId, new PersonalDetails(), new HashSet<>(asList(offence)), 2);

            final CaseDetail caseDetail = CaseDetailBuilder.aCase()
                    .withCaseId(caseId)
                    .withProsecutingAuthority(prosecutingAuthority)
                    .withPostingDate(postingDate)
                    .withCompleted(completed)
                    .withAssigneeId(assigneeId)
                    .addDefendantDetail(defendant)
                    .build();

            final StreamStatus caseStreamStatus = new StreamStatus(caseId, version);

            em.persist(caseDetail);
            em.persist(caseStreamStatus);

            return caseDetail;
        }
    }
}
