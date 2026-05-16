package uk.gov.moj.cpp.sjp.event.processor.utils.fake;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.sjp.filestore.azure.FileStorer;
import uk.gov.moj.cpp.sjp.filestore.azure.StoragePath;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FakeFileStorer extends FileStorer {

    public static class StoredFile {
        private final StoragePath storagePath;
        private final UUID correlationId;
        private final String filename;
        private final byte[] content;
        private final UUID fileId;

        public StoredFile(final StoragePath storagePath, final UUID correlationId, final String filename, final byte[] content, final UUID fileId) {
            this.storagePath = storagePath;
            this.correlationId = correlationId;
            this.filename = filename;
            this.content = content;
            this.fileId = fileId;
        }

        public StoragePath getStoragePath() { return storagePath; }
        public UUID getCorrelationId() { return correlationId; }
        public String getFilename() { return filename; }
        public byte[] getContent() { return content; }
        public UUID getFileId() { return fileId; }
    }

    private final List<StoredFile> stored = new ArrayList<>();

    @Override
    public UUID store(final StoragePath storagePath, final UUID correlationId, final String filename, final InputStream content) {
        try {
            final UUID fileId = randomUUID();
            stored.add(new StoredFile(storagePath, correlationId, filename, content.readAllBytes(), fileId));
            return fileId;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<StoredFile> getAll() {
        return new ArrayList<>(stored);
    }
}
