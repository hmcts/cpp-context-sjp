package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import java.time.LocalDate;

public class EnforcementPendingApplicationNotificationTemplateDataBuilder {

    private String gobAccountNumber;
    private int divisionCode;
    private String caseReference;
    private LocalDate dateApplicationIsListed;
    private String defendantName;
    private String title;

    public EnforcementPendingApplicationNotificationTemplateDataBuilder withGobAccountNumber(final String gobAccountNumber) {
        this.gobAccountNumber = gobAccountNumber;
        return this;
    }

    public EnforcementPendingApplicationNotificationTemplateDataBuilder withDivisionCode(final int divisionCode) {
        this.divisionCode = divisionCode;
        return this;
    }

    public EnforcementPendingApplicationNotificationTemplateDataBuilder withCaseReference(final String caseReference) {
        this.caseReference = caseReference;
        return this;
    }

    public EnforcementPendingApplicationNotificationTemplateDataBuilder withDateApplicationIsListed(final LocalDate dateApplicationIsListed) {
        this.dateApplicationIsListed = dateApplicationIsListed;
        return this;
    }

    public EnforcementPendingApplicationNotificationTemplateDataBuilder withDefendantName(final String defendantName) {
        this.defendantName = defendantName;
        return this;
    }

    public EnforcementPendingApplicationNotificationTemplateDataBuilder withTitle(final String title) {
        this.title = title;
        return this;
    }

    public EnforcementPendingApplicationNotificationTemplateData build() {
        return new EnforcementPendingApplicationNotificationTemplateData(
                this.gobAccountNumber,
                this.divisionCode,
                this.caseReference,
                this.dateApplicationIsListed,
                this.defendantName,
                this.title
        );
    }
}

