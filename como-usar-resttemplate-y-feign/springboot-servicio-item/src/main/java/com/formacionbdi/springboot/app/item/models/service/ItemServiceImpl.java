package com.formacionbdi.springboot.app.item.models.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.formacionbdi.springboot.app.item.models.Producto;
import com.formacionbdi.springboot.app.item.models.Item;

@Service("serviceRestTemplate")
public class ItemServiceImpl implements ItemService {

	//  Utilizamos RestTemplate en las clases de Servicio inyectandolos con @Autowired
	@Autowired
	private RestTemplate clienteRest;
	
	@Override
	public List<Item> findAll() {
		// Aquí se está buscando todos los productos con el metodo getForObject
		// intentando conectarse con el microservicio Productos
		// el cual para este caso está recibiendo dos parametros
		// El primer parámetro es la URL, normalmente se indicaria como http://localhost:8001/listar
		// Para cuando se implementa ribbon, ya no es necesario especificar la ruta original,
		// sino en lugar de ello solo colocar el nombre del servicio al cual va a consultarse.
		// El segundo parámetro es lo que retornará, en este caso es un arreglo de Productos
		// Luego el arreglo se transforma a una lista de Productos
		List<Producto> productos = Arrays.asList(clienteRest.getForObject("http://servicio-productos/listar", Producto[].class));

		return productos.stream().map(p -> new Item(p, 1)).collect(Collectors.toList());
	}

	@Override
	public Item findById(Long id, Integer cantidad) {
		Map<String, String> pathVariables = new HashMap<String, String>();
		pathVariables.put("id", id.toString());
		// Aquí se está buscando un objeto por su identificador
		// Para ello se recibe un tercer parámetro adicional que es el pathVariables
		// el cual deberá ser un tipo Map con clave con el nombre del pathVariable y valor el que tenga en ese momento
		// puede ser más de un pathVariable ya que justamente por eso se está utilizando un Map
		Producto producto = clienteRest.getForObject("http://servicio-productos/ver/{id}", Producto.class, pathVariables);
		return new Item(producto, cantidad);
	}

}
