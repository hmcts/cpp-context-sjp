package uk.gov.moj.cpp.sjp.event.processor.utils.fake;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.api.FileStorer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class FakeFileStorer implements FileStorer {

    private Map<UUID, Pair<JsonObject, InputStream>> storage = new HashMap<>();

    @Override
    public UUID store(final JsonObject metadata, final InputStream fileContentStream) throws FileServiceException {
        final UUID fileId = UUID.randomUUID();
        this.storage.put(fileId, new ImmutablePair<>(metadata, fileContentStream));
        return fileId;
    }

    @Override
    public void delete(final UUID fileId) throws FileServiceException {
        this.storage.remove(fileId);
    }

    public List<Pair<JsonObject, InputStream>> getAll() {
        return new ArrayList<>(this.storage.values());
    }
}
