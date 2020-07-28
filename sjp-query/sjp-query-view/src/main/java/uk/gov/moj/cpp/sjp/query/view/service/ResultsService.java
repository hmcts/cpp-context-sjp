package uk.gov.moj.cpp.sjp.query.view.service;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.ACCOUNT_DIVISION_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.CREATED;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.ENFORCING_COURT_CODE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.FINANCIAL_IMPOSITION;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.ID;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.INTERIM;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.OFFENCES;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.OFFENCE_DECISIONS;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.TYPE;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.query.view.converter.DecisionSavedOffenceConverter;
import uk.gov.moj.cpp.sjp.query.view.converter.ReferencedDecisionSavedOffenceConverter;
import uk.gov.moj.cpp.sjp.query.view.exception.EnforcementAreaNotFoundException;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDecisionView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.FinancialImpositionView;
import uk.gov.moj.cpp.sjp.query.view.response.SessionView;
import uk.gov.moj.cpp.sjp.query.view.response.OffenceDecisionView ;
import uk.gov.moj.cpp.sjp.query.view.response.OffenceView ;


import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class ResultsService {

    private static final String CASE_ID = "caseId";
    private static final String SJP_SESSION_ID = "sjpSessionId";
    private static final String DECISION_ID = "decisionId";
    private static final String RESULTED_ON = "resultedOn";

    private CaseService caseService;

    private ReferenceDataService referenceDataService;

    private DecisionSavedOffenceConverter decisionSavedOffenceConverter;

    private ReferencedDecisionSavedOffenceConverter referencedDecisionSavedOffenceConverter;

    private OffenceHelper offenceHelper;

    private EmployerService employerService;

    @Inject
    public ResultsService(final CaseService caseService,
                          final ReferenceDataService referenceDataService,
                          final DecisionSavedOffenceConverter decisionSavedOffenceConverter,
                          final ReferencedDecisionSavedOffenceConverter referencedDecisionSavedOffenceConverter,
                          final OffenceHelper offenceHelper,
                          final EmployerService employerService){
        this.caseService = caseService;
        this.referenceDataService = referenceDataService;
        this.decisionSavedOffenceConverter = decisionSavedOffenceConverter;
        this.referencedDecisionSavedOffenceConverter = referencedDecisionSavedOffenceConverter;
        this.offenceHelper = offenceHelper;
        this.employerService  = employerService;
    }

    public JsonObject findCaseResults(JsonEnvelope envelope) {

        final JsonObjectBuilder caseResultsPayload = createObjectBuilder();

        final UUID caseId = fromString(envelope.payloadAsJsonObject().getString(CASE_ID));
        caseResultsPayload.add(CASE_ID, caseId.toString());

        final CaseView aCase = caseService.findCase(caseId);

        final JsonArrayBuilder decisions = createArrayBuilder();

        aCase.getCaseDecisions().forEach(caseDecision -> {
           // enrich with verdict information in case
            caseDecision.getOffenceDecisions().forEach(offenceDecision -> enrichOffenceDecision(aCase,offenceDecision));

            // query the view and covert to the event payload structure
            final JsonEnvelope decisionSavedEvent = convertToDecisionSavedEvent(envelope, caseDecision);

            final JsonObject enforcementArea = getEnforcementArea(decisionSavedEvent, caseDecision.getSession(), aCase);

            // convert to the RESULTING structure
            final JsonEnvelope referencedDecisionSavedEvent = convertToReferencedDecisionSaved(decisionSavedEvent, enforcementArea);

            caseResultsPayload.add("accountDivisionCode", referencedDecisionSavedEvent.payloadAsJsonObject().getInt("accountDivisionCode"));
            caseResultsPayload.add("enforcingCourtCode", referencedDecisionSavedEvent.payloadAsJsonObject().getInt("enforcingCourtCode"));

            // // convert to the generic/common RESULTS structure
            final JsonObject publicReferencedDecisionSaved = convertToPublicReferencedDecisionSavedEvent(referencedDecisionSavedEvent, aCase);

            decisions.add(publicReferencedDecisionSaved);
        });
        caseResultsPayload.add("caseDecisions", decisions);
        return caseResultsPayload.build();

    }

    private JsonObject convertToPublicReferencedDecisionSavedEvent(JsonEnvelope referencedDecisionSavedEnvelope, CaseView caseView) {
        final JsonObject referencedDecisionSavedPayload = referencedDecisionSavedEnvelope.payloadAsJsonObject();

        final JsonObjectBuilder publicReferencedDecisionSavedEventBuilder = createObjectBuilder()
                .add(SJP_SESSION_ID, referencedDecisionSavedPayload.getString(SJP_SESSION_ID))
                .add(RESULTED_ON, referencedDecisionSavedPayload.getString(CREATED));

        final Employer employer = employerService.getEmployer(caseView.getDefendant().getId())
                .orElseGet(() -> new Employer(null, null, null, null, null));

        final JsonArray offences = offenceHelper.populateOffences(caseView, employer, referencedDecisionSavedEnvelope);
        return publicReferencedDecisionSavedEventBuilder.add(OFFENCES, offences).build();
    }

    private JsonEnvelope convertToReferencedDecisionSaved(final JsonEnvelope decisionSavedEvent, JsonObject enforcementArea) {
        final JsonObject decisionSavedEventPayload = decisionSavedEvent.payloadAsJsonObject();

        final JsonArray offenceDecisions = decisionSavedEventPayload.getJsonArray(OFFENCE_DECISIONS);

        final JsonObject referencedDecisionSaved = createObjectBuilder()
                .add(ID, decisionSavedEventPayload.getString(DECISION_ID))
                .add(CASE_ID, decisionSavedEventPayload.getString(CASE_ID))
                .add(SJP_SESSION_ID, decisionSavedEventPayload.getString(SJP_SESSION_ID))
                .add(CREATED, decisionSavedEventPayload.getString(RESULTED_ON))
                .add(ACCOUNT_DIVISION_CODE, enforcementArea.getInt(ACCOUNT_DIVISION_CODE))
                .add(ENFORCING_COURT_CODE, enforcementArea.getInt(ENFORCING_COURT_CODE))
                .add(OFFENCES, referencedDecisionSavedOffenceConverter.convertOffenceDecisions(decisionSavedEvent))
                .add(INTERIM, isInterim(offenceDecisions))
                .build();

        return envelopeFrom(metadataFrom(decisionSavedEvent.metadata()), referencedDecisionSaved);
    }

    private JsonEnvelope convertToDecisionSavedEvent(final JsonEnvelope envelope, final CaseDecisionView caseDecisionView) {
        final String caseId = envelope.payloadAsJsonObject().getString(CASE_ID);
        final JsonObjectBuilder decisionSavedPayload = createObjectBuilder()
                .add(CASE_ID, caseId)
                .add(SJP_SESSION_ID, caseDecisionView.getSession().getSessionId().toString())
                .add(DECISION_ID, caseDecisionView.getId().toString())
                .add(RESULTED_ON, caseDecisionView.getSavedAt().toString());

        final FinancialImpositionView financialImpositionView = caseDecisionView.getFinancialImposition();
        if(nonNull(financialImpositionView)) {
            decisionSavedPayload.add(FINANCIAL_IMPOSITION, decisionSavedOffenceConverter.convertFinancialImposition(financialImpositionView));
        }

        final JsonArrayBuilder offenceDecisions = createArrayBuilder();

        caseDecisionView.getOffenceDecisions().stream()
                .forEach(offenceDecisionView -> {
                    final JsonObject jsonObject = decisionSavedOffenceConverter.convertOffenceDecision(offenceDecisionView);
                    offenceDecisions.add(jsonObject);
                });

        decisionSavedPayload.add(OFFENCE_DECISIONS, offenceDecisions);

        return envelopeFrom(metadataFrom(envelope.metadata()), decisionSavedPayload);
    }

    private JsonObject getEnforcementArea(final JsonEnvelope envelope, final SessionView sessionView, final CaseView caseView) {
        final String postcode = caseView.getDefendant().getPersonalDetails().getAddress().getPostcode();
        final String localJusticeAreaNationalCourtCode = sessionView.getLocalJusticeAreaNationalCourtCode();

        return getEnforcementArea(postcode, localJusticeAreaNationalCourtCode, envelope);
    }

    private static boolean isInterim(final JsonArray offenceDecisions) {
        return !offenceDecisions.getValuesAs(JsonObject.class).stream()
                .map(offenceDecision -> DecisionType.valueOf(offenceDecision.getString(TYPE)))
                .allMatch(DecisionType::isFinal);
    }

    private JsonObject getEnforcementArea(final String defendantPostcode, final String localJusticeAreaNationalCourtCode, final JsonEnvelope sourceEnvelope) {
        return ofNullable(defendantPostcode)
                .flatMap(postcode -> referenceDataService.getEnforcementAreaByPostcode(postcode, sourceEnvelope))
                .orElseGet(() -> referenceDataService.getEnforcementAreaByLocalJusticeAreaNationalCourtCode(localJusticeAreaNationalCourtCode, sourceEnvelope)
                        .orElseThrow(() -> new EnforcementAreaNotFoundException(defendantPostcode, localJusticeAreaNationalCourtCode)));

    }

    private  void enrichOffenceDecision(final CaseView aCase, final OffenceDecisionView offenceDecision) {
         aCase.getDefendant().getOffences().stream()
                .filter(offence -> offence.getId().equals(offenceDecision.getOffenceId()))
                .findFirst().ifPresent(offence  -> addVerdictToOffenceDecision(offenceDecision,offence));
    }

    private  void addVerdictToOffenceDecision(final OffenceDecisionView offenceDecision, final OffenceView offence) {
        if (isNull(offenceDecision.getVerdict())){
            offenceDecision.setVerdict(offence.getConviction());
        }

    }

}

