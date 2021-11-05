package uk.gov.moj.cpp.sjp.event.processor.results.converter;

import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.justice.json.schemas.domains.sjp.results.PublicHearingResulted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseListedInCriminalCourtsV2;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;

import javax.inject.Inject;

public class SjpToHearingConverter {

    @Inject
    private SjpApplicationDecisionToHearingResultConverter sjpApplicationDecisionToHearingResultConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private SjpCaseDecisionToHearingResultConverter sjpCaseDecisionToHearingResultConverter;

    // return the hearing object structure
    public PublicHearingResulted convertCaseDecision(final JsonEnvelope decisionSavedEventEnvelope) {
        // convert the results\
        final DecisionSaved decisionSaved = jsonObjectToObjectConverter.convert(decisionSavedEventEnvelope.payloadAsJsonObject(), DecisionSaved.class);
        final Envelope<DecisionSaved> decisionSavedEnvelope = envelop(decisionSaved)
                .withName(DecisionSaved.EVENT_NAME)
                .withMetadataFrom(decisionSavedEventEnvelope);
        return sjpCaseDecisionToHearingResultConverter.convertCaseDecision(decisionSavedEnvelope);
    }

    // return the hearing object structure
    public PublicHearingResulted convertApplicationDecision(final Envelope<ApplicationDecisionSaved>
                                                                    applicationDecisionSavedEnvelope) {
        return sjpApplicationDecisionToHearingResultConverter.convertApplicationDecision(applicationDecisionSavedEnvelope);
    }

    public PublicHearingResulted convertCaseDecisionInCcForReferToCourt( final JsonEnvelope caseListedInCcForReferToCourtEnvelope) {
        // convert the results\
        final CaseListedInCriminalCourtsV2 caseListedInCcForReferToCourt = jsonObjectToObjectConverter.convert(caseListedInCcForReferToCourtEnvelope.payloadAsJsonObject(), CaseListedInCriminalCourtsV2.class);
        final Envelope<CaseListedInCriminalCourtsV2> caseListedInCcForReferToCourtEnvelopeObj = envelop(caseListedInCcForReferToCourt)
                .withName(CaseListedInCriminalCourtsV2.EVENT_NAME)
                .withMetadataFrom(caseListedInCcForReferToCourtEnvelope);
        return sjpCaseDecisionToHearingResultConverter.convertCaseDecisionInCcForReferToCourt(caseListedInCcForReferToCourtEnvelopeObj);
    }

}
