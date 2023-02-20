package com.formacionbdi.springboot.app.item;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

// Para comenzar a utilizar Ribbon, se deberá anotar con @RibbonClient en "singular",
// e indicar el nombre del servicio con el que se conetará que en este caso es "servicio-productos"
@RibbonClient(name = "servicio-productos")
@EnableFeignClients
@SpringBootApplication
public class SpringbootServicioItemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootServicioItemApplication.class, args);
	}

}
