package com.formacionbdi.springboot.app.usuarios.models.dao;

import com.formacionbdi.springboot.app.commons.usuarios.models.entity.Usuario;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

// La interfaz está anotada con @RepositoryRestResource que indica que esta interfaz expone
// un recurso REST para la entidad Usuario, con el nombre de ruta usuarios.
// Esa anotación da por defecto un CRUD completo usando la ruta del controlador como /usuarios
@RepositoryRestResource(path = "usuarios")
public interface UsuarioDao extends PagingAndSortingRepository<Usuario, Long> {

    // El método findByUsername que usa @RestResource para exponer el método como un
    // endpoint REST personalizado, que busca un usuario por su nombre de usuario.
    @RestResource(path = "buscar-username")
    public Usuario findByUsername(@Param("username") String username);

    @Query("select u from Usuario u where u.username = ?1")
    public Usuario obtenerPorUsername(String username);
}
