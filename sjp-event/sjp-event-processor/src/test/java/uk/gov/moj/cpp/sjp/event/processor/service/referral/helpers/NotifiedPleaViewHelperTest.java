package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.json.schemas.domains.sjp.PleaType.GUILTY;
import static uk.gov.justice.json.schemas.domains.sjp.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.justice.json.schemas.domains.sjp.PleaType.NOT_GUILTY;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Offence.offence;
import static uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers.NotifiedPleaViewHelper.createNotifiedPleaView;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.json.schemas.domains.sjp.PleaType;
import uk.gov.moj.cpp.sjp.model.prosecution.NotifiedPleaView;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class NotifiedPleaViewHelperTest {

    private static ZonedDateTime REFERRAL_DATE = ZonedDateTime.now();
    private static ZonedDateTime YESTERDAY = ZonedDateTime.now().minusDays(1);
    private static final UUID OFFENCE_ID = randomUUID();

    static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(GUILTY, YESTERDAY, "NOTIFIED_GUILTY", YESTERDAY.toLocalDate()),
                Arguments.of(GUILTY_REQUEST_HEARING, YESTERDAY, "NOTIFIED_GUILTY", YESTERDAY.toLocalDate()),
                Arguments.of(NOT_GUILTY, YESTERDAY, "NOTIFIED_NOT_GUILTY", YESTERDAY.toLocalDate()),
                Arguments.of(null, null, "NO_NOTIFICATION", REFERRAL_DATE.toLocalDate())
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldCreateNotifiedPleaView(PleaType plea, ZonedDateTime pleaDate, String notifiedPlea, LocalDate notifiedPleaDate) {
        final NotifiedPleaView notifiedPleaView = createNotifiedPleaView(
                REFERRAL_DATE.toLocalDate(),
                offence()
                        .withId(OFFENCE_ID)
                        .withPlea(plea)
                        .withPleaDate(pleaDate)
                        .build());

        assertThat(notifiedPleaView.getOffenceId(), is(OFFENCE_ID));
        assertThat(notifiedPleaView.getNotifiedPleaDate(), is(notifiedPleaDate));
        assertThat(notifiedPleaView.getNotifiedPleaValue(), is(notifiedPlea));
    }

}
