package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class DefendantTitleParserTest {

    private final String DEFAULT_TITLE = "MR";

    private final String  MS = "MS";

    private final String  MISS = "MISS";

    private final String  MRS ="MRS";

    @Test
    public void shouldDefaultForBlank(){
        final String title = DefendantTitleParser.parse("");
        assertThat(title, is(DEFAULT_TITLE));
    }

    @Test
    public void shouldDefaultIfNoMatch(){
        final String title = DefendantTitleParser.parse("Doctor");
        assertThat(title, is(DEFAULT_TITLE));
    }

    @Test
    public void shouldDefaultForNullValue(){
        final String title = DefendantTitleParser.parse(null);
        assertThat(title, is(DEFAULT_TITLE));
    }

    @Test
    public void shouldTrimBeforeParse(){
        final String title = DefendantTitleParser.parse("  ms   ");
        assertThat(title, is(MS));
    }

    @Test
    public void shouldBeCaseInsensitive(){
        final String title = DefendantTitleParser.parse("Miss");
        assertThat(title, is(MISS));
    }

    @Test
    public void shouldMapToCorrectTitle(){
        final String title = DefendantTitleParser.parse("MRs");
        assertThat(title, is(MRS));
    }

    @Test
    public void shouldReturnValidTitleList() throws NoSuchFieldException, IllegalAccessException {
        Field validTitleListField = DefendantTitleParser.class.getDeclaredField( "VALID_TITLES" );
        validTitleListField.setAccessible(true);
        List<String> validTitleList = (List<String>) validTitleListField.get(new ArrayList());
        List<String> expectedTitleList = asList("MR",  "MS", "MISS", "MRS");
        validTitleListField.setAccessible(false);
        assertThat(validTitleList.size(), is(4));
        assertThat(validTitleList.containsAll(expectedTitleList), is(TRUE));
    }
}
