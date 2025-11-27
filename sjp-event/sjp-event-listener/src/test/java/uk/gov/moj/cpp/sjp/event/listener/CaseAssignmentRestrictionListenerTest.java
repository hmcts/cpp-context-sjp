package uk.gov.moj.cpp.sjp.event.listener;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.json.schemas.domains.sjp.events.CaseAssignmentRestrictionAdded;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseAssignmentRestrictionRepository;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseAssignmentRestrictionListenerTest {

    private static final String PROSECUTING_AUTHORITY = "TFL";
    private static final List<String> EXCLUDE = singletonList("1234");
    private static final List<String> INCLUDE_ONLY = singletonList("9876");
    private static final ZonedDateTime DATE_TIME_CREATED = now();

    @Mock
    private CaseAssignmentRestrictionRepository repository;

    @InjectMocks
    private CaseAssignmentRestrictionListener listener;

    @Test
    public void shouldAddCaseAssignmentRestriction() throws JsonProcessingException {
        final CaseAssignmentRestrictionAdded event = new CaseAssignmentRestrictionAdded(DATE_TIME_CREATED.toString(), EXCLUDE, INCLUDE_ONLY, PROSECUTING_AUTHORITY,
                DATE_TIME_CREATED.toLocalDate().toString(), DATE_TIME_CREATED.toLocalDate().toString());
        final Envelope<CaseAssignmentRestrictionAdded> caseNoteAddedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.case-assignment-restriction-added"), event);
        listener.handleCaseAssignmentRestrictionAdded(caseNoteAddedEvent);

        verify(repository, times(1)).saveCaseAssignmentRestriction(PROSECUTING_AUTHORITY, "[\"9876\"]", "[\"1234\"]", DATE_TIME_CREATED,
                DATE_TIME_CREATED.toLocalDate(), DATE_TIME_CREATED.toLocalDate());
    }
}
