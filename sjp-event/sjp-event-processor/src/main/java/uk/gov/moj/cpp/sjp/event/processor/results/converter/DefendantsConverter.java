package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static javax.json.Json.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionSavedToJudicialResultsConverter;
import uk.gov.moj.cpp.sjp.event.processor.service.ProsecutionCaseFileService;
import uk.gov.moj.cpp.sjp.event.processor.service.ReferenceDataService;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

public class DefendantsConverter {

    private static final String DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE = "0800NP0100000000001H";

    @Inject
    private PersonDefendantConverter personDefendantConverter;

    @Inject
    private OffencesConverter offencesConverter;

    @Inject
    private DecisionSavedToJudicialResultsConverter referencedDecisionSavedOffenceConverter;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ProsecutionCaseFileService prosecutionCaseFileService;

    public List<Defendant> getDefendants(final JsonObject sjpSessionPayload,
                                         final CaseDetails caseDetails,
                                         final Metadata metadata,
                                         final DecisionAggregate resultsAggregate) {
        final List<Defendant> defendants = new ArrayList<>();

        final uk.gov.justice.json.schemas.domains.sjp.queries.Defendant defendant = caseDetails.getDefendant();
        final JsonEnvelope emptyEnvelope = envelopeFrom(metadataFrom(metadata), NULL);

        final List<Offence> offences = offencesConverter.getOffences(sjpSessionPayload, caseDetails, resultsAggregate);

        final JsonObject caseFileDefendantDetails = prosecutionCaseFileService.getCaseFileDefendantDetails(caseDetails.getId(), emptyEnvelope).orElse(null); // this service returns first defendant as in the SJP there is only 1 defendant.
        final Optional<JsonObject> defendantSelfDefinedInformationOptional = Optional.ofNullable(caseFileDefendantDetails)
                .map(defendantDetails -> (JsonObject) defendantDetails.getOrDefault("selfDefinedInformation", createObjectBuilder().build()));

        final String countryCJSCode = defendantSelfDefinedInformationOptional
                .map(selfDefinedInformation -> selfDefinedInformation.getString("nationality", null))
                .flatMap(selfDefinedNationality -> referenceDataService.getNationality(selfDefinedNationality, emptyEnvelope))
                .map(referenceDataNationality -> referenceDataNationality.getString("isoCode", null))
                .orElse(null);

        // prosecutor reference
        String prosecutorReference  = DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE;
        if(caseFileDefendantDetails != null) {
            prosecutorReference = caseFileDefendantDetails.getString("asn",null) != null ? caseFileDefendantDetails.getString("asn",null) : DEFAULT_NON_POLICE_PROSECUTOR_REFERENCE;
        }

        final Defendant defendant1 = Defendant.defendant()
                .withId(defendant.getId())
                .withCourtProceedingsInitiated(ZonedDateTime.now())
                .withMasterDefendantId(defendant.getId())
                .withProsecutionCaseId(caseDetails.getId())
                .withPersonDefendant(personDefendantConverter.getPersonDefendant(defendant, countryCJSCode, metadata))
                .withOffences(offences)
                .withCourtProceedingsInitiated(ZonedDateTimes.fromString(sjpSessionPayload.getString("startedAt", null)))
                .withProsecutionAuthorityReference(prosecutorReference)
                .build();
        defendant1.getOffences().get(0);

        defendants.add(defendant1);

        return defendants;
    }


}
