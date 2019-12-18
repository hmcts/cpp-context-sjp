package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferForCourtHearingDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.query.view.response.courtextract.Address;
import uk.gov.moj.cpp.sjp.query.view.response.courtextract.CaseCourtExtractView;
import uk.gov.moj.cpp.sjp.query.view.response.courtextract.CaseDetailsView;
import uk.gov.moj.cpp.sjp.query.view.response.courtextract.OffenceDecisionLineView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CourtExtractDataService {

    @Inject
    private CaseService caseService;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private UserAndGroupsService userAndGroupsService;

    public Optional<CaseCourtExtractView> getCourtExtractData(UUID caseId) {
        return caseService.getCase(caseId).map(caseDetail -> {
            final CaseCourtExtractView caseCourtExtractView = new CaseCourtExtractView(caseDetail);
            this.resolveReferenceData(caseDetail, caseCourtExtractView);
            return caseCourtExtractView;
        });
    }

    private void resolveReferenceData(final CaseDetail caseDetail,
                                      final CaseCourtExtractView caseCourtExtract) {
        resolveProsecutorDetails(caseDetail, caseCourtExtract);
        resolveOffenceDetails(caseDetail, caseCourtExtract);
        resolveDecisionView(caseDetail, caseCourtExtract);
    }

    private void resolveOffenceDetails(final CaseDetail caseDetail,
                                       final CaseCourtExtractView caseCourtExtract) {

        caseCourtExtract.getOffences().forEach(offenceDetailsView ->
                caseDetail.getDefendant().getOffences().
                        stream().
                        filter(ofDetail -> ofDetail.getSequenceNumber() == offenceDetailsView.getSequenceNumber()).
                        findFirst().ifPresent(offenceDetail -> referenceDataService.getOffenceData(offenceDetail.getCode()).
                        ifPresent(offenceRefData -> {
                            offenceDetailsView.setTitle(offenceRefData.getString("title"));
                            offenceDetailsView.setLegislation(offenceRefData.getString("legislation"));
                        })));
    }

    private void resolveProsecutorDetails(final CaseDetail caseDetail, final CaseCourtExtractView caseCourtExtract) {
        referenceDataService.getProsecutorsByProsecutorCode(caseDetail.getProsecutingAuthority().name()).
                ifPresent(prosecutors -> {
                    if (!prosecutors.isEmpty()) {
                        final JsonObject prosecutor = prosecutors.getJsonObject(0);
                        final CaseDetailsView caseDetails = caseCourtExtract.getCaseDetails();
                        caseDetails.setProsecutorAddress(
                                ofNullable(prosecutor.getJsonObject("address"))
                                        .map(this::addressString)
                                        .orElse(null)
                        );
                        caseDetails.setProsecutor(prosecutor.getString("fullName"));
                    }
                });
    }

    private void resolveDecisionView(CaseDetail caseDetail, CaseCourtExtractView caseCourtExtractView) {
        resolveResultDetail(caseDetail, caseCourtExtractView);
        resolveDecisionMadeDetail(caseDetail, caseCourtExtractView);
    }

    private void resolveResultDetail(final CaseDetail caseDetail, final CaseCourtExtractView caseCourtExtractView) {
        final List<OffenceDecision> offenceDecisions = caseDetail.getCaseDecisions()
                .stream()
                .flatMap(caseDecision -> caseDecision.getOffenceDecisions().stream())
                .collect(Collectors.toList());

        offenceDecisions.forEach(offenceDecision ->
                caseCourtExtractView.getOffences()
                        .stream()
                        .flatMap(offenceDetailsView -> offenceDetailsView.getOffenceDecisions().stream())
                        .filter(decisionView -> decisionView.getOffenceId().equals(offenceDecision.getOffenceId())
                                && decisionView.getOffenceDecisionId().equals(offenceDecision.getCaseDecisionId()))
                        .findFirst()
                        .ifPresent(oDecisionView -> oDecisionView.getLines()
                                .stream()
                                .filter(offenceDecisionLineView -> "Result".equals(offenceDecisionLineView.getLabel()))
                                .findFirst().ifPresent(offenceDecisionLineView -> updateResultDetail(offenceDecision, offenceDecisionLineView))));

    }

    private void resolveDecisionMadeDetail(final CaseDetail caseDetail, final CaseCourtExtractView caseCourtExtractView) {

        final List<OffenceDecision> offenceDecisions = caseDetail.getCaseDecisions()
                .stream()
                .flatMap(caseDecision -> caseDecision.getOffenceDecisions().stream())
                .collect(Collectors.toList());

        offenceDecisions.forEach(offenceDecision -> {
            final CaseDecision caseDecision = caseDetail.getCaseDecisions()
                    .stream()
                    .filter(decision -> decision.getId().equals(offenceDecision.getCaseDecisionId()))
                    .findFirst().get();

            caseCourtExtractView.getOffences()
                    .stream()
                    .flatMap(offenceDetailsView -> offenceDetailsView.getOffenceDecisions().stream())
                    .filter(decisionView -> decisionView.getOffenceId().equals(offenceDecision.getOffenceId())
                            && decisionView.getOffenceDecisionId().equals(caseDecision.getId()))
                    .findFirst()
                    .ifPresent(decisionView -> decisionView.getLines()
                            .stream()
                            .filter(offenceDecisionLineView -> "Decision made".equals(offenceDecisionLineView.getLabel()))
                            .findFirst().ifPresent(offenceDecisionLineView -> updateDecisionMadeDetail(caseDecision.getSession(), offenceDecisionLineView)));

        });
    }

    private void updateDecisionMadeDetail(final Session session, final OffenceDecisionLineView offenceDecisionLineView) {
        final StringBuilder sb = new StringBuilder(offenceDecisionLineView.getValue());
        sb.append("\n");
        if (session.getType().equals(MAGISTRATE)) {
            session.getMagistrate()
                    .ifPresent(magistrate -> sb.append(magistrate).append(" (Magistrate)"));
        } else {
            sb.append(userAndGroupsService.getUserDetails(session.getUserId())).append(" (Legal Adviser)");
        }
        offenceDecisionLineView.setValue(sb.toString());
    }

    private void updateResultDetail(final OffenceDecision offenceDecision, final OffenceDecisionLineView offenceDecisionLineView) {
        final StringBuilder sb = new StringBuilder(offenceDecisionLineView.getValue());

        final DecisionType decisionType = offenceDecision.getDecisionType();
        if (REFER_FOR_COURT_HEARING.equals(decisionType)) {
            final String referralReason = getReferralReason(((ReferForCourtHearingDecision) offenceDecision).getReferralReasonId());
            sb.append("\nReason: ").append(referralReason);
        }
        offenceDecisionLineView.setValue(sb.toString());
    }

    private String getReferralReason(final UUID referralReasonId) {
        return referenceDataService
                .getReferralReasons()
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(referralReason -> referralReason.getString("id").equals(referralReasonId.toString()))
                .findFirst()
                .map(referralReason -> referralReason.getString("reason"))
                .orElseThrow(() -> new ReferralReasonNotFoundException(referralReasonId));
    }

    private Address addressString(JsonObject address) {
        final String line1 = address.containsKey("address1") ? address.getString("address1") : null;
        final String line2 = address.containsKey("address2") ? address.getString("address2") : null;
        final String line3 = address.containsKey("address3") ? address.getString("address3") : null;
        final String line4 = address.containsKey("address4") ? address.getString("address4") : null;
        final String line5 = address.containsKey("address5") ? address.getString("address5") : null;
        final String postcode = address.containsKey("postcode") ? address.getString("postcode") : null;

        return new Address(line1, line2, line3, line4, line5, postcode);
    }
}
