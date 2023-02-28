package com.formacionbdi.springboot.app.commons.usuarios;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// exclude = {DataSourceAutoConfiguration.class} excluye la configuración
// automática del origen de datos (DataSourceAutoConfiguration)
// de Spring Boot utilizando la anotación @SpringBootApplication.
// Esto indica que la aplicación no necesita una conexión a una base de datos y se puede ejecutar sin una
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SpringbootServicioUsuariosCommonsApplication {
	// Se quita lo que está en el interior del main
	public static void main(String[] args) {
	}
}
