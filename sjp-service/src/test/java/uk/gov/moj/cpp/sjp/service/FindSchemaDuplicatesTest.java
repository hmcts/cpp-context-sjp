package uk.gov.moj.cpp.sjp.service;

import uk.gov.justice.services.test.utils.core.schema.SchemaDuplicateTestHelper;

import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class FindSchemaDuplicatesTest {

    @Test
    @Ignore
    public void shouldFindSchemaDuplicatesTest() {
        SchemaDuplicateTestHelper.failTestIfDifferentSchemasWithSameName();
    }
}
