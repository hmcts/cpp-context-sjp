package uk.gov.moj.cpp.sjp.event.processor.service.referral;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDecision;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.justice.json.schemas.domains.sjp.queries.OnlinePleaDetail;
import uk.gov.justice.json.schemas.domains.sjp.query.DefendantsOnlinePlea;
import uk.gov.justice.json.schemas.domains.sjp.query.EmployerDetails;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.SessionCourt;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataOffencesService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.ProsecutionCasesViewHelper;
import uk.gov.moj.cpp.sjp.model.prosecution.ProsecutionCaseView;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

@SuppressWarnings("squid:S107")
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
            final CaseDecision caseDecision,
            final List<OffenceDecisionInformation> getReferredOffences,
            final ZonedDateTime referredAt,
            final DefendantCourtOptions defendantCourtOptions,
            final LocalDate convictionDate,
            final SessionCourt convictingCourt,
            final DefendantsOnlinePlea defendantPleaDetails,
            final JsonObject prosecutionCaseFile,
            final JsonObject caseFileDefendantDetails,
            final JsonEnvelope emptyEnvelopeWithReferralEventMetadata) {

        final List<Offence> referredOffences = getReferredOffences(
                getReferredOffences,
                caseDetails);

        final Map<String, JsonObject> offenceDefinition = referenceDataOffencesService.getOffenceDefinitionByOffenceCode(
                getOffenceCodes(referredOffences),
                referredAt.toLocalDate(),
                emptyEnvelopeWithReferralEventMetadata);

        final JsonObject prosecutor = referenceDataService.getProsecutors(
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
                caseDecision,
                prosecutor,
                prosecutionCaseFile,
                caseFileDefendantDetails,
                employer,
                nationalityId,
                ethnicityId,
                referredAt,
                defendantCourtOptions,
                convictionDate,
                convictingCourt,
                pleaMitigation,
                offenceDefinition,
                referredOffences,
                emptyEnvelopeWithReferralEventMetadata);
    }

    @SuppressWarnings({"squid:S6204"})
    private List<Offence> getReferredOffences(
            final List<OffenceDecisionInformation> referredOffencesWithVerdict,
            final CaseDetails caseDetails) {

        final Map<UUID, OffenceDecisionInformation> offenceMap = new HashMap<>();
        referredOffencesWithVerdict.forEach(offenceDecisionInformation -> offenceMap.put(offenceDecisionInformation.getOffenceId(), offenceDecisionInformation));

        return caseDetails
                .getDefendant()
                .getOffences()
                .stream()
                .filter(offence -> offenceMap.containsKey(offence.getId()))
                .map(offence -> checkAndAddVerdictInOffence(offence, offenceMap))
                .toList();
    }

    private Set<String> getOffenceCodes(final List<Offence> referredOffences) {
        return referredOffences
                .stream()
                .map(Offence::getCjsCode)
                .collect(Collectors.toSet());
    }

    private uk.gov.justice.json.schemas.domains.sjp.queries.Offence checkAndAddVerdictInOffence(uk.gov.justice.json.schemas.domains.sjp.queries.Offence sjpOffence, Map<UUID, OffenceDecisionInformation> offenceMap) {
        if (VerdictType.PROVED_SJP.equals(offenceMap.get(sjpOffence.getId()).getVerdict())) {
            return Offence.offence()
                    .withValuesFrom(sjpOffence)
                    .withConviction(uk.gov.moj.cpp.core.sjp.verdict.VerdictType.PROVED_SJP)
                    .build();

        }
        return sjpOffence;
    }
}
