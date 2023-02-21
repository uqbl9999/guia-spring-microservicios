package com.formacionbdi.springboot.app.item.controllers;

import java.util.List;

import com.formacionbdi.springboot.app.item.models.Producto;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.formacionbdi.springboot.app.item.models.Item;
import com.formacionbdi.springboot.app.item.models.service.ItemService;

@RestController
public class ItemController {
	
	@Autowired
	@Qualifier("serviceFeign")
	private ItemService itemService;
	
	@GetMapping("/listar")
	public List<Item> listar(){
		return itemService.findAll();
	}

	// El metodo a utilizar se deberá anotar con @HystrixCommand
	// Al cual se le indicará el metodo que ejecutará en caso que falle
	@HystrixCommand(fallbackMethod = "metodoAlternativo")
	@GetMapping("/ver/{id}/cantidad/{cantidad}")
	public Item detalle(@PathVariable Long id, @PathVariable Integer cantidad) {
		return itemService.findById(id, cantidad);
	}

	// Este método se ejecutará siempre que "detalle" falle
	// este método deberá recibir los mismos parámetros y devolver el mismo objeto que el metodo  que falló anteriormente (fallbackMethod)
	public Item metodoAlternativo(Long id, Integer cantidad) {
		Item item = new Item();
		Producto producto = new Producto();

		item.setCantidad(cantidad);
		producto.setId(id);
		producto.setNombre("camara sony");
		producto.setPrecio(500.00);
		item.setProducto(producto);

		return item;
	}
}
