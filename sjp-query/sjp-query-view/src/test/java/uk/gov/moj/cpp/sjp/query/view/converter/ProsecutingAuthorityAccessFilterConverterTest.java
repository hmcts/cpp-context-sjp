package uk.gov.moj.cpp.sjp.query.view.converter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(value = Parameterized.class)
public class ProsecutingAuthorityAccessFilterConverterTest {

    private static final String PROSECUTING_AUTHORITY = "PROC1";

    @Parameterized.Parameter(0)
    public ProsecutingAuthorityAccess prosecutingAuthorityAccess;

    @Parameterized.Parameter(1)
    public String expectedFilterValue;

    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;

    @Parameterized.Parameters(name = "{index}: prosecutingAuthorityAccess={0}, expectedFilterValue={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {ProsecutingAuthorityAccess.NONE, null},
                {ProsecutingAuthorityAccess.of(PROSECUTING_AUTHORITY), PROSECUTING_AUTHORITY},
                {ProsecutingAuthorityAccess.ALL, "%"}
        });
    }

    @Before
    public void init() {
        prosecutingAuthorityAccessFilterConverter = new ProsecutingAuthorityAccessFilterConverter();
    }

    @Test
    public void shouldConvertToProsecutingAuthorityAccessFilterValue() {
        assertThat(prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityAccess),
                equalTo(expectedFilterValue));
    }
}
