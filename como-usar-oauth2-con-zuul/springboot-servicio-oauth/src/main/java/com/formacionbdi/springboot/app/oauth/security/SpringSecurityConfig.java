package com.formacionbdi.springboot.app.oauth.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// Esta clase buscará definir tres cosas
// 1. Establecer el método de encriptación,
// 2. Establecer el userService que se usará
// 3. Establecer el manejador de error y éxito en la autenticación mediante el authenticationEventPublisher
@Configuration
public class SpringSecurityConfig {

    @Autowired
    private UserDetailsService usuarioService;

    @Autowired
    private AuthenticationEventPublisher eventPublisher;

    // El método authenticationManager es un método de configuración
    // que define cómo se manejarán las autenticaciones en la aplicación
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationManagerBuilder auth) throws Exception {
        // Aquí se construye con una instancia de AuthenticationManager
        // Se pasa una instancia del UserDetailsService
        // Se especifica un codificador de contraseña para encriptar y comparar las contraseñas de los usuarios
        return auth.userDetailsService(this.usuarioService)
                    .passwordEncoder(passwordEncoder())
                    .and()
                    .authenticationEventPublisher(eventPublisher)
                    .build();
    }

    // Se crea un bean ESTATICO para la encriptación de las contraseñas, recordar que debe ser estático el BCrypt
    @Bean
    public static BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
