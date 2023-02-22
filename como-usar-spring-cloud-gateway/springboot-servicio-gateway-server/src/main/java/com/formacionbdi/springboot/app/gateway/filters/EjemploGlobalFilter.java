package com.formacionbdi.springboot.app.gateway.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;

//    Se define la anotación @Component para que Spring lo reconozca
//    como un componente que debe ser administrado por el contenedor de Spring.
//    Se define la clase EjemploGlobalFilter que implementa la interfaz GlobalFilter
//    y la interfaz Ordered. GlobalFilter es una interfaz que proporciona un punto de
//    extensión para el procesamiento de solicitudes y respuestas en Spring Cloud Gateway,
//    y Ordered es una interfaz que se utiliza para definir la orden de los filtros.
@Component
public class EjemploGlobalFilter implements GlobalFilter, Ordered {

    private final Logger logger = LoggerFactory.getLogger(EjemploGlobalFilter.class);

    @Override
    // Este método básicamente se define el filtro pre y post
    // lo que está antes del retorno del método es el filtro pre
    // lo que está en el retorno del método es el filtro post
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("Ejecutando filtro pre");
        // exchange representa la petición que llega al gateway y la respuesta que se enviará de regreso
        // así como modificar sus atributos y headers
        // Se utiliza mutate para modificar los headers ya que son inmutables
        exchange.getRequest().mutate().headers(httpHeaders -> httpHeaders.add("token", "123456"));

        // chain representa la cadena de filtros que se ejecutarán en orden para procesar la petición
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            logger.info("Ejecutando filtro post");
            // Aca se comprueba si el header token está presente en la solicitud.
            // En caso afirmativo, se agrega el mismo header a la respuesta
            Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("token")).ifPresent(valor -> {
                exchange.getResponse().getHeaders().add("token", valor);
            });
            // se agrega una cookie llamada color con el valor rojo a la respuesta
            exchange.getResponse().getCookies().add("color", ResponseCookie.from("color", "rojo").build());
            //exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
        }));
    }

    // Aqui se define el orden de ejecución del filtro respecto a otros
    @Override
    public int getOrder() {
        return 1;
    }
}
