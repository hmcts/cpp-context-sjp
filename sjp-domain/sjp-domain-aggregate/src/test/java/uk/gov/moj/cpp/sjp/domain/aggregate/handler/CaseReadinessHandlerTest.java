package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_GRANTED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_PENDING;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_REFUSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.STAT_DEC;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseAggregateConfig.ApplicationBuilder.application;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseAggregateConfig.Builder.caseAggregateConfigBuilder;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseReadinessEventRaised.CASE_EXPECTED_DATE_READY_CHANGED_RAISED;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseReadinessEventRaised.CASE_MARKED_READY_FOR_DECISION_RAISED;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseReadinessEventRaised.CASE_UNMARKED_READY_FOR_DECISION_RAISED;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseReadinessEventRaised.NONE;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED_APPLICATION_PENDING;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.NO_PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_NOT_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.PLEA_RECEIVED_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REFERRED_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REOPENED_IN_LIBRA;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.SET_ASIDE_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.UNKNOWN;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.WITHDRAWAL_REQUEST_READY_FOR_DECISION;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.common.CaseState;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.event.CaseExpectedDateReadyChanged;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseStatusChanged;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CaseReadinessHandlerTest {

    private static final LocalDate DAYS_AGO_2 = LocalDate.now().minusDays(2);
    private static final LocalDate IN_5_DAYS = LocalDate.now().plusDays(5);
    private static final LocalDate IN_10_DAYS = LocalDate.now().plusDays(10);
    private static final LocalDate IN_28_DAYS = LocalDate.now().plusDays(28);

    private final CaseReadinessHandler caseReadinessHandler = CaseReadinessHandler.INSTANCE;
    private final UUID caseId = randomUUID();

    private CaseAggregateState state;

    @Parameterized.Parameter
    public CaseStatus previousStatus;

    @Parameterized.Parameter(1)
    public CaseStatus currentStatus;

    @Parameterized.Parameter(2)
    public CaseAggregateConfig aggregateConfig;

    @Parameterized.Parameter(3)
    public Boolean isCaseStatusChanged;

    @Parameterized.Parameter(4)
    public CaseReadinessEventRaised caseReadinessEventRaised;

    @Parameterized.Parameter(5)
    public LocalDate expectedDateReady;

    @Parameterized.Parameters(name = "previous case status={0}, current case status={1},  aggregateConfig={2}," +
            "is case status changed={3}" + ", case readiness event raised={4}, expectedDateReady={5}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {NO_PLEA_RECEIVED, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).build(),
                        false, NONE, null},

                {COMPLETED, COMPLETED_APPLICATION_PENDING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {NO_PLEA_RECEIVED, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {NO_PLEA_RECEIVED, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {NO_PLEA_RECEIVED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_10_DAYS},

                {NO_PLEA_RECEIVED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).withNotGuiltyPlea().build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_10_DAYS},

                {NO_PLEA_RECEIVED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_10_DAYS).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {NO_PLEA_RECEIVED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).withGuiltyPlea().build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_5_DAYS},

                {NO_PLEA_RECEIVED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).build(),
                        true, NONE, null},

                {NO_PLEA_RECEIVED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).withNotGuiltyPlea().build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_10_DAYS},

                {NO_PLEA_RECEIVED, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                // NOT POSSIBLE TRANSITION
                {NO_PLEA_RECEIVED, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {NO_PLEA_RECEIVED, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {NO_PLEA_RECEIVED, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {NO_PLEA_RECEIVED, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).build(),
                        true, NONE, null},

                {NO_PLEA_RECEIVED_READY_FOR_DECISION, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_5_DAYS},

                {NO_PLEA_RECEIVED_READY_FOR_DECISION, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        false, NONE, null},

                {NO_PLEA_RECEIVED_READY_FOR_DECISION, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {NO_PLEA_RECEIVED_READY_FOR_DECISION, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_10_DAYS},

                {NO_PLEA_RECEIVED_READY_FOR_DECISION, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {NO_PLEA_RECEIVED_READY_FOR_DECISION, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {NO_PLEA_RECEIVED_READY_FOR_DECISION, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {NO_PLEA_RECEIVED_READY_FOR_DECISION, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {NO_PLEA_RECEIVED_READY_FOR_DECISION, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, DAYS_AGO_2},

                {PLEA_RECEIVED_READY_FOR_DECISION, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_5_DAYS},

                {PLEA_RECEIVED_READY_FOR_DECISION, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {PLEA_RECEIVED_READY_FOR_DECISION, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        false, NONE, null},

                {PLEA_RECEIVED_READY_FOR_DECISION, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withNotGuiltyPlea().withAdjournedTo(IN_10_DAYS).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_10_DAYS},

                {PLEA_RECEIVED_READY_FOR_DECISION, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {PLEA_RECEIVED_READY_FOR_DECISION, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {PLEA_RECEIVED_READY_FOR_DECISION, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {PLEA_RECEIVED_READY_FOR_DECISION, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {PLEA_RECEIVED_READY_FOR_DECISION, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, DAYS_AGO_2},

                {PLEA_RECEIVED_NOT_READY_FOR_DECISION, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_5_DAYS},

                {PLEA_RECEIVED_NOT_READY_FOR_DECISION, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).withNotGuiltyPlea().build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_10_DAYS},

                {PLEA_RECEIVED_NOT_READY_FOR_DECISION, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {PLEA_RECEIVED_NOT_READY_FOR_DECISION, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {PLEA_RECEIVED_NOT_READY_FOR_DECISION, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withNotGuiltyPlea().withAdjournedTo(IN_10_DAYS).build(),
                        false, NONE, null},

                {PLEA_RECEIVED_NOT_READY_FOR_DECISION, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                // NOT POSSIBLE TRANSITION
                {PLEA_RECEIVED_NOT_READY_FOR_DECISION, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {PLEA_RECEIVED_NOT_READY_FOR_DECISION, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {PLEA_RECEIVED_NOT_READY_FOR_DECISION, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {PLEA_RECEIVED_NOT_READY_FOR_DECISION, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {WITHDRAWAL_REQUEST_READY_FOR_DECISION, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_5_DAYS},

                {WITHDRAWAL_REQUEST_READY_FOR_DECISION, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {WITHDRAWAL_REQUEST_READY_FOR_DECISION, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {WITHDRAWAL_REQUEST_READY_FOR_DECISION, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_10_DAYS},

                {WITHDRAWAL_REQUEST_READY_FOR_DECISION, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        false, NONE, null},

                {WITHDRAWAL_REQUEST_READY_FOR_DECISION, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {WITHDRAWAL_REQUEST_READY_FOR_DECISION, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {WITHDRAWAL_REQUEST_READY_FOR_DECISION, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {WITHDRAWAL_REQUEST_READY_FOR_DECISION, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, DAYS_AGO_2},

                // NOT POSSIBLE TRANSITION
                {REFERRED_FOR_COURT_HEARING, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REFERRED_FOR_COURT_HEARING, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REFERRED_FOR_COURT_HEARING, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REFERRED_FOR_COURT_HEARING, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REFERRED_FOR_COURT_HEARING, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REFERRED_FOR_COURT_HEARING, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        false, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REFERRED_FOR_COURT_HEARING, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REFERRED_FOR_COURT_HEARING, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {REFERRED_FOR_COURT_HEARING, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {COMPLETED, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {COMPLETED, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {COMPLETED, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {COMPLETED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {COMPLETED, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {COMPLETED, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {COMPLETED, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        false, NONE, null},

                {COMPLETED, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {COMPLETED, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {COMPLETED_APPLICATION_PENDING, SET_ASIDE_READY_FOR_DECISION,
                        caseAggregateConfigBuilder()
                            .withPostingDateExpirationDate(DAYS_AGO_2)
                            .withApplication(
                                    application(STAT_DEC).withApplicationStatus(STATUTORY_DECLARATION_GRANTED).build()
                            ).build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {COMPLETED_APPLICATION_PENDING, COMPLETED,
                        caseAggregateConfigBuilder()
                                .withPostingDateExpirationDate(DAYS_AGO_2)
                                .withApplication(
                                        application(STAT_DEC).withApplicationStatus(STATUTORY_DECLARATION_REFUSED).build()
                                ).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, DAYS_AGO_2},

                // NOT POSSIBLE TRANSITION
                {REOPENED_IN_LIBRA, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REOPENED_IN_LIBRA, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REOPENED_IN_LIBRA, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REOPENED_IN_LIBRA, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REOPENED_IN_LIBRA, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REOPENED_IN_LIBRA, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {REOPENED_IN_LIBRA, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REOPENED_IN_LIBRA, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        false, NONE, null},

                // NOT POSSIBLE TRANSITION
                {REOPENED_IN_LIBRA, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // Assume the UNKNOWN Status is a NOT_READY
                {UNKNOWN, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).build(),
                        true, NONE, null},

                {UNKNOWN, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {UNKNOWN, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                {UNKNOWN, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_10_DAYS},

                {UNKNOWN, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).withNotGuiltyPlea().build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_10_DAYS},

                {UNKNOWN, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null},

                // NOT POSSIBLE TRANSITION
                {UNKNOWN, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {UNKNOWN, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                // NOT POSSIBLE TRANSITION
                {UNKNOWN, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null},

                {UNKNOWN, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        false, NONE, null},
        });
    }

    @Before
    public void onceBeforeEachTest() {
        state = new CaseAggregateState();
        state.setCaseId(caseId);
        state.setDatesToAvoidExpirationDate(aggregateConfig.getDatesToAvoidExpirationDate());
        if (nonNull(aggregateConfig.getDatesToAvoidExpirationDate())) {
            state.setDatesToAvoidPreviouslyRequested();
        }

        state.setAdjournedTo(aggregateConfig.getAdjournedTo());
        state.setPostingDate(aggregateConfig.getPostingDateExpirationDate().minusDays(28));
        state.setExpectedDateReady(aggregateConfig.getPostingDateExpirationDate());
        setPleasFromConfig();

        if (this.previousStatus.equals(REFERRED_FOR_COURT_HEARING)) {
            state.markCaseReferredForCourtHearing();
        }

        if (this.previousStatus.equals(COMPLETED)) {
            state.markCaseCompleted();
        }
        state.setCurrentApplication(aggregateConfig.getApplication());
    }

    private void setPleasFromConfig() {
        final List<Plea> pleas = new ArrayList<>();
        if (aggregateConfig.isNotGuiltyPleaPresent()) {
            pleas.add(new Plea(randomUUID(), randomUUID(), NOT_GUILTY));
            state.setDatesToAvoidPreviouslyRequested();
        }

        if (aggregateConfig.isGuiltyPleaPresent()) {
            pleas.add(new Plea(randomUUID(), randomUUID(), GUILTY));
        }
        state.setPleas(pleas);
    }

    @Test
    public void resolveCaseReadiness() {
        int eventsRaised = 0;
        final List<Object> events = whenStatusChangedTo(this.previousStatus, this.currentStatus);

        if (this.shouldMarkedReadyRaised()) {
            thenCaseMarkedReadyIsRaised(events);
            eventsRaised++;
        } else if (this.shouldUnMarkedReadyRaised()) {
            thenCaseUnMarkedReadyIsRaised(events);
            eventsRaised++;
        } else if (this.shouldExpectedDateReadyChangedRaised()) {
            thenCaseExpectedDateReadyChangedIsRaised(events);
            eventsRaised++;
        }

        if (isCaseStatusChanged) {
            thenCaseStatusChangedIsRaised(events);
            eventsRaised++;
        }

        assertThat(events.size(), is(eventsRaised));
    }

    private boolean shouldMarkedReadyRaised() {
        return this.caseReadinessEventRaised == CASE_MARKED_READY_FOR_DECISION_RAISED;
    }

    private boolean shouldUnMarkedReadyRaised() {
        return this.caseReadinessEventRaised == CASE_UNMARKED_READY_FOR_DECISION_RAISED;
    }

    private boolean shouldExpectedDateReadyChangedRaised() {
        return this.caseReadinessEventRaised == CASE_EXPECTED_DATE_READY_CHANGED_RAISED;
    }

    private List<Object> whenStatusChangedTo(final CaseStatus fromStatus, final CaseStatus toStatus) {
        return caseReadinessHandler.resolveCaseReadiness(state, new CaseState(fromStatus), new CaseState(toStatus))
                .collect(toList());
    }

    private void thenCaseMarkedReadyIsRaised(final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseMarkedReadyForDecision.class),
                Matchers.<CaseMarkedReadyForDecision>hasProperty("caseId", is(caseId)),
                Matchers.<CaseMarkedReadyForDecision>hasProperty("reason", notNullValue()),
                Matchers.<CaseMarkedReadyForDecision>hasProperty("markedAt", notNullValue()),
                Matchers.<CaseMarkedReadyForDecision>hasProperty("sessionType", notNullValue()),
                Matchers.<CaseMarkedReadyForDecision>hasProperty("priority", notNullValue()))));
    }

    private void thenCaseUnMarkedReadyIsRaised(final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseUnmarkedReadyForDecision.class),
                Matchers.<CaseMarkedReadyForDecision>hasProperty("caseId", is(this.caseId)),
                Matchers.<CaseMarkedReadyForDecision>hasProperty("expectedDateReady", is(this.expectedDateReady)))));
    }

    private void thenCaseExpectedDateReadyChangedIsRaised(final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseExpectedDateReadyChanged.class),
                Matchers.<CaseStatusChanged>hasProperty("oldExpectedDateReady", is(this.state.getExpectedDateReady())),
                Matchers.<CaseStatusChanged>hasProperty("newExpectedDateReady", is(this.expectedDateReady)))
        ));
    }

    private void thenCaseStatusChangedIsRaised(final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseStatusChanged.class),
                Matchers.<CaseStatusChanged>hasProperty("caseId", is(caseId)),
                Matchers.<CaseStatusChanged>hasProperty("caseStatus", is(currentStatus)))
        ));
    }
}

