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

@Service
public class UsuarioService implements UserDetailsService, IUsuarioService {

    private Logger log = LoggerFactory.getLogger(UsuarioService.class);

    @Autowired
    private UsuarioFeignClient client;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            return generatedUser(username);
        } catch (FeignException e) {
            log.error("Error en el login, no existe  el usuario '" + username + "' en el sistema");
            throw new UsernameNotFoundException("Error en el login, no existe el usuario '" + username + "' en el sistema");
        }
    }

    private User generatedUser(String username){
        Usuario usuario = client.findByUsername(username);
        validateUserPersonalized(usuario);
        List<GrantedAuthority> authorities = getAndTransformAuthorities(usuario);
        log.info("Usuario autenticado:" + username);
        return createSpringUserWithAuthorities(usuario, authorities);
    }

    private void validateUserPersonalized(Usuario usuario) {
        if (usuario == null) {
            log.error("Error en el login, no existe el usuario '" + usuario.getUsername() + "' en el sistema");
            throw new UsernameNotFoundException("Error en el login, no existe el usuario '" + usuario.getUsername() + "' en el sistema");
        }
    }

    private List<GrantedAuthority> getAndTransformAuthorities(Usuario usuario){
        return usuario.getRoles()
                        .stream()
                        .map(role -> new SimpleGrantedAuthority(role.getNombre()))
                        .peek(authority -> log.info("Role: " + authority.getAuthority()))
                        .collect(Collectors.toList());
    }

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

    @Override
    public Usuario findByUsername(String username) {
        return client.findByUsername(username);
    }

    @Override
    public Usuario update(Usuario usuario, Long id) {
        return client.update(usuario, id);
    }
}
