package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_DVLA_ENDORSEMENT_CODE;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_DVLA_ENDORSEMENT_CODE2;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_DVLA_ENDORSEMENT_CODE3;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_DVLA_ENDORSEMENT_CODE4;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_ORIGINAL_CONVICTION_DATE;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_ORIGINAL_CONVICTION_DATE2;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_ORIGINAL_CONVICTION_DATE3;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_ORIGINAL_CONVICTION_DATE4;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_ORIGINAL_COURT_CODE;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_ORIGINAL_COURT_CODE2;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_ORIGINAL_COURT_CODE3;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_ORIGINAL_COURT_CODE4;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_ORIGINAL_OFFENCE_DATE;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_ORIGINAL_OFFENCE_DATE2;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_ORIGINAL_OFFENCE_DATE3;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.DER_ORIGINAL_OFFENCE_DATE4;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.PROSECUTOR_TOBE_NOTIFIED;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.REOPENED_CONVICTION_AND_SENTENCE_IMPOSED_AT_SET_ASIDE;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.REOPENED_FINANCIAL_PENALTIES_TOBE_WRITTEN_OF;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.REOPENED_SENTENCE_IMPOSED_AT_SET_ASIDE;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.REOPENED_UNDER;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.REOPENED_VEHICLE_REGISTRATION_MARK;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.RFSD_REASONS;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.STAT_DEC_FINANCIAL_PENALTIES_TOBE_WRITTEN_OF;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.STAT_DEC_SERVICE_ACCEPTED_OUTSIDE_OF_21_DAYS_LIMIT;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.STAT_DEC_STATUTORY_DECLARATION_MADE_UNDER;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationPrompt.STAT_DEC_VEHICLE_REGISTRATION_MARK;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationResultCode.DER;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationResultCode.RFSD;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationResultCode.ROPENED;
import static uk.gov.moj.cpp.sjp.query.view.service.ApplicationResultCode.STDEC;

