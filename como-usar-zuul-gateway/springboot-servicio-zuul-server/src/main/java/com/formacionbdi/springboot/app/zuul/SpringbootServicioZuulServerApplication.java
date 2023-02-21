package com.formacionbdi.springboot.app.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

// El gateway debe ser anotado como cliente de eureka con @EnableEurekaClient
// Para poder iniciar zuul, este deberá ser anotado con @EnableZuulProxy
@SpringBootApplication
@EnableEurekaClient
@EnableZuulProxy
public class SpringbootServicioZuulServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootServicioZuulServerApplication.class, args);
	}

}
