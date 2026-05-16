package uk.gov.moj.cpp.sjp.filestore.azure;

public class AzureBlobContainerClientCreationException extends RuntimeException {

    public AzureBlobContainerClientCreationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
