package uk.gov.moj.sjp.it.util;


import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.http.RestPoller;

public class RestPollerWithDefaults {

    public static final int DELAY_IN_MILLIS = 0;
    public static final int INTERVAL_IN_MILLIS = 100;

    public static RestPoller pollWithDefaults(final RequestParamsBuilder requestParamsBuilder) {
        return pollWithDefaults(requestParamsBuilder.build());
    }

    public static RestPoller pollWithDefaults(final RequestParams requestParams) {
        return poll(requestParams)
                .pollDelay(DELAY_IN_MILLIS, MILLISECONDS)
                .pollInterval(INTERVAL_IN_MILLIS, MILLISECONDS);
    }

    private RestPollerWithDefaults() {
    }
}
