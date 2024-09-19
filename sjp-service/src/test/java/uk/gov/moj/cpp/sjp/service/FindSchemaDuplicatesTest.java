package uk.gov.moj.cpp.sjp.service;

import org.junit.jupiter.api.Disabled;
import uk.gov.justice.services.test.utils.core.schema.SchemaDuplicateTestHelper;

import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class FindSchemaDuplicatesTest {

    @Test
    @Disabled
    public void shouldFindSchemaDuplicatesTest() {
        SchemaDuplicateTestHelper.failTestIfDifferentSchemasWithSameName();
    }
}
