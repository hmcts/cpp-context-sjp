package uk.gov.moj.cpp.sjp.query.view.service.defendantcase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PotentialCases {

    private static final Logger LOGGER = LoggerFactory.getLogger(PotentialCases.class);

    private static final PotentialCases  EMPTY_POTENTIAL_CASE = new PotentialCases();

    private static final Comparator<CaseOffenceDetails> sjpCasesDateComparator =
            Comparator.comparing(CaseOffenceDetails::getPostingOrHearingDate);

    private static final Comparator<CaseOffenceDetails> courtCasesDateComparator =
                     Comparator.comparing(CaseOffenceDetails::getPostingOrHearingDate).reversed();

    private final List<CaseOffenceDetails> sjpOpenCases = new ArrayList<>();
    private final List<CaseOffenceDetails> sjpClosedCases = new ArrayList<>();
    private final List<CaseOffenceDetails> courtOpenCases = new ArrayList<>();
    private final List<CaseOffenceDetails> courtClosedCases = new ArrayList<>();

    public void add(CaseRuleType ruleType, CaseOffenceDetails caseOffenceDetails) {
        switch (ruleType) {
            case SJP_OPEN:
                sjpOpenCases.add(caseOffenceDetails);
                break;
            case SJP_CLOSED:
                sjpClosedCases.add(caseOffenceDetails);
                break;
            case COURT_CASE_OPEN:
                courtOpenCases.add(caseOffenceDetails);
                break;
            case COURT_CASE_CLOSED:
                courtClosedCases.add(caseOffenceDetails);
                break;
            default:
                LOGGER.warn("Unrecognized case rule matching type - ruleType={}", ruleType);
        }
    }

    public List<CaseOffenceDetails> getSjpOpenCases() {
        return new ArrayList<>(sjpOpenCases.stream().sorted(sjpCasesDateComparator).collect(Collectors.toList()));
    }

    public List<CaseOffenceDetails> getSjpClosedCases() {
        return new ArrayList<>(sjpClosedCases.stream().sorted(sjpCasesDateComparator).collect(Collectors.toList()));
    }

    public List<CaseOffenceDetails> getCourtOpenCases() {
        return new ArrayList<>(courtOpenCases.stream().sorted(courtCasesDateComparator).collect(Collectors.toList()));
    }

    public List<CaseOffenceDetails> getCourtClosedCases() {
        return new ArrayList<>(courtClosedCases.stream().sorted(courtCasesDateComparator).collect(Collectors.toList()));
    }

    @Override
    public String toString() {
        return "PotentialCasesResponse{" +
                "sjpOpenCases=" + sjpOpenCases +
                ", sjpClosedCases=" + sjpClosedCases +
                ", courtOpenCases=" + courtOpenCases +
                ", courtClosedCases=" + courtClosedCases +
                '}';
    }

    public static PotentialCases emptyPotentialCase() {
        return EMPTY_POTENTIAL_CASE;
    }
}
