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
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportMetadataAdded;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

public class TransparencyReportAggregate implements Aggregate {

    private transient UUID transparencyReportId;

    private static final long serialVersionUID = 1L;

    private transient ReportMetadata englishReportMetadata;

    private transient ReportMetadata welshReportMetadata;

    private static final String ENGLISH = "en";
    private static final String WELSH = "cy";

    public Stream<Object> requestTransparencyReport(final UUID reportId, final ZonedDateTime requestedAt) {
        return apply(of(new TransparencyReportRequested(reportId,requestedAt)));
    }

    public Stream<Object> startTransparencyReportGeneration(final List<UUID> caseIds) {
        final Stream.Builder<Object> sb = Stream.builder();

        if (!isNull(caseIds)) {
            sb.add(new TransparencyReportGenerationStarted(this.transparencyReportId, caseIds));
        }

        return apply(sb.build());
    }

    public Stream<Object> updateMetadataForLanguage(final String language, final JsonObject metadata) {
        final Stream.Builder<Object> sb = Stream.builder();
        final ReportMetadata reportMetadata = transformJsonObjectToReportMetadata(metadata);
        ReportMetadata currentMetadata = null;
        if(language.equalsIgnoreCase(ENGLISH)){
            currentMetadata = englishReportMetadata;
        } else if(language.equalsIgnoreCase(WELSH)) {
            currentMetadata = welshReportMetadata;
        } else {
            return apply(sb.build());
        }

        if(isNull(currentMetadata) || !currentMetadata.equals(reportMetadata)) {
            sb.add(new TransparencyReportMetadataAdded(transparencyReportId, reportMetadata, language));
        }
        return apply(sb.build());
    }

    private void updateMetadata(final String language, final ReportMetadata metadata) {
        if(language.equalsIgnoreCase(ENGLISH)) {
            this.englishReportMetadata = metadata;
        } else if(language.equalsIgnoreCase(WELSH)) {
            this.welshReportMetadata = metadata;
        }
    }


    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(TransparencyReportRequested.class).apply(e -> this.transparencyReportId = e.getTransparencyReportId()),
                when(TransparencyReportGenerationStarted.class).apply(e -> doNothing()),
                when(TransparencyReportMetadataAdded.class).apply(e -> this.updateMetadata(e.getLanguage(), e.getMetadata())),
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
