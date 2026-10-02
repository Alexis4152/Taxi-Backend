package com.bitfx.taxi;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integracion real (MockMvc + contexto completo + base de datos real), a proposito
 * distinta de una prueba unitaria con mocks: /api/auth/refresh solo falla con un
 * LazyInitializationException cuando corre en una peticion HTTP separada, sin transaccion/sesion
 * de Hibernate abierta de por medio (exactamente lo que pasa al recargar la pagina en el
 * navegador). Un mock de User nunca reproduce eso porque no es un proxy real de Hibernate.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void refrescarLaSesionEnUnaPeticionSeparadaFuncionaSinLazyInitializationException() throws Exception {
        String phone = String.format("59%08d", System.currentTimeMillis() % 100000000L);
        String registerBody = objectMapper.writeValueAsString(new RegisterPayload("Integration Test", phone, "Password123!", phone + "@ejemplo.com", true));

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = registerResult.getResponse().getCookie("taxi_refresh_token");
        assertNotNull(refreshCookie, "El registro debe dejar la cookie de refresh (httpOnly)");

        // Peticion nueva e independiente, solo con la cookie (simula recargar la pagina: el
        // navegador ya no tiene el access token en memoria, solo la cookie).
        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.phone").value(phone));
    }

    private record RegisterPayload(String name, String phone, String password, String email, boolean acceptedTerms) {
    }
}
