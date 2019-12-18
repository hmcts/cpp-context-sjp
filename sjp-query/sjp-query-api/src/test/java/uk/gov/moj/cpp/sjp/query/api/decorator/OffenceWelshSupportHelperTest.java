package uk.gov.moj.cpp.sjp.query.api.decorator;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.json.JsonObject;

import org.junit.Test;

public class OffenceWelshSupportHelperTest {

    private final OffenceHelper offenceHelper = new OffenceHelper();

    @Test
    public void shouldReturnWelshTileAndLegislation() {
        final String welshTitle = "Teitl";
        final String welshLegislation = "Deddfwriaeth";

        final JsonObject offenceDefinition = createObjectBuilder()
                .add("details", createObjectBuilder()
                        .add("document", createObjectBuilder()
                                .add("welsh", createObjectBuilder()
                                        .add("welshoffencetitle", welshTitle)
                                        .add("welshlegislation", welshLegislation)))).build();

        assertThat(offenceHelper.getWelshTitle(offenceDefinition).get(), is(welshTitle));
        assertThat(offenceHelper.getWelshLegislation(offenceDefinition).get(), is(welshLegislation));
    }

    @Test
    public void shouldReturnEmptyWelshTileAndLegislationIfNotPresent() {
        final JsonObject offenceDefinition = createObjectBuilder()
                .add("details", createObjectBuilder()
                        .add("document", createObjectBuilder()
                                .add("welsh", createObjectBuilder()))).build();

        assertThat(offenceHelper.getWelshTitle(offenceDefinition).isPresent(), is(false));
        assertThat(offenceHelper.getWelshLegislation(offenceDefinition).isPresent(), is(false));
    }

    @Test
    public void shouldReturnEmptyWelshTileAndLegislationIfWelshNotSupported() {
        final JsonObject offenceDefinition = createObjectBuilder()
                .add("details", createObjectBuilder()
                        .add("document", createObjectBuilder())).build();

        assertThat(offenceHelper.getWelshTitle(offenceDefinition).isPresent(), is(false));
        assertThat(offenceHelper.getWelshLegislation(offenceDefinition).isPresent(), is(false));
    }
}
