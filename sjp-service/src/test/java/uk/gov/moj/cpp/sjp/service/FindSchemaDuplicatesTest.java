package uk.gov.moj.cpp.sjp.service;

import uk.gov.justice.services.test.utils.core.schema.SchemaDuplicateTestHelper;

public class FindSchemaDuplicatesTest {

    public void shouldFindSchemaDuplicatesTest() {
        SchemaDuplicateTestHelper.failTestIfDifferentSchemasWithSameName();
    }
}
