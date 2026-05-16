package uk.gov.moj.cpp.sjp.event.processor.utils.fake;

import uk.gov.moj.cpp.sjp.filestore.azure.SasUriGenerator;
import uk.gov.moj.cpp.sjp.filestore.azure.StoragePath;

import java.net.URI;
import java.util.UUID;

public class FakeSasUriGenerator extends SasUriGenerator {

    @Override
    public URI generateReadUri(final StoragePath storagePath, final UUID fileId) {
        return URI.create("https://fake.blob.core.windows.net/sjp-files/" + storagePath.blobName(fileId) + "?sv=fake");
    }
}
