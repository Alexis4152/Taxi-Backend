package com.bitfx.taxi.dto.admin;

import com.bitfx.taxi.model.DriverProfile;

public record DriverResponse(
        Long driverId,
        Long userId,
        String name,
        String phone,
        String email,
        String photoUrl,
        String bankAccount,
        String address,
        double ratingAvg,
        int ratingCount,
        boolean active,
        Long organizationId,
        String organizationName,
        Long taxiId,
        String taxiUnitNumber,
        String taxiPlates,
        String taxiBrand,
        String taxiModel
) {
    public static DriverResponse from(DriverProfile d) {
        return new DriverResponse(
                d.getId(),
                d.getUser().getId(),
                d.getUser().getName(),
                d.getUser().getPhone(),
                d.getUser().getEmail(),
                d.getPhotoUrl(),
                d.getBankAccount(),
                d.getAddress(),
                d.getRatingAvg().doubleValue(),
                d.getRatingCount(),
                d.isActive(),
                d.getOrganization().getId(),
                d.getOrganization().getName(),
                d.getTaxi() != null ? d.getTaxi().getId() : null,
                d.getTaxi() != null ? d.getTaxi().getUnitNumber() : null,
                d.getTaxi() != null ? d.getTaxi().getPlates() : null,
                d.getTaxi() != null ? d.getTaxi().getBrand() : null,
                d.getTaxi() != null ? d.getTaxi().getModel() : null
        );
    }
}
