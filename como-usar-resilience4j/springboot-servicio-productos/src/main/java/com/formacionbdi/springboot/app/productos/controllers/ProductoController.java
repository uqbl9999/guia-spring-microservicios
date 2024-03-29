package com.formacionbdi.springboot.app.productos.controllers;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.formacionbdi.springboot.app.productos.models.entity.Producto;
import com.formacionbdi.springboot.app.productos.models.service.IProductoService;

@RestController
public class ProductoController {

	@Autowired
	private Environment env;

	@Value("${server.port}")
	private Integer port;

	@Autowired
	private IProductoService productoService;
	
	@GetMapping("/listar")
	public List<Producto> listar(){
		return productoService.findAll().stream().map(producto -> {
			producto.setPort(Integer.parseInt(env.getProperty("local.server.port")));
			//producto.setPort(port);
			return producto;
		}).collect(Collectors.toList());
	}
	
	@GetMapping("/ver/{id}")
	public Producto detalle(@PathVariable Long id) throws InterruptedException {
		// Esta modificación es para simular el uso del circuitBreaker con el id 10
		if (id.equals(10L)){
			throw new IllegalStateException("Producto no encontrado!");
		}

		// Esta modificación es para simular las llamadas lentas y los timeout con el id 7
		if (id.equals(7L)){
			TimeUnit.SECONDS.sleep(5L);
		}

		Producto producto = productoService.findById(id);
		producto.setPort(Integer.parseInt(env.getProperty("local.server.port")));
		//producto.setPort(port);

		// Se ha descomentado las siguientes lineas para poder realizar la prueba con el timeout
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException e) {
//			throw new RuntimeException(e);
//		}
		return productoService.findById(id);
	}

}
