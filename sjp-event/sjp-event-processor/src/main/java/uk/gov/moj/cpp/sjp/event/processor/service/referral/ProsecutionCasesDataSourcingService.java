package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.justice.json.schemas.domains.sjp.query.PleaDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.NotifiedPleaView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseView;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.ResultingService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
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
    private SjpService sjpService;

    @Inject
    private NotifiedPleaViewHelper notifiedPleaViewHelper;

    @Inject
    private ResultingService resultingService;

    public List<ProsecutionCaseView> createProsecutionCaseViews(
            final CaseDetails caseDetails,
            final CaseReferredForCourtHearing caseReferredForCourtHearing,
            final DefendantsOnlinePlea defendantPleaDetails,
            final JsonObject caseFileDefendantDetails,
            final JsonEnvelope emptyEnvelopeWithReferralEventMetadata) {

        final JsonObject prosecutor = referenceDataService.getProsecutor(
                caseDetails.getProsecutingAuthority(),
                emptyEnvelopeWithReferralEventMetadata);
        final JsonObject referenceDataOffences = referenceDataOffencesService.getOffences(
                caseDetails.getDefendant().getOffences().get(0),
                emptyEnvelopeWithReferralEventMetadata);
        final JsonObject caseDecisions = resultingService.getCaseDecisions(caseDetails.getId(), emptyEnvelopeWithReferralEventMetadata);
        final EmployerDetails employer = sjpService.getEmployerDetails(caseDetails.getDefendant().getId(), emptyEnvelopeWithReferralEventMetadata);

        final Optional<JsonObject> defendantSelfDefinedInformationOptional = Optional.ofNullable(caseFileDefendantDetails)
                .map(defendantDetails -> (JsonObject) defendantDetails.getOrDefault("selfDefinedInformation", createObjectBuilder().build()));

        final String nationalityId = defendantSelfDefinedInformationOptional
                .map(selfDefinedInformation -> selfDefinedInformation.getString("nationality", null))
                .flatMap(selfDefinedNationality -> referenceDataService.getNationality(selfDefinedNationality, emptyEnvelopeWithReferralEventMetadata))
                .map(referenceDataNationality -> referenceDataNationality.getString("id"))
                .orElse(null);

        final String ethnicityId = defendantSelfDefinedInformationOptional
                .map(selfDefinedInformation -> selfDefinedInformation.getString("ethnicity", null))
                .flatMap(defendantDefinedEthnicity -> referenceDataService.getEthnicity(defendantDefinedEthnicity, emptyEnvelopeWithReferralEventMetadata))
                .map(ethnicityJsonObject -> ethnicityJsonObject.getString("id"))
                .orElse(null);

        final NotifiedPleaView notifiedPleaView = notifiedPleaViewHelper.createNotifiedPleaView(
                caseReferredForCourtHearing,
                caseDetails.getDefendant().getOffences());

        final JsonObject caseDecision = caseDecisions.getJsonArray("caseDecisions").getJsonObject(0);

        final String pleaMitigation = Optional.ofNullable(defendantPleaDetails)
                .map(DefendantsOnlinePlea::getPleaDetails)
                .map(PleaDetails::getMitigation)
                .orElse(null);

        return prosecutionCasesViewHelper.createProsecutionCaseViews(
                caseDetails,
                referenceDataOffences,
                prosecutor,
                caseDecision,
                caseFileDefendantDetails,
                employer,
                nationalityId,
                ethnicityId,
                caseReferredForCourtHearing.getReferredAt().toLocalDate(),
                notifiedPleaView,
                pleaMitigation);
    }
}
