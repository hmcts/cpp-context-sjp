package uk.gov.moj.cpp.sjp.event.processor.utils;

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
    private static final Map<String, ApplicationStatus> resultToActionByCodeMap = new HashMap();

    static {

        resultToActionMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType() + G.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_GRANTED);
        resultToActionMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType() + STDEC.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_GRANTED);
        resultToActionMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType() + RFSD.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_REFUSED);
        resultToActionMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationType() + WDRN.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_WITHDRAWN);

        resultToActionMap.put(APPLICATION_TO_REOPEN_CASE.getApplicationType() + G.getResultId(), ApplicationStatus.REOPENING_GRANTED);
        resultToActionMap.put(APPLICATION_TO_REOPEN_CASE.getApplicationType() + ROPENED.getResultId(), ApplicationStatus.REOPENING_GRANTED);
        resultToActionMap.put(APPLICATION_TO_REOPEN_CASE.getApplicationType() + RFSD.getResultId(), ApplicationStatus.REOPENING_REFUSED);
        resultToActionMap.put(APPLICATION_TO_REOPEN_CASE.getApplicationType() + WDRN.getResultId(), ApplicationStatus.REOPENING_WITHDRAWN);

        // APPEAL_ALLOWED
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + AACA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + AASA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + AACA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + AASA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + AACA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + AASA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);

        // APPEAL REFUSED
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + RFSD.getResultId(), ApplicationStatus.APPEAL_REFUSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + RFSD.getResultId(), ApplicationStatus.APPEAL_REFUSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + RFSD.getResultId(), ApplicationStatus.APPEAL_REFUSED);

        // APPEAL WITHDRAWN
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + WDRN.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + AW.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + WDRN.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + AW.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + WDRN.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + AW.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);

        // APPEAL DISMISSED
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + AACD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + AASD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + ACSD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + ASV.getResultId(), ApplicationStatus.APPLICATION_DISMISSED_SENTENCE_VARIED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + AACD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + AASD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + ACSD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + ASV.getResultId(), ApplicationStatus.APPLICATION_DISMISSED_SENTENCE_VARIED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + AACD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + AASD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + ACSD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + ASV.getResultId(), ApplicationStatus.APPLICATION_DISMISSED_SENTENCE_VARIED);

        // APPEAL ABANDONED
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + APA.getResultId(), ApplicationStatus.APPEAL_ABANDONED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + APA.getResultId(), ApplicationStatus.APPEAL_ABANDONED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + APA.getResultId(), ApplicationStatus.APPEAL_ABANDONED);

        //Code mapper
        resultToActionByCodeMap.put(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AACA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AASA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AACD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AASD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + ACSD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + DISM.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + WDRN.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);
        resultToActionByCodeMap.put(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AW.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);

        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AACA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AASA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AACD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AASD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + ACSD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + DISM.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + WDRN.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AW.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);

        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AACA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AASA.getResultId(), ApplicationStatus.APPEAL_ALLOWED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AACD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AASD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + ACSD.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + DISM.getResultId(), ApplicationStatus.APPEAL_DISMISSED);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + WDRN.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);
        resultToActionByCodeMap.put(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT.getApplicationCode() + AW.getResultId(), ApplicationStatus.APPEAL_WITHDRAWN);

        resultToActionByCodeMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationCode() + G.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_GRANTED);
        resultToActionByCodeMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationCode() + STDEC.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_GRANTED);
        resultToActionByCodeMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationCode() + RFSD.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_REFUSED);
        resultToActionByCodeMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP.getApplicationCode() + WDRN.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_WITHDRAWN);

        resultToActionByCodeMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP.getApplicationCode() + G.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_GRANTED);
        resultToActionByCodeMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP.getApplicationCode() + STDEC.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_GRANTED);
        resultToActionByCodeMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP.getApplicationCode() + RFSD.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_REFUSED);
        resultToActionByCodeMap.put(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP.getApplicationCode() + WDRN.getResultId(), ApplicationStatus.STATUTORY_DECLARATION_WITHDRAWN);

        resultToActionByCodeMap.put(APPLICATION_TO_REOPEN_CASE.getApplicationCode() + G.getResultId(), ApplicationStatus.REOPENING_GRANTED);
        resultToActionByCodeMap.put(APPLICATION_TO_REOPEN_CASE.getApplicationCode() + ROPENED.getResultId(), ApplicationStatus.REOPENING_GRANTED);
        resultToActionByCodeMap.put(APPLICATION_TO_REOPEN_CASE.getApplicationCode() + RFSD.getResultId(), ApplicationStatus.REOPENING_REFUSED);
        resultToActionByCodeMap.put(APPLICATION_TO_REOPEN_CASE.getApplicationCode() + WDRN.getResultId(), ApplicationStatus.REOPENING_WITHDRAWN);

    }

    private ApplicationResultStatusResolver() {
    }

    public static String getApplicationStatus(String courtApplicationType, UUID resultDefinitionId, String courtApplicationCode) {

        final ApplicationStatus applicationStatus = Optional.ofNullable(resultToActionMap.get(courtApplicationType + resultDefinitionId.toString())).
                orElse(resultToActionByCodeMap.get(courtApplicationCode + resultDefinitionId.toString()));

        return applicationStatus == null ? ApplicationStatus.APPLICATION_STATUS_NOT_KNOWN.toString() : String.valueOf(applicationStatus);
    }

}
