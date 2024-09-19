package uk.gov.moj.cpp.sjp.query.view;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class ExportTypeTest {

    @Test
    public void shouldParseValues() {
        assertThat(ExportType.of("public"), equalTo(ExportType.PUBLIC));
        assertThat(ExportType.of("PUBLIC"), equalTo(ExportType.PUBLIC));
        assertThat(ExportType.of(" Public "), equalTo(ExportType.PUBLIC));
        assertThat(ExportType.of("press"), equalTo(ExportType.PRESS));
        assertThat(ExportType.of("PRESS"), equalTo(ExportType.PRESS));
        assertThat(ExportType.of(" Press "), equalTo(ExportType.PRESS));
    }

    @Test
    public void shouldReturnPublicForUnknownTypes() {
        assertThat(ExportType.of("Unknown"), equalTo(ExportType.PUBLIC));
        assertThat(ExportType.of(null), equalTo(ExportType.PUBLIC));
    }
}
