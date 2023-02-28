package com.formacionbdi.springboot.app.oauth.security.event;

import com.formacionbdi.springboot.app.commons.usuarios.models.entity.Usuario;
import com.formacionbdi.springboot.app.oauth.services.IUsuarioService;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessErrorHandler implements AuthenticationEventPublisher {

    private Logger log = LoggerFactory.getLogger(AuthenticationSuccessErrorHandler.class);

    @Autowired
    private IUsuarioService usuarioService;

    // Este método se ejecuta cuando el usuario se autentica correctamente
    // Permite hacer una verificación a los detalles del authentication
    // con la finalidad de que no se procesen peticiones que sean instancias de WebAuthenticationDetails
    // ya que esta ocurriendo que se está entrando a este método dos veces al intentar logearse
    // Luego se busca busca al usuario por id para resetear los intentos en caso haya mas de cero intentos
    // y que luego de esos haya conseguido logearse exitosamente
    @Override
    public void publishAuthenticationSuccess(Authentication authentication) {
        if(authentication.getDetails() instanceof WebAuthenticationDetails){
            return;
        }
        logUser(authentication);
        Usuario usuario = usuarioService.findByUsername(authentication.getName());
        resetAttempts(usuario);
    }

    private void logUser(Authentication authentication){
        UserDetails user = (UserDetails) authentication.getPrincipal();
        String mensaje = "Success Login: " + user.getUsername();
        System.out.println(mensaje);
        log.info(mensaje);
    }

    private void resetAttempts(Usuario usuario){
        if (usuario.getIntentos() != null && usuario.getIntentos() > 0) {
            usuario.setIntentos(0);
            log.info("Los intentos actual es de: " + usuario.getIntentos());
            usuarioService.update(usuario, usuario.getId());
        }
    }

    // Este método se ejecuta cuando el usuario no se autentica correctamente,
    // pero solo cuando se equivoca de contraseña es que puede entrar aqui
    // Dentro del método se verifica si el usuario es nulo
    // Luego se verifica el máximo de intentos,
    // si es mayor o igual a 3 entonces el usuario se deshabilita
    // por último se actualizan los datos en la base de datos del usuario
    // En caso haya algun error como que el usuario ingresa su contraseña incorrecta, se lanzara una FeignException
    @Override
    public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
        logMessageLogin(exception);
        try {
            Usuario usuario = usuarioService.findByUsername(authentication.getName());
            checkUserNull(usuario);
            logAttempts(usuario);
            checkMaxAttempts(usuario);
            usuarioService.update(usuario, usuario.getId());
        }catch (FeignException e){
            log.error(String.format("El usuario %s no existe en el sistema", authentication.getName()));
        }
    }

    private void logMessageLogin(AuthenticationException exception){
        String mensaje = "Error en el login: " + exception.getMessage();
        log.error(mensaje);
        System.out.println(mensaje);
    }

    private void checkUserNull(Usuario usuario){
        if (usuario.getIntentos() == null) {
            usuario.setIntentos(0);
        }
    }

    private void logAttempts(Usuario usuario){
        log.info("Los intentos actual es de: " + usuario.getIntentos());
        usuario.setIntentos(usuario.getIntentos() + 1);
        log.info("Los intentos después es de: " + usuario.getIntentos());
    }

    private void checkMaxAttempts(Usuario usuario){
        if (usuario.getIntentos() >= 3) {
            log.error(String.format("El usuario %s deshabilitado por maximos intentos.", usuario.getUsername()));
            usuario.setEnabled(false);
        }
    }
}
