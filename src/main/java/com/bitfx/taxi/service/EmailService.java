package com.bitfx.taxi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Envio de correo "best effort": si falla el SMTP o esta deshabilitado (app.mail.enabled=false),
 * se registra en el log pero nunca bloquea ni revierte la operacion de negocio que lo disparo
 * (mismo criterio que el EmailService del POS de referencia).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled}")
    private boolean mailEnabled;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        if (!mailEnabled) {
            // Correo deshabilitado (dev/local): se registra el enlace solo aqui, nunca cuando el
            // correo si se envia de verdad, para no dejar el token en claro en logs de produccion.
            log.info("Correo deshabilitado, enlace de recuperacion para '{}': {}", toEmail, link);
        }
        String body = "Solicitaste recuperar tu contrasena en NexoraTaxis.\n\n" +
                "Da clic en el siguiente enlace (valido por 30 minutos):\n" + link +
                "\n\nSi tu no solicitaste esto, ignora este correo.";
        send(toEmail, "Recuperacion de contrasena - NexoraTaxis", body);
    }

    @Async
    public void sendDriverWelcomeEmail(String toEmail, String name, String phone, String temporaryPassword) {
        String body = "Hola " + name + ", se creo tu acceso de operador en NexoraTaxis.\n\n" +
                "Telefono (usuario): " + phone + "\n" +
                "Contrasena temporal: " + temporaryPassword + "\n\n" +
                "Se te pedira cambiarla al iniciar sesion por primera vez.";
        send(toEmail, "Bienvenido a NexoraTaxis - Acceso de operador", body);
    }

    @Async
    public void sendDriverSelfRegisteredEmail(String toEmail, String name, String taxiUnitNumber, String taxiPlates) {
        String body = "Hola " + name + ", tu cuenta de operador en NexoraTaxis ya esta activa.\n\n" +
                "Taxi vinculado: Unidad " + taxiUnitNumber + " (" + taxiPlates + ")\n\n" +
                "Ya puedes conectarte y empezar a recibir solicitudes de viaje.";
        send(toEmail, "Bienvenido a NexoraTaxis", body);
    }

    @Async
    public void sendTaxiChangeRequestReceivedEmail(String toEmail, String name, String requestedPlates) {
        String body = "Hola " + name + ", recibimos tu solicitud para cambiar de taxi (placas " + requestedPlates + ").\n\n" +
                "Un administrador la revisara pronto; te avisaremos por aqui en cuanto se resuelva.";
        send(toEmail, "Solicitud de cambio de taxi recibida - NexoraTaxis", body);
    }

    @Async
    public void sendTaxiChangeRequestResolvedEmail(String toEmail, String name, boolean approved, String requestedPlates) {
        String body = approved
                ? "Hola " + name + ", tu solicitud para cambiar al taxi con placas " + requestedPlates + " fue aprobada. Ya puedes conectarte con tu nueva unidad."
                : "Hola " + name + ", tu solicitud para cambiar al taxi con placas " + requestedPlates + " no fue aprobada. Contacta al administrador si tienes dudas.";
        send(toEmail, approved ? "Tu cambio de taxi fue aprobado - NexoraTaxis" : "Tu cambio de taxi fue rechazado - NexoraTaxis", body);
    }

    private void send(String toEmail, String subject, String body) {
        if (!mailEnabled || toEmail == null || toEmail.isBlank()) {
            log.info("Correo omitido (mail deshabilitado o sin destinatario). Asunto: '{}' para '{}'", subject, toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("No se pudo enviar el correo '{}' a '{}'", subject, toEmail, e);
        }
    }
}
