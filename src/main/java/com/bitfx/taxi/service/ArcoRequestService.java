package com.bitfx.taxi.service;

import com.bitfx.taxi.dto.privacy.ArcoRequestResponse;
import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.ArcoRequest;
import com.bitfx.taxi.model.ArcoRequestStatus;
import com.bitfx.taxi.model.ArcoRequestType;
import com.bitfx.taxi.model.User;
import com.bitfx.taxi.repository.ArcoRequestRepository;
import com.bitfx.taxi.util.TokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Solicitudes de derechos ARCO (LFPDPPP). El plazo legal para responder es de 20 dias habiles
 * desde que se recibe; aqui solo se descuentan sabados y domingos (los dias festivos oficiales no
 * se descuentan, asi que la fecha limite calculada es conservadora: nunca queda despues del plazo real).
 */
@Service
@RequiredArgsConstructor
public class ArcoRequestService {

    static final int RESPONSE_BUSINESS_DAYS = 20;

    private static final List<ArcoRequestStatus> OPEN_STATUSES = List.of(ArcoRequestStatus.PENDING, ArcoRequestStatus.IN_PROGRESS);

    static final Map<ArcoRequestType, String> TYPE_LABELS = Map.of(
            ArcoRequestType.ACCESS, "acceso",
            ArcoRequestType.RECTIFICATION, "rectificacion",
            ArcoRequestType.CANCELLATION, "cancelacion",
            ArcoRequestType.OPPOSITION, "oposicion",
            ArcoRequestType.REVOKE_CONSENT, "revocacion del consentimiento",
            ArcoRequestType.LIMIT_USE, "limitacion de uso"
    );

    private final ArcoRequestRepository arcoRequestRepository;
    private final EmailService emailService;

    @Transactional
    public ArcoRequestResponse create(User user, ArcoRequestType type, String description) {
        LocalDateTime now = LocalDateTime.now();
        ArcoRequest request = arcoRequestRepository.save(ArcoRequest.builder()
                .folio(generateUniqueFolio())
                .user(user)
                .type(type)
                .description(description.trim())
                .createdAt(now)
                .dueAt(addBusinessDays(now, RESPONSE_BUSINESS_DAYS))
                .build());
        emailService.sendArcoRequestReceivedEmail(user.getEmail(), user.getName(), request.getFolio(),
                TYPE_LABELS.get(type), request.getDueAt().toLocalDate());
        return ArcoRequestResponse.from(request);
    }

    /**
     * "Eliminar mi cuenta" es en si una solicitud de cancelacion: se registra como tal (con folio y
     * plazo) para que quede evidencia y el super admin la vea en la bandeja de ARCO. Si ya hay una
     * abierta, se reutiliza en vez de duplicarla.
     */
    @Transactional
    public ArcoRequestResponse registerAccountDeletion(User user) {
        var open = arcoRequestRepository.findByUser_IdAndTypeAndStatusIn(user.getId(), ArcoRequestType.CANCELLATION, OPEN_STATUSES);
        if (!open.isEmpty()) {
            return ArcoRequestResponse.from(open.get(0));
        }
        return create(user, ArcoRequestType.CANCELLATION, "Solicito la eliminacion de mi cuenta y mis datos personales (desde la opcion \"Eliminar mi cuenta\").");
    }

    public List<ArcoRequestResponse> listMine(User user) {
        return arcoRequestRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream().map(ArcoRequestResponse::from).toList();
    }

    public List<ArcoRequestResponse> listAll() {
        return arcoRequestRepository.findAllByOrderByCreatedAtDesc().stream().map(ArcoRequestResponse::from).toList();
    }

    @Transactional
    public ArcoRequestResponse resolve(Long id, ArcoRequestStatus status, String response, User admin) {
        ArcoRequest request = arcoRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));
        if (!OPEN_STATUSES.contains(request.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "La solicitud ya fue cerrada");
        }
        boolean closing = status == ArcoRequestStatus.RESOLVED || status == ArcoRequestStatus.REJECTED;
        if (closing && (response == null || response.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Escribe la respuesta que se le enviara al titular");
        }
        request.setStatus(status);
        if (response != null && !response.isBlank()) {
            request.setResponse(response.trim());
        }
        if (closing) {
            request.setResolvedAt(LocalDateTime.now());
            request.setResolvedBy(admin);
            User user = request.getUser();
            emailService.sendArcoRequestResolvedEmail(user.getEmail(), user.getName(), request.getFolio(),
                    status == ArcoRequestStatus.RESOLVED, request.getResponse());
        }
        return ArcoRequestResponse.from(arcoRequestRepository.save(request));
    }

    /**
     * Al anonimizar una cuenta se cierran sus solicitudes de cancelacion abiertas. Se llama ANTES de
     * borrar el correo del usuario, para que todavia pueda recibir la confirmacion.
     */
    @Transactional
    public void closeOpenCancellations(User user, User admin) {
        var open = arcoRequestRepository.findByUser_IdAndTypeAndStatusIn(user.getId(), ArcoRequestType.CANCELLATION, OPEN_STATUSES);
        String response = "Tu cuenta fue eliminada: tus datos personales se anonimizaron. Solo se conservan, sin datos que te "
                + "identifiquen, los registros de viajes que la ley nos obliga a guardar.";
        for (ArcoRequest request : open) {
            request.setStatus(ArcoRequestStatus.RESOLVED);
            request.setResponse(response);
            request.setResolvedAt(LocalDateTime.now());
            request.setResolvedBy(admin);
            arcoRequestRepository.save(request);
            emailService.sendArcoRequestResolvedEmail(user.getEmail(), user.getName(), request.getFolio(), true, response);
        }
    }

    static LocalDateTime addBusinessDays(LocalDateTime from, int days) {
        LocalDateTime result = from;
        int added = 0;
        while (added < days) {
            result = result.plusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return result;
    }

    private String generateUniqueFolio() {
        String folio;
        do {
            folio = "ARCO-" + TokenGenerator.generateReferralCode();
        } while (arcoRequestRepository.existsByFolio(folio));
        return folio;
    }
}
