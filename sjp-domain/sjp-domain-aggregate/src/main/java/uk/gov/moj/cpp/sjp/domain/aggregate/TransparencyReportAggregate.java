package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Stream.of;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.doNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.sjp.domain.transparency.ReportMetadata;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportGenerated;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

public class TransparencyReportAggregate implements Aggregate {

    private static final long serialVersionUID = 1L;

    // All the transparency reports shared the same streamId. Do not modify it!
    public static final UUID TRANSPARENCY_REPORT_STREAM_ID = fromString("37c62719-f1cc-4a84-bca4-14087d9d826c");

    private ReportMetadata englishReportMetadata;

    public Stream<Object> requestTransparencyReport(final ZonedDateTime requestedAt) {
        return apply(of(new TransparencyReportRequested(requestedAt)));
    }

    public Stream<Object> generateTransparencyReport(final List<UUID> caseIds, final JsonObject englishReportMetadata, final JsonObject welshReportMetadata) {
        final Stream.Builder<Object> sb = Stream.builder();

        if (isNull(this.englishReportMetadata) || isNull(this.englishReportMetadata.getFileId()) ||
                !this.englishReportMetadata.getFileId().toString().equals(englishReportMetadata.getString("fileId"))) {
            sb.add(new TransparencyReportGenerated(caseIds,
                    transformJsonObjectToReportMetadata(englishReportMetadata),
                    transformJsonObjectToReportMetadata(welshReportMetadata)));
        }

        return apply(sb.build());
    }

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(TransparencyReportRequested.class).apply(e -> doNothing()),
                when(TransparencyReportGenerated.class).apply(e -> this.englishReportMetadata = e.getEnglishReportMetadata()),
                otherwiseDoNothing());
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
