package uk.gov.justice.api.resource;

import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.api.resource.DefaultQueryApiTransparencyReportContentFileIdResource.PDF_CONTENT_TYPE;

import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.domain.FileReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

public class ResourceUtility {

    static Response getResponse(final FileRetriever fileRetriever, final UUID fileId, final String fileName) {
        final Optional<FileReference> fileRetrieverOptional;

        try {
            fileRetrieverOptional = fileRetriever.retrieve(fileId);
        } catch (FileServiceException fileServiceException) {
            throw new RuntimeException(fileServiceException);
        }

        final Response.ResponseBuilder responseBuilder;

        if (fileRetrieverOptional.isPresent()) {

            try (final FileReference fileReference = fileRetrieverOptional.get();
                 InputStream contentStream = fileReference.getContentStream()) {
                responseBuilder = status(OK).entity(contentStream);
                responseBuilder.header(CONTENT_TYPE, PDF_CONTENT_TYPE);
                responseBuilder.header(CONTENT_DISPOSITION, fileName + fileId.toString() + ".pdf");
                
            } catch (IOException e) {
                throw new RuntimeException("Error while processing the File Retriever: " , e);
            }

        } else {
            throw new RuntimeException("No File present for the fileId : " + fileId.toString());
        }

        return responseBuilder.build();
    }
}
