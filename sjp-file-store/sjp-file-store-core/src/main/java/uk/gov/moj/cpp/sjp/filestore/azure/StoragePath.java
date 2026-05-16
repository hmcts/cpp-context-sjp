package uk.gov.moj.cpp.sjp.filestore.azure;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.UUID;

public class StoragePath {

    private final String prefix;

    private StoragePath(final String prefix) {
        this.prefix = prefix;
    }

    public static StoragePath internal() {
        return new StoragePath("internal");
    }

    public static StoragePath published(final String topic) {
        requireNonNull(topic, "topic must not be null");
        return new StoragePath("published/" + topic);
    }

    public static StoragePath inbox(final String topic) {
        requireNonNull(topic, "topic must not be null");
        return new StoragePath("inbox/" + topic);
    }

    public String blobName(final UUID fileId) {
        return prefix + "/" + fileId;
    }

    public String prefix() {
        return prefix;
    }

    @Override
    public String toString() {
        return prefix;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (!(other instanceof StoragePath)) return false;
        return Objects.equals(prefix, ((StoragePath) other).prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix);
    }
}
