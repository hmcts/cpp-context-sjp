package uk.gov.moj.cpp.sjp.command.controller;

import static java.lang.reflect.Modifier.isPublic;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.services.core.annotation.Handles;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.google.common.base.CaseFormat;
import org.junit.jupiter.api.Test;

public class NoActionControllerTest {

    @Test
    public void shouldHaveCommandAnnotation() {
        assertThat(NoActionController.class, isHandlerClass(COMMAND_CONTROLLER));
    }

    @Test
    public void verifyPublicMethodsNames() {
        final Method[] methods = NoActionController.class.getDeclaredMethods();

        Arrays.stream(methods)
                .filter(method -> isPublic(method.getModifiers()))
                .forEach(method -> {
                    assertThat(method.isAnnotationPresent(Handles.class), is(true));

                    final String commandName = method.getAnnotation(Handles.class).value();
                    final String methodName = extractMethodName(commandName).intern();

                    assertThat(NoActionController.class, method(methodName)
                                    .thatHandles(commandName)
                                    .withSenderPassThrough());
                });
    }

    private static String extractMethodName(final String commandName) {
        assertThat(commandName, startsWith("sjp.command."));

        return CaseFormat.LOWER_HYPHEN.to(
                CaseFormat.LOWER_CAMEL,
                commandName.replaceFirst("sjp.command.", ""));
    }

}