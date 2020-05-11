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
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyReportMetadataAdded;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyReportRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

public class PressTransparencyReportAggregate implements Aggregate {

    private static final long serialVersionUID = 1L;

    private transient UUID pressTransparencyReportId;

    private ReportMetadata reportMetadata;

    public Stream<Object> requestPressTransparencyReport(final UUID reportId, final ZonedDateTime requestedAt) {
        return apply(of(new PressTransparencyReportRequested(reportId, requestedAt)));
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(PressTransparencyReportRequested.class).apply(e -> this.pressTransparencyReportId = e.getPressTransparencyReportId()),
                when(PressTransparencyReportGenerationStarted.class).apply(e -> doNothing()),
                when(PressTransparencyReportMetadataAdded.class).apply(e -> this.reportMetadata = e.getMetadata()),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> startTransparencyReportGeneration(final List<UUID> caseIds) {
        final Stream.Builder<Object> sb = Stream.builder();
        if(!isNull(caseIds)) {
            sb.add(new PressTransparencyReportGenerationStarted(this.pressTransparencyReportId,caseIds));
        }

        return apply(sb.build());
    }

    public Stream<Object> updateMetadata(final JsonObject metadata) {
        final Stream.Builder<Object> sb = Stream.builder();
        final ReportMetadata newReportMetadata = transformJsonObjectToReportMetadata(metadata);
        if(isNull(this.reportMetadata) || !newReportMetadata.equals(this.reportMetadata)) {
            sb.add(new PressTransparencyReportMetadataAdded(pressTransparencyReportId, newReportMetadata));
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
}
