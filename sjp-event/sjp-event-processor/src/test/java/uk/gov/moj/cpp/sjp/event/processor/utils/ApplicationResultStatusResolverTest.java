package uk.gov.moj.cpp.sjp.event.processor.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP;
import static uk.gov.moj.cpp.sjp.event.processor.utils.SjpApplicationTypes.APPLICATION_TO_REOPEN_CASE;

class ApplicationResultStatusResolverTest {

    private static Stream<Arguments> statdecApplicationTypes() {
        return Stream.of(Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, G, STATUTORY_DECLARATION_GRANTED),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, STDEC, STATUTORY_DECLARATION_GRANTED),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, RFSD, STATUTORY_DECLARATION_REFUSED),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, WDRN, STATUTORY_DECLARATION_WITHDRAWN),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, DISM, STATUTORY_DECLARATION_DISMISSED),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, G, STATUTORY_DECLARATION_GRANTED),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, STDEC, STATUTORY_DECLARATION_GRANTED),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, RFSD, STATUTORY_DECLARATION_REFUSED),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, WDRN, STATUTORY_DECLARATION_WITHDRAWN));
    }

    private static Stream<Arguments> reopenApplicationTypes() {
        return Stream.of(Arguments.of(APPLICATION_TO_REOPEN_CASE, G, REOPENING_GRANTED),
                Arguments.of(APPLICATION_TO_REOPEN_CASE, ROPENED, REOPENING_GRANTED),
                Arguments.of(APPLICATION_TO_REOPEN_CASE, RFSD, REOPENING_REFUSED),
                Arguments.of(APPLICATION_TO_REOPEN_CASE, WDRN, REOPENING_WITHDRAWN),
                Arguments.of(APPLICATION_TO_REOPEN_CASE, DISM, REOPENING_DISMISSED));
    }

    private static Stream<Arguments> appealApplicationTypes() {
        return Stream.of(Arguments.of(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AACA, APPEAL_ALLOWED),
                Arguments.of(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AASA, APPEAL_ALLOWED),
                Arguments.of(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AACD, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AASD, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, ACSD, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, DISM, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, WDRN, APPEAL_WITHDRAWN),
                Arguments.of(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AW, APPEAL_WITHDRAWN),
                Arguments.of(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, RFSD, APPEAL_REFUSED),
                Arguments.of(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, APA, APPEAL_ABANDONED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AACA, APPEAL_ALLOWED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AASA, APPEAL_ALLOWED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AACD, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AASD, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, ACSD, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, DISM, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, WDRN, APPEAL_WITHDRAWN),
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, AW, APPEAL_WITHDRAWN),
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, RFSD, APPEAL_REFUSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, APA, APPEAL_ABANDONED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, ASV, APPLICATION_DISMISSED_SENTENCE_VARIED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AACA, APPEAL_ALLOWED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AASA, APPEAL_ALLOWED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AACD, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AASD, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, ACSD, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, DISM, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, WDRN, APPEAL_WITHDRAWN),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AW, APPEAL_WITHDRAWN),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, RFSD, APPEAL_REFUSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, APA, APPEAL_ABANDONED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, ASV, APPLICATION_DISMISSED_SENTENCE_VARIED));
    }

    @ParameterizedTest()
    @MethodSource("statdecApplicationTypes")
    void testApplicationStatusForStatDecApplications(SjpApplicationTypes applicationType, ApplicationResult applicationResult, ApplicationStatus expectedApplicationStatus) {

        assertThat(ApplicationResultStatusResolver.getFinalApplicationStatus(courtApplication(applicationType, applicationResult.getResultId())), is(Optional.of(expectedApplicationStatus.toString())));
    }

    @ParameterizedTest()
    @MethodSource("reopenApplicationTypes")
    void testApplicationStatusForReopenApplications(SjpApplicationTypes applicationType, ApplicationResult applicationResult, ApplicationStatus expectedApplicationStatus) {

        assertThat(ApplicationResultStatusResolver.getFinalApplicationStatus(courtApplication(applicationType, applicationResult.getResultId())), is(Optional.of(expectedApplicationStatus.toString())));
    }

    @ParameterizedTest()
    @MethodSource("appealApplicationTypes")
    void testApplicationStatusForAppealApplications(SjpApplicationTypes applicationType, ApplicationResult applicationResult, ApplicationStatus expectedApplicationStatus) {

        assertThat(ApplicationResultStatusResolver.getFinalApplicationStatus(courtApplication(applicationType, applicationResult.getResultId())), is(Optional.of(expectedApplicationStatus.toString())));
    }

    private static Stream<Arguments> unrecognisedResults() {
        return Stream.of(
                // DISM is not mapped for stat dec other than SJP (criminal court only, unchanged)
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, DISM.getResultId()),
                // ASV is not mapped for sentence-only appeals
                Arguments.of(APPEAL_AGAINST_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, ASV.getResultId()),
                // ancillary / adjournment results
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, randomUUID().toString()),
                Arguments.of(APPLICATION_TO_REOPEN_CASE, randomUUID().toString()),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, randomUUID().toString()));
    }

    private static Stream<Arguments> multipleResults() {
        final String ancillary = randomUUID().toString();
        return Stream.of(
                // ancillary result first must not hide the decision (DD-19109)
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, new String[]{ancillary, G.getResultId()}, STATUTORY_DECLARATION_GRANTED),
                Arguments.of(APPLICATION_TO_REOPEN_CASE, new String[]{ancillary, RFSD.getResultId()}, REOPENING_REFUSED),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, new String[]{ancillary, DISM.getResultId()}, STATUTORY_DECLARATION_DISMISSED),
                // several recognised results: the first recognised one wins
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, new String[]{AACD.getResultId(), AASA.getResultId()}, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_AND_SENTENCE_BY_MAGISTRATE_TO_CROWN_COURT, new String[]{ancillary, AASA.getResultId(), AACD.getResultId()}, APPEAL_ALLOWED),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, new String[]{WDRN.getResultId(), RFSD.getResultId()}, STATUTORY_DECLARATION_WITHDRAWN),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, new String[]{APA.getResultId(), ASV.getResultId()}, APPEAL_ABANDONED));
    }

    @ParameterizedTest()
    @MethodSource("unrecognisedResults")
    void shouldReturnEmptyForUnrecognisedResult(SjpApplicationTypes applicationType, String resultDefinitionId) {

        assertThat(ApplicationResultStatusResolver.getFinalApplicationStatus(courtApplication(applicationType, resultDefinitionId)), is(Optional.empty()));
    }

    @ParameterizedTest()
    @MethodSource("multipleResults")
    void shouldResolveFinalStatusFromAllApplicationResults(SjpApplicationTypes applicationType, String[] resultDefinitionIds, ApplicationStatus expectedApplicationStatus) {

        assertThat(ApplicationResultStatusResolver.getFinalApplicationStatus(courtApplication(applicationType, resultDefinitionIds)),
                is(Optional.of(expectedApplicationStatus.toString())));
    }

    @Test
    void shouldReturnEmptyWhenNoResultIsRecognised() {

        assertThat(ApplicationResultStatusResolver.getFinalApplicationStatus(courtApplication(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_SJP, randomUUID().toString(), randomUUID().toString())),
                is(Optional.empty()));
    }

    @Test
    void shouldReturnEmptyWhenThereAreNoApplicationResults() {

        assertThat(ApplicationResultStatusResolver.getFinalApplicationStatus(courtApplication(APPLICATION_TO_REOPEN_CASE)), is(Optional.empty()));
        assertThat(ApplicationResultStatusResolver.getFinalApplicationStatus(CourtApplication.courtApplication()
                .withType(new CourtApplicationType.Builder().withCode(APPLICATION_TO_REOPEN_CASE.getApplicationCode()).build())
                .build()), is(Optional.empty()));
    }

    private static CourtApplication courtApplication(final SjpApplicationTypes applicationType, final String... resultDefinitionIds) {
        return CourtApplication.courtApplication()
                .withId(randomUUID())
                .withType(new CourtApplicationType.Builder()
                        .withType(applicationType.getApplicationType())
                        .withCode(applicationType.getApplicationCode())
                        .build())
                .withJudicialResults(resultDefinitionIds.length == 0 ? emptyList() : Arrays.stream(resultDefinitionIds)
                        .map(id -> new JudicialResult.Builder().withJudicialResultTypeId(UUID.fromString(id)).build())
                        .collect(Collectors.toList()))
                .build();
    }
}
