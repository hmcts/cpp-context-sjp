package uk.gov.moj.cpp.sjp.query.view.response.onlineplea;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;

import java.util.UUID;

public class OnlinePleaDetailView {

    private UUID id;

    private UUID offenceId;

    private UUID caseId;

    private UUID defendantId;

    private PleaType plea;

    private String mitigation;

    private String notGuiltyBecause;

    private String offenceTitle;

    public OnlinePleaDetailView(final OnlinePleaDetail onlinePleaDetail) {
        this.id = onlinePleaDetail.getId();
        this.offenceId = onlinePleaDetail.getOffenceId();
        this.caseId = onlinePleaDetail.getCaseId();
        this.defendantId = onlinePleaDetail.getDefendantId();
        this.plea = onlinePleaDetail.getPlea();
        this.mitigation = onlinePleaDetail.getMitigation();
        this.notGuiltyBecause = onlinePleaDetail.getNotGuiltyBecause();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public PleaType getPlea() {
        return plea;
    }

    public String getMitigation() {
        return mitigation;
    }

    public String getNotGuiltyBecause() {
        return notGuiltyBecause;
    }

    public String getOffenceTitle() {
        return offenceTitle;
    }

    public void setOffenceTitle(final String offenceTitle) {
        this.offenceTitle = offenceTitle;
    }
}
