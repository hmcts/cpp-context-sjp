package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataToString;

import java.time.ZonedDateTime;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.Metadata;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.RuntimeService;
import org.slf4j.Logger;

@SuppressWarnings("WeakerAccess")
@Named
public class TimerExpirationProcess {

    private static final String TIMER_TIMEOUT = "timerTimeout";
    private static final Logger LOGGER = getLogger(TimerExpirationProcess.class);
    private final RuntimeService runtimeService;

    @Inject
    public TimerExpirationProcess(final RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public void startTimerForDelayAndCommand(final UUID caseId, final LocalDate expirationDay, final String commandToSend, final Metadata metadata) {
        final Map<String, Object> params = getCommonParams(metadata);
        params.put("expiration", ZonedDateTimes.toString(expirationDay.atStartOfDay(ZoneId.of("UTC"))));
        params.put("commandToSend", commandToSend);

        runtimeService.startProcessInstanceByKey(TIMER_TIMEOUT, caseId.toString(), params);

        LOGGER.info("{} command timeout started for case {}, expiration Time {}", commandToSend, caseId, expirationDay);
    }

    public void startTimerForDelayAndCommand(final UUID caseId, final ZonedDateTime expirationDay, final String commandToSend, final Metadata metadata) {
        final Map<String, Object> params = getCommonParams(metadata);
        params.put("expiration", ZonedDateTimes.toString(expirationDay));
        params.put("commandToSend", commandToSend);

        runtimeService.startProcessInstanceByKey(TIMER_TIMEOUT, caseId.toString(), params);

        LOGGER.info("{} command timeout started for case {}, expiration Time {}", commandToSend, caseId, expirationDay);
    }

    private static Map<String, Object> getCommonParams(final Metadata metadata) {
        final Map<String, Object> params = new HashMap<>();
        params.put(METADATA_VARIABLE, metadataToString(metadata));

        return params;
    }
}
