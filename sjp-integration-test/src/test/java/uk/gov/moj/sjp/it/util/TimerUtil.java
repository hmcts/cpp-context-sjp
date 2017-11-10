package uk.gov.moj.sjp.it.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimerUtil.class);

    private static final int DEFAULT_DELAY_IN_MILLIS = 1000;

    public static void introduceDelay() {
        introduceDelay(DEFAULT_DELAY_IN_MILLIS);
    }

    public static void introduceDelay(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            LOGGER.warn("Error introducing delay", e);
        }
    }
}
