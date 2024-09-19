package uk.gov.moj.cpp.sjp.persistence.repository;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.ContactDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.entity.ReserveCase;
import uk.gov.moj.cpp.sjp.persistence.entity.StreamStatus;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.time.LocalDate.now;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.WITHDRAWAL_REQUESTED;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

@RunWith(CdiTestRunner.class)
public class AssignmentRepositoryTest extends BaseTransactionalJunit4Test {

    private static final ZonedDateTime TODAY_MIDNIGHT = ZonedDateTime.now(UTC).truncatedTo(ChronoUnit.DAYS);
    private final int NO_LIMIT = Integer.MAX_VALUE;
    @Inject
    private AssignmentRepository assignmentRepository;

    @Inject
    private EntityManager em;

    private UUID assigneeId;

    @Override
    public void setUpBefore() {
        assigneeId = UUID.randomUUID();
    }

    @Test
    public void shouldLimitResultsForMagistrateSession() {
        CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(1).plea(GUILTY).save(em, PLEADED_GUILTY);
        CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(2).plea(GUILTY).save(em, PLEADED_GUILTY);

        assertThat(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TFL"), 3), hasSize(2));
        assertThat(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TFL"), 2), hasSize(2));
        assertThat(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TFL"), 1), hasSize(1));
        assertThat(assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TFL"), 0), hasSize(0));
    }

    @Test
    public void shouldLimitResultsForDelegatePowersSession() {
        CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(1).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);
        CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(2).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);

        assertThat(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), 3), hasSize(2));
        assertThat(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), 2), hasSize(2));
        assertThat(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), 1), hasSize(1));
        assertThat(assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), 0), hasSize(0));
    }

    @Test
    public void shouldExcludeProsecutorsForMagistrateSession() {
        final CaseDetail tflCase = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(3).plea(GUILTY).save(em, PLEADED_GUILTY);
        final CaseDetail tvlCase = CaseSaver.prosecutingAuthority("TVL").postedDaysAgo(2).plea(GUILTY).save(em, PLEADED_GUILTY);
        final CaseDetail dvlaCase = CaseSaver.prosecutingAuthority("DVLA").postedDaysAgo(1).plea(GUILTY).save(em, PLEADED_GUILTY);

        List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TVL", "DVLA"), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(tvlCase, dvlaCase)));

        magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TFL", "DVLA"), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(tflCase, dvlaCase)));

        magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("DVLA"), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(dvlaCase)));

        magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities(), NO_LIMIT);
        assertThat(magistrateSessionCandidates, hasSize(0));
    }

    @Test
    public void shouldIncludeProsecutorsForMagistrateSession() {
        final CaseDetail tflCase = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(3).plea(GUILTY).save(em, PLEADED_GUILTY);
        final CaseDetail tvlCase = CaseSaver.prosecutingAuthority("TVL").postedDaysAgo(2).plea(GUILTY).save(em, PLEADED_GUILTY);
        final CaseDetail dvlaCase = CaseSaver.prosecutingAuthority("DVLA").postedDaysAgo(1).plea(GUILTY).save(em, PLEADED_GUILTY);

        List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("CPS"), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), hasSize(0));

        magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TVL"), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(tvlCase)));

        magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TVL", "TFL"), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(tflCase, tvlCase)));

        magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TVL", "TFL", "DVLA"), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(tflCase, tvlCase, dvlaCase)));
    }

    @Test
    public void shouldExcludeProsecutorsInDelegatedPowersSession() {
        final CaseDetail tflCase = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(3).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);
        final CaseDetail tvlCase = CaseSaver.prosecutingAuthority("TVL").postedDaysAgo(2).pendingWithdrawal(true).save(em, WITHDRAWAL_REQUESTED);
        final CaseDetail dvlaCase = CaseSaver.prosecutingAuthority("DVLA").postedDaysAgo(1).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);

        List<AssignmentCandidate> delegatedPowersSessionCandidates;

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TVL", "DVLA"), NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(tvlCase, dvlaCase)));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL", "DVLA"), NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(tflCase, dvlaCase)));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("DVLA"), NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(dvlaCase)));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities(), NO_LIMIT);
        assertThat(delegatedPowersSessionCandidates, hasSize(0));
    }

    @Test
    public void shouldIncludeProsecutorsInDelegatedPowersSession() {
        final CaseDetail tflCase = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(3).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);
        final CaseDetail tvlCase = CaseSaver.prosecutingAuthority("TVL").postedDaysAgo(2).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);
        final CaseDetail dvlaCase = CaseSaver.prosecutingAuthority("DVLA").postedDaysAgo(1).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);

        List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("CPS"), NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), hasSize(0));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TVL"), NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(tvlCase)));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TVL", "TFL"), NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(tflCase, tvlCase)));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TVL", "TFL", "DVLA"), NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(tflCase, tvlCase, dvlaCase)));
    }

    @Test
    public void shouldReturnCasesReadyForDecisionBasedOnSessionType() {
        final CaseDetail guiltyPleaded = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(3).plea(GUILTY).save(em, PLEADED_GUILTY);
        final CaseDetail pia = CaseSaver.prosecutingAuthority("TVL").postedDaysAgo(30).save(em, PIA);
        final CaseDetail notGuiltyPleaded = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(3).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);
        final CaseDetail guiltyPleadedCourtHearingRequested = CaseSaver.prosecutingAuthority("TVL").postedDaysAgo(2).plea(GUILTY_REQUEST_HEARING).save(em, PLEADED_GUILTY_REQUEST_HEARING);
        final CaseDetail withdrawalRequested = CaseSaver.prosecutingAuthority("DVLA").postedDaysAgo(1).pendingWithdrawal(true).save(em, WITHDRAWAL_REQUESTED);

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TFL", "TVL", "DVLA"), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(guiltyPleaded, pia)));

        final List<AssignmentCandidate> delegatedPowerSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL", "TVL", "DVLA"), NO_LIMIT);
        assertThat(getIds(delegatedPowerSessionCandidates), contains(getIds(
                withdrawalRequested,
                notGuiltyPleaded,
                guiltyPleadedCourtHearingRequested
        )));
    }

    @Test
    public void shouldPrioritizeCasesForMagistrateSession() {
        final CaseDetail guiltyPleaded10DaysOld = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(10).plea(GUILTY).save(em, PLEADED_GUILTY);
        final CaseDetail guiltyPleaded20DaysOld = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(20).plea(GUILTY).save(em, PLEADED_GUILTY);
        final CaseDetail pia30DaysOld = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(30).save(em, PIA);
        final CaseDetail pia40DaysOld = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(40).save(em, PIA);

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(
                guiltyPleaded20DaysOld,
                guiltyPleaded10DaysOld,
                pia40DaysOld,
                pia30DaysOld
        )));
    }

    @Test
    public void shouldPrioritizeCasesForDelegatedPowersSession() {
        final CaseDetail pleadedNotGuilty10DaysOld = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(10).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);
        final CaseDetail pleadedNotGuilty20DaysOld = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(20).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);

        final CaseDetail courtHearingRequested15DaysOld = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(15).plea(GUILTY_REQUEST_HEARING).save(em, PLEADED_GUILTY_REQUEST_HEARING);
        final CaseDetail courtHearingRequested25DaysOld = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(25).plea(GUILTY_REQUEST_HEARING).save(em, PLEADED_GUILTY_REQUEST_HEARING);

        final CaseDetail pendingWithdrawal10DaysOld = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(10).pendingWithdrawal(true).save(em, WITHDRAWAL_REQUESTED);
        final CaseDetail pendingWithdrawal20DaysOld = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(20).pendingWithdrawal(true).save(em, WITHDRAWAL_REQUESTED);

        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);
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
        final CaseDetail guiltyPleaded = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(3).plea(GUILTY).save(em, PLEADED_GUILTY);
        final CaseDetail piaAfter40Days = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(40).save(em, PIA);
        final CaseDetail assigned = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(30).assigneeId(assigneeId).save(em, PIA);

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(
                assigned,
                guiltyPleaded,
                piaAfter40Days
        )));
    }

    @Test
    public void shouldTopPrioritizeCasesAlreadyAssignedInDelegatedPowersSession() {
        final CaseDetail pleadedNotGuiltyAssigned = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(10).notGuiltyWithDatesToAvoid().assigneeId(assigneeId).save(em, PLEADED_NOT_GUILTY);
        final CaseDetail pleadedNotGuilty = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(10).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);
        final CaseDetail courtHearingRequested = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(11).plea(GUILTY_REQUEST_HEARING).pendingWithdrawal(false).save(em, PLEADED_GUILTY_REQUEST_HEARING);
        final CaseDetail pendingWithdrawal = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(11).pendingWithdrawal(true).save(em, WITHDRAWAL_REQUESTED);

        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);

        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(
                pleadedNotGuiltyAssigned,
                pendingWithdrawal,
                courtHearingRequested,
                pleadedNotGuilty
        )));
    }

    @Test
    public void shouldIgnoreAlreadyCompletedCases() {
        CaseSaver.prosecutingAuthority("TFL").plea(GUILTY).completed(true).save(em, CaseSaver.EXPECT_NOT_TO_BE_READY);
        final CaseDetail pleadedGuilty = CaseSaver.prosecutingAuthority("TFL").plea(GUILTY).save(em, PLEADED_GUILTY);

        CaseSaver.prosecutingAuthority("TFL").plea(NOT_GUILTY).completed(true).save(em, CaseSaver.EXPECT_NOT_TO_BE_READY);
        final CaseDetail pleadedNotGuilty = CaseSaver.prosecutingAuthority("TFL").notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);
        assertThat(getIds(magistrateSessionCandidates), contains(getIds(pleadedGuilty)));

        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);
        assertThat(getIds(delegatedPowersSessionCandidates), contains(getIds(pleadedNotGuilty)));
    }

    @Test
    public void shouldReturnCaseVersion() {
        CaseSaver.prosecutingAuthority("TFL").version(2).plea(GUILTY).completed(false).save(em, PLEADED_GUILTY);
        CaseSaver.prosecutingAuthority("TFL").version(3).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);

        final List<AssignmentCandidate> magistrateSessionCandidates = assignmentRepository.getAssignmentCandidatesForMagistrateSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);
        assertThat(magistrateSessionCandidates, hasSize(1));
        assertThat(magistrateSessionCandidates.get(0).getCaseStreamVersion(), equalTo(2));

        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);
        assertThat(delegatedPowersSessionCandidates, hasSize(1));
        assertThat(delegatedPowersSessionCandidates.get(0).getCaseStreamVersion(), equalTo(3));
    }

    @Test
    public void shouldReturnRightAssignmentWhenCaseDetailsDatesToAvoidIsNull() {
        // GIVEN - all of them will be returned as datesToAvoid IS NULL
        final Function<Integer, CaseDetail> saveCaseDetails = pastDaysFromNow -> CaseSaver.prosecutingAuthority("TFL")
                .plea(NOT_GUILTY)
                .datesToAvoid(null)
                .pendingDatesToAvoid(TODAY_MIDNIGHT.minusDays(pastDaysFromNow))
                .save(em, pastDaysFromNow > 10 ? PLEADED_NOT_GUILTY : CaseSaver.EXPECT_NOT_TO_BE_READY);

        // dates to avoid is not set -> return only older than 10 days
        final AssignmentCandidate[] expectedAssignments = Stream.of(0, 1, 9, 10, 11, 20, 100)
                .map(pastDaysFromNow -> Pair.of(pastDaysFromNow, saveCaseDetails.apply(pastDaysFromNow).getId()))
                .filter(p -> p.getKey() > 10)
                .map(Pair::getValue)
                .map(caseId -> new AssignmentCandidate(caseId, 1))
                .toArray(AssignmentCandidate[]::new);

        // WHEN
        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);

        assertThat(delegatedPowersSessionCandidates, not(emptyList()));
        assertThat(delegatedPowersSessionCandidates, containsInAnyOrder(expectedAssignments));
    }

    @Test
    public void shouldReturnRightAssignmentWhenCaseDetailsDatesToAvoidIsNotNull() {
        // GIVEN - all of them will be returned as datesToAvoid IS NOT NULL
        final Function<Integer, CaseDetail> saveCaseDetails = notGuiltyPleaDaysAgo -> CaseSaver.prosecutingAuthority("TFL")
                .plea(NOT_GUILTY)
                .pendingDatesToAvoid(TODAY_MIDNIGHT.minusDays(notGuiltyPleaDaysAgo))
                .datesToAvoid("dates-to-avoid" + notGuiltyPleaDaysAgo)
                .save(em, PLEADED_NOT_GUILTY);

        // dates to avoid is set -> return all of them
        final AssignmentCandidate[] expectedAssignments = Stream.of(9, 10, 11)
                .map(saveCaseDetails)
                .map(CaseDetail::getId)
                .map(caseId -> new AssignmentCandidate(caseId, 1))
                .toArray(AssignmentCandidate[]::new);

        // WHEN
        final List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);

        assertThat(delegatedPowersSessionCandidates, not(emptyList()));
        assertThat(delegatedPowersSessionCandidates, containsInAnyOrder(expectedAssignments));
    }

    @Test
    public void shouldGetFirstReservedCase(){
        final CaseDetail pleadedNotGuiltyAssigned = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(10).notGuiltyWithDatesToAvoid().assigneeId(assigneeId).save(em, PLEADED_NOT_GUILTY);
        final CaseDetail pleadedNotGuilty = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(10).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);

        List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);

        assertThat(delegatedPowersSessionCandidates.get(0).getCaseId(), is(pleadedNotGuiltyAssigned.getId()));
        assertThat(delegatedPowersSessionCandidates.get(1).getCaseId(), is(pleadedNotGuilty.getId()));

        reserveCase(em, pleadedNotGuilty.getId(), assigneeId, ZonedDateTime.now());
        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);

        assertThat(delegatedPowersSessionCandidates.get(0).getCaseId(), is(pleadedNotGuilty.getId()));
        assertThat(delegatedPowersSessionCandidates.get(1).getCaseId(), is(pleadedNotGuiltyAssigned.getId()));
    }

    @Test
    public void shouldNotReturnReservedCaseForOtherUsers(){
        final CaseDetail pleadedNotGuiltyAssigned = CaseSaver.prosecutingAuthority("TFL").postedDaysAgo(10).notGuiltyWithDatesToAvoid().save(em, PLEADED_NOT_GUILTY);

        final UUID reserveUserId = randomUUID();
        reserveCase(em, pleadedNotGuiltyAssigned.getId(), reserveUserId, ZonedDateTime.now());

        List<AssignmentCandidate> delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(reserveUserId, prosecutingAuthorities("TFL"), NO_LIMIT);

        assertThat(delegatedPowersSessionCandidates.size(), is(1));
        assertThat(delegatedPowersSessionCandidates.get(0).getCaseId(), is(pleadedNotGuiltyAssigned.getId()));

        delegatedPowersSessionCandidates = assignmentRepository.getAssignmentCandidatesForDelegatedPowersSession(assigneeId, prosecutingAuthorities("TFL"), NO_LIMIT);

        assertThat(delegatedPowersSessionCandidates.size(), is(0));
    }

    private static List<UUID> getIds(final List<AssignmentCandidate> assignmentCandidates) {
        return assignmentCandidates.stream().map(AssignmentCandidate::getCaseId).collect(toList());
    }

    private static UUID[] getIds(final CaseDetail... assignmentCandidates) {
        return Stream.of(assignmentCandidates).map(CaseDetail::getId).toArray(UUID[]::new);
    }

    private static Set<String> prosecutingAuthorities(final String... prosecutingAuthorities) {
        return stream(prosecutingAuthorities).collect(toSet());
    }

    private void reserveCase(EntityManager em, final UUID caseId, final UUID userId, final ZonedDateTime reservedAt){
        final ReserveCase reserveCase = new ReserveCase(caseId, "CASEURN", userId, reservedAt);
        em.persist(reserveCase);
    }

    private static class CaseSaver {

        static final CaseReadinessReason EXPECT_NOT_TO_BE_READY = null;

        private String prosecutingAuthority;
        private LocalDate postingDate;
        private boolean completed;
        private boolean pendingWithdrawal;
        private UUID assigneeId;
        private PleaType plea;
        private int version = 1;
        private String datesToAvoid;
        private ZonedDateTime datesToAvoidPleaDate;

        static CaseSaver prosecutingAuthority(final String prosecutingAuthority) {
            return new CaseSaver(prosecutingAuthority);
        }

        private CaseSaver(final String prosecutingAuthority) {
            this.prosecutingAuthority = prosecutingAuthority;
        }


        private CaseSaver notGuiltyWithDatesToAvoid() {
            return plea(NOT_GUILTY)
                    .datesToAvoid("dates-to-avoid")
                    .pendingDatesToAvoid(TODAY_MIDNIGHT);
        }

        private CaseSaver postedDaysAgo(int daysAgo) {
            this.postingDate = TODAY_MIDNIGHT.minusDays(daysAgo).toLocalDate();
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

        CaseDetail save(EntityManager em, CaseReadinessReason expectedCaseReadinessReason) {
            final UUID caseId = randomUUID();
            final UUID defendantId = randomUUID();
            final UUID offenceId = randomUUID();

            final OffenceDetail offence = new OffenceDetail.OffenceDetailBuilder()
                    .setId(offenceId)
                    .setPlea(plea)
                    .setSequenceNumber(1)
                    .build();

            final DefendantDetail defendant = new DefendantDetail(defendantId, new PersonalDetails(), singletonList(offence), 2, new LegalEntityDetails(), new Address(), new ContactDetails());

            final CaseDetail caseDetail = CaseDetailBuilder.aCase()
                    .withCaseId(caseId)
                    .withProsecutingAuthority(prosecutingAuthority)
                    .withPostingDate(postingDate)
                    .withCompleted(completed)
                    .withAssigneeId(assigneeId)
                    .withDatesToAvoid(datesToAvoid)
                    .withDefendantDetail(defendant)
                    .build();

            final StreamStatus caseStreamStatus = new StreamStatus(caseId, "sjp", "EVENT_LISTENER", version);
            em.persist(caseDetail);
            em.persist(caseStreamStatus);

            //If the dates to avoid are provided, the case shouldn't be added to pending dates to avoid table
            final boolean datesToAvoidPresent = datesToAvoidPleaDate != null && StringUtils.isEmpty(datesToAvoid);
            if (datesToAvoidPresent) {
                em.persist(buildPendingDatesToAvoid(caseDetail, datesToAvoidPleaDate));
            } else {
                assertThat(em.find(PendingDatesToAvoid.class, caseDetail.getId()), nullValue());
            }

            CaseReadinessReason caseReadinessReason = EXPECT_NOT_TO_BE_READY;
            if (!completed && expectedCaseReadinessReason != EXPECT_NOT_TO_BE_READY) {
                // TODO: this business logic should come from prod

                if (pendingWithdrawal) {
                    caseReadinessReason = CaseReadinessReason.WITHDRAWAL_REQUESTED;
                } else if (plea != null &&
                        (!PleaType.NOT_GUILTY.equals(plea) ||
                                (!StringUtils.isEmpty(datesToAvoid) ||
                                        Optional.ofNullable(datesToAvoidPleaDate)
                                                .map(TODAY_MIDNIGHT.minusDays(10)::isAfter)
                                                .orElse(false)))) {
                    caseReadinessReason = caseReadinessReasonForPlea(plea);
                } else if (postingDate != null && postingDate.isBefore(now().minusDays(28))) {
                    caseReadinessReason = PIA;
                }


                assertThat(caseReadinessReason, equalTo(expectedCaseReadinessReason));

                em.persist(new ReadyCase(caseId, caseReadinessReason, assigneeId, getSessionType(pendingWithdrawal, plea), getPriority(pendingWithdrawal, plea), caseDetail.getProsecutingAuthority(), caseDetail.getPostingDate(), now()));
            }

            return caseDetail;
        }

        private static CaseReadinessReason caseReadinessReasonForPlea(final PleaType pleaType) {
            switch (pleaType) {
                case GUILTY:
                    return PLEADED_GUILTY;
                case NOT_GUILTY:
                    return PLEADED_NOT_GUILTY;
                case GUILTY_REQUEST_HEARING:
                    return PLEADED_GUILTY_REQUEST_HEARING;
                default:
                    throw new AssertionError("PleaType not mapped!");
            }
        }

        private static PendingDatesToAvoid buildPendingDatesToAvoid(final CaseDetail caseDetail,
                                                                    final ZonedDateTime datesToAvoidPleaDate) {
            PendingDatesToAvoid pendingDatesToAvoid = new PendingDatesToAvoid(caseDetail);
            pendingDatesToAvoid.setPleaDate(datesToAvoidPleaDate);

            return pendingDatesToAvoid;
        }

        private Integer getPriority(final boolean pendingWithdrawal, final PleaType plea) {
            if (pendingWithdrawal) {
                return 1;
            } else if (plea != null) {
                return 2;
            } else {
                return 3;
            }

        }

        private SessionType getSessionType(final boolean pendingWithdrawal, final PleaType plea) {
            if (pendingWithdrawal) {
                return DELEGATED_POWERS;
            }
            if (plea == GUILTY) {
                return MAGISTRATE;
            } else if (plea == NOT_GUILTY || plea == GUILTY_REQUEST_HEARING) {
                return DELEGATED_POWERS;
            }
            return MAGISTRATE;
        }

    }

}