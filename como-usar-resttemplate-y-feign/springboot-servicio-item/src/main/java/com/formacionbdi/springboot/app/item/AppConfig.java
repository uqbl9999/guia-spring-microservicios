package com.formacionbdi.springboot.app.item;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

	// Para utilizar RestTemplate, se debe crear el bean dentro de una clase de configuración
	// Por ejemplo aquí en AppConfig, se está creando el @Bean con el nombre de "clienteRest"
	// esta función retornará una instancia de RestTemplate
	// NOTA: la anotación @LoadBalanced sirve para aplicar el balanceo de carga de Ribbon a RestTemplete
	// para cuando este se use.
	@Bean("clienteRest")
	@LoadBalanced
	public RestTemplate registrarRestTemplate() {
		return new RestTemplate();
	}
}
