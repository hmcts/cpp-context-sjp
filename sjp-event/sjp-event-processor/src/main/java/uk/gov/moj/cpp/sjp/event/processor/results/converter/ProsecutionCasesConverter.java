package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ProsecutionCasesConverter {

    @Inject
    private DefendantsConverter defendantsConverter;

    @Inject
    private ProsecutionCaseIdentifierConverter pciConverter;

    public List<ProsecutionCase> convert(final JsonObject sjpSessionPayload,
                                         final CaseDetails caseDetails,
                                         final Metadata sourceMetadata,
                                         final DecisionAggregate resultsAggregate) {
        final List<ProsecutionCase> prosecutionsCases = new ArrayList<>();
        final ProsecutionCaseIdentifier prosecutionCaseIdentified =
                pciConverter.getProsecutionCaseIdentifier(
                        caseDetails.getProsecutingAuthority(),
                        caseDetails.getUrn());

        final ProsecutionCase.Builder caseBuilder = ProsecutionCase.prosecutionCase()
                .withId(caseDetails.getId())
                .withProsecutionCaseIdentifier(prosecutionCaseIdentified)
                .withInitiationCode(InitiationCode.J)
                .withDefendants(defendantsConverter.getDefendants(sjpSessionPayload, caseDetails, sourceMetadata, resultsAggregate));
        prosecutionsCases.add(caseBuilder.build());

        return prosecutionsCases;
    }
}
