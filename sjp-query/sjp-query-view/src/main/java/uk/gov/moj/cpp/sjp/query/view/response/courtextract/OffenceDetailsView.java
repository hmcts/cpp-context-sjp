package uk.gov.moj.cpp.sjp.query.view.response.courtextract;

import static java.util.Collections.unmodifiableList;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Representation of each item in the 'offences' array of the court extract json payload.
 */
public class OffenceDetailsView {

    private final int sequenceNumber;

    private String title;

    private final String wording;

    private String legislation;

    private final List<OffenceDecisionView> offenceDecisions;

    public OffenceDetailsView(final CaseDetail caseDetail,
                              final OffenceDetail offenceDetail,
                              final Collection<Pair<CaseDecision, OffenceDecision>> offenceDecisions) {
        this.sequenceNumber = offenceDetail.getSequenceNumber();
        this.wording = offenceDetail.getWording();
        this.offenceDecisions = offenceDecisions.stream().
                map(caseAndOffenceDecision ->
                        new OffenceDecisionView(caseDetail,
                                caseAndOffenceDecision.getLeft(),
                                caseAndOffenceDecision.getRight()))
                .collect(Collectors.toList());
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getWording() {
        return wording;
    }

    public String getLegislation() {
        return legislation;
    }

    public void setLegislation(final String legislation) {
        this.legislation = legislation;
    }

    public List<OffenceDecisionView> getOffenceDecisions() {
        return unmodifiableList(offenceDecisions);
    }
}
