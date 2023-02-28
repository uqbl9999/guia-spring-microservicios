package com.formacionbdi.springboot.app.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
// Utilizamos la anotación @EntityScan que se utiliza para especificar el paquete donde se encuentran las entidades de JPA.
// De la libreria que estamos importando en maven que hemos hecho
@EntityScan({"com.formacionbdi.springboot.app.commons.usuarios.models.entity"})
public class SpringbootServicioUsuarioApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootServicioUsuarioApplication.class, args);
	}

}
