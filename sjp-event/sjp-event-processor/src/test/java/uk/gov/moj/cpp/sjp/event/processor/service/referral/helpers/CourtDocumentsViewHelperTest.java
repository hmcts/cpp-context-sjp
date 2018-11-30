package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.json.schemas.domains.sjp.queries.Document;
import uk.gov.justice.json.schemas.domains.sjp.queries.Offence;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.CourtDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.MaterialView;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentsViewHelperTest {

    private static final String CASE_ID = randomUUID().toString();
    private static final String DEFENDANT_ID = randomUUID().toString();
    private static final String OFFENCE_ID = randomUUID().toString();
    private static final String DOCUMENT_ID = randomUUID().toString();
    private static final UUID DOCUMENT_TYPE_ID = randomUUID();
    private static final UUID MATERIAL_ID = randomUUID();
    private static final String MATERIAL_NAME = "Material Name";
    private static final ZonedDateTime DOCUMENT_DATE = ZonedDateTime.now();
    private static final String MIME_TYPE = "Mime Type";

    private CourtDocumentsViewHelper courtDocumentsViewHelper = new CourtDocumentsViewHelper();

    @Test
    public void shouldCreateCourtDocumentViewsFromReferenceData() {
        final CaseDetails caseDetails = createCaseDetails();
        final Map<String, MaterialView> documentIdToMaterialViewMock = createDocumentIdToMaterialView();
        final Map<String, UUID> documentIdToDocumentTypeIdMock = createDocumentIdToDocumentTypeId();

        final List<CourtDocumentView> courtDocumentViews = courtDocumentsViewHelper
                .createCourtDocumentViews(caseDetails, documentIdToMaterialViewMock, documentIdToDocumentTypeIdMock);

        assertThat(courtDocumentViews.size(), is(1));

        final CourtDocumentView courtDocumentView = courtDocumentViews.get(0);

        assertThat(courtDocumentView.getCourtDocumentId(), is(fromString(DOCUMENT_ID)));
        assertThat(courtDocumentView.getDocumentCategory().getDefendantDocument().getProsecutionCaseId(), is(fromString(CASE_ID)));
        assertThat(courtDocumentView.getDocumentCategory().getDefendantDocument().getDefendants().get(0), is(fromString(DEFENDANT_ID)));
        assertThat(courtDocumentView.getDocumentTypeId(), is(DOCUMENT_TYPE_ID));
        assertThat(courtDocumentView.getMaterials().get(0).getId(), is(MATERIAL_ID));
        assertThat(courtDocumentView.getMaterials().get(0).getName(), is(MATERIAL_NAME));
        assertThat(courtDocumentView.getMaterials().get(0).getMimeType(), is(MIME_TYPE));
    }

    private Map<String, UUID> createDocumentIdToDocumentTypeId() {
        Map<String, UUID> expectedDocumentTypeId = new HashMap<>();
        expectedDocumentTypeId.put(DOCUMENT_ID, DOCUMENT_TYPE_ID);

        return expectedDocumentTypeId;
    }

    private Map<String, MaterialView> createDocumentIdToMaterialView() {
        final Map<String, MaterialView> expectedMaterialViews = new HashMap<>();
        expectedMaterialViews.put(DOCUMENT_ID,
                new MaterialView(MATERIAL_ID, MATERIAL_NAME, DOCUMENT_DATE, MIME_TYPE));

        return expectedMaterialViews;
    }

    private CaseDetails createCaseDetails() {
        return CaseDetails.caseDetails()
                .withId(CASE_ID)
                .withDefendant(Defendant.defendant()
                        .withId(DEFENDANT_ID)
                        .withOffences(
                                singletonList(
                                        Offence.offence()
                                                .withId(OFFENCE_ID)
                                                .build()))
                        .build())
                .withCaseDocuments(singletonList(
                        Document.document()
                                .withId(DOCUMENT_ID)
                                .withMaterialId(MATERIAL_ID.toString())
                                .build()
                ))
                .build();
    }
}
