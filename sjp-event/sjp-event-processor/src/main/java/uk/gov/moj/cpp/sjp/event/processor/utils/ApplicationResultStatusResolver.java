package uk.gov.moj.cpp.sjp.event.processor.utils;

import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPEAL_ABANDONED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPEAL_ALLOWED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPEAL_DISMISSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPEAL_REFUSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPEAL_WITHDRAWN;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPLICATION_DISMISSED_SENTENCE_VARIED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_DISMISSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_GRANTED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_REFUSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.REOPENING_WITHDRAWN;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.STATUTORY_DECLARATION_DISMISSED;
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

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ApplicationResultStatusResolver {
    //retaining the existing behaviour of using  applicationType to look up the result for backward compatibility.
    private static final Map<String, ApplicationStatus> resultToActionMap = new HashMap<>();
    //using applicationTypeCode to look up the result instead of type where code exists.
    private static final Map<String, ApplicationStatus> resultToActionByCodeMap = new ApplicationStatusByCodeMapBuilder()
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AACA, APPEAL_ALLOWED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AASA, APPEAL_ALLOWED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AACD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AASD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, ACSD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, DISM, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, WDRN, APPEAL_WITHDRAWN)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AW, APPEAL_WITHDRAWN)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, RFSD, APPEAL_REFUSED)
            .add(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, APA, APPEAL_ABANDONED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AACA, APPEAL_ALLOWED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AASA, APPEAL_ALLOWED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AACD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AASD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, ACSD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, DISM, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, WDRN, APPEAL_WITHDRAWN)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AW, APPEAL_WITHDRAWN)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, RFSD, APPEAL_REFUSED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, APA, APPEAL_ABANDONED)
            .add(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, ASV, APPLICATION_DISMISSED_SENTENCE_VARIED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AACA, APPEAL_ALLOWED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AASA, APPEAL_ALLOWED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AACD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AASD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, ACSD, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, DISM, APPEAL_DISMISSED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, WDRN, APPEAL_WITHDRAWN)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AW, APPEAL_WITHDRAWN)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, RFSD, APPEAL_REFUSED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, APA, APPEAL_ABANDONED)
            .add(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, ASV, APPLICATION_DISMISSED_SENTENCE_VARIED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, G, STATUTORY_DECLARATION_GRANTED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, STDEC, STATUTORY_DECLARATION_GRANTED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, RFSD, STATUTORY_DECLARATION_REFUSED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, WDRN, STATUTORY_DECLARATION_WITHDRAWN)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, DISM, STATUTORY_DECLARATION_DISMISSED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, G, STATUTORY_DECLARATION_GRANTED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, STDEC, STATUTORY_DECLARATION_GRANTED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, RFSD, STATUTORY_DECLARATION_REFUSED)
            .add(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, WDRN, STATUTORY_DECLARATION_WITHDRAWN)
            .add(APPLICATION_TO_REOPEN_CASE, G, REOPENING_GRANTED)
            .add(APPLICATION_TO_REOPEN_CASE, ROPENED, REOPENING_GRANTED)
            .add(APPLICATION_TO_REOPEN_CASE, RFSD, REOPENING_REFUSED)
            .add(APPLICATION_TO_REOPEN_CASE, WDRN, REOPENING_WITHDRAWN)
            .add(APPLICATION_TO_REOPEN_CASE, DISM, REOPENING_DISMISSED)
            .build();

    // mapping by application type for backward compatibility.
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
        resultToActionMap.put(APPEAL_AGAINST_CONVICTION.getApplicationType() + APA.getResultId(), APPEAL_ABANDONED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE.getApplicationType() + APA.getResultId(), APPEAL_ABANDONED);
        resultToActionMap.put(APPEAL_AGAINST_SENTENCE_AND_CONVICTION.getApplicationType() + APA.getResultId(), APPEAL_ABANDONED);
    }

    // When an application has several recognised results, the favourable outcome wins:
    // granted/allowed, then refused/dismissed/sentence varied, then withdrawn/abandoned.
    private static final Set<ApplicationStatus> GRANTED_OR_ALLOWED = EnumSet.of(
            STATUTORY_DECLARATION_GRANTED, REOPENING_GRANTED, APPEAL_ALLOWED);
    private static final Set<ApplicationStatus> REFUSED_OR_DISMISSED = EnumSet.of(
            STATUTORY_DECLARATION_REFUSED, REOPENING_REFUSED, APPEAL_REFUSED,
            STATUTORY_DECLARATION_DISMISSED, REOPENING_DISMISSED, APPEAL_DISMISSED,
            APPLICATION_DISMISSED_SENTENCE_VARIED);

    private ApplicationResultStatusResolver() {
    }

    /**
     * Resolves the final status of a court application from all of its application-level results.
     * Unrecognised results are ignored; if none is recognised the result is empty, meaning the
     * current status (pending or a previous final status) must be kept.
     */
    public static Optional<String> getFinalApplicationStatus(final CourtApplication courtApplication) {
        final List<JudicialResult> judicialResults = courtApplication.getJudicialResults();
        final CourtApplicationType type = courtApplication.getType();
        if (judicialResults == null || type == null) {
            return Optional.empty();
        }

        return judicialResults.stream()
                .map(JudicialResult::getJudicialResultTypeId)
                .filter(Objects::nonNull)
                .map(resultDefinitionId -> resolve(type.getType(), resultDefinitionId, type.getCode()))
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(ApplicationResultStatusResolver::rank))
                .map(ApplicationStatus::toString);
    }

    /**
     * Maps a single application-level result to an application status, or {@code null} when the
     * result is not a recognised final outcome for the application type (e.g. adjournment, costs).
     */
    private static ApplicationStatus resolve(final String courtApplicationType, final UUID resultDefinitionId, final String courtApplicationCode) {
        final String resultId = resultDefinitionId.toString();
        return Optional.ofNullable(resultToActionMap.get(courtApplicationType + resultId))
                .orElse(resultToActionByCodeMap.get(courtApplicationCode + resultId));
    }

    private static int rank(final ApplicationStatus applicationStatus) {
        if (GRANTED_OR_ALLOWED.contains(applicationStatus)) {
            return 0;
        }
        if (REFUSED_OR_DISMISSED.contains(applicationStatus)) {
            return 1;
        }
        return 2;
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