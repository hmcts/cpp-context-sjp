package uk.gov.moj.cpp.sjp.event.processor.model.referral;

public class OffenceFactsView {

    private String vehicleRegistration;

    private Integer alcoholReadingAmount;

    private String alcoholReadingMethodCode;

    private String alcoholReadingMethodDescription;

    private String vehicleMake;

    private VehicleCode vehicleCode;

    public OffenceFactsView(final String vehicleRegistration,
                            final Integer alcoholReadingAmount,
                            final String alcoholReadingMethodCode,
                            final String alcoholReadingMethodDescription,
                            final String vehicleMake,
                            final VehicleCode vehicleCode) {
        this.vehicleRegistration = vehicleRegistration;
        this.alcoholReadingAmount = alcoholReadingAmount;
        this.alcoholReadingMethodCode = alcoholReadingMethodCode;
        this.alcoholReadingMethodDescription = alcoholReadingMethodDescription;
        this.vehicleMake = vehicleMake;
        this.vehicleCode = vehicleCode;
    }

    public String getVehicleRegistration() {
        return vehicleRegistration;
    }

    public Integer getAlcoholReadingAmount() {
        return alcoholReadingAmount;
    }

    public String getAlcoholReadingMethodCode() {
        return alcoholReadingMethodCode;
    }

    public String getAlcoholReadingMethodDescription() {
        return alcoholReadingMethodDescription;
    }

    public String getVehicleMake() {
        return vehicleMake;
    }

    public VehicleCode getVehicleCode() {
        return vehicleCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String vehicleRegistration;
        private Integer alcoholReadingAmount;
        private String alcoholReadingMethodCode;
        private String alcoholReadingMethodDescription;
        private String vehicleMake;
        private VehicleCode vehicleCode;

        public Builder withVehicleRegistration(final String vehicleRegistration){
            this.vehicleRegistration = vehicleRegistration;
            return this;
        }

        public Builder withVehicleMake(String vehicleMake){
            this.vehicleMake = vehicleMake;
            return this;
        }

        public Builder withAlcoholReadingAmount(Integer alcoholReadingAmount){
            this.alcoholReadingAmount = alcoholReadingAmount;
            return this;
        }

        public Builder withAlcoholReadingMethodCode(String alcoholReadingMethodCode){
            this.alcoholReadingMethodCode = alcoholReadingMethodCode;
            return this;
        }

        public Builder withAlcoholReadingMethodDescription(String alcoholReadingMethodDescription){
            this.alcoholReadingMethodDescription = alcoholReadingMethodDescription;
            return this;
        }

        public Builder withVehicleCode(VehicleCode vehicleCode){
            this.vehicleCode = vehicleCode;
            return this;
        }

        public OffenceFactsView build(){
            return new OffenceFactsView(
                    vehicleRegistration,
                    alcoholReadingAmount,
                    alcoholReadingMethodCode,
                    alcoholReadingMethodDescription,
                    vehicleMake,
                    vehicleCode);
        }


    }
}
