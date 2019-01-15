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
    private UUID englishFileServiceId;

    @Column(name = "english_number_of_pages")
    private Integer englishNumberOfPages;

    @Column(name = "english_size_in_bytes")
    private Integer englishSizeInBytes;

    @Column(name = "welsh_file_service_id")
    private UUID welshFileServiceId;

    @Column(name = "welsh_number_of_pages")
    private Integer welshNumberOfPages;

    @Column(name = "welsh_size_in_bytes")
    private Integer welshSizeInBytes;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    // for JPA
    public TransparencyReportMetadata() {

    }

    public TransparencyReportMetadata(final UUID englishFileServiceId,
                                      final Integer englishNumberOfPages,
                                      final Integer englishSizeInBytes,
                                      final UUID welshFileServiceId,
                                      final Integer welshNumberOfPages,
                                      final Integer welshSizeInBytes,
                                      final LocalDateTime generatedAt) {
        this.id = randomUUID();
        this.englishFileServiceId = englishFileServiceId;
        this.englishNumberOfPages = englishNumberOfPages;
        this.englishSizeInBytes = englishSizeInBytes;
        this.welshFileServiceId = welshFileServiceId;
        this.welshNumberOfPages = welshNumberOfPages;
        this.welshSizeInBytes = welshSizeInBytes;
        this.generatedAt = generatedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getEnglishFileServiceId() {
        return englishFileServiceId;
    }

    public void setEnglishFileServiceId(final UUID englishFileServiceId) {
        this.englishFileServiceId = englishFileServiceId;
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

    public UUID getWelshFileServiceId() {
        return welshFileServiceId;
    }

    public void setWelshFileServiceId(final UUID welshFileServiceId) {
        this.welshFileServiceId = welshFileServiceId;
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

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(final LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
