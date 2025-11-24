package uk.gov.moj.cpp.sjp.domain;

import static io.netty.util.internal.StringUtil.EMPTY_STRING;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class InterpreterTest {

    private static final String INTERPRETER_LANGUAGE = "French";
    private static final Interpreter NULL_INTERPRETER = Interpreter.of(null);
    private static final Interpreter INTERPRETER = Interpreter.of(INTERPRETER_LANGUAGE);

    @Test
    public void shouldEmptyInterpreterShareSameInstance() {
        final Interpreter RECREATION_NULL_INTERPRETER_INSTANCE = Interpreter.of(null);

        assertThat(NULL_INTERPRETER, sameInstance(RECREATION_NULL_INTERPRETER_INSTANCE));
        assertThat(NULL_INTERPRETER, sameInstance(Interpreter.of(EMPTY_STRING)));
    }

    @Test
    public void shouldEmptyInterpreterNotBeNeeded() {
        assertThat(NULL_INTERPRETER.isNeeded(), is(false));
    }

    @Test
    public void testInterpreter() {
        assertThat(INTERPRETER, not(sameInstance(Interpreter.of(INTERPRETER_LANGUAGE))));
        assertThat(INTERPRETER.isNeeded(), is(true));
        assertThat(INTERPRETER.getLanguage(), is(INTERPRETER_LANGUAGE));
    }

    @Test
    public void testStaticIsNeeded() {
        assertThat(Interpreter.isNeeded(null), is(false));
        assertThat(Interpreter.isNeeded(""), is(false));
        assertThat(Interpreter.isNeeded("any-string"), is(true));
    }

}