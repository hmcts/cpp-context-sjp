package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.json.schemas.domains.sjp.events.CaseAssignmentRestrictionAdded;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseAssignmentRestrictionRepository;

import java.time.ZonedDateTime;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ServiceComponent(EVENT_LISTENER)
public class CaseAssignmentRestrictionListener {

    @Inject
    private CaseAssignmentRestrictionRepository caseAssignmentRestrictionRepository;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperProducer().objectMapper();

    @Handles("sjp.events.case-assignment-restriction-added")
    public void handleCaseAssignmentRestrictionAdded(final Envelope<CaseAssignmentRestrictionAdded> eventEnvelope) throws JsonProcessingException {
        final CaseAssignmentRestrictionAdded caseAssignmentRestrictionAdded = eventEnvelope.payload();
        caseAssignmentRestrictionRepository.saveCaseAssignmentRestriction(caseAssignmentRestrictionAdded.getProsecutingAuthority(),
                OBJECT_MAPPER.writeValueAsString(caseAssignmentRestrictionAdded.getIncludeOnly()),
                OBJECT_MAPPER.writeValueAsString(caseAssignmentRestrictionAdded.getExclude()),
                ZonedDateTime.parse(caseAssignmentRestrictionAdded.getDateTimeCreated()));
    }
}
