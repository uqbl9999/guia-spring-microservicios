package com.formacionbdi.springboot.app.gateway.filters.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

// Se anota con @Component para indicar a spring que será un componente dentro del contenedor
// extiende AbstractGatewayFilterFactory para crear fábricas de filtros personalizados
@Component
public class EjemploGatewayFilterFactory extends AbstractGatewayFilterFactory<EjemploGatewayFilterFactory.Configuracion> {

    private Logger logger = LoggerFactory.getLogger(EjemploGatewayFilterFactory.class);

    // En el constructor pasar por el super la clase abstracta de configuración
    public EjemploGatewayFilterFactory() {
        super(Configuracion.class);
    }

    // Aqui se indica el orden de los campos en la configuración
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("mensaje", "cookieNombre", "cookieValor");
    }

    // con el metodo name() se indica el nombre que tendrá el filtro
    // Generalmente los nombrese se colocan asi <Nombre>GatewayFilterFactory
    // tomara lo que este dentro del diamante
    // si se quiere cambiar dicho valor entonces usar el método name()
    @Override
    public String name() {
        return "EjemploCookie";
    }

    // El método apply implementa una lógica personalizada en el pre y post-procesamiento de la solicitud entrante
    @Override
    public GatewayFilter apply(Configuracion config) {
        return (exchange, chain) -> {
            // En esta parte se define el preprocesamiento del filtro
            logger.info("ejecutando pre gateway filter factory: " + config.mensaje);

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                // En esta parte se define el postprocesamiento del filtro
                Optional.ofNullable(config.cookieValor).ifPresent(cookie -> {
                    exchange.getResponse().addCookie(ResponseCookie.from(config.cookieNombre, cookie).build());
                });

                logger.info("ejecutando post gateway filter factory: " + config.mensaje);
            }));
        };



//        return new OrderedGatewayFilter((exchange, chain) -> {
//            logger.info("ejecutando pre gateway filter factory: " + config.mensaje);
//            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
//
//                Optional.ofNullable(config.cookieValor).ifPresent(cookie -> {
//                    exchange.getResponse().addCookie(ResponseCookie.from(config.cookieNombre, cookie).build());
//                });
//
//                logger.info("ejecutando post gateway filter factory: " + config.mensaje);
//            }));
//        },2);
    }

    // Se define una subclase estática Configuracion,
    // que se utiliza para almacenar la configuración específica del filtro.
    // En este caso, la clase Configuracion tiene tres propiedades: mensaje, cookieValor y cookieNombre
    public static class Configuracion {
        private String mensaje;
        private String cookieValor;
        private String cookieNombre;

        public String getMensaje() {
            return mensaje;
        }

        public void setMensaje(String mensaje) {
            this.mensaje = mensaje;
        }

        public String getCookieValor() {
            return cookieValor;
        }

        public void setCookieValor(String cookieValor) {
            this.cookieValor = cookieValor;
        }

        public String getCookieNombre() {
            return cookieNombre;
        }

        public void setCookieNombre(String cookieNombre) {
            this.cookieNombre = cookieNombre;
        }
    }
}
