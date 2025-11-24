package uk.gov.moj.cpp.sjp.persistence.entity;

import static java.util.UUID.randomUUID;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "press_transparency_report_metadata")
public class PressTransparencyReportMetadata {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "file_service_id")
    private UUID fileServiceId;

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @Column(name = "size_in_bytes")
    private Integer sizeInBytes;

    @Column(name = "document_format")
    private String documentFormat;

    @Column(name = "document_request_type")
    private String documentRequestType;

    @Column(name = "title")
    private String title;

    @Column(name = "language")
    private String language;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    // for JPA
    public PressTransparencyReportMetadata() {

    }

    public PressTransparencyReportMetadata(final UUID fileServiceId,
                                           final Integer englishNumberOfPages,
                                           final Integer sizeInBytes,
                                           final String documentFormat,
                                           final String documentRequestType,
                                           final String title,
                                           final String language,
                                           final LocalDateTime generatedAt) {
        this.id = randomUUID();
        this.fileServiceId = fileServiceId;
        this.numberOfPages = englishNumberOfPages;
        this.sizeInBytes = sizeInBytes;
        this.documentFormat = documentFormat;
        this.documentRequestType = documentRequestType;
        this.title = title;
        this.language = language;
        this.generatedAt = generatedAt;
    }

    public PressTransparencyReportMetadata(final UUID id, final LocalDateTime generatedAt) {
        this.id = id;
        this.generatedAt = generatedAt;
    }

    public PressTransparencyReportMetadata(final UUID id, final String documentFormat, final String documentRequestType, final String title,
                                           final String language, final LocalDateTime generatedAt) {
        this.id = id;
        this.documentFormat = documentFormat;
        this.documentRequestType = documentRequestType;
        this.title = title;
        this.language = language;
        this.generatedAt = generatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getFileServiceId() {
        return fileServiceId;
    }

    public void setFileServiceId(final UUID fileServiceId) {
        this.fileServiceId = fileServiceId;
    }

    public Integer getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(final Integer numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public Integer getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(final Integer sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public String getDocumentFormat() {
        return documentFormat;
    }

    public void setDocumentFormat(final String documentFormat) {
        this.documentFormat = documentFormat;
    }

    public void setDocumentRequestType(final String documentRequestType) {
        this.documentRequestType = documentRequestType;
    }

    public String getDocumentRequestType() {
        return documentRequestType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public void setGeneratedAt(final LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
