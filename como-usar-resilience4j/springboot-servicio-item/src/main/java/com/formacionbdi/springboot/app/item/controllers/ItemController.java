package com.formacionbdi.springboot.app.item.controllers;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.formacionbdi.springboot.app.item.models.Producto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.web.bind.annotation.*;

import com.formacionbdi.springboot.app.item.models.Item;
import com.formacionbdi.springboot.app.item.models.service.ItemService;

@RestController
public class ItemController {

	private Logger logger = LoggerFactory.getLogger(ItemController.class);
	@Autowired
	private CircuitBreakerFactory cbFactory;
	@Autowired
	@Qualifier("serviceFeign")
	private ItemService itemService;
	
	@GetMapping("/listar")
	public List<Item> listar(
			@RequestParam(name = "nombre", required = false) String nombre,
			@RequestHeader(name = "token-request", required = false) String token
	){
		System.out.println("nombre = " + nombre);
		System.out.println("token-request = " + token);
		return itemService.findAll();
	}

	@GetMapping("/ver/{id}/cantidad/{cantidad}")
	public Item detalle(@PathVariable Long id, @PathVariable Integer cantidad) {
		// cbFactory.create("items") crea una instancia del Circuit Breaker
		// de Resilience4j con el identificador "items".
		return cbFactory.create("items")
				// ejecuta el código de la llamada al servicio de itemService dentro del circuito del Circuit Breaker.
				// Si la llamada al servicio es exitosa,
				// se devuelve el resultado al usuario final. Si la llamada al servicio falla,
				// se llama al método alternativo metodoAlternativo(id, cantidad, e)
				// en lugar de devolver un error al usuario final
				.run(() -> itemService.findById(id, cantidad), e -> metodoAlternativo(id, cantidad, e));
	}

	// Si se van a usar anotaciones,
	// quien aplica la configuración de las anotaciones es el applitacion.yml
	// En la anotación @CircuitBreaker se indica que se creará una instancia items
	// Si falla entonces se ejecutará el metodoAlternativo
	@CircuitBreaker(name = "items", fallbackMethod = "metodoAlternativo")
	@GetMapping("/ver2/{id}/cantidad/{cantidad}")
	public Item detalle2(@PathVariable Long id, @PathVariable Integer cantidad) {
		return itemService.findById(id, cantidad);
	}

	// En la anotación @CircuitBreaker se indica que se creará una instancia items
	// Si falla entonces se ejecutará el metodoAlternativo2
	// Recordar solo colocar como metodo fallBack en el @CircuitBreaker
	// esto también aplicará para el timeLimiter
	@CircuitBreaker(name = "items", fallbackMethod = "metodoAlternativo2")
	@TimeLimiter(name = "items")
	@GetMapping("/ver3/{id}/cantidad/{cantidad}")
	public CompletableFuture<Item> detalle3(@PathVariable Long id, @PathVariable Integer cantidad) {
		return CompletableFuture.supplyAsync(() -> itemService.findById(id, cantidad));
	}

	public Item metodoAlternativo(Long id, Integer cantidad, Throwable e) {
		logger.info(e.getMessage());
		Item item = new Item();
		Producto producto = new Producto();

		item.setCantidad(cantidad);
		producto.setId(id);
		producto.setNombre("camara sony");
		producto.setPrecio(500.00);
		item.setProducto(producto);

		return item;
	}

	public CompletableFuture<Item> metodoAlternativo2(Long id, Integer cantidad, Throwable e) {
		logger.info(e.getMessage());
		Item item = new Item();
		Producto producto = new Producto();

		item.setCantidad(cantidad);
		producto.setId(id);
		producto.setNombre("Camara sony");
		producto.setPrecio(500.00);
		item.setProducto(producto);

		return CompletableFuture.supplyAsync(() -> item);
	}
}
