package uk.gov.moj.sjp.it.commandclient;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.sjp.it.util.commandclient.CommandClient;
import uk.gov.moj.sjp.it.util.commandclient.CommandExecutor;
import uk.gov.moj.sjp.it.util.commandclient.EventHandler;
import uk.gov.moj.sjp.it.model.Defendant;
import uk.gov.moj.sjp.it.util.UrnProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Builder;

@Builder
@CommandClient(
        URI = "/cases",
        contentType = "application/vnd.sjp.create-sjp-case+json"
)
public class CreateCaseClient {

    @NotNull(message = "ID is required")
    public UUID id;

    @Builder.Default
    @NotNull(message = "URN is required")
    public String urn = UrnProvider.generate(ProsecutingAuthority.TFL);

    @Builder.Default
    public String ptiUrn = UrnProvider.generate(ProsecutingAuthority.TFL);

    @Builder.Default
    @NotNull(message = "Prosecuting authority is required")
    public ProsecutingAuthority prosecutingAuthority = ProsecutingAuthority.TFL;

    @Builder.Default
    public String initiationCode = "J";

    @Builder.Default
    public String summonsCode = "M";

    @Builder.Default
    public String libraOriginatingOrg = "GAFTL00";

    @Builder.Default
    public String libraHearingLocation = "B01CE03";

    @Builder.Default
    public String dateOfHearing = "2016-01-01";

    @Builder.Default
    public String timeOfHearing = "11:00";

    @Builder.Default
    public BigDecimal costs = BigDecimal.valueOf(1.23);

    @Builder.Default
    @NotNull(message = "Posting date is required")
    public String postingDate = LocalDates.to(LocalDate.of(2015, 12, 2));

    @Valid
    @Builder.Default
    @NotNull(message = "Defendant is required")
    public Defendant defendant = Defendant.builder().build();

    @EventHandler(CaseMarkedReadyForDecision.EVENT_NAME)
    public Consumer markedReadyForDecisionHandler;

    public CommandExecutor getExecutor() {
        return new CommandExecutor(this);
    }

}
