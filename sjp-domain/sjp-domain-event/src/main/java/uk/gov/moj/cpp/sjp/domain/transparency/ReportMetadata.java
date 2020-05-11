package uk.gov.moj.cpp.sjp.domain.transparency;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ReportMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fileName;
    private Integer numberOfPages;
    private Integer fileSize;
    private UUID fileId;

    @JsonCreator
    public ReportMetadata(@JsonProperty("fileName") final String fileName,
                          @JsonProperty("numberOfPages") final Integer numberOfPages,
                          @JsonProperty("fileSize") final Integer fileSize,
                          @JsonProperty("fileId") final UUID fileId) {
        this.fileName = fileName;
        this.numberOfPages = numberOfPages;
        this.fileSize = fileSize;
        this.fileId = fileId;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public Integer getNumberOfPages() {
        return numberOfPages;
    }

    public String getFileName() {
        return fileName;
    }

    public UUID getFileId() {
        return fileId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportMetadata)) {
            return false;
        }
        final ReportMetadata that = (ReportMetadata) o;
        return fileName.equals(that.fileName) &&
                numberOfPages.equals(that.numberOfPages) &&
                fileSize.equals(that.fileSize) &&
                Objects.equals(fileId, that.fileId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, numberOfPages, fileSize, fileId);
    }
}
