package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Objects.isNull;
import static java.util.UUID.fromString;
import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.domain.transparency.ReportMetadata;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyJSONReportGenerationFailed;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyJSONReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyJSONReportMetadataAdded;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyJSONReportRequested;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyPDFReportGenerationFailed;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyPDFReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyPDFReportMetadataAdded;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyPDFReportRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

public class PressTransparencyReportAggregate implements Aggregate {

    private static final long serialVersionUID = 1L;
    private static final String PDF = "PDF";
    private transient UUID pressTransparencyReportId;
    private transient ReportMetadata reportMetadata;

    public Stream<Object> requestPressTransparencyReport(final UUID reportId, String documentFormat, String requestType, final String language, final ZonedDateTime requestedAt) {
        if (PDF.equals(documentFormat)) {
            return apply(of(new PressTransparencyPDFReportRequested(reportId, requestType, language, requestedAt)));
        } else {
            return apply(of(new PressTransparencyJSONReportRequested(reportId, requestType, language, requestedAt)));
        }
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(PressTransparencyPDFReportRequested.class).apply(e -> this.pressTransparencyReportId = e.getPressTransparencyReportId()),
                when(PressTransparencyPDFReportGenerationStarted.class).apply(e -> doNothing()),
                when(PressTransparencyPDFReportMetadataAdded.class).apply(e -> this.reportMetadata = e.getMetadata()),
                when(PressTransparencyPDFReportGenerationFailed.class).apply(e -> doNothing()),

                when(PressTransparencyJSONReportRequested.class).apply(e -> this.pressTransparencyReportId = e.getPressTransparencyReportId()),
                when(PressTransparencyJSONReportGenerationStarted.class).apply(e -> doNothing()),
                when(PressTransparencyJSONReportMetadataAdded.class).apply(e -> this.reportMetadata = e.getMetadata()),
                when(PressTransparencyJSONReportGenerationFailed.class).apply(e -> doNothing()),

                otherwiseDoNothing()
        );
    }

    public Stream<Object> startTransparencyReportGeneration(final List<UUID> caseIds, final JsonObject payload) {
        final String documentFormat = payload.getString("format");
        final String language = payload.getString("language");
        final String type = payload.getString("requestType");
        final String title = payload.getString("title");
        final Stream.Builder<Object> sb = Stream.builder();
        if (!isNull(caseIds)) {
            if (PDF.equals(documentFormat)) {
                sb.add(new PressTransparencyPDFReportGenerationStarted(this.pressTransparencyReportId, documentFormat, type, title, language, caseIds));
            } else {
                sb.add(new PressTransparencyJSONReportGenerationStarted(this.pressTransparencyReportId, caseIds));
            }
        }

        return apply(sb.build());
    }

    public Stream<Object> updateMetadata(final JsonObject metadata) {
        final Stream.Builder<Object> sb = Stream.builder();
        final ReportMetadata newReportMetadata = transformJsonObjectToReportMetadata(metadata);
        if (isNull(this.reportMetadata) || !newReportMetadata.equals(this.reportMetadata)) {
            sb.add(new PressTransparencyPDFReportMetadataAdded(pressTransparencyReportId, newReportMetadata));
        }
        return apply(sb.build());
    }

    private ReportMetadata transformJsonObjectToReportMetadata(final JsonObject reportMetadata) {
        return new ReportMetadata(
                reportMetadata.getString("fileName"),
                reportMetadata.getInt("numberOfPages"),
                reportMetadata.getInt("fileSize"),
                fromString(reportMetadata.getString("fileId"))
        );
    }

    public Stream<Object> pressTransparencyReportFailed() {
        final Stream.Builder<Object> sb = Stream.builder();
        sb.add(new PressTransparencyPDFReportGenerationFailed(pressTransparencyReportId));
        return apply(sb.build());
    }
}
