package com.bitfx.taxi.controller;

import com.bitfx.taxi.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Este backend es solo una API: no sirve paginas para navegar directamente en el puerto 8081 (por
 * eso cualquier otra ruta responde "No autenticado" si no llevas un token). Esta raiz existe solo
 * para que entrar por error a la URL del API no se vea como un fallo, y para señalar donde esta la
 * aplicacion real (el frontend).
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ApiResponse<Map<String, String>> root() {
        return ApiResponse.ok(
                "NexoraTaxis API (BITFX). Este puerto es solo el backend; usa la aplicacion web (frontend) para operar el sistema.",
                Map.of("service", "taxiapp-backend", "status", "ok")
        );
    }
}
