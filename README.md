# Cómo usar RestTemplate y Feign

## RestTemplate
### En la clase de configuración
```java
// Para utilizar RestTemplate, se debe crear el bean dentro de una clase de configuración
// Por ejemplo aquí en AppConfig, se está creando el @Bean con el nombre de "clienteRest"
// esta función retornará una instancia de RestTemplate
// NOTA: la anotación @LoadBalanced sirve para aplicar el balanceo de carga de Ribbon a RestTemplete
// para cuando este se use.
@Bean("clienteRest")
@LoadBalanced
public RestTemplate registrarRestTemplate() {
    return new RestTemplate();
}
```

### En la clase Service
```java
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
```

## Feign
### En la clase principal de Spring
```java
@RibbonClient(name = "servicio-productos")
// Se debe anotar con @EnableFeignClients em la clase principal para empezar a usar feign
@EnableFeignClients
@SpringBootApplication
public class SpringbootServicioItemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootServicioItemApplication.class, args);
	}

}
```

### Interface que define los metodos que feign utilizará  
```java
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
```

### En la clase Servicio  
```java
@Service("serviceFeign")
public class ItemServiceFeign implements ItemService {

	// La clase de feing que se conecta al microservicio, se inyecta dentro del service
	@Autowired
	private ProductoClienteRest clienteFeign;
	// Por último se llama a los metodos necesarios para realizar las consultas
	@Override
	public List<Item> findAll() {
		return clienteFeign.listar().stream().map(p -> new Item(p, 1)).collect(Collectors.toList());
	}

	@Override
	public Item findById(Long id, Integer cantidad) {
		return new Item(clienteFeign.detalle(id), cantidad);
	}

}
```


# Cómo usar Ribbon

### Agregar la dependencia de Ribbon
```xml
<!-- Esta es la dependencia para Ribbon, considerar que ribbon ya no funciona en versiones de springboot 2.4 para adelante -->
<!--	Por ello este proyecto esta con una version 2.3.x	-->
<!--	A partir de la version 2.4 en adelante se utiliza Eureka y todo el ecosistema de springcloud	-->
<!--	NOTA: RECORDAR QUE AL MODIFICAR LA VERSION DE SPRING SE DEBE MODIFICAR TAMBIEN LA VERSION DE SPRINGCLOUD YA QUE VAN DE LA MANO	-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-ribbon</artifactId>
</dependency>
```

### En la clase principal de Spring
```java
// Para comenzar a utilizar Ribbon, se deberá anotar con @RibbonClient en "singular",
// e indicar el nombre del servicio con el que se conetará que en este caso es "servicio-productos"
@RibbonClient(name = "servicio-productos")
@EnableFeignClients
@SpringBootApplication
public class SpringbootServicioItemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootServicioItemApplication.class, args);
	}

}
```

### En el application.properties
```
## Se debe especificar que instancias y en que puertos estarán los microservicios con el que se conectará este proyecto
## recordar usar el mismo nombre que se colocó en el proyecto del microservicio productos
servicio-productos.ribbon.listOfServers=localhost:8001,localhost:9001
```
