package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class DefendantTitleParserTest {

    private final String MR = "MR";

    private final String  MS = "MS";

    private final String  MISS = "MISS";

    private final String  MRS ="MRS";

    @Test
    public void shouldDefaultForBlank(){
        final String title = DefendantTitleParser.parse("");
        assertThat(title, is(MR));
    }

    @Test
    public void shouldDefaultIfNoMatch(){
        final String title = DefendantTitleParser.parse("Doctor");
        assertThat(title, is(MR));
    }

    @Test
    public void shouldDefaultForNullValue(){
        final String title = DefendantTitleParser.parse(null);
        assertThat(title, is(MR));
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
}
