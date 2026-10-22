package com.bitfx.taxi.dto.admin;

import com.bitfx.taxi.model.DriverProfile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
        String taxiModel,
        boolean online,
        BigDecimal pendingAmount,
        int unpaidTripCount,
        LocalDateTime paymentDueAt,
        boolean paymentBlocked
) {
    public static DriverResponse from(DriverProfile d) {
        return from(d, false);
    }

    // El propio operador necesita saber si el backend todavia lo considera "en linea" para poder
    // restaurar ese estado al abrir/refrescar la app (ver GET /api/driver/me) - sin esto, un
    // refresh de pagina lo deja viendose desconectado aunque la BD diga lo contrario, y deja de
    // recibir ofertas de viaje por WebSocket hasta que vuelva a tocar "Conectarme" a mano.
    public static DriverResponse from(DriverProfile d, boolean online) {
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
                d.getTaxi() != null ? d.getTaxi().getModel() : null,
                online,
                d.getPendingAmount(),
                d.getUnpaidTripCount(),
                d.getPaymentDueAt(),
                d.isPaymentBlocked()
        );
    }
}
