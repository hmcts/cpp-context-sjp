package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static uk.gov.moj.cpp.sjp.event.CaseNoteAdded.caseNoteAdded;

import uk.gov.justice.json.schemas.domains.sjp.Note;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.CaseNoteRejected;

import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseNoteHandler {

    public static final CaseNoteHandler INSTANCE = new CaseNoteHandler();
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseNoteHandler.class);

    private CaseNoteHandler() {
    }

    public Stream<Object> addCaseNote(final UUID caseId,
                                      final Note note,
                                      final User author,
                                      final UUID decisionId,
                                      final CaseAggregateState state) {

        if (!state.isCaseIdEqualTo(caseId)) {
            LOGGER.error("Mismatch of IDs in aggregate: {} != {}", state.getCaseId(), caseId);
            return Stream.of(new CaseNotFound(caseId, "Add notes to case"));
        }
        if (state.isCaseReferredForCourtHearing()) {
            return Stream.of(new CaseNoteRejected(caseId, "Case referred for court hearing"));
        }
        return Stream.of(caseNoteAdded()
                .withCaseId(caseId)
                .withNote(note)
                .withAuthor(author)
                .withDecisionId(decisionId)
                .build());
    }
}
