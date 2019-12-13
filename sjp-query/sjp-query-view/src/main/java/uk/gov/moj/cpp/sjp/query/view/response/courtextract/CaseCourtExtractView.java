package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import static java.time.LocalDate.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static java.util.Objects.*;
import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;

public class CaseCourtExtractView {
    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter TIME_FORMAT = ofPattern("h:mma");

    private final String generationDate;

    private final String generationTime;

    private final DefendantDetailsView defendant;

    private final CaseDetailsView caseDetails;

    private final List<OffenceDetailsView> offences;

    private final PaymentView payment;

    private final boolean hasFinancialImposition;

    public CaseCourtExtractView(final CaseDetail caseDetail) {
        this.generationDate = now().format(DATE_FORMAT);
        this.generationTime = LocalTime.now().format(TIME_FORMAT).toLowerCase();
        this.defendant = new DefendantDetailsView(caseDetail.getDefendant());
        this.caseDetails = new CaseDetailsView(caseDetail);
        this.offences = caseDetail.getDefendant().getOffences().stream()
                .map(offence -> new OffenceDetailsView(caseDetail,
                        offence,
                        getDecisionsForOffence(caseDetail, offence.getId()))
                )
                .collect(toList());
        this.payment = PaymentView.getPayment(caseDetail);
        this.hasFinancialImposition = nonNull(this.payment);
    }

    private Collection<Pair<CaseDecision, OffenceDecision>> getDecisionsForOffence(CaseDetail caseDetail, UUID offenceId) {
        return caseDetail.getCaseDecisions()
                .stream()
                .sorted(comparing(CaseDecision::getSavedAt).reversed())
                .map(caseDecision -> Pair.of(caseDecision, getOffenceDecision(caseDecision, offenceId)))
                .filter(caseAndOffenceDecisionOpt -> caseAndOffenceDecisionOpt.getRight().isPresent())
                .map(pairWithOptional -> Pair.of(pairWithOptional.getLeft(), pairWithOptional.getRight().get()))
                .collect(toList());
    }

    private Optional<OffenceDecision> getOffenceDecision(final CaseDecision caseDecision, final UUID offenceId) {
        return caseDecision.getOffenceDecisions().stream().
                filter(offenceDecision ->
                        offenceDecision.getOffenceId().equals(offenceId)).findFirst();
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

    public List<OffenceDetailsView> getOffences() {
        return unmodifiableList(offences);
    }

    public PaymentView getPayment() {
        return payment;
    }

    public boolean getHasFinancialImposition() {
        return hasFinancialImposition;
    }
}
