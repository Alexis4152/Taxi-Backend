package com.bitfx.taxi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableScheduling
public class TaxiAppBackendApplication {

	// Fija la zona horaria del JVM antes de que arranque Spring: todo el sistema usa
	// LocalDateTime.now() (viajes programados, turnos, tokens, etc.) y los contenedores de
	// Coolify/Hostinger arrancan en UTC, lo que desfasaba 6 horas la hora de Mexico.
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"));
	}

	public static void main(String[] args) {
		SpringApplication.run(TaxiAppBackendApplication.class, args);
	}

}
