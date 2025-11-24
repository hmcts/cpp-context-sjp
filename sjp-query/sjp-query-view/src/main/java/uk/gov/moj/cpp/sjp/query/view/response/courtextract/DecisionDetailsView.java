package uk.gov.moj.cpp.sjp.query.view.response.courtextract;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.query.view.response.ApplicationDecisionView;
import uk.gov.moj.cpp.sjp.query.view.response.OffenceDecisionView;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

/**
 * Representation of each item in the 'offences' array of the court extract json payload.
 */
public class DecisionDetailsView {

    private String type;

    private int sequenceNumber;

    private String title;

    private final String wording;

    private String legislation;

    private List<DecisionView> offenceDecisions = new ArrayList<>();

    private DecisionView applicationDecision;



    public DecisionDetailsView(final List<CaseDecisionCourtExtractView> offenceApplicationDecision,
                               final CaseDetail caseDetail,
                               final OffenceDetail offenceDetail,
                               final Collection<Pair<CaseDecisionCourtExtractView, OffenceDecisionView>> offenceDecisions) {

        this.sequenceNumber = offenceDetail.getSequenceNumber();
        this.type = "Offence";
        this.wording = offenceDetail.getWording();
        this.offenceDecisions = offenceDecisions.stream().
                map(caseAndOffenceDecision ->
                        new DecisionView(offenceApplicationDecision,caseDetail,
                                caseAndOffenceDecision.getLeft(),
                                caseAndOffenceDecision.getRight()))
                .collect(Collectors.toList());
    }

    public DecisionDetailsView(final List<CaseDecisionCourtExtractView> offenceApplicationDecision,
                               final CaseDetail caseDetail,
                               final CaseDecisionCourtExtractView caseApplication) {

        this.type = "Application";
        this.wording = prepareWordingApplication(caseApplication.getApplicationDecision());
        this.applicationDecision = new DecisionView(offenceApplicationDecision,caseDetail,caseApplication);
    }

    private String prepareWordingApplication(final ApplicationDecisionView caseApplication) {
        return ofNullable(caseApplication)
                .map(ApplicationDecisionView::getApplicationType)
                .map(value -> {
                    switch (value) {
                        case STAT_DEC:
                            return "Appearance to make statutory declaration (SJP case)";
                        case REOPENING:
                            return "Application to reopen case";
                        default:
                            return null;
                    }
                }).orElse(null);
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

    public List<DecisionView> getOffenceDecisions() {
        return unmodifiableList(offenceDecisions);
    }

    public void setOffenceDecisions(List<DecisionView> offenceDecisions) {
        this.offenceDecisions = unmodifiableList(offenceDecisions);
    }

    public DecisionView getApplicationDecision() {
        return applicationDecision;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


}
