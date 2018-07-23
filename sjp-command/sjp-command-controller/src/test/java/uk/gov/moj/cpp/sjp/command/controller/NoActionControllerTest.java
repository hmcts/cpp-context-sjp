package uk.gov.moj.cpp.sjp.command.controller;

import static java.lang.String.format;
import static java.lang.reflect.Modifier.isPublic;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.google.common.base.CaseFormat;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NoActionControllerTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private NoActionController controller;

    @Test
    public void shouldHaveCommandAnnotation() {
        assertThat(NoActionController.class.isAnnotationPresent(ServiceComponent.class), is(true));
        assertThat(NoActionController.class.getAnnotation(ServiceComponent.class).value(), is(COMMAND_CONTROLLER));
    }

    @Test
    public void verifyPulicMethodsNames() throws Exception {
        Method[] methods = NoActionController.class.getDeclaredMethods();

        Arrays.stream(methods)
                .filter(method -> isPublic(method.getModifiers()))
                .forEach(method -> {
                    assertThat(
                            format("%s annotation not present for %s.%s", Handles.class.getSimpleName(), NoActionController.class.getSimpleName(), method.getName()),
                            method.isAnnotationPresent(Handles.class), is(true));

                    assertThat(method.getName(), hasRightMethodName(method.getAnnotation(Handles.class).value()));

                    try {
                        checkCommonSenderIsCalledWithUntouchedPayload(method);
                    } catch (Exception e) {
                        fail(format("Controller Implementation of %s not valid. Please use \"send(envelope);\". %s", method.getName(), e.getMessage()));
                    }
                });
    }

    private static Matcher<String> hasRightMethodName(final String commandName) {
        assertThat(commandName, startsWith("sjp.command."));

        final String expectedMethodName = CaseFormat.LOWER_HYPHEN.to(
                CaseFormat.LOWER_CAMEL,
                commandName.replaceFirst("sjp.command.", ""));

        return equalTo(expectedMethodName);
    }

    private void checkCommonSenderIsCalledWithUntouchedPayload(Method controllerMethodName) throws Exception {
        final JsonEnvelope mockEnvelope = mock(JsonEnvelope.class);

        controllerMethodName.invoke(controller, mockEnvelope);

        verify(sender).send(mockEnvelope);
    }

}