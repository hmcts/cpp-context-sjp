package uk.gov.moj.cpp.sjp.query.view.service.defendantcase;

import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleType.COURT_CASE_CLOSED;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleType.COURT_CASE_OPEN;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleType.SJP_CLOSED;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleType.SJP_OPEN;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleUtils.findLastHearingDate;

import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDecisionRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;
import uk.gov.moj.cpp.sjp.query.view.service.DefendantService;
import uk.gov.moj.cpp.sjp.query.view.service.ProgressionService;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.AbstractCaseRule;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleResult;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleType;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CourtCaseClosedRule;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CourtCaseOpenRule;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.SjpClosedRule;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.SjpOpenRule;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCase;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.UnifiedDefendantCaseSearcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.persistence.PersistenceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefendantPotentialCaseServiceImpl implements DefendantPotentialCaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantPotentialCaseServiceImpl.class);

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private ReadyCaseRepository readyCaseRepository;

    @Inject
    private CaseDecisionRepository caseDecisionRepository;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private DefendantService defendantService;

    @Inject
    private UnifiedDefendantCaseSearcher defendantCaseSearcher;

    private final Map<CaseRuleType, AbstractCaseRule> potentialCaseRules = new LinkedHashMap<>();

    public DefendantPotentialCaseServiceImpl() {
        final AbstractCaseRule sjpOpenRule = new SjpOpenRule();
        final AbstractCaseRule sjpClosedRule = new SjpClosedRule(this);
        final AbstractCaseRule courtCaseOpenRule = new CourtCaseOpenRule(this);
        final AbstractCaseRule courtCaseClosedRule = new CourtCaseClosedRule(this);

        potentialCaseRules.put(sjpOpenRule.getRuleType(), sjpOpenRule);
        potentialCaseRules.put(sjpClosedRule.getRuleType(), sjpClosedRule);
        potentialCaseRules.put(courtCaseOpenRule.getRuleType(), courtCaseOpenRule);
        potentialCaseRules.put(courtCaseClosedRule.getRuleType(), courtCaseClosedRule);
    }

    @Override
    public boolean hasDefendantPotentialCase(final Envelope<?> envelope, final UUID defendantId) {
        final DefendantDetail defendantDetail = defendantService.findDefendantDetailById(defendantId);
        if (defendantDetail == null) {
            return false;
        }
        final List<CaseRuleResult> result = findDefendantCaseMatchingRule(envelope, defendantDetail);

        return result.size() > 1;
    }

    @Override
    public PotentialCases findDefendantPotentialCases(Envelope<?> envelope,
                                                      UUID defendantId) {
        LOGGER.info("Populating all potential cases - defendantId={}", defendantId);
        final DefendantDetail defendantDetail = defendantService.findDefendantDetailById(defendantId);
        if (defendantDetail == null) {
            LOGGER.warn("Found no potential cases as defendant id invalid - defendantId={}", defendantId);
            return PotentialCases.emptyPotentialCase();
        }

        final List<DefendantCase> defendantCases =
                defendantCaseSearcher.searchDefendantCases(envelope, defendantDetail);
        LOGGER.info("Number of defendant cases matching defendant is {}", defendantCases.size());
        if (defendantCases.isEmpty()) {
            return PotentialCases.emptyPotentialCase();
        }

        final PotentialCases response = new PotentialCases();
        for (final DefendantCase defendantCase : defendantCases) {
            for (final AbstractCaseRule rule : potentialCaseRules.values()) {
                final CaseRuleResult ruleResult = rule.executeRule(defendantCase);
                if (ruleResult.isMatch()) {
                    final CaseOffenceDetails.CaseOffenceDetailsBuilder
                            builder = CaseOffenceDetails.createBuilder();
                    builder.withCaseId(defendantCase.getCaseId()).
                            withCaseRef(defendantCase.getCaseReference());
                    decorateBuilderByRuleType(builder,
                            rule.getRuleType(),
                            defendantDetail, defendantCase);
                    response.add(ruleResult.getRuleType(), builder.build());
                    break;
                }
            }
        }

        return response;
    }

    private void decorateBuilderByRuleType(CaseOffenceDetails.CaseOffenceDetailsBuilder builder,
                                           CaseRuleType ruleType,
                                           final DefendantDetail defendant,
                                           DefendantCase defendantCase) {
        final CaseDetail caseDetail = caseRepository.findBy(defendantCase.getCaseId());


        if (ruleType == SJP_OPEN || ruleType == SJP_CLOSED) {
            final ReadyCase readyCase = readyCaseRepository.findBy(defendantCase.getCaseId());
            final List<String> offences = caseDetail.getDefendant().getOffences()
                    .stream()
                    .map(OffenceDetail::getWording)
                    .toList();

            builder.withPostingOrHearingDate(caseDetail.getPostingDate())
                    .withProsecutorName(getProsecutingAuthority(caseDetail))
                    .withExpiryDate(getExpiryDateForCase(readyCase, caseDetail.getPostingDate()))
                    .withOffenceTitles(offences);

        } else if (ruleType == COURT_CASE_OPEN || ruleType == COURT_CASE_CLOSED) {
            final LocalDate lastHearingDate = findLastHearingDate(defendantCase.getHearings());
            final List<String> defendantOffences = progressionService.findDefendantOffences(defendantCase.getCaseId(), defendant);

            builder.withPostingOrHearingDate(lastHearingDate)
                    .withProsecutorName(getProsecutingAuthority(caseDetail))
                    .withOffenceTitles(defendantOffences);
        } else {
            LOGGER.warn("Invalid rule type provided - ruleType={}", ruleType);
        }
    }


    public List<CaseRuleResult> findDefendantCaseMatchingRule(Envelope<?> envelope,
                                                              DefendantDetail defendant) {
        LOGGER.info("Checking whether defendant has any potential Open/Closed Sjp " +
                "or CourtCase - defendantId={}", defendant.getId());
        final List<DefendantCase> defendantCases =
                defendantCaseSearcher.searchDefendantCases(envelope, defendant);
        final List<CaseRuleResult> caseRuleResultList = new ArrayList<>();
        LOGGER.info("Number of defendant cases matching search criteria is {}", defendantCases.size());
        for (final DefendantCase defendantCase : defendantCases) {
            for (final AbstractCaseRule rule : potentialCaseRules.values()) {
                final CaseRuleResult ruleResult = rule.executeRule(defendantCase);
                if (ruleResult.isMatch()) {
                    LOGGER.info("Defendant has at potential case matching rule={}", ruleResult.getRuleType());
                    caseRuleResultList.add(ruleResult);
                }
            }
        }
        LOGGER.info("Defendant has number of potential case matching rule = {}", caseRuleResultList.size());
        if (caseRuleResultList.isEmpty()) {
            caseRuleResultList.add(new CaseRuleResult(CaseRuleType.NONE, false));
        }
        return caseRuleResultList;
    }

    public CaseDecision findCaseDecisionById(final UUID caseId) {
        CaseDecision caseDecision = null;
        try {
            caseDecision = caseDecisionRepository.findCaseDecisionById(caseId);
        } catch (PersistenceException exception) {
            LOGGER.error("No casedecision found for given caseId {} with message {} ", caseId, exception);
        }
        return caseDecision;
    }

    public Optional<JsonObject> findProgressionCaseById(final UUID caseId) {
        return progressionService.findCaseById(caseId);
    }

    private static String getProsecutingAuthority(final CaseDetail caseDetail) {
        return Optional.ofNullable(caseDetail)
                .map(CaseDetail::getProsecutingAuthority)
                .orElse("");
    }

    private String getExpiryDateForCase(final ReadyCase readyCase, final LocalDate postingDate) {
        if (Objects.isNull(readyCase)) {
            final LocalDate currentDate = LocalDate.now();
            final LocalDate futureExpiryDate = postingDate.plusDays(28);

            if (currentDate.isBefore(postingDate.plusDays(28))) {
                return String.valueOf(futureExpiryDate);
            }
        }
        return "";
    }
}
