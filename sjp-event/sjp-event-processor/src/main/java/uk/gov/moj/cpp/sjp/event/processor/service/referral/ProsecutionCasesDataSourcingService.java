package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import static java.util.Collections.emptyList;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.queries.OnlinePleaDetail;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.ProsecutionCaseView;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.ProsecutionCasesViewHelper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ProsecutionCasesDataSourcingService {

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ProsecutionCasesViewHelper prosecutionCasesViewHelper;

    @Inject
    private SjpService sjpService;

    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;

    public List<ProsecutionCaseView> createProsecutionCaseViews(
            final CaseDetails caseDetails,
            final CaseReferredForCourtHearing caseReferredForCourtHearing,
            final DefendantsOnlinePlea defendantPleaDetails,
            final JsonObject caseFileDefendantDetails,
            final JsonEnvelope emptyEnvelopeWithReferralEventMetadata) {

        final List<Offence> referredOffences = getReferredOffences(
                caseReferredForCourtHearing.getReferredOffences(),
                caseDetails);

        final Map<String, UUID> offenceDefinitionIdByOffenceCode = referenceDataOffencesService.getOffenceDefinitionIdByOffenceCode(
                getOffenceCodes(referredOffences),
                caseReferredForCourtHearing.getReferredAt().toLocalDate(),
                emptyEnvelopeWithReferralEventMetadata);

        final JsonObject prosecutor = referenceDataService.getProsecutor(
                caseDetails.getProsecutingAuthority(),
                emptyEnvelopeWithReferralEventMetadata);

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

        // send first mitigation if there are more than one mitigation
        final String pleaMitigation = Optional.ofNullable(defendantPleaDetails)
                .map(DefendantsOnlinePlea::getOnlinePleaDetails)
                .orElse(emptyList())
                .stream()
                .map(OnlinePleaDetail::getMitigation)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        return prosecutionCasesViewHelper.createProsecutionCaseViews(
                caseDetails,
                prosecutor,
                caseFileDefendantDetails,
                employer,
                nationalityId,
                ethnicityId,
                caseReferredForCourtHearing,
                pleaMitigation,
                offenceDefinitionIdByOffenceCode,
                referredOffences);
    }

    private List<Offence> getReferredOffences(
            final List<OffenceDecisionInformation> referredOffencesWithVerdict,
            final CaseDetails caseDetails) {

        final List<UUID> offenceIds =
                referredOffencesWithVerdict
                        .stream()
                        .map(OffenceDecisionInformation::getOffenceId)
                        .collect(Collectors.toList());

        return caseDetails
                .getDefendant()
                .getOffences()
                .stream()
                .filter(offence -> offenceIds.contains(offence.getId()))
                .collect(Collectors.toList());
    }

    private Set<String> getOffenceCodes(final List<Offence> referredOffences) {
        return referredOffences
                .stream()
                .map(Offence::getCjsCode)
                .collect(Collectors.toSet());
    }
}
