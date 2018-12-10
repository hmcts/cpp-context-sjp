package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static com.google.common.collect.Iterables.getFirst;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.PleaDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseView;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.ResultingService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.NotifiedPleaViewHelper;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.ProsecutionCasesViewHelper;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ProsecutionCasesDataSourcingService {

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;

    @Inject
    private ProsecutionCasesViewHelper prosecutionCasesViewHelper;

    @Inject
    private NotifiedPleaViewHelper notifiedPleaViewHelper;

    @Inject
    private ResultingService resultingService;

    public List<ProsecutionCaseView> createProsecutionCaseViews(
            final CaseDetails caseDetails,
            final CaseReferredForCourtHearing caseReferredForCourtHearing,
            final DefendantsOnlinePlea defendantPleaDetails,
            final JsonEnvelope emptyEnvelopeWithReferralEventMetadata) {

        final JsonObject prosecutor = referenceDataService.getProsecutor(
                caseDetails.getProsecutingAuthority(),
                emptyEnvelopeWithReferralEventMetadata);
        final JsonObject referenceDataOffences = referenceDataOffencesService.getOffences(
                caseDetails.getDefendant().getOffences().get(0),
                emptyEnvelopeWithReferralEventMetadata);
        final JsonObject caseDecisions = resultingService.getCaseDecisions(caseDetails.getId(), emptyEnvelopeWithReferralEventMetadata);

        final NotifiedPleaView notifiedPleaView = notifiedPleaViewHelper.createNotifiedPleaView(
                caseReferredForCourtHearing,
                caseDetails.getDefendant().getOffences());

        final JsonObject caseDecision = (JsonObject) Optional.ofNullable(getFirst(caseDecisions.getJsonArray("caseDecisions"), null))
                .orElse(null);

        return prosecutionCasesViewHelper.createProsecutionCaseViews(
                caseDetails,
                referenceDataOffences,
                prosecutor,
                caseDecision,
                caseReferredForCourtHearing.getReferredAt().toLocalDate(),
                notifiedPleaView,
                Optional.ofNullable(defendantPleaDetails)
                        .map(DefendantsOnlinePlea::getPleaDetails)
                        .map(PleaDetails::getMitigation)
                        .orElse(null));
    }
}
