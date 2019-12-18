package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.ImmutableSortedMap.of;
import static java.lang.String.format;
import static java.text.NumberFormat.getCurrencyInstance;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REOPENED_IN_LIBRA;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFERRED_FOR_FUTURE_SJP_SESSION;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFERRED_TO_OPEN_COURT;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.persistence.entity.AdjournOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargePeriod;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferredToOpenCourtDecision;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Representation of each offence decision inside the offences->offenceDecisions array.
 */
public class OffenceDecisionView {

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("hh:mm a");
    private static final String NO_VERDICT_TEXT = "No verdict";

    private final List<OffenceDecisionLineView> lines;
    private String heading;
    private final UUID offenceDecisionId;
    private final UUID offenceId;
    private final OffenceDecision offenceDecision;

    private static final String LAST_DECISION_HEADING = "Court decision";
    private static final String PREVIOUS_DECISION_HEADING = "Previous court decision";

    private static final Map<PleaType, String> PLEA_NAMES = of(
            GUILTY, "Guilty",
            NOT_GUILTY, "Not Guilty",
            GUILTY_REQUEST_HEARING, "Guilty, court hearing requested"
    );

    private static final Map<VerdictType, String> VERDICT_NAMES = of(
            FOUND_GUILTY, "Guilty plea accepted",
            FOUND_NOT_GUILTY, "Found not guilty",
            NO_VERDICT, NO_VERDICT_TEXT,
            PROVED_SJP, "Proved SJP"
    );

    private static final Map<DischargeType, String> DISCHARGE_TYPES = of(
            ABSOLUTE, "Discharged absolutely",
            CONDITIONAL, "Discharged conditionally"
    );


    public OffenceDecisionView(final CaseDetail caseDetail, final CaseDecision caseDecision,
                               final OffenceDecision offenceDecision) {
        this.offenceDecisionId = offenceDecision.getCaseDecisionId();
        this.offenceId = offenceDecision.getOffenceId();
        this.offenceDecision = offenceDecision;

        final CaseStatus caseStatus = caseDetail.getCaseStatus();
        final String savedDate = ofNullable(caseDecision.getSavedAt())
                .map(savedAt -> savedAt.toLocalDate().format(DATE_FORMAT))
                .orElse("");

        this.lines = new ArrayList<>();

        addHeadingLine(caseDetail, caseStatus);
        addPleaLine();
        addVerdictLine(savedDate);
        addResultLine();
        if (isDischarge()) {
            addConditionalDischargePeriodLine();
        }

        if (isDischarge() || isFinancialPenalty()) {
            addFineLine();
            addCompensationLine();
            addNoCompensationReasonLine();
            addGuiltyPleaTakenIntoAccountLine();
        }
        addDecisionMadeLine(savedDate);
        addDecisionSetAsideLine(caseDetail, caseStatus);
    }

    private void addFineLine() {
        if (isFinancialPenalty()) {
            final BigDecimal fine = asFinancialPenalty().getFine();
            if (BigDecimal.ZERO.compareTo(fine) <= 0) {
                addLine("To pay a fine of", formatCurrency(fine));
            }
        }
    }

    private void addConditionalDischargePeriodLine() {
        if (asDischarge().getDischargeType() == CONDITIONAL) {
            final DischargePeriod dischargePeriod = asDischarge().getDischargePeriod();
            final String unit = dischargePeriod.getUnit().name();
            final int value = dischargePeriod.getValue();
            addLine("Period", format("%s %s", value,
                    value > 1 ? unit.toLowerCase() + "s" : unit.toLowerCase()));
        }
    }

    private void addCompensationLine() {
        final BigDecimal compensation = isDischarge() ? asDischarge().getCompensation() : asFinancialPenalty().getCompensation();
        if (compensation.compareTo(BigDecimal.ZERO) > 0) {
            addLine("To pay compensation of", formatCurrency(compensation));
        }
    }

    private void addNoCompensationReasonLine() {
        final String noCompensationReason = isDischarge() ? asDischarge().getNoCompensationReason() : asFinancialPenalty().getNoCompensationReason();
        if (isNotBlank(noCompensationReason)) {
            addLine("No compensation ordered\nbecause", noCompensationReason);
        }
    }

    private void addGuiltyPleaTakenIntoAccountLine() {
        final Boolean guiltyPleaTakenIntoAccount = isDischarge() ? asDischarge().isGuiltyPleaTakenIntoAccount() : asFinancialPenalty().isGuiltyPleaTakenIntoAccount();
        if (Boolean.TRUE.equals(guiltyPleaTakenIntoAccount)) {
            addLine("Defendant's guilty plea", "Taken into account when imposing sentence");
        }
    }

    private void addHeadingLine(final CaseDetail caseDetail, final CaseStatus caseStatus) {
        final boolean isLastDecision = calcIsLastDecision(caseDetail, offenceDecision);
        this.heading = getCourtDecisionHeader(caseStatus, isLastDecision);
    }

    private void addPleaLine() {
        final Optional<String> plea = getPlea(offenceDecision);
        if (plea.isPresent()) {
            addLine("Plea", plea.get());
            getPleaDate(offenceDecision)
                    .map(pleaDate -> new OffenceDecisionLineView("Plea date", pleaDate))
                    .ifPresent(lines::add);
        } else {
            addLine("Plea", "No plea received");
        }
    }

