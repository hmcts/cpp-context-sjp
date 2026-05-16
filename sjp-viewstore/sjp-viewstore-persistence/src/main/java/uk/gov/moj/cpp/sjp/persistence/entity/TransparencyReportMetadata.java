package uk.gov.moj.cpp.sjp.persistence.entity;

import static java.util.UUID.randomUUID;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "transparency_report_metadata")
public class TransparencyReportMetadata {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "english_file_service_id")
    private UUID englishBlobFileId;

    @Column(name = "english_number_of_pages")
    private Integer englishNumberOfPages;

    @Column(name = "english_size_in_bytes")
    private Integer englishSizeInBytes;

    @Column(name = "welsh_file_service_id")
    private UUID welshBlobFileId;

    @Column(name = "welsh_number_of_pages")
    private Integer welshNumberOfPages;

    @Column(name = "welsh_size_in_bytes")
    private Integer welshSizeInBytes;

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

    @Column(name = "file_service_id")
    private UUID blobFileId;

    @Column(name = "number_of_pages")
    private Integer numberOfPages;

    @Column(name = "size_in_bytes")
    private Integer sizeInBytes;

    // for JPA
    public TransparencyReportMetadata() {

    }

    public TransparencyReportMetadata(final UUID englishBlobFileId,
                                      final Integer englishNumberOfPages,
                                      final Integer englishSizeInBytes,
                                      final UUID welshBlobFileId,
                                      final Integer welshNumberOfPages,
                                      final Integer welshSizeInBytes,
                                      final LocalDateTime generatedAt
    ) {
        this.id = randomUUID();
        this.englishBlobFileId = englishBlobFileId;
        this.englishNumberOfPages = englishNumberOfPages;
        this.englishSizeInBytes = englishSizeInBytes;
        this.welshBlobFileId = welshBlobFileId;
        this.welshNumberOfPages = welshNumberOfPages;
        this.welshSizeInBytes = welshSizeInBytes;
        this.generatedAt = generatedAt;
    }

    public TransparencyReportMetadata(final UUID id, final String documentFormat, final String documentRequestType, final String title, final String language, final LocalDateTime generatedAt) {
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

    public UUID getEnglishBlobFileId() {
        return englishBlobFileId;
    }

    public void setEnglishBlobFileId(final UUID englishBlobFileId) {
        this.englishBlobFileId = englishBlobFileId;
    }

    public Integer getEnglishNumberOfPages() {
        return englishNumberOfPages;
    }

    public void setEnglishNumberOfPages(final Integer englishNumberOfPages) {
        this.englishNumberOfPages = englishNumberOfPages;
    }

    public Integer getEnglishSizeInBytes() {
        return englishSizeInBytes;
    }

    public void setEnglishSizeInBytes(final Integer englishSizeInBytes) {
        this.englishSizeInBytes = englishSizeInBytes;
    }

    public UUID getWelshBlobFileId() {
        return welshBlobFileId;
    }

    public void setWelshBlobFileId(final UUID welshBlobFileId) {
        this.welshBlobFileId = welshBlobFileId;
    }

    public Integer getWelshNumberOfPages() {
        return welshNumberOfPages;
    }

    public void setWelshNumberOfPages(final Integer welshNumberOfPages) {
        this.welshNumberOfPages = welshNumberOfPages;
    }

    public Integer getWelshSizeInBytes() {
        return welshSizeInBytes;
    }

    public void setWelshSizeInBytes(final Integer welshSizeInBytes) {
        this.welshSizeInBytes = welshSizeInBytes;
    }

    public String getDocumentFormat() {
        return documentFormat;
    }

    public void setDocumentFormat(final String documentFormat) {
        this.documentFormat = documentFormat;
    }

    public String getDocumentRequestType() {
        return documentRequestType;
    }

    public void setDocumentRequestType(final String documentRequestType) {
        this.documentRequestType = documentRequestType;
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

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(final LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public UUID getBlobFileId() {
        return blobFileId;
    }

    public void setBlobFileId(final UUID blobFileId) {
        this.blobFileId = blobFileId;
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
}
