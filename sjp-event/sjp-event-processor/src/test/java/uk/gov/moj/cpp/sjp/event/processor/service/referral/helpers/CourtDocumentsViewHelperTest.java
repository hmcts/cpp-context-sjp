package uk.gov.moj.cpp.sjp.event.processor.service.referral.helpers;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails.caseDetails;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Defendant.defendant;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Document.document;
import static uk.gov.justice.json.schemas.domains.sjp.queries.Offence.offence;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.CourtDocumentView;
import uk.gov.moj.cpp.sjp.event.processor.model.referral.MaterialView;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CourtDocumentsViewHelperTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID OFFENCE_ID = randomUUID();
    private static final UUID DOCUMENT_ID = randomUUID();
    private static final UUID DOCUMENT_TYPE_ID = randomUUID();
    private static final UUID MATERIAL_ID = randomUUID();
    private static final String MATERIAL_NAME = "Material Name";
    private static final String MIME_TYPE = "Mime Type";
    private static final ZonedDateTime DOCUMENT_UPLOAD_DATE = ZonedDateTime.now();

    private CourtDocumentsViewHelper courtDocumentsViewHelper = new CourtDocumentsViewHelper();

    @Test
    public void shouldCreateCourtDocumentViewsFromReferenceData() {
        final CaseDetails caseDetails = createCaseDetails();
        final Map<UUID, MaterialView> documentIdToMaterialViewMock = createDocumentIdToMaterialView();
        final Map<UUID, UUID> documentIdToDocumentTypeIdMock = createDocumentIdToDocumentTypeId();

        final List<CourtDocumentView> courtDocumentViews = courtDocumentsViewHelper
                .createCourtDocumentViews(caseDetails, documentIdToMaterialViewMock, documentIdToDocumentTypeIdMock);

        assertThat(courtDocumentViews, hasSize(1));

        final CourtDocumentView courtDocumentView = courtDocumentViews.get(0);

        assertThat(courtDocumentView.getCourtDocumentId(), is(DOCUMENT_ID));
        assertThat(courtDocumentView.getDocumentCategory().getDefendantDocument().getProsecutionCaseId(), is(CASE_ID));
        assertThat(courtDocumentView.getDocumentCategory().getDefendantDocument().getDefendants().get(0), is(DEFENDANT_ID));
        assertThat(courtDocumentView.getDocumentTypeId(), is(DOCUMENT_TYPE_ID));
        assertThat(courtDocumentView.getMaterials().get(0).getId(), is(MATERIAL_ID));
        assertThat(courtDocumentView.getMaterials().get(0).getName(), is(MATERIAL_NAME));
        assertThat(courtDocumentView.getMaterials().get(0).getMimeType(), is(MIME_TYPE));
    }

    private Map<UUID, UUID> createDocumentIdToDocumentTypeId() {
        return ImmutableMap.of(DOCUMENT_ID, DOCUMENT_TYPE_ID);
    }

    private Map<UUID, MaterialView> createDocumentIdToMaterialView() {
        return ImmutableMap.of(DOCUMENT_ID, new MaterialView(MATERIAL_ID, MATERIAL_NAME, DOCUMENT_UPLOAD_DATE, MIME_TYPE));
    }

    private CaseDetails createCaseDetails() {
        return caseDetails()
                .withId(CASE_ID)
                .withDefendant(defendant()
                        .withId(DEFENDANT_ID)
                        .withOffences(
                                singletonList(offence().withId(OFFENCE_ID).build()))
                        .build())
                .withCaseDocuments(singletonList(
                        document()
                                .withId(DOCUMENT_ID)
                                .withMaterialId(MATERIAL_ID)
                                .build()
                ))
                .build();
    }
}
