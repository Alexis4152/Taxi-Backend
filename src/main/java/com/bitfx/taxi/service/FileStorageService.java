package com.bitfx.taxi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    // Por debajo de esto ya no sirve como foto/logo reconocible (y de paso descarta el caso real
    // que motivo esta validacion: un PNG de 1x1 subido por error que se ve como un cuadro solido).
    private static final int MIN_DIMENSION_PX = 32;

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo esta vacio");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Solo se permiten imagenes JPEG, PNG o WEBP");
        }
        validateIsRealImage(file);
        try {
            Path dir = Path.of(uploadsDir);
            Files.createDirectories(dir);
            String extension = switch (file.getContentType()) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };
            String filename = UUID.randomUUID() + extension;
            Path target = dir.resolve(filename);
            file.transferTo(target);
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el archivo", e);
        }
    }

    private void validateIsRealImage(MultipartFile file) {
        BufferedImage image;
        try {
            image = ImageIO.read(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo como imagen");
        }
        if (image == null) {
            throw new IllegalArgumentException("El archivo no es una imagen valida");
        }
        if (image.getWidth() < MIN_DIMENSION_PX || image.getHeight() < MIN_DIMENSION_PX) {
            throw new IllegalArgumentException("La imagen es demasiado pequena (minimo " + MIN_DIMENSION_PX + "x" + MIN_DIMENSION_PX + " px)");
        }
    }
}
