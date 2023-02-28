package com.formacionbdi.springboot.app.usuarios;

import com.formacionbdi.springboot.app.commons.usuarios.models.entity.Usuario;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
// RepositoryRestConfigurer: es una interfaz de Spring Framework que proporciona métodos de configuración
// para personalizar la exposición de los recursos REST generados por Spring Data REST
public class RepositoryConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        // Cuando se envía una solicitud HTTP para obtener un objeto Usuario o Role, la respuesta incluirá el identificador único de ese objeto en la base de datos.
        config.exposeIdsFor(Usuario.class, Role.class);
    }
}
