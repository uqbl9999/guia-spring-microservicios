package com.formacionbdi.springboot.app.oauth.services;

import com.formacionbdi.springboot.app.commons.usuarios.models.entity.Usuario;
import com.formacionbdi.springboot.app.oauth.clients.UsuarioFeignClient;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

// Se implementa UserDetailsService para poder implementar un metodo que devuelve el User por defecto de Spring
// Se implementa IUsuarioService para poder hacer uso del método update
@Service
public class UsuarioService implements UserDetailsService, IUsuarioService {

    private Logger log = LoggerFactory.getLogger(UsuarioService.class);

    // Se inyecta un cliente Feign para hacer peticiones HTTP al servidor de usuarios,
    // que se supone que proporciona una API para acceder a los detalles de los usuarios
    @Autowired
    private UsuarioFeignClient client;

    // Este es el metodo que proviene de implementar UserDetailsService para cargar el user por el username
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            return generatedUser(username);
        } catch (FeignException e) {
            log.error("Error en el login, no existe  el usuario '" + username + "' en el sistema");
            throw new UsernameNotFoundException("Error en el login, no existe el usuario '" + username + "' en el sistema");
        }
    }

    // Este metodo permite generar el usuario
    private User generatedUser(String username){
        Usuario usuario = client.findByUsername(username);
        validateUserPersonalized(usuario);
        List<GrantedAuthority> authorities = getAndTransformAuthorities(usuario);
        log.info("Usuario autenticado:" + username);
        return createSpringUserWithAuthorities(usuario, authorities);
    }

    // Este es el método personalizado validateUserPersonalized()
    // que verifica si el usuario encontrado en la base de datos es nulo o no.
    // Si el usuario es nulo, se lanza una excepción UsernameNotFoundException.
    private void validateUserPersonalized(Usuario usuario) {
        if (usuario == null) {
            log.error("Error en el login, no existe el usuario '" + usuario.getUsername() + "' en el sistema");
            throw new UsernameNotFoundException("Error en el login, no existe el usuario '" + usuario.getUsername() + "' en el sistema");
        }
    }

    // Este es el método personalizado getAndTransformAuthorities()
    // que obtiene y transforma las autoridades del usuario (por ejemplo, roles)
    // en una lista de objetos GrantedAuthority
    private List<GrantedAuthority> getAndTransformAuthorities(Usuario usuario){
        return usuario.getRoles()
                        .stream()
                        .map(role -> new SimpleGrantedAuthority(role.getNombre()))
                        .peek(authority -> log.info("Role: " + authority.getAuthority()))
                        .collect(Collectors.toList());
    }

    // Este método crea la transforma el entity Usuario con los Authorities a un tipo User
    private User createSpringUserWithAuthorities(Usuario usuario, List<GrantedAuthority> authorities) {
        return new User(
                usuario.getUsername(),
                usuario.getPassword(),
                usuario.getEnabled(),
                true,
                true,
                true,
                authorities
        );
    }

    // Este método se utiliza para retornar el Usuario para la clase InfoAdicionalToken
    // Ya que dicha clase necesita algunos datos del usuario para agregar al token
    @Override
    public Usuario findByUsername(String username) {
        return client.findByUsername(username);
    }

    // Este metodo permite actualizar el usuario para cuando se hagan
    // las validaciones de los intentos validos en la clase de AuthenticationSuccessHandler
    @Override
    public Usuario update(Usuario usuario, Long id) {
        return client.update(usuario, id);
    }
}
