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
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REOPENED_IN_LIBRA;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.NO_SEPARATE_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFERRED_FOR_FUTURE_SJP_SESSION;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFERRED_TO_OPEN_COURT;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.SET_ASIDE;
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
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriodTimeUnit;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.persistence.entity.AdjournOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargePeriod;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.NoSeparatePenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferredToOpenCourtDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.PressRestriction;



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
@SuppressWarnings("squid:S1151")
public class OffenceDecisionView {

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter HOUR_FORMAT = ofPattern("hh:mm a");
    private static final String NO_VERDICT_TEXT = "o verdict";
    private static final String DRIVER_RECORD_ENDORSED  = "Driver record endorsed";
    private static final String DISCHARGE_PERIOD = "Period";


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
            addExcisePenaltyLine();
            addBackDutyLine();
            addCompensationLine();
            addNoCompensationReasonLine();
            addGuiltyPleaTakenIntoAccountLine();

        }
        addEndorsement();
        addDisqualification();

        addDecisionMadeLine(savedDate);
        addDecisionSetAsideLine(caseDetail, caseStatus);
        addPressRestrictions(offenceDecision);

    }
  private void addPressRestrictions  (OffenceDecision offenceDecision){
      final PressRestriction pressRestriction =offenceDecision.getPressRestriction();
      if (nonNull(pressRestriction)){
          final StringBuilder result = new StringBuilder();
          if (pressRestriction.getRequested()) {
              result.append("Direction made under Section 45 of the Youth Justice and Criminal Evidence Act 1999  in respect of "+pressRestriction.getName());
          } else {
              result.append("Direction restricting publicity revoked");
          }
          addLine("Reporting restriction", result.toString());
      }
  }

    private void addFineLine() {
        if (isFinancialPenalty()) {
            final BigDecimal fine = asFinancialPenalty().getFine();
            if (nonNull(fine) && BigDecimal.ZERO.compareTo(fine) <= 0) {
                addLine("To pay a fine of", formatCurrency(fine));
            }
        }
    }

    private void addExcisePenaltyLine() {
        if (isFinancialPenalty()) {
            final BigDecimal excisePenalty = asFinancialPenalty().getExcisePenalty();
            if (nonNull(excisePenalty) && BigDecimal.ZERO.compareTo(excisePenalty) < 0) {
                addLine("To pay an excise penalty of", formatCurrency(excisePenalty));
            }
        }
    }

    private void addBackDutyLine() {
        BigDecimal backDuty = null;
        if (isFinancialPenalty()) {
            backDuty = asFinancialPenalty().getBackDuty();
        } else if (isDischarge()) {
            backDuty = asDischarge().getBackDuty();
        }

        if (nonNull(backDuty) && BigDecimal.ZERO.compareTo(backDuty) < 0) {
            addLine("To pay back duty of", formatCurrency(backDuty));
        }
    }

    private void addConditionalDischargePeriodLine() {
        if (asDischarge().getDischargeType() == CONDITIONAL) {
            final DischargePeriod dischargePeriod = asDischarge().getDischargePeriod();
            final String unit = dischargePeriod.getUnit().name();
            final int dischargePeriodValue = dischargePeriod.getValue();
            addLine(DISCHARGE_PERIOD, format("%s %s", dischargePeriodValue,
                    dischargePeriodValue > 1 ? unit.toLowerCase() + "s" : unit.toLowerCase()));
        }
    }


    private void addCompensationLine() {
        final BigDecimal compensation = isDischarge() ? asDischarge().getCompensation() : asFinancialPenalty().getCompensation();
        if (nonNull(compensation) && compensation.compareTo(BigDecimal.ZERO) > 0) {
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
        if (nonNull(guiltyPleaTakenIntoAccount) && Boolean.TRUE.equals(guiltyPleaTakenIntoAccount)) {
            addLine("Defendant's guilty plea", "Taken into account when imposing sentence");
        }
    }

    private void addHeadingLine(final CaseDetail caseDetail, final CaseStatus caseStatus) {
        final boolean isLastDecision = calcIsLastDecision(caseDetail, offenceDecision);
        this.heading = getCourtDecisionHeader(caseStatus, isLastDecision);
    }

    private void addPleaLine() {
        final Optional<String> plea = getPlea(offenceDecision);
        final DecisionType decisionType = offenceDecision.getDecisionType();
        if (! SET_ASIDE.equals(decisionType)) {

            if (plea.isPresent()) {
                addLine("Plea", plea.get());
                getPleaDate(offenceDecision)
                        .map(pleaDate -> new OffenceDecisionLineView("Plea date", pleaDate))
                        .ifPresent(lines::add);
            } else {
                addLine("Plea", "No plea received");
            }
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
        if (!REOPENED_IN_LIBRA.equals(caseStatus)) {
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

        switch (decisionType) {
            case FINANCIAL_PENALTY:
                return;
            case DISCHARGE:
                result.append(DISCHARGE_TYPES.get(asDischarge().getDischargeType()));
                break;
            case ADJOURN:
                result.append(getResultNames().get(decisionType));
                result.append("\nOn or after ").append(((AdjournOffenceDecision) offenceDecision).getAdjournedTo().format(DATE_FORMAT));
                break;
            case REFERRED_TO_OPEN_COURT:
                result.append(getResultNames().get(decisionType));
                final ReferredToOpenCourtDecision referredToOpenCourtDecision = (ReferredToOpenCourtDecision) offenceDecision;
                result.append(format("%nSummons issued for hearing before %s on %s at %s at %s",
                        referredToOpenCourtDecision.getReferredToCourt(),
                        referredToOpenCourtDecision.getReferredToDateTime().format(DATE_FORMAT),
                        referredToOpenCourtDecision.getMagistratesCourt(),
                        referredToOpenCourtDecision.getReferredToDateTime().format(HOUR_FORMAT)));
                result.append(format("%nReason: %s", referredToOpenCourtDecision.getReason()));
                break;
            default:
                result.append(getResultNames().get(decisionType));

        }
        addLine("Result", result.toString());

    }

    private void addEndorsement() {
        final DecisionType decisionType = offenceDecision.getDecisionType();

        if (FINANCIAL_PENALTY.equals(decisionType) && ofNullable(asFinancialPenalty().getLicenceEndorsement()).orElse(false)) {
            addEndorsement(ofNullable(asFinancialPenalty().getDisqualification()).orElse(false), asFinancialPenalty().getPenaltyPointsImposed(), asFinancialPenalty().getAdditionalPointsReason());
        }

        if (DISCHARGE.equals(decisionType) && ofNullable(asDischarge().getLicenceEndorsement()).orElse(false)) {
            addEndorsement(ofNullable(asDischarge().getDisqualification()).orElse(false), asDischarge().getPenaltyPointsImposed(), asDischarge().getAdditionalPointsReason());
        }

        if (NO_SEPARATE_PENALTY.equals(decisionType) && ofNullable(asNoSeparatePenalty().getLicenceEndorsement()).orElse(false)) {
            ofNullable(asNoSeparatePenalty().getLicenceEndorsement()).ifPresent(value -> addLine(DRIVER_RECORD_ENDORSED, EMPTY));
        }
    }

    private void addEndorsement(final boolean isDisqualification, final Integer penaltyPointsImposed, final String additionalPointsReason) {
        if(!isDisqualification) {
            addLine(DRIVER_RECORD_ENDORSED, EMPTY);
        }
        ofNullable(penaltyPointsImposed).ifPresent(value -> addLine("Penalty points", value.toString()));
        ofNullable(additionalPointsReason).ifPresent(value -> addLine("Reason for applying points on more than 1 offence", value));
    }

    private void addDisqualification() {
        final DecisionType decisionType = offenceDecision.getDecisionType();
        if (FINANCIAL_PENALTY.equals(decisionType) && ofNullable(asFinancialPenalty().getDisqualification()).orElse(false)) {
            addDisqualification(asFinancialPenalty().getDisqualificationType(),
                    asFinancialPenalty().getDisqualificationPeriodUnit(),
                    asFinancialPenalty().getDisqualificationPeriodValue(),
                    asFinancialPenalty().getNotionalPenaltyPoints());
        }
        if (DISCHARGE.equals(decisionType) && ofNullable(asDischarge().getDisqualification()).orElse(false)) {
            addDisqualification(asDischarge().getDisqualificationType(),
                    asDischarge().getDisqualificationPeriodUnit(),
                    asDischarge().getDisqualificationPeriodValue(),
                    asDischarge().getNotionalPenaltyPoints());
        }
    }

    private void addDisqualification(final DisqualificationType disqualificationType,
                                     final DisqualificationPeriodTimeUnit disqualificationPeriodUnit,
                                     final Integer disqualificationPeriodValue,
                                     final Integer notionalPenaltyPoints) {
        final String disqualificationTypeText = getDisqualificationTypeText(disqualificationType);
        ofNullable(disqualificationTypeText).ifPresent(value -> addLine(DRIVER_RECORD_ENDORSED, value));
        addLine("Disqualified for", format("%s %s", disqualificationPeriodValue,
                disqualificationPeriodValue > 1 ? disqualificationPeriodUnit.toString().toLowerCase()+ "s" : disqualificationPeriodUnit.toString().toLowerCase()));
        ofNullable(notionalPenaltyPoints).ifPresent(value -> addLine("Notional penalty points", value.toString()));
    }

    private String getDisqualificationTypeText(final DisqualificationType disqualificationType) {
        return ofNullable(disqualificationType)
                .map(value -> {
                    switch (value) {
                        case POINTS:
                            return "Section 35(1) Road Traffic Offenders Act 1988";
                        case OBLIGATORY:
                            return "Section 34(1) Road Traffic Offenders Act 1988";
                        case DISCRETIONARY:
                            return "Section 34(2) Road Traffic Offenders Act 1988";
                        default:
                            return null;
                    }
                }).orElse(null);
    }

    private Map<DecisionType, String> getResultNames() {
        final Map<DecisionType, String> resultNames = new EnumMap<>(DecisionType.class);
        resultNames.put(DISMISS, "Dismissed");
        resultNames.put(WITHDRAW, "Withdrawn");
        resultNames.put(ADJOURN, "Adjourn to later SJP hearing.");
        resultNames.put(REFER_FOR_COURT_HEARING, "Referred for court hearing.");
        resultNames.put(DISCHARGE, "Discharged");
        resultNames.put(REFERRED_TO_OPEN_COURT, "Referred to full court hearing");
        resultNames.put(REFERRED_FOR_FUTURE_SJP_SESSION, "Referred for future SJP session");
        resultNames.put(SET_ASIDE, "Decision set aside");
        resultNames.put(NO_SEPARATE_PENALTY, "No seperate penalty");

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

    private NoSeparatePenaltyOffenceDecision asNoSeparatePenalty() {
        return (NoSeparatePenaltyOffenceDecision) offenceDecision;
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


