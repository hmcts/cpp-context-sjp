package uk.gov.moj.cpp.sjp.event;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.reflections.Reflections;

@RunWith(Parameterized.class)
public class AllEventsTest {

    @Parameter(0)
    public Class<?> eventClass;

    @Parameters(name = "{0}")
    public static Collection<Class<?>> data() {
        return new Reflections(AllEventsTest.class.getPackage())
                .getTypesAnnotatedWith(Event.class);
    }

    @Test
    public void shouldNotOverrideEquals() {
        final boolean hasEquals = ReflectionUtil.methodOf(eventClass, "equals").isPresent();

        assertThat(format("Event %s should not override `equals` method.", eventClass.getSimpleName()),
                hasEquals, is(false));
    }

    @Test
    public void shouldNotOverrideHashCode() {
        final boolean hasHashCode = ReflectionUtil.methodOf(eventClass, "hashCode").isPresent();

        assertThat(format("Event %s should not override `hashCode` method.", eventClass.getSimpleName()),
                hasHashCode, is(false));
    }

}
