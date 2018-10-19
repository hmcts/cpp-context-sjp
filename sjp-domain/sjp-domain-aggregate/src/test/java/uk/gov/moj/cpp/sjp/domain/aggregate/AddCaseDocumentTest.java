package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.cpp.sjp.domain.CaseDocument;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseDocumentBuilder;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAlreadyExists;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class AddCaseDocumentTest extends CaseAggregateBaseTest {

    private static final CaseDocument caseDocument = CaseDocumentBuilder.defaultCaseDocument();

    @Test
    public void uploadCaseDocument_caseDocumentUploadedKeepsTrackOfNthDocumentOfType() {
        assertUploadCaseDocument("SJPN", 1);
        assertUploadCaseDocument("OTHER-Travel Card", 1);
        assertUploadCaseDocument("OTHER", 1);
        assertUploadCaseDocument("SJPN", 2);
        assertUploadCaseDocument("SJPN", 3);
        assertUploadCaseDocument("OTHER-Travel Card", 2);
    }

    @Test
    public void testCaseInsensitivityWhilstUploadingCaseDocument() {
        assertUploadCaseDocument("OTHER-TravelCard", 1);
        assertUploadCaseDocument("OTHER-travelcard", 2);
        assertUploadCaseDocument("OTHER-travel card", 3);
    }

    private void assertUploadCaseDocument(String documentType, int expectedIndexWithinDocumentType) {
        //when
        Stream<Object> eventStream = caseAggregate.addCaseDocument(UUID.randomUUID(), new CaseDocument(UUID.randomUUID(), UUID.randomUUID(), documentType, null));
        List<Object> events = asList(eventStream.toArray());

        //then
        assertEquals(1, events.size());
        assertEquals(expectedIndexWithinDocumentType, ((CaseDocumentAdded) events.get(0)).getIndexWithinDocumentType());
    }

    @Test
    public void caseDocumentAdded_apply() {
        //given
        final CaseDocument sjpn =
                new CaseDocument(UUID.randomUUID(), UUID.randomUUID(), "SJPN", null);
        final CaseDocumentAdded firstCaseDocumentAddedEvent = (CaseDocumentAdded) caseAggregate.apply(new CaseDocumentAdded(UUID.randomUUID(), sjpn, 1));
        assertEquals(1, firstCaseDocumentAddedEvent.getIndexWithinDocumentType());

        final CaseDocumentAdded secondCaseDocumentAddedEvent = (CaseDocumentAdded) caseAggregate.apply(new CaseDocumentAdded(UUID.randomUUID(), sjpn, 2));
        assertEquals(2, secondCaseDocumentAddedEvent.getIndexWithinDocumentType());

        //when
        Stream<Object> eventStream = caseAggregate.addCaseDocument(aCase.getId(), new CaseDocument(UUID.randomUUID(), UUID.randomUUID(), "SJPN", null));
        List<Object> events = asList(eventStream.toArray());

        //then
        assertEquals(1, events.size());
        assertEquals(3, ((CaseDocumentAdded) events.get(0)).getIndexWithinDocumentType());
    }

    @Test
    public void testAddCaseDocument_shouldTriggerCaseDocumentAdded() {
        Stream<Object> eventStream = caseAggregate.addCaseDocument(aCase.getId(), caseDocument);

        List<Object> events = asList(eventStream.toArray());
        assertThat("Has caseDocumentAdded Event", events, hasItem(isA(CaseDocumentAdded.class)));
        CaseDocumentAdded caseDocumentAddedEvent = (CaseDocumentAdded) events.stream()
                .filter(e -> e.getClass().equals(CaseDocumentAdded.class))
                .findFirst()
                .get();
        assertThat("CaseDocumentAdded case id", caseDocumentAddedEvent.getCaseId(), is(aCase.getId()));
        assertThat("CaseDocumentAdded id", caseDocumentAddedEvent.getCaseDocument().getId(), is(caseDocument.getId()));
        assertThat("CaseDocumentAdded material id", caseDocumentAddedEvent.getCaseDocument().getMaterialId(), is(caseDocument.getMaterialId()));
        assertThat("CaseDocumentAdded type", caseDocumentAddedEvent.getCaseDocument().getDocumentType(), is(caseDocument.getDocumentType()));
    }

    @Test
    public void testAddCaseDocument_shouldUpdateAggState() {
        final Stream<Object> eventStream = caseAggregate.addCaseDocument(aCase.getId(), caseDocument);

        final CaseDocumentAdded caseDocumentAddedEvent = (CaseDocumentAdded) eventStream.findFirst().get();

        assertTrue("CaseDocumentAdded event created", nonNull(caseDocumentAddedEvent));
        assertThat(caseDocumentAddedEvent.getCaseId(), is(aCase.getId()));
        assertThat(caseDocumentAddedEvent.getCaseDocument(), is(caseDocument));
        assertThat(caseDocumentAddedEvent.getIndexWithinDocumentType(), is(1));
    }

    @Test
    public void shouldReturnCaseDocumentAlreadyExistsWhenCaseDocumentAlreadyExists_shouldThrowException() {
        // given
        caseAggregate.addCaseDocument(aCase.getId(), caseDocument);

        // when
        final List<Object> objects = caseAggregate.addCaseDocument(aCase.getId(), caseDocument).collect(toList());

        // then
        assertThat(objects.size(), is(1));

        Object object = objects.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(CaseDocumentAlreadyExists.class)));
    }
}
