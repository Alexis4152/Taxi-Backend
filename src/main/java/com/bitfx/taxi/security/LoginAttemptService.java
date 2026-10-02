package com.bitfx.taxi.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Limite simple de intentos de login en memoria (sin dependencias externas): bloquea un
 * telefono/usuario tras varios intentos fallidos seguidos durante una ventana corta.
 */
@Component
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_WINDOW_SECONDS = 300;

    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> lockedUntil = new ConcurrentHashMap<>();

    public void checkNotLocked(String key) {
        Instant until = lockedUntil.get(key);
        if (until != null) {
            if (Instant.now().isBefore(until)) {
                throw new IllegalStateException("Demasiados intentos fallidos. Intenta de nuevo en unos minutos.");
            }
            lockedUntil.remove(key);
            attempts.remove(key);
        }
    }

    public void onFailure(String key) {
        int count = attempts.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        if (count >= MAX_ATTEMPTS) {
            lockedUntil.put(key, Instant.now().plusSeconds(LOCK_WINDOW_SECONDS));
        }
    }

    public void onSuccess(String key) {
        attempts.remove(key);
        lockedUntil.remove(key);
    }
}
