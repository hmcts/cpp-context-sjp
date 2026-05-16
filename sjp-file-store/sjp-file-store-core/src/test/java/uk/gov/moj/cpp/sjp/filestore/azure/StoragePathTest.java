package uk.gov.moj.cpp.sjp.filestore.azure;

import static java.util.UUID.fromString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class StoragePathTest {

    private static final UUID FILE_ID = fromString("a1b2c3d4-0000-0000-0000-000000000001");

    @Test
    public void shouldBuildInternalBlobName() {
        assertThat(StoragePath.internal().blobName(FILE_ID), is("internal/" + FILE_ID));
    }

    @Test
    public void shouldBuildPublishedBlobName() {
        assertThat(StoragePath.published("transparency-reports").blobName(FILE_ID),
                is("published/transparency-reports/" + FILE_ID));
    }

    @Test
    public void shouldBuildInboxBlobName() {
        assertThat(StoragePath.inbox("sdg-output").blobName(FILE_ID),
                is("inbox/sdg-output/" + FILE_ID));
    }

    @Test
    public void shouldReturnPrefix() {
        assertThat(StoragePath.internal().prefix(), is("internal"));
        assertThat(StoragePath.published("court-docs").prefix(), is("published/court-docs"));
        assertThat(StoragePath.inbox("sdg-output").prefix(), is("inbox/sdg-output"));
    }

    @Test
    public void shouldEqualWhenSamePrefix() {
        assertThat(StoragePath.internal().equals(StoragePath.internal()), is(true));
        assertThat(StoragePath.published("topic").equals(StoragePath.published("topic")), is(true));
        assertThat(StoragePath.internal().equals(StoragePath.published("topic")), is(false));
    }
}
