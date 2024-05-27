package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Objects.isNull;
import static java.util.UUID.fromString;
import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.domain.transparency.ReportMetadata;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyJSONReportGenerationFailed;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyJSONReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyJSONReportMetadataAdded;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyJSONReportRequested;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportGenerationFailed;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportMetadataAdded;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

public class TransparencyReportAggregate implements Aggregate {

    private static final long serialVersionUID = 1L;
    private static final String ENGLISH = "en";
    private static final String WELSH = "cy";
    private static final String PDF = "PDF";

    private transient UUID transparencyReportId;
    private transient ReportMetadata englishReportMetadata;
    private transient ReportMetadata welshReportMetadata;
    private transient List<UUID> caseIds;
    private transient boolean reportGenerationFailedPreviously;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(TransparencyPDFReportRequested.class).apply(e -> this.transparencyReportId = e.getTransparencyReportId()),
                when(TransparencyPDFReportGenerationStarted.class).apply(e -> this.caseIds = e.getCaseIds()),
                when(TransparencyPDFReportMetadataAdded.class).apply(e -> this.updateMetadata(e.getLanguage(), e.getMetadata())),
                when(TransparencyPDFReportGenerationFailed.class).apply(e -> this.reportGenerationFailedPreviously = true),

                when(TransparencyJSONReportRequested.class).apply(e -> this.transparencyReportId = e.getTransparencyReportId()),
                when(TransparencyJSONReportGenerationStarted.class).apply(e -> this.caseIds = e.getCaseIds()),
                when(TransparencyJSONReportMetadataAdded.class).apply(e -> this.updateMetadata(e.getLanguage(), e.getMetadata())),
                when(TransparencyJSONReportGenerationFailed.class).apply(e -> this.reportGenerationFailedPreviously = true),

                otherwiseDoNothing());
    }

    public Stream<Object> requestTransparencyReport(final UUID reportId, String documentFormat, String requestType, final String language, final ZonedDateTime requestedAt) {
        if (PDF.equals(documentFormat)) {
            return apply(of(new TransparencyPDFReportRequested(reportId, requestType, language, requestedAt)));
        } else {
            return apply(of(new TransparencyJSONReportRequested(reportId, requestType, language, requestedAt)));
        }
    }

    public Stream<Object> startTransparencyReportGeneration(final List<UUID> caseIds, final JsonObject payload) {

        final String documentFormat = payload.getString("format");
        final String language = payload.getString("language");
        final String type = payload.getString("requestType");
        final String title = payload.getString("title");
        final Stream.Builder<Object> sb = Stream.builder();
        if (!isNull(caseIds)) {
            if (PDF.equals(documentFormat)) {
                sb.add(new TransparencyPDFReportGenerationStarted(this.transparencyReportId, documentFormat, type, title, language, caseIds));
            } else {
                sb.add(new TransparencyJSONReportGenerationStarted(this.transparencyReportId, caseIds));
            }
        }
        return apply(sb.build());
    }

    public Stream<Object> updateMetadataForLanguage(final String language, final JsonObject metadata) {
        final Stream.Builder<Object> sb = Stream.builder();
        final ReportMetadata reportMetadata = transformJsonObjectToReportMetadata(metadata);
        ReportMetadata currentMetadata;
        if (language.equalsIgnoreCase(ENGLISH)) {
            currentMetadata = englishReportMetadata;
        } else if (language.equalsIgnoreCase(WELSH)) {
            currentMetadata = welshReportMetadata;
        } else {
            return apply(sb.build());
        }

        if (isNull(currentMetadata) || !currentMetadata.equals(reportMetadata)) {
            sb.add(new TransparencyPDFReportMetadataAdded(transparencyReportId, reportMetadata, language));
        }
        return apply(sb.build());
    }

    public Stream<Object> transparencyReportFailed(final String templateIdentifier) {
        final Stream.Builder<Object> sb = Stream.builder();
        sb.add(new TransparencyPDFReportGenerationFailed(transparencyReportId, templateIdentifier, caseIds, reportGenerationFailedPreviously));
        return apply(sb.build());
    }

    private void updateMetadata(final String language, final ReportMetadata metadata) {
        if (language.equalsIgnoreCase(ENGLISH)) {
            this.englishReportMetadata = metadata;
        } else if (language.equalsIgnoreCase(WELSH)) {
            this.welshReportMetadata = metadata;
        }
    }

    private ReportMetadata transformJsonObjectToReportMetadata(final JsonObject reportMetadata) {
        return new ReportMetadata(
                reportMetadata.getString("fileName"),
                reportMetadata.getInt("numberOfPages"),
                reportMetadata.getInt("fileSize"),
                fromString(reportMetadata.getString("fileId"))
        );
    }
}