    private void addVerdictLine(final String savedDate) {
        final String verdict = ofNullable(offenceDecision.getVerdictType())
                .map(VERDICT_NAMES::get)
                .orElse(NO_VERDICT_TEXT);

        if (!NO_VERDICT_TEXT.equals(verdict)) {
            addLine("Verdict", verdict);
            addLine("Date of verdict", savedDate);
        }
    }

    private void addDecisionSetAsideLine(final CaseDetail caseDetail, final CaseStatus caseStatus) {
        if (!caseStatus.equals(REOPENED_IN_LIBRA)) {
            return;
        }
        final String value = format("Case reopened %s%nLibra account no. %s",
                DATE_FORMAT.format(caseDetail.getReopenedDate()),
                caseDetail.getLibraCaseNumber());
        addLine("Decision set aside", value);
    }

    private void addDecisionMadeLine(String savedDate) {
        this.lines.add(new OffenceDecisionLineView("Decision made", savedDate));
    }

    private void addResultLine() {
        final StringBuilder result = new StringBuilder();
        final DecisionType decisionType = offenceDecision.getDecisionType();
        if (FINANCIAL_PENALTY.equals(decisionType)) {
            return;
        }
        if (DISCHARGE.equals(decisionType)) {
            result.append(DISCHARGE_TYPES.get(asDischarge().getDischargeType()));
        } else {
            result.append(getResultNames().get(decisionType));
            if (ADJOURN.equals(decisionType)) {
                result.append("\nOn or after ").append(((AdjournOffenceDecision) offenceDecision).getAdjournedTo().format(DATE_FORMAT));
            } else if (REFERRED_TO_OPEN_COURT.equals(decisionType)) {
                final ReferredToOpenCourtDecision referredToOpenCourtDecision = (ReferredToOpenCourtDecision) offenceDecision;
                result.append(format("%nSummons issued for hearing before %s on %s at %s at %s",
                        referredToOpenCourtDecision.getReferredToCourt(),
                        referredToOpenCourtDecision.getReferredToDateTime().format(DATE_FORMAT),
                        referredToOpenCourtDecision.getMagistratesCourt(),
                        referredToOpenCourtDecision.getReferredToDateTime().format(HOUR_FORMAT)));
                result.append(format("%nReason: %s", referredToOpenCourtDecision.getReason()));
            }
        }
        addLine("Result", result.toString());
    }

    private Map<DecisionType, String> getResultNames () {
        final Map<DecisionType, String> resultNames = new EnumMap<>(DecisionType.class);
        resultNames.put(DISMISS, "Dismissed");
        resultNames.put(WITHDRAW, "Withdrawn");
        resultNames.put(ADJOURN, "Adjourn to later SJP hearing.");
        resultNames.put(REFER_FOR_COURT_HEARING, "Referred for court hearing.");
        resultNames.put(DISCHARGE, "Discharged");
        resultNames.put(REFERRED_TO_OPEN_COURT, "Referred to full court hearing");
        resultNames.put(REFERRED_FOR_FUTURE_SJP_SESSION, "Referred for future SJP session");
        return copyOf(resultNames);
    }

    private String getCourtDecisionHeader(final CaseStatus caseStatus, final boolean isLastDecision) {
        final StringBuilder offenceDecisionHeading = new StringBuilder();
        offenceDecisionHeading.append(isLastDecision ? LAST_DECISION_HEADING : PREVIOUS_DECISION_HEADING);
        if (nonNull(caseStatus) && caseStatus.equals(REOPENED_IN_LIBRA)) {
            offenceDecisionHeading.append(" (set aside)");
        }
        return offenceDecisionHeading.toString();
    }

    private Optional<String> getPlea(final OffenceDecision offenceDecision) {
        return ofNullable(offenceDecision.getPleaAtDecisionTime()).map(PLEA_NAMES::get);
    }

    private Optional<String> getPleaDate(final OffenceDecision offenceDecision) {
        return ofNullable(offenceDecision.getPleaDate()).map(DATE_FORMAT::format);
    }


    private boolean calcIsLastDecision(CaseDetail caseDetail, OffenceDecision offenceDecision) {
        return caseDetail.getCaseDecisions().stream()
                .sorted(comparing(CaseDecision::getSavedAt, reverseOrder()))
                .flatMap(decision -> decision.getOffenceDecisions().stream())
                .collect(Collectors.groupingBy(OffenceDecision::getOffenceId))
                .entrySet().stream()
                .anyMatch(entry -> entry.getKey().equals(offenceDecision.getOffenceId())
                        && entry.getValue().get(0).equals(offenceDecision));
    }

    private void addLine(String label, String value) {
        this.lines.add(new OffenceDecisionLineView(label, value));
    }


    public List<OffenceDecisionLineView> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public String getHeading() {
        return heading;
    }

    public UUID getOffenceDecisionId() {
        return offenceDecisionId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    private DischargeOffenceDecision asDischarge() {
        return (DischargeOffenceDecision) offenceDecision;
    }

    private FinancialPenaltyOffenceDecision asFinancialPenalty() {
        return (FinancialPenaltyOffenceDecision) offenceDecision;
    }

    private boolean isDischarge() {
        return offenceDecision instanceof DischargeOffenceDecision;
    }

    private boolean isFinancialPenalty() {
        return offenceDecision instanceof FinancialPenaltyOffenceDecision;
    }

    private String formatCurrency(BigDecimal value) {
        return getCurrencyInstance(Locale.UK).format(value).replace(".00", "");
    }

}


