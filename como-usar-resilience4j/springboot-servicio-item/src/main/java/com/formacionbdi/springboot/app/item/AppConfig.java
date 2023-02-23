package com.formacionbdi.springboot.app.item;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AppConfig {

	@Bean("clienteRest")
	@LoadBalanced
	public RestTemplate registrarRestTemplate() {
		return new RestTemplate();
	}

	// Se crea un @Bean para utilizar la configuración de resilience
	// Recordar que la configuracion en bean es de menor precedencia que un archivo properties
	@Bean
	public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer(){
	//	factory.configureDefault(id -> new Resilience4JConfigBuilder(id): Configura el CircuitBreakerFactory con una nueva instancia de Resilience4JConfigBuilder.
	//	El parámetro "id" es un identificador único para el CircuitBreaker,
	//	por ahi es donde se pasa el nombre de la instancia por ejemplo "items".
		return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
				// .circuitBreakerConfig(CircuitBreakerConfig.custom(): Configura la configuración del CircuitBreaker
				.circuitBreakerConfig(CircuitBreakerConfig.custom()
						// Establece que analizara cada grupo de 10 peticiones como muestra
						.slidingWindowSize(10)
						// Establece un umbral de taza de falla del 50% para el circuit breaker
						// Si falla mas del 50% de una muestra de 10 peticiones, entonces el circuito se abre
						.failureRateThreshold(50)
						// Establece la duración de espera en el estado abierto del CircuitBreaker en 10 segundos.
						.waitDurationInOpenState(Duration.ofSeconds(10))
						// Establece el número de llamadas permitidas en el estado semi-abierto del CircuitBreaker en 5.
						.permittedNumberOfCallsInHalfOpenState(5)
						// Establece el umbral de tasa de llamadas lentas en 50%.
						// Si más del 50% de una muestra de 10 llamadas se ponen lentas, entonces el circuito se abre
						.slowCallRateThreshold(50)
						// Establece la duración máxima de una las llamadas lentas en 2 segundos
						.slowCallDurationThreshold(Duration.ofSeconds(2L))
						.build())
				// Configura el TimeLimiter del CircuitBreaker.
				// Aquí se establece el tiempo máximo de espera para una llamada en 3 segundos
				// El TimeLimiter se utiliza para definir el tiempo máximo que una llamada
				// puede tardar antes de que se agote el tiempo y se interrumpa, es decir (timeout)
				.timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(3L)).build())
				.build());
	}
}
