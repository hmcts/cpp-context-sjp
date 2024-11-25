package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_GRANTED;
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

import java.time.ZonedDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
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
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;

public class CaseReadinessHandlerTest {

    private static final LocalDate DAYS_AGO_2 = LocalDate.now().minusDays(2);
    private static final LocalDate IN_5_DAYS = LocalDate.now().plusDays(5);
    private static final LocalDate IN_10_DAYS = LocalDate.now().plusDays(10);
    private static final LocalDate IN_28_DAYS = LocalDate.now().plusDays(28);

    private final CaseReadinessHandler caseReadinessHandler = CaseReadinessHandler.INSTANCE;
    private final UUID caseId = randomUUID();

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(NO_PLEA_RECEIVED, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).build(),
                        false, NONE, null),

                Arguments.of(COMPLETED, COMPLETED_APPLICATION_PENDING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(NO_PLEA_RECEIVED, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(NO_PLEA_RECEIVED, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(NO_PLEA_RECEIVED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_10_DAYS),

                Arguments.of(NO_PLEA_RECEIVED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).withNotGuiltyPlea().build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_10_DAYS),

                Arguments.of(NO_PLEA_RECEIVED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_10_DAYS).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(NO_PLEA_RECEIVED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).withGuiltyPlea().build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_5_DAYS),

                Arguments.of(NO_PLEA_RECEIVED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).build(),
                        true, NONE, null),

                Arguments.of(NO_PLEA_RECEIVED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).withNotGuiltyPlea().build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_10_DAYS),

                Arguments.of(NO_PLEA_RECEIVED, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(NO_PLEA_RECEIVED, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(NO_PLEA_RECEIVED, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(NO_PLEA_RECEIVED, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(NO_PLEA_RECEIVED, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).build(),
                        true, NONE, null),

                Arguments.of(NO_PLEA_RECEIVED_READY_FOR_DECISION, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_5_DAYS),

                Arguments.of(NO_PLEA_RECEIVED_READY_FOR_DECISION, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        false, NONE, null),

                Arguments.of(NO_PLEA_RECEIVED_READY_FOR_DECISION, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(NO_PLEA_RECEIVED_READY_FOR_DECISION, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_10_DAYS),

                Arguments.of(NO_PLEA_RECEIVED_READY_FOR_DECISION, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(NO_PLEA_RECEIVED_READY_FOR_DECISION, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(NO_PLEA_RECEIVED_READY_FOR_DECISION, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(NO_PLEA_RECEIVED_READY_FOR_DECISION, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(NO_PLEA_RECEIVED_READY_FOR_DECISION, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, DAYS_AGO_2),

                Arguments.of(PLEA_RECEIVED_READY_FOR_DECISION, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_5_DAYS),

                Arguments.of(PLEA_RECEIVED_READY_FOR_DECISION, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(PLEA_RECEIVED_READY_FOR_DECISION, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        false, NONE, null),

                Arguments.of(PLEA_RECEIVED_READY_FOR_DECISION, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withNotGuiltyPlea().withAdjournedTo(IN_10_DAYS).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_10_DAYS),

                Arguments.of(PLEA_RECEIVED_READY_FOR_DECISION, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(PLEA_RECEIVED_READY_FOR_DECISION, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(PLEA_RECEIVED_READY_FOR_DECISION, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(PLEA_RECEIVED_READY_FOR_DECISION, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(PLEA_RECEIVED_READY_FOR_DECISION, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, DAYS_AGO_2),

                Arguments.of(PLEA_RECEIVED_NOT_READY_FOR_DECISION, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_5_DAYS),

                Arguments.of(PLEA_RECEIVED_NOT_READY_FOR_DECISION, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).withNotGuiltyPlea().build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_10_DAYS),

                Arguments.of(PLEA_RECEIVED_NOT_READY_FOR_DECISION, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(PLEA_RECEIVED_NOT_READY_FOR_DECISION, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(PLEA_RECEIVED_NOT_READY_FOR_DECISION, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withNotGuiltyPlea().withAdjournedTo(IN_10_DAYS).build(),
                        false, NONE, null),

                Arguments.of(PLEA_RECEIVED_NOT_READY_FOR_DECISION, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(PLEA_RECEIVED_NOT_READY_FOR_DECISION, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(PLEA_RECEIVED_NOT_READY_FOR_DECISION, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(PLEA_RECEIVED_NOT_READY_FOR_DECISION, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(PLEA_RECEIVED_NOT_READY_FOR_DECISION, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(WITHDRAWAL_REQUEST_READY_FOR_DECISION, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_5_DAYS),

                Arguments.of(WITHDRAWAL_REQUEST_READY_FOR_DECISION, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(WITHDRAWAL_REQUEST_READY_FOR_DECISION, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(WITHDRAWAL_REQUEST_READY_FOR_DECISION, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_10_DAYS),

                Arguments.of(WITHDRAWAL_REQUEST_READY_FOR_DECISION, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        false, NONE, null),

                Arguments.of(WITHDRAWAL_REQUEST_READY_FOR_DECISION, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(WITHDRAWAL_REQUEST_READY_FOR_DECISION, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(WITHDRAWAL_REQUEST_READY_FOR_DECISION, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(WITHDRAWAL_REQUEST_READY_FOR_DECISION, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, DAYS_AGO_2),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REFERRED_FOR_COURT_HEARING, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REFERRED_FOR_COURT_HEARING, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REFERRED_FOR_COURT_HEARING, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REFERRED_FOR_COURT_HEARING, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REFERRED_FOR_COURT_HEARING, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REFERRED_FOR_COURT_HEARING, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        false, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REFERRED_FOR_COURT_HEARING, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REFERRED_FOR_COURT_HEARING, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(REFERRED_FOR_COURT_HEARING, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(COMPLETED, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(COMPLETED, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(COMPLETED, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(COMPLETED, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(COMPLETED, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(COMPLETED, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(COMPLETED, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        false, NONE, null),

                Arguments.of(COMPLETED, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(COMPLETED, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(COMPLETED_APPLICATION_PENDING, SET_ASIDE_READY_FOR_DECISION,
                        caseAggregateConfigBuilder()
                            .withPostingDateExpirationDate(DAYS_AGO_2)
                            .withApplication(
                                    application(STAT_DEC).withApplicationStatus(STATUTORY_DECLARATION_GRANTED).build()
                            ).build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(COMPLETED_APPLICATION_PENDING, COMPLETED,
                        caseAggregateConfigBuilder()
                                .withPostingDateExpirationDate(DAYS_AGO_2)
                                .withApplication(
                                        application(STAT_DEC).withApplicationStatus(STATUTORY_DECLARATION_REFUSED).build()
                                ).build(),
                        true, CASE_UNMARKED_READY_FOR_DECISION_RAISED, DAYS_AGO_2),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REOPENED_IN_LIBRA, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REOPENED_IN_LIBRA, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REOPENED_IN_LIBRA, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REOPENED_IN_LIBRA, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REOPENED_IN_LIBRA, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REOPENED_IN_LIBRA, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(REOPENED_IN_LIBRA, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REOPENED_IN_LIBRA, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        false, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(REOPENED_IN_LIBRA, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // Assume the UNKNOWN Status is a NOT_READY
                Arguments.of(UNKNOWN, NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS).build(),
                        true, NONE, null),

                Arguments.of(UNKNOWN, NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(UNKNOWN, PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(UNKNOWN, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_5_DAYS).withAdjournedTo(IN_10_DAYS).build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_10_DAYS),

                Arguments.of(UNKNOWN, PLEA_RECEIVED_NOT_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withAdjournedTo(IN_5_DAYS).withDatesToAvoidExpirationDate(IN_10_DAYS).withNotGuiltyPlea().build(),
                        true, CASE_EXPECTED_DATE_READY_CHANGED_RAISED, IN_10_DAYS),

                Arguments.of(UNKNOWN, WITHDRAWAL_REQUEST_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(UNKNOWN, REFERRED_FOR_COURT_HEARING,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(UNKNOWN, COMPLETED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                // NOT POSSIBLE TRANSITION
                Arguments.of(UNKNOWN, REOPENED_IN_LIBRA,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        true, NONE, null),

                Arguments.of(UNKNOWN, UNKNOWN,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(DAYS_AGO_2).withNotGuiltyPlea().build(),
                        false, NONE, null),

                Arguments.of(NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        NO_PLEA_RECEIVED_READY_FOR_DECISION,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS)
                                .withCaseReserved(false).withCaseReadinessReason(CaseReadinessReason.UNKNOWN).build(),
                        false, CASE_MARKED_READY_FOR_DECISION_RAISED, null),

                Arguments.of(NO_PLEA_RECEIVED,
                        NO_PLEA_RECEIVED,
                        caseAggregateConfigBuilder().withPostingDateExpirationDate(IN_28_DAYS)
                                .withCaseReserved(false).withCaseReadinessReason(CaseReadinessReason.UNKNOWN).build(),
                        false, CASE_UNMARKED_READY_FOR_DECISION_RAISED, IN_28_DAYS)
        );
    }

    private void setPleasFromConfig(CaseAggregateConfig aggregateConfig, CaseAggregateState state) {
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

    @ParameterizedTest
    @MethodSource("data")
    public void resolveCaseReadiness(CaseStatus previousStatus, CaseStatus currentStatus, CaseAggregateConfig aggregateConfig, Boolean isCaseStatusChanged, CaseReadinessEventRaised caseReadinessEventRaised, LocalDate expectedDateReady) {
        var state = new CaseAggregateState();
        state.setCaseId(caseId);
        state.setDatesToAvoidExpirationDate(aggregateConfig.getDatesToAvoidExpirationDate());
        if (nonNull(aggregateConfig.getDatesToAvoidExpirationDate())) {
            state.setDatesToAvoidPreviouslyRequested();
        }

        state.setAdjournedTo(aggregateConfig.getAdjournedTo());
        state.setPostingDate(aggregateConfig.getPostingDateExpirationDate().minusDays(28));
        state.setExpectedDateReady(aggregateConfig.getPostingDateExpirationDate());
        setPleasFromConfig(aggregateConfig, state);

        if (previousStatus.equals(REFERRED_FOR_COURT_HEARING)) {
            state.markCaseReferredForCourtHearing();
        }

        if (previousStatus.equals(COMPLETED)) {
            state.markCaseCompleted();
        }
        state.setCurrentApplication(aggregateConfig.getApplication());
        if(CaseReadinessReason.UNKNOWN.equals(aggregateConfig.getCaseReadinessReason())){
            state.markReady(ZonedDateTime.now(), CaseReadinessReason.UNKNOWN);
        }
        if(aggregateConfig.getCaseReserved()){
            state.markCaseReserved();
        }
        int eventsRaised = 0;
        final List<Object> events = whenStatusChangedTo(previousStatus, currentStatus, state);

        if (this.shouldMarkedReadyRaised(caseReadinessEventRaised)) {
            thenCaseMarkedReadyIsRaised(events);
            eventsRaised++;
        } else if (this.shouldUnMarkedReadyRaised(caseReadinessEventRaised)) {
            thenCaseUnMarkedReadyIsRaised(events, expectedDateReady);
            eventsRaised++;
        } else if (this.shouldExpectedDateReadyChangedRaised(caseReadinessEventRaised)) {
            thenCaseExpectedDateReadyChangedIsRaised(events, state, expectedDateReady);
            eventsRaised++;
        }

        if (isCaseStatusChanged) {
            thenCaseStatusChangedIsRaised(events, currentStatus);
            eventsRaised++;
        }

        assertThat(events.size(), is(eventsRaised));
    }

    private boolean shouldMarkedReadyRaised(CaseReadinessEventRaised caseReadinessEventRaised) {
        return caseReadinessEventRaised == CASE_MARKED_READY_FOR_DECISION_RAISED;
    }

    private boolean shouldUnMarkedReadyRaised(CaseReadinessEventRaised caseReadinessEventRaised) {
        return caseReadinessEventRaised == CASE_UNMARKED_READY_FOR_DECISION_RAISED;
    }

    private boolean shouldExpectedDateReadyChangedRaised(CaseReadinessEventRaised caseReadinessEventRaised) {
        return caseReadinessEventRaised == CASE_EXPECTED_DATE_READY_CHANGED_RAISED;
    }

    private List<Object> whenStatusChangedTo(final CaseStatus fromStatus, final CaseStatus toStatus, CaseAggregateState state) {
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

    private void thenCaseUnMarkedReadyIsRaised(final List<Object> eventList, LocalDate expectedDateReady) {
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseUnmarkedReadyForDecision.class),
                Matchers.<CaseMarkedReadyForDecision>hasProperty("caseId", is(this.caseId)),
                Matchers.<CaseMarkedReadyForDecision>hasProperty("expectedDateReady", is(expectedDateReady)))));
    }

    private void thenCaseExpectedDateReadyChangedIsRaised(final List<Object> eventList, CaseAggregateState state, LocalDate expectedDateReady) {
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseExpectedDateReadyChanged.class),
                Matchers.<CaseStatusChanged>hasProperty("oldExpectedDateReady", is(state.getExpectedDateReady())),
                Matchers.<CaseStatusChanged>hasProperty("newExpectedDateReady", is(expectedDateReady)))
        ));
    }

    private void thenCaseStatusChangedIsRaised(final List<Object> eventList, CaseStatus currentStatus) {
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseStatusChanged.class),
                Matchers.<CaseStatusChanged>hasProperty("caseId", is(caseId)),
                Matchers.<CaseStatusChanged>hasProperty("caseStatus", is(currentStatus)))
        ));
    }
}

