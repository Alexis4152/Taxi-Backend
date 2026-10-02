package com.bitfx.taxi.security;

import com.bitfx.taxi.exception.ApiException;
import com.bitfx.taxi.model.Organization;
import com.bitfx.taxi.model.Role;
import com.bitfx.taxi.model.User;
import org.springframework.http.HttpStatus;

/**
 * Replica el patron TenantScope del POS de referencia: un ADMIN queda fijo a su propia
 * organizacion, mientras que un SUPER_ADMIN puede operar sobre cualquiera (scope null).
 */
public final class OrgScope {

    private OrgScope() {
    }

    public static Organization resolve(User actingUser) {
        if (actingUser.getRole() == Role.SUPER_ADMIN) {
            return null;
        }
        if (actingUser.getRole() == Role.ADMIN) {
            if (actingUser.getOrganization() == null) {
                throw new ApiException(HttpStatus.CONFLICT, "Tu usuario administrador no tiene una organizacion asignada");
            }
            return actingUser.getOrganization();
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "No tienes permiso para esta accion");
    }
}
