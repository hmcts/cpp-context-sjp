package uk.gov.moj.cpp.sjp.event.processor.activiti;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.activiti.engine.impl.test.JobTestHelper.waitForJobExecutorToProcessAllJobs;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.MockedDelegate;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.test.ActivitiRule;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DelegatesVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelegatesVerifier.class);

    enum Delegate {
        CASE_STARTED,
        PROVED_IN_ABSENCE,
        READY_CASE,
        PLEA_UPDATED,
        PLEA_CANCELLED,
        DATES_TO_AVOID_PROCESSED,
        WITHDRAWAL_REQUESTED,
        WITHDRAWAL_REQUEST_CANCELLED,
        ASSIGNMENT_TIMED_OUT,
        CASE_COMPLETED;

        /**
         * @return bean name
         */
        @Override
        public String toString() {
            return UPPER_UNDERSCORE.to(LOWER_CAMEL, this.name()) + "Delegate";
        }
    }

    private final ActivitiRule rule;

    public DelegatesVerifier(final ActivitiRule rule) {
        this.rule = rule;

        stream(DelegatesVerifier.Delegate.values()).map(this::getDelegateExecution).forEach(MockedDelegate::resetExecutions);
    }

    public MockedDelegate getDelegateExecution(final Delegate delegateName) {
        return (MockedDelegate) ((StandaloneProcessEngineConfiguration) rule.getProcessEngine().getProcessEngineConfiguration()).getBeans()
                .get(delegateName.toString());
    }

    public void assertDelegateCalledWith(final Delegate delegateName, final int index, final Matcher<DelegateExecution> matchers) {
        final MockedDelegate delegateExecution = getDelegateExecution(delegateName);

        assertThat(
                format("Expected execution #%s of %s never happened", index, delegateName),
                delegateExecution.getExecutionTimes(),
                greaterThan(index));
        assertThat(delegateExecution.getDelegateExecutions().get(index), matchers);
    }

    public boolean isActivitiItemRunning(final String processInstanceId, final String processName) {
        LOGGER.info("Available processes: " + rule.getRuntimeService()
                .createExecutionQuery()
                .processInstanceId(processInstanceId)
                .list()
                .stream()
                .map(Execution::getActivityId)
                .filter(StringUtils::isNotEmpty)
                .collect(toList()));

        return rule.getRuntimeService()
                .createExecutionQuery()
                .processInstanceId(processInstanceId)
                .activityId(processName)
                .count() > 0;
    }

    public void assertNumberEventSubscriptions(final String processInstanceId, final String signalName, final Matcher<Integer> matcher) {
        final int countEventSubscriptions = Long.valueOf(
                rule.getRuntimeService()
                        .createExecutionQuery()
                        .processInstanceId(processInstanceId)
                        .signalEventSubscriptionName(signalName)
                        .count()
        ).intValue();

        assertThat(countEventSubscriptions, matcher);
    }

    public void verifyDelegatesInteractionWith(final Delegate... expectedDelegates) {
        stream(Delegate.values())
                .forEach(delegateName -> {
                    int executionTimes = getDelegateExecution(delegateName).getExecutionTimes();
                    assertThat(
                            delegateName + " executed unexpected number of times",
                            executionTimes,
                            (contains(expectedDelegates, delegateName) ? greaterThan(0) : is(0)));
                });
    }

    public void verifyNumberOfExecution(final Delegate delegateName, final int times) {
        assertThat(delegateName + " executed unexpected number of times", getDelegateExecution(delegateName).getExecutionTimes(), is(times));
    }

    public void assertProcessFinished(final String processInstanceId, final boolean expectFinished) {
        final ProcessInstance processInstance = rule.getProcessEngine()
                .getRuntimeService()
                .createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        assertThat(processInstance == null, is(expectFinished));
    }

    public void tryProcessPendingJobs() {
        try {
            waitForJobExecutorToProcessAllJobs(rule, 10000, 10);
        } catch (final ActivitiException e) {
            LOGGER.warn("Exception thrown while waiting for jobs to be processed", e);
        }
    }

}
