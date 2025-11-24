package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import org.apache.commons.lang3.tuple.Pair;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplication;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDecisionView;
import uk.gov.moj.cpp.sjp.query.view.response.OffenceDecisionView;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

public class CaseCourtExtractView {
    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter TIME_FORMAT = ofPattern("h:mma");
    private static final String LONDON_ZONE_ID = "Europe/London";

    private final String generationDate;

    private final String generationTime;

    private final DefendantDetailsView defendant;

    private final CaseDetailsView caseDetails;

    private List<DecisionCourtExtractView> decisionCourtExtractView;

    private List<CaseDecisionCourtExtractView> caseDecisions;


    public CaseCourtExtractView(final CaseDetail caseDetail) {
        this.generationDate = ZonedDateTime.now(ZoneId.of(LONDON_ZONE_ID)).format(DATE_FORMAT);
        this.generationTime = ZonedDateTime.now(ZoneId.of(LONDON_ZONE_ID)).format(TIME_FORMAT).toLowerCase();
        this.defendant = new DefendantDetailsView(caseDetail.getDefendant());
        this.caseDetails = new CaseDetailsView(caseDetail);
        this.caseDecisions = new LinkedList<>();

        final List<CaseDecisionCourtExtractView> caseOffenceDecisions = caseDetail.getCaseDecisions()
                .stream()
                .map(CaseDecisionCourtExtractView::new)
                .sorted()
                .collect(toList());


        final List<CaseDecisionCourtExtractView> caseApplicationsDecision = getCaseApplicationsDecision(caseDetail);

        caseDecisions.addAll(caseOffenceDecisions);
        caseDecisions.addAll(caseApplicationsDecision);
        caseDecisions = caseDecisions.stream().sorted(comparing(CaseDecisionView::getSavedAt).reversed()).collect(toList());

        final AtomicInteger groupDecision = new AtomicInteger(0);
        caseDecisions.forEach(caseDecisionView -> {
            if (nonNull(caseDecisionView.getApplicationDecision())) {
                caseDecisionView.setGroup(groupDecision.incrementAndGet());
                groupDecision.getAndIncrement();
            } else {
                caseDecisionView.setGroup(groupDecision.get());
            }
        });
        decisionCourtExtractView = caseDecisions.stream().collect(Collectors.groupingBy(CaseDecisionCourtExtractView::getGroup))
                .entrySet().stream()
                .map(decision -> mapByDecision(caseDetail, decision.getValue()))
                .collect(toList());

    }

    private List<CaseDecisionCourtExtractView> getCaseApplicationsDecision(CaseDetail caseDetail) {
        return ofNullable(caseDetail.getApplications())
                .map(applications -> applications.stream()
                        .map(CaseApplication::getApplicationDecision)
                        .filter(Objects::nonNull)
                        .map(CaseDecisionCourtExtractView::new)
                        .sorted(comparing(CaseDecisionView::getSavedAt).reversed())
                        .collect(toList()))
                .orElse(new LinkedList<>());
    }

    private Collection<Pair<CaseDecisionCourtExtractView, OffenceDecisionView>> getDecisionsViewPerOffence(final List<CaseDecisionCourtExtractView> caseDecisionsView, final UUID offenceId) {

        return caseDecisionsView.stream()
                .map(caseDecisionView -> Pair.of(caseDecisionView, getCaseOffenceDecision(caseDecisionView, offenceId)))
                .filter(caseAndOffenceDecisionOpt -> caseAndOffenceDecisionOpt.getRight().isPresent())
                .map(pairWithOptional -> Pair.of(pairWithOptional.getLeft(), pairWithOptional.getRight().get()))
                .collect(toList());
    }

    private Optional<OffenceDecisionView> getCaseOffenceDecision(final CaseDecisionCourtExtractView caseDecision, final UUID offenceId) {
        return caseDecision.getOffenceDecisions().stream()
                .filter(offenceDecision -> offenceDecision.getOffenceId().equals(offenceId)).findFirst();

    }

    private DecisionCourtExtractView mapByDecision(final CaseDetail caseDetail, final List<CaseDecisionCourtExtractView> caseDecisionsView) {

        final List<DecisionDetailsView> decisionsDetails ;
        final DecisionCourtExtractView decisionCourtExtract = new DecisionCourtExtractView();

        if (isNotEmpty(caseDecisionsView.get(0).getOffenceDecisions())) {
            decisionsDetails = caseDetail.getDefendant().getOffences().stream()
                    .map(offence -> new DecisionDetailsView(caseDecisions, caseDetail,
                            offence,
                            getDecisionsViewPerOffence(caseDecisionsView, offence.getId())))
                    .collect(toList());
            decisionCourtExtract.setOffencesApplicationsDecisions(decisionsDetails);
            decisionCourtExtract.setPayment(PaymentView.getPayment(caseDecisionsView));
            decisionCourtExtract.setHasFinancialImposition(nonNull(decisionCourtExtract.getPayment()));

        } else {
            decisionsDetails = Collections.singletonList(new DecisionDetailsView(caseDecisions, caseDetail, caseDecisionsView.get(0)));
            decisionCourtExtract.setOffencesApplicationsDecisions(decisionsDetails);
        }
        return decisionCourtExtract;
    }

    public String getGenerationDate() {
        return generationDate;
    }

    public String getGenerationTime() {
        return generationTime;
    }

    public DefendantDetailsView getDefendant() {
        return defendant;
    }

    public CaseDetailsView getCaseDetails() {
        return caseDetails;
    }

    public List<DecisionCourtExtractView> getDecisionCourtExtractView() {
        return unmodifiableList(decisionCourtExtractView);
    }

}
