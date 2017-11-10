package uk.gov.moj.sjp.it.util;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MethodPoller<T> {

    private Duration timeout;

    private Duration interval;

    private Supplier<T> pollMethod;

    private Predicate<T> pollResultPredicate;

    public MethodPoller(Duration timeout, Duration interval) {
        this.timeout = timeout;
        this.interval = interval;
    }

    public MethodPoller<T> poll(Supplier<T> pollMethod) {
        this.pollMethod = pollMethod;
        return this;
    }

    public MethodPoller<T> until(Predicate<T> pollResultPredicate) {
        this.pollResultPredicate = pollResultPredicate;
        return this;
    }

    public T execute() {
        T result = null;

        boolean pollSucceeded = false;
        long start = System.currentTimeMillis();

        while (!pollSucceeded) {
            if (System.currentTimeMillis() - start > timeout.toMillis()) {
                break;
            }
            try {
                Thread.sleep(interval.toMillis());
            } catch (InterruptedException e) {
                // ignore
            }

            result = pollMethod.get();
            pollSucceeded = pollResultPredicate.test(result);
        }
        return result;
    }
}