import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.persistence.entity.ApplicationStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.ApplicationType;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseApplicationRepository;
import uk.gov.moj.cpp.sjp.query.view.response.ApplicationDecisionView;
import uk.gov.moj.cpp.sjp.query.view.response.ApplicationView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDecisionView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.OffenceDecisionView;
import uk.gov.moj.cpp.sjp.query.view.response.OffenceView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class CaseApplicationService {

    private static final String DVLA_CODE_TT99 = "TT99";
    public static final String ID = "id";
    public static final String TYPE = "type";
    public static final String APPLICATION_RECEIVED_DATE = "applicationReceivedDate";
    public static final String APPLICATION_REFERENCE = "applicationReference";
    public static final String APPLICANT = "applicant";
    public static final String RESPONDENTS = "respondents";
    public static final String THIRD_PARTIES = "thirdParties";
    public static final String SUBJECT = "subject";
    public static final String APPLICATION_PARTICULARS = "applicationParticulars";
    public static final String APPLICATION_STATUS = "applicationStatus";
    public static final String PLEA = "plea";
    public static final String VERDICT = "verdict";

    public static final String JUDICIAL_RESULTS = "judicialResults";
    public static final String JUDICIAL_RESULT_ID = "judicialResultId";
    public static final String JUDICIAL_RESULT_PROMPTS = "judicialResultPrompts";

    public static final String ORDERED_HEARING_ID = "orderedHearingId";
    public static final String JUDICIAL_RESULT_PROMPT_TYPE_ID = "judicialResultPromptTypeId";
    public static final String VALUE = "value";
    public static final String REFERENCE = "reference";
    public static final String PROSECUTORTOBENOTIFIED_ORGANISATION_NAME = "prosecutortobenotifiedOrganisationName";
    public static final String PROSECUTORTOBENOTIFIED_ADDRESS_1 = "prosecutortobenotifiedAddress1";
    public static final String PROSECUTORTOBENOTIFIED_ADDRESS_2 = "prosecutortobenotifiedAddress2";
    public static final String PROSECUTORTOBENOTIFIED_ADDRESS_4 = "prosecutortobenotifiedAddress4";
    public static final String PROSECUTORTOBENOTIFIED_ADDRESS_3 = "prosecutortobenotifiedAddress3";
    public static final String PROSECUTORTOBENOTIFIED_ADDRESS_5 = "prosecutortobenotifiedAddress5";
    public static final String PROSECUTORTOBENOTIFIED_POST_CODE = "prosecutortobenotifiedPostCode";

    public static final String FULL_NAME = "fullName";
    public static final String ADDRESS = "address";
    public static final String ADDRESS_1 = "address1";
    public static final String ADDRESS_2 = "address2";
    public static final String ADDRESS_3 = "address3";
    public static final String ADDRESS_4 = "address4";
    public static final String ADDRESS_5 = "address5";
    public static final String POSTCODE = "postcode";
    public static final String STATDEC_MADE_UNDER = "Section 14 of the Magistrates' Courts Act 1980";
    public static final String REOPNED_MADE_UNDER = "Section 14 of the Magistrates' Courts Act 1980";
    public static final String DVLA_CODE = "dvlaCode";
    public static final String COURT_APPLICATION_CASES = "courtApplicationCases";

    @Inject
    private CaseApplicationRepository caseApplicationRepository;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private CaseService caseService;

    @Inject
    private ReferenceDataOffencesService referenceDataOffencesService;

    public Optional<ApplicationView> findApplication(final UUID applicationId) {

        return ofNullable(caseApplicationRepository.findBy(applicationId)).map(ApplicationView::new);
    }

    public JsonObject collateApplicationForResults(final UUID caseId) {
        final JsonObjectBuilder collatedApplicationBuilder = createObjectBuilder();
        final CaseView caseView = caseService.findCase(caseId);
        if (caseView.getCaseApplication() != null) {
            final ApplicationView applicationView = caseView.getCaseApplication();
            collatedApplicationBuilder.add(ID, applicationView.getApplicationId().toString());
            collatedApplicationBuilder.add(TYPE, applicationView.getInitiatedApplication().getJsonObject(TYPE));
            collatedApplicationBuilder.add(APPLICATION_RECEIVED_DATE, applicationView.getDateReceived().toString());

            addAttribute(APPLICATION_REFERENCE, applicationView.getApplicationReference(), collatedApplicationBuilder);
            
            addAttribute(APPLICANT, applicationView.getInitiatedApplication().getJsonObject(APPLICANT), collatedApplicationBuilder);

            addAttribute(RESPONDENTS, applicationView.getInitiatedApplication().getJsonArray(RESPONDENTS), collatedApplicationBuilder);

            addAttribute(THIRD_PARTIES,applicationView.getInitiatedApplication().getJsonArray(THIRD_PARTIES), collatedApplicationBuilder);

            addAttribute(SUBJECT,applicationView.getInitiatedApplication().getJsonObject(SUBJECT), collatedApplicationBuilder);

            addAttribute(APPLICATION_PARTICULARS, applicationView.getInitiatedApplication().getString(APPLICATION_PARTICULARS, null), collatedApplicationBuilder);

            collatedApplicationBuilder.add(APPLICATION_STATUS, ApplicationStatus.FINALISED.toString());

            addAttribute(PLEA, applicationView.getInitiatedApplication().getJsonObject(PLEA), collatedApplicationBuilder);

            addAttribute(VERDICT, applicationView.getInitiatedApplication().getJsonObject(VERDICT), collatedApplicationBuilder);

            addAttribute(COURT_APPLICATION_CASES, applicationView.getInitiatedApplication().getJsonArray(COURT_APPLICATION_CASES), collatedApplicationBuilder);

            // populate the results
            final String vehicleRegistrationMark = getVehicleRegistrationMark(caseView);
            final boolean conviction = offenceWithConviction(caseView);
            final String prosecutingAuthority = caseView.getProsecutingAuthority();

            final JsonArrayBuilder judicialResultsBuilder = createArrayBuilder();
            if (applicationView.getApplicationDecision().isGranted()) {
                if (applicationView.getApplicationType().equals(ApplicationType.STAT_DEC)) {
                    judicialResultsBuilder.add(handleStatDec(applicationView.getApplicationDecision(),
                            vehicleRegistrationMark,
                            prosecutingAuthority));
                } else if (applicationView.getApplicationType().equals(ApplicationType.REOPENING)) {
                    judicialResultsBuilder.add(handleReopening(applicationView.getApplicationDecision(),
                            vehicleRegistrationMark,
                            prosecutingAuthority,
                            conviction));
                }
                final List<OffenceDecisionView> offenceDecisionWithDisqualifications = getOffenceDecisionWithDisqualifications(caseView);
                if(!offenceDecisionWithDisqualifications.isEmpty()) {
                    judicialResultsBuilder.add(handleEndorsements(caseView, applicationView));
                }
            } else {
                judicialResultsBuilder.add(handleRefused(applicationView));
            }

            collatedApplicationBuilder.add(JUDICIAL_RESULTS, judicialResultsBuilder.build());
        }
        return collatedApplicationBuilder.build();
    }

    private void addAttribute(final String attributeName, final String value, final JsonObjectBuilder collatedApplicationBuilder) {
        if(value != null) {
            collatedApplicationBuilder.add(attributeName, value);
        }
    }

    private void addAttribute(final String attributeName, final JsonValue value,final JsonObjectBuilder collatedApplicationBuilder) {
        if(value != null) {
            collatedApplicationBuilder.add(attributeName, value);
        }
    }

    private String getVehicleRegistrationMark(final CaseView caseView) {
        final Optional<OffenceView> optionalOffenceView =
                         caseView.getDefendant()
                        .getOffences()
                        .stream()
                        .filter(offenceView -> offenceView.getVehicleRegistrationMark() != null)
                        .findFirst();
        String vehicleRegistrationMark = null;

        if (optionalOffenceView.isPresent()) {
            vehicleRegistrationMark = optionalOffenceView.get().getVehicleRegistrationMark();
        }
        return vehicleRegistrationMark;
    }

    private JsonObject handleRefused(final ApplicationView applicationView) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add(JUDICIAL_RESULT_ID, RFSD.getResultDefinitionId());
        jsonObjectBuilder.add(ORDERED_HEARING_ID, applicationView.getApplicationDecision().getSession().getSessionId().toString());

        final JsonArrayBuilder judicialResultsPromptsBuilder = createArrayBuilder();
        judicialResultsPromptsBuilder.add(createObjectBuilder()
                .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, RFSD_REASONS.getPromptId())
                .add(VALUE, applicationView.getApplicationDecision().getRejectionReason())
                .build());
        jsonObjectBuilder.add(JUDICIAL_RESULT_PROMPTS, judicialResultsPromptsBuilder.build());
        return jsonObjectBuilder.build();
    }

    private JsonObject handleStatDec(final ApplicationDecisionView applicationDecisionView,
                               final String vehicleRegistrationMark,
                               final String prosecutingAuthority) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add(JUDICIAL_RESULT_ID, STDEC.getResultDefinitionId());
        jsonObjectBuilder.add(ORDERED_HEARING_ID, applicationDecisionView.getSession().getSessionId().toString());

        // declaration
        final JsonArrayBuilder judicialResultsPromptsBuilder = createArrayBuilder();
        judicialResultsPromptsBuilder.add(createObjectBuilder()
                .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, STAT_DEC_STATUTORY_DECLARATION_MADE_UNDER.getPromptId())
                .add(VALUE, STATDEC_MADE_UNDER)
                .build());
        // financial penalties
        judicialResultsPromptsBuilder.add(createObjectBuilder()
                .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, STAT_DEC_FINANCIAL_PENALTIES_TOBE_WRITTEN_OF.getPromptId())
                .add(VALUE, applicationDecisionView.getPreviousFinalDecisionObject().getFinancialImposition() != null)
                .build());

        // vehicle registration mark
        if (null != vehicleRegistrationMark) {
            judicialResultsPromptsBuilder.add(createObjectBuilder()
                    .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, STAT_DEC_VEHICLE_REGISTRATION_MARK.getPromptId())
                    .add(VALUE, vehicleRegistrationMark)
                    .build());
        }

        // prosecutor
        buildProsecutor(prosecutingAuthority, judicialResultsPromptsBuilder);

        if (applicationDecisionView.getOutOfTimeReason() != null) {
            judicialResultsPromptsBuilder.add(createObjectBuilder()
                    .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, STAT_DEC_SERVICE_ACCEPTED_OUTSIDE_OF_21_DAYS_LIMIT.getPromptId())
                    .add(VALUE, applicationDecisionView.getOutOfTimeReason())
                    .build());
        }

        jsonObjectBuilder.add(JUDICIAL_RESULT_PROMPTS, judicialResultsPromptsBuilder.build());

        return jsonObjectBuilder.build();
    }

    private JsonObject handleReopening(final ApplicationDecisionView applicationDecisionView,
                                 final String vehicleRegistrationMark,
                                 final String prosecutingAuthority,
                                 final boolean conviction) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add(JUDICIAL_RESULT_ID, ROPENED.getResultDefinitionId());
        jsonObjectBuilder.add(ORDERED_HEARING_ID, applicationDecisionView.getSession().getSessionId().toString());

        final JsonArrayBuilder judicialResultsPromptsBuilder = createArrayBuilder();
        judicialResultsPromptsBuilder.add(createObjectBuilder()
                .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, REOPENED_UNDER.getPromptId())
                .add(VALUE, REOPNED_MADE_UNDER)
                .build());

        judicialResultsPromptsBuilder.add(createObjectBuilder()
                .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, REOPENED_FINANCIAL_PENALTIES_TOBE_WRITTEN_OF.getPromptId())
                .add(VALUE, applicationDecisionView.getPreviousFinalDecisionObject().getFinancialImposition() != null)
                .build());

        if (null != vehicleRegistrationMark) {
            judicialResultsPromptsBuilder.add(createObjectBuilder()
                    .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, REOPENED_VEHICLE_REGISTRATION_MARK.getPromptId())
                    .add(VALUE, vehicleRegistrationMark)
                    .build());
        }

        // prosecutor
        buildProsecutor(prosecutingAuthority, judicialResultsPromptsBuilder);

        if (conviction) {
            judicialResultsPromptsBuilder.add(createObjectBuilder()
                    .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, REOPENED_CONVICTION_AND_SENTENCE_IMPOSED_AT_SET_ASIDE.getPromptId())
                    .add(VALUE, applicationDecisionView.getPreviousFinalDecision().toString())
                    .build());
        } else {
            judicialResultsPromptsBuilder.add(createObjectBuilder()
                    .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, REOPENED_SENTENCE_IMPOSED_AT_SET_ASIDE.getPromptId())
                    .add(VALUE, applicationDecisionView.getPreviousFinalDecision().toString())
                    .build());
        }

        jsonObjectBuilder.add(JUDICIAL_RESULT_PROMPTS, judicialResultsPromptsBuilder.build());

        return jsonObjectBuilder.build();
    }

    @SuppressWarnings("sqid:S1199")
    private JsonObject handleEndorsements(final CaseView caseView, final ApplicationView applicationView) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        jsonObjectBuilder.add(JUDICIAL_RESULT_ID, DER.getResultDefinitionId());
        jsonObjectBuilder.add(ORDERED_HEARING_ID, applicationView.getApplicationDecision().getSession().getSessionId().toString());

        final List<OffenceDecisionView> offenceDecisionWithDisqualifications = getOffenceDecisionWithDisqualifications(caseView);

        final JsonArrayBuilder judicialResultsPromptsBuilder = createArrayBuilder();
        int index = 0;
        for (final OffenceDecisionView offenceDecisionView : offenceDecisionWithDisqualifications) {
            switch (index) {
                case 0:
                    createDrivingEndorsementToBeRemoved(offenceDecisionView, caseView, DER_ORIGINAL_COURT_CODE.getPromptId(), DER_ORIGINAL_CONVICTION_DATE.getPromptId(), DER_DVLA_ENDORSEMENT_CODE.getPromptId(), DER_ORIGINAL_OFFENCE_DATE.getPromptId(), judicialResultsPromptsBuilder);
                    index++;
                    break;
                case 1:
                    createDrivingEndorsementToBeRemoved(offenceDecisionView, caseView, DER_ORIGINAL_COURT_CODE2.getPromptId(), DER_ORIGINAL_CONVICTION_DATE2.getPromptId(), DER_DVLA_ENDORSEMENT_CODE2.getPromptId(), DER_ORIGINAL_OFFENCE_DATE2.getPromptId(), judicialResultsPromptsBuilder);
                    index++;
                    break;
                case 2:
                    createDrivingEndorsementToBeRemoved(offenceDecisionView, caseView, DER_ORIGINAL_COURT_CODE3.getPromptId(), DER_ORIGINAL_CONVICTION_DATE3.getPromptId(), DER_DVLA_ENDORSEMENT_CODE3.getPromptId(), DER_ORIGINAL_OFFENCE_DATE3.getPromptId(),judicialResultsPromptsBuilder );
                    index++;
                    break;
                case 3:
                    createDrivingEndorsementToBeRemoved(offenceDecisionView, caseView, DER_ORIGINAL_COURT_CODE4.getPromptId(), DER_ORIGINAL_CONVICTION_DATE4.getPromptId(), DER_DVLA_ENDORSEMENT_CODE4.getPromptId(), DER_ORIGINAL_OFFENCE_DATE4.getPromptId(), judicialResultsPromptsBuilder);
                    index++;
                    break;
                default:
            }
        }
        jsonObjectBuilder.add(JUDICIAL_RESULT_PROMPTS, judicialResultsPromptsBuilder.build());
        return jsonObjectBuilder.build();
    }

    private List<OffenceDecisionView> getOffenceDecisionWithDisqualifications(final CaseView caseView) {
        final CaseDecisionView previousFinalDecision = caseView
                .getCaseApplication()
                .getApplicationDecision()
                .getPreviousFinalDecisionObject();

        return previousFinalDecision
                .getOffenceDecisions()
                .stream()
                .filter(this::hasEndorsementsOrDisqualification)
                .collect(Collectors.toList());
    }

    private void buildProsecutor(final String prosecutingAuthority, final JsonArrayBuilder judicialResultsPromptsBuilder) {
        final JsonObject prosecutor = referenceDataService.getProsecutor(prosecutingAuthority);
        judicialResultsPromptsBuilder.add(createObjectBuilder()
                .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, PROSECUTOR_TOBE_NOTIFIED.getPromptId())
                .add(REFERENCE, PROSECUTORTOBENOTIFIED_ORGANISATION_NAME)
                .add(VALUE, prosecutor.getString(FULL_NAME, ""))
                .build());

        ofNullable(prosecutor.getJsonObject(ADDRESS))
                .map(address -> address.getString(ADDRESS_1, null))
                .ifPresent(address1 -> judicialResultsPromptsBuilder.add(createObjectBuilder()
                        .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, PROSECUTOR_TOBE_NOTIFIED.getPromptId())
                        .add(REFERENCE, PROSECUTORTOBENOTIFIED_ADDRESS_1)
                        .add(VALUE, address1)
                        .build()));

        ofNullable(prosecutor.getJsonObject(ADDRESS))
                .map(address -> address.getString(ADDRESS_2, null))
                .ifPresent(address2 -> judicialResultsPromptsBuilder.add(createObjectBuilder()
                        .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, PROSECUTOR_TOBE_NOTIFIED.getPromptId())
                        .add(REFERENCE, PROSECUTORTOBENOTIFIED_ADDRESS_2)
                        .add(VALUE, address2)
                        .build()));

        ofNullable(prosecutor.getJsonObject(ADDRESS))
                .map(address -> address.getString(ADDRESS_3, null))
                .ifPresent(address3 -> judicialResultsPromptsBuilder.add(createObjectBuilder()
                        .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, PROSECUTOR_TOBE_NOTIFIED.getPromptId())
                        .add(REFERENCE, PROSECUTORTOBENOTIFIED_ADDRESS_3)
                        .add(VALUE, address3)
                        .build()));

        ofNullable(prosecutor.getJsonObject(ADDRESS))
                .map(address -> address.getString(ADDRESS_4, null))
                .ifPresent(address4 -> judicialResultsPromptsBuilder.add(createObjectBuilder()
                        .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, PROSECUTOR_TOBE_NOTIFIED.getPromptId())
                        .add(REFERENCE, PROSECUTORTOBENOTIFIED_ADDRESS_4)
                        .add(VALUE, address4)
                        .build()));

        ofNullable(prosecutor.getJsonObject(ADDRESS))
                .map(address -> address.getString(ADDRESS_5, null))
                .ifPresent(address5 -> judicialResultsPromptsBuilder.add(createObjectBuilder()
                        .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, PROSECUTOR_TOBE_NOTIFIED.getPromptId())
                        .add(REFERENCE, PROSECUTORTOBENOTIFIED_ADDRESS_5)
                        .add(VALUE, address5)
                        .build()));

        ofNullable(prosecutor.getJsonObject(ADDRESS))
                .map(address -> address.getString(POSTCODE, null))
                .ifPresent(postCode -> judicialResultsPromptsBuilder.add(createObjectBuilder()
                        .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, PROSECUTOR_TOBE_NOTIFIED.getPromptId())
                        .add(REFERENCE, PROSECUTORTOBENOTIFIED_POST_CODE)
                        .add(VALUE, postCode)
                        .build()));
    }

    @SuppressWarnings("sqid:S3655")
    private void createDrivingEndorsementToBeRemoved(final OffenceDecisionView offenceDecisionView,
                                                           final CaseView caseView,
                                                           final String originalCourtCodePromptId,
                                                           final String originalConvictionDatePromptId,
                                                           final String endorsementCodePromptId,
                                                           final String originalOffenceDatePromptId,
                                                           final JsonArrayBuilder judicialResultsPromptsBuilder) {

        final ApplicationView applicationView = caseView.getCaseApplication();
        // lja code
        final String ljaCode = applicationView.getApplicationDecision().getSession().getLocalJusticeAreaNationalCourtCode();

        // original conviction date
        final ApplicationDecisionView applicationDecisionView = applicationView.getApplicationDecision();
        final String originalConvictionDate =
                ofNullable(offenceDecisionView.getConvictionDate())
                        .orElse(applicationDecisionView.getPreviousFinalDecision().toLocalDate()).toString();

        // offence start date
        final Optional<OffenceView> offenceStartDateOptional = caseView
                .getDefendant()
                .getOffences()
                .stream()
                .filter(offenceView -> offenceView.getId().equals(offenceDecisionView.getOffenceId()))
                .findFirst();

        String offenceStartDate = null;
        if (offenceStartDateOptional.isPresent()) {
            offenceStartDate = offenceStartDateOptional.get().getStartDate().toString();
        }

        // dvla code
        final Optional<OffenceView> offenceCodeOptional = caseView
                .getDefendant()
                .getOffences()
                .stream()
                .filter(offenceView -> offenceView.getId().equals(offenceDecisionView.getOffenceId()))
                .findFirst();

        String offenceCode = null;
        if (offenceCodeOptional.isPresent()) {
            offenceCode = offenceCodeOptional.get().getOffenceCode();
        }

        final JsonObject offenceReferenceData = getOffenceReferenceData(offenceCode, offenceStartDate);
        final String dvlaCode = hasPointsDisqualification(offenceDecisionView) ? DVLA_CODE_TT99 : offenceReferenceData.getString(DVLA_CODE, null);

        judicialResultsPromptsBuilder.add(createObjectBuilder()
                .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, originalCourtCodePromptId)
                .add(VALUE, ljaCode)
                .build());

        judicialResultsPromptsBuilder.add(createObjectBuilder()
                .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, originalConvictionDatePromptId)
                .add(VALUE, originalConvictionDate)
                .build());

        judicialResultsPromptsBuilder.add(createObjectBuilder()
                .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, originalOffenceDatePromptId)
                .add(VALUE, offenceStartDate)
                .build());

        judicialResultsPromptsBuilder.add(createObjectBuilder()
                .add(JUDICIAL_RESULT_PROMPT_TYPE_ID, endorsementCodePromptId)
                .add(VALUE, dvlaCode)
                .build());
    }

    public boolean hasPointsDisqualification(final OffenceDecisionView offenceDecisionView) {
        return isTrue(offenceDecisionView.getDisqualification())
                && offenceDecisionView.getDisqualificationType().equals(DisqualificationType.POINTS);
    }

    private boolean hasEndorsementsOrDisqualification(final OffenceDecisionView offenceDecisionView) {
        final boolean licenceEndorsed = isTrue(offenceDecisionView.getLicenceEndorsement());
        final boolean disqualification = isTrue(offenceDecisionView.getDisqualification());
        return licenceEndorsed || disqualification;
    }

    private Boolean offenceWithConviction(final CaseView caseView) {
        return caseView
                .getDefendant()
                .getOffences()
                .stream()
                .anyMatch(offenceView -> offenceView.getConviction() != null);
    }

    private JsonObject getOffenceReferenceData(final String offenceCode, final String startDate) {
        return referenceDataOffencesService.getOffenceReferenceData(offenceCode, startDate);
    }
}