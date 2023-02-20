package com.formacionbdi.springboot.app.item.clientes;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.formacionbdi.springboot.app.item.models.Producto;

// Se debe crear primero una interface con los métodos que se conectarán al otro servicio
// normalmente en el name se tendría que colocar el nombre del servicio  como localhost:8001
// pero como se está usando Ribbon, entonces para este caso solo se usa el nombre del microservicio
@FeignClient(name = "servicio-productos")
public interface ProductoClienteRest {
	// Se definen los metodos con sus anotaciones respectivas como si fuera un controller
	// con la finalidad de indicarles a que ruta se quieren conectar del microservicio con el que se va a comunicar
	// y tambien se deberá definir bien el tipo de dato a devolver y los parametros de entrada
	@GetMapping("/listar")
	public List<Producto> listar();
	
	@GetMapping("/ver/{id}")
	public Producto detalle(@PathVariable Long id);

}
