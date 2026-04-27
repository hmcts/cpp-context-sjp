package uk.gov.moj.cpp.sjp.event.processor.utils;

import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPEAL_ALLOWED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPEAL_DISMISSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPEAL_WITHDRAWN;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_GRANTED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_REFUSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_WITHDRAWN;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_GRANTED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_REFUSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_WITHDRAWN;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.AACA;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.AACD;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.AASA;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.AASD;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.ACSD;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.APA;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.ASV;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.AW;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.DISM;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.G;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.RFSD;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.ROPENED;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.STDEC;
import static uk.gov.moj.cpp.sjp.event.processor.utils.ApplicationResult.WDRN;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEAL_AGAINST_CONVICTION;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEAL_AGAINST_SENTENCE;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEAL_AGAINST_SENTENCE_AND_CONVICTION;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPLICATION_TO_REOPEN_CASE;

import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ApplicationResultStatusResolver {

    private static final Map<String, ApplicationStatus> resultToActionMap = new HashMap();
    private static final Map<String, ApplicationStatus> resultToActionByCodeMap = new ApplicationStatusByCodeMapBuilder()
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AACA, APPEAL_ALLOWED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AASA, APPEAL_ALLOWED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AACD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AASD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, ACSD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, DISM, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, WDRN, APPEAL_WITHDRAWN)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AW, APPEAL_WITHDRAWN)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AACA, APPEAL_ALLOWED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AASA, APPEAL_ALLOWED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AACD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AASD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, ACSD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, DISM, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, WDRN, APPEAL_WITHDRAWN)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AW, APPEAL_WITHDRAWN)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AACA, APPEAL_ALLOWED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AASA, APPEAL_ALLOWED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AACD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AASD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, ACSD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, DISM, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, WDRN, APPEAL_WITHDRAWN)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AW, APPEAL_WITHDRAWN)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, G, STATUTORY_DECLARATION_GRANTED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, STDEC, STATUTORY_DECLARATION_GRANTED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, RFSD, STATUTORY_DECLARATION_REFUSED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, WDRN, STATUTORY_DECLARATION_WITHDRAWN)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, G, STATUTORY_DECLARATION_GRANTED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, STDEC, STATUTORY_DECLARATION_GRANTED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, RFSD, STATUTORY_DECLARATION_REFUSED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, WDRN, STATUTORY_DECLARATION_WITHDRAWN)
            .add(APPLICATION_TO_REOPEN_CASE, G, REOPENING_GRANTED)
            .add(APPLICATION_TO_REOPEN_CASE, ROPENED, REOPENING_GRANTED)
            .add(APPLICATION_TO_REOPEN_CASE, RFSD, REOPENING_REFUSED)
            .add(APPLICATION_TO_REOPEN_CASE, WDRN, REOPENING_WITHDRAWN)
            .build();

    static {

        resultToActionMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType() + G.getResultId(), STATUTORY_DECLARATION_GRANTED);
        resultToActionMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType() + STDEC.getResultId(), STATUTORY_DECLARATION_GRANTED);
        resultToActionMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType() + RFSD.getResultId(), STATUTORY_DECLARATION_REFUSED);
        resultToActionMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType() + WDRN.getResultId(), STATUTORY_DECLARATION_WITHDRAWN);

        resultToActionMap.put(APPLICATION_TO_REOPEN_CASE.getApplicationType() + G.getResultId(), REOPENING_GRANTED);
        resultToActionMap.put(APPLICATION_TO_REOPEN_CASE.getApplicationType() + ROPENED.getResultId(), REOPENING_GRANTED);
        resultToActionMap.put(APPLICATION_TO_REOPEN_CASE.getApplicationType() + RFSD.getResultId(), REOPENING_REFUSED);
        resultToActionMap.put(APPLICATION_TO_REOPEN_CASE.getApplicationType() + WDRN.getResultId(), REOPENING_WITHDRAWN);

        // APPEAL_ALLOWED
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + AACA.getResultId(), APPEAL_ALLOWED);
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + AASA.getResultId(), APPEAL_ALLOWED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + AACA.getResultId(), APPEAL_ALLOWED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + AASA.getResultId(), APPEAL_ALLOWED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + AACA.getResultId(), APPEAL_ALLOWED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + AASA.getResultId(), APPEAL_ALLOWED);

        // APPEAL REFUSED
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + RFSD.getResultId(), ApplicationStatus.APPEAL_REFUSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + RFSD.getResultId(), ApplicationStatus.APPEAL_REFUSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + RFSD.getResultId(), ApplicationStatus.APPEAL_REFUSED);

        // APPEAL WITHDRAWN
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + WDRN.getResultId(), APPEAL_WITHDRAWN);
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + AW.getResultId(), APPEAL_WITHDRAWN);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + WDRN.getResultId(), APPEAL_WITHDRAWN);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + AW.getResultId(), APPEAL_WITHDRAWN);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + WDRN.getResultId(), APPEAL_WITHDRAWN);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + AW.getResultId(), APPEAL_WITHDRAWN);

        // APPEAL DISMISSED
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + AACD.getResultId(), APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + AASD.getResultId(), APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + ACSD.getResultId(), APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + ASV.getResultId(), ApplicationStatus.APPLICATION_DISMISSED_SENTENCE_VARIED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + AACD.getResultId(), APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + AASD.getResultId(), APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + ACSD.getResultId(), APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + ASV.getResultId(), ApplicationStatus.APPLICATION_DISMISSED_SENTENCE_VARIED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + AACD.getResultId(), APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + AASD.getResultId(), APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + ACSD.getResultId(), APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + ASV.getResultId(), ApplicationStatus.APPLICATION_DISMISSED_SENTENCE_VARIED);

        // APPEAL ABANDONED
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + APA.getResultId(), ApplicationStatus.APPEAL_ABANDONED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + APA.getResultId(), ApplicationStatus.APPEAL_ABANDONED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + APA.getResultId(), ApplicationStatus.APPEAL_ABANDONED);
    }

    private ApplicationResultStatusResolver() {
    }

    public static String getApplicationStatus(String courtApplicationType, UUID resultDefinitionId, String courtApplicationCode) {

        final ApplicationStatus applicationStatus = Optional.ofNullable(resultToActionMap.get(courtApplicationType + resultDefinitionId.toString())).
                orElse(resultToActionByCodeMap.get(courtApplicationCode + resultDefinitionId.toString()));

        return applicationStatus == null ? ApplicationStatus.APPLICATION_STATUS_NOT_KNOWN.toString() : String.valueOf(applicationStatus);
    }

    private static final class ApplicationStatusByCodeMapBuilder {
        private final Map<String, ApplicationStatus> mappings = new HashMap<>();

        private ApplicationStatusByCodeMapBuilder add(SjpApplicationTypes courtApplicationType,
                                                      ApplicationResult applicationResult,
                                                      ApplicationStatus applicationStatus) {
            mappings.put(buildCodeKey(courtApplicationType, applicationResult), applicationStatus);
            return this;
        }

        private String buildCodeKey(SjpApplicationTypes courtApplicationType, ApplicationResult applicationResult) {
            return courtApplicationType.getApplicationCode() + applicationResult.getResultId();
        }

        private Map<String, ApplicationStatus> build() {
            return mappings;
        }
    }

}
