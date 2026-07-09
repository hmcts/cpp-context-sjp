package uk.gov.moj.cpp.sjp.event.processor.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;

import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPEAL_ABANDONED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPEAL_ALLOWED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPEAL_DISMISSED;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus.APPEAL_REFUSED;
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
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, G, STATUTORY_DECLARATION_GRANTED),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, STDEC, STATUTORY_DECLARATION_GRANTED),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, RFSD, STATUTORY_DECLARATION_REFUSED),
                Arguments.of(APPEARANCE_TO_MAKE_STATUTORY_DECLARATION_OTHER_THAN_SJP, WDRN, STATUTORY_DECLARATION_WITHDRAWN));
    }

    private static Stream<Arguments> reopenApplicationTypes() {
        return Stream.of(Arguments.of(APPLICATION_TO_REOPEN_CASE, G, REOPENING_GRANTED),
                Arguments.of(APPLICATION_TO_REOPEN_CASE, ROPENED, REOPENING_GRANTED),
                Arguments.of(APPLICATION_TO_REOPEN_CASE, RFSD, REOPENING_REFUSED),
                Arguments.of(APPLICATION_TO_REOPEN_CASE, WDRN, REOPENING_WITHDRAWN));
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
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AACA, APPEAL_ALLOWED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AASA, APPEAL_ALLOWED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AACD, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AASD, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, ACSD, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, DISM, APPEAL_DISMISSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, WDRN, APPEAL_WITHDRAWN),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, AW, APPEAL_WITHDRAWN),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, RFSD, APPEAL_REFUSED),
                Arguments.of(APPEAL_AGAINST_CONVICTION_BY_MAGISTRATE_TO_CROWN_COURT, APA, APPEAL_ABANDONED));
    }

    @ParameterizedTest()
    @MethodSource("statdecApplicationTypes")
    void testApplicationStatusForStatDecApplications(SjpApplicationTypes applicationType, ApplicationResult applicationResult, ApplicationStatus expectedApplicationStatus) {

        assertThat(ApplicationResultStatusResolver.getApplicationStatus(applicationType.getApplicationType(), UUID.fromString(applicationResult.getResultId()), applicationType.getApplicationCode()), is(expectedApplicationStatus.toString()));
    }

    @ParameterizedTest()
    @MethodSource("reopenApplicationTypes")
    void testApplicationStatusForReopenApplications(SjpApplicationTypes applicationType, ApplicationResult applicationResult, ApplicationStatus expectedApplicationStatus) {

        assertThat(ApplicationResultStatusResolver.getApplicationStatus(applicationType.getApplicationType(), UUID.fromString(applicationResult.getResultId()), applicationType.getApplicationCode()), is(expectedApplicationStatus.toString()));
    }

    @ParameterizedTest()
    @MethodSource("appealApplicationTypes")
    void testApplicationStatusForAppealApplications(SjpApplicationTypes applicationType, ApplicationResult applicationResult, ApplicationStatus expectedApplicationStatus) {

        assertThat(ApplicationResultStatusResolver.getApplicationStatus(applicationType.getApplicationType(), UUID.fromString(applicationResult.getResultId()), applicationType.getApplicationCode()), is(expectedApplicationStatus.toString()));
    }
}
