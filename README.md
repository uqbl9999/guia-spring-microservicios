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

# Cómo usar Eureka Server con Microservicios

## Eureka Server
### En el pom.xml
```xml
<!-- Las dependencia utilizada es la de spring-cloud-starter-netflix-eureka-server -->
<!-- para eureka server -->
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
<!-- Es importante importar jaxb-runtime ya que eureka no lo incluye para versiondes de java 11 en adelante -->
<!-- Para versiones inferiores a la version 11, como la 8 por ejemplo, entonces ya no es necesario importar jaxb -->
<dependency>
	<groupId>org.glassfish.jaxb</groupId>
	<artifactId>jaxb-runtime</artifactId>
</dependency>
```

### En el application.properties
```yml
spring.application.name=servicio-eureka-server
server.port=8761

## Esta configuración permite indicar a eureka que este mismo módulo sera un cliente o no,
## en caso que implemente algún servicio
## como para este caso no se necesitara usar al servidor de eureka como cliente tambien
## entonces se coloca como falso
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
```

### En la clase principal de Spring
```java
@SpringBootApplication
// Basta con anotar la clase principal de spring con @EnableEurekaServer
@EnableEurekaServer
public class SpringbootServicioEurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringbootServicioEurekaServerApplication.class, args);
    }
}
```

## Eureka Client (Microservicio)
### En el pom.xml
```xml
<!-- Se debe importar la dependencia spring-cloud-starter-netflix-eureka-client -->
<!-- para manejarse como cliente de eureka -->
<!-- lo mismo deberá hacerse para cada microservicio que quiera registrarse como cliente -->
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### En el application.properties
```yml
spring.application.name=servicio-productos
## Para este microservicio se le indica que el puerto será dinamico con la variable ${PORT:0}
## ya que se necesitará varias instancias de este microservicio
server.port=${PORT:0}

## La siguiente linea es para indicar al cliente de eureka un nombre dinamico como nombre de instancia,
## esto solo aplica para los microservicios que queramos escalar o replicar
eureka.instance.instance-id=${spring.application.name}:${spring.application.instance_id:${random.value}}
## La siguiente linea es para indicar al microservicio en donde se encuentra el servidor de eureka
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
```

### En la clase principal de Spring
```java
@SpringBootApplication
// Con @EnableEurekaClient en la clase principal se indica que este microservicio será un cliente
// que tendrá que ser registrado en un servidor de Eureka
@EnableEurekaClient
public class SpringbootServicioProductosApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootServicioProductosApplication.class, args);
	}
}
```

# Cómo usar Hystrix y Hystrix con Ribbon

## Microservicio Items
### En el pom.xml
```xml
<!-- Hystrix tampoco es compatible con versiones de spring 2.4 en adelante -->
<!-- Para realizar lo mismo en versiones superiores se utiliza Resilience4J -->
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
</dependency>
```

### En el pom.xml
```yml
## Esta configuración se realiza para hystrix y ribbon
## Lo que pasa es que el metodo anotado con @HystrixCommand puede tardar mas del tiempo que puede soportar hystrix
## Por ello es necesario establecer los timeout
## Ribbon envuelve a hystrix, por tanto es necesario que se coloque el tiempo de hystrix mayor al tiempo de ribbon
## como se ven en las siguientes lineas: El tiempo de ribbon sumado es 13000ms que es menor a los 20000ms de hystrix
hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 20000
ribbon.ConnectTimeout: 3000
ribbon.ReadTimeout: 10000
```


### En la clase principal de spring
```java
@EnableFeignClients
@SpringBootApplication
@EnableEurekaClient
// Se debe anotar la clase principal de spring con @EnableCircuitBreaker
@EnableCircuitBreaker
@RibbonClient(name = "servicio-productos")
public class SpringbootServicioItemApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootServicioItemApplication.class, args);
	}

}
```

### En la clase controller
```java
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
```


## Microservicio Productos
### En el pom.xml
```java
@GetMapping("/ver/{id}")
public Producto detalle(@PathVariable Long id) {
	Producto producto = productoService.findById(id);
	//producto.setPort(Integer.parseInt(env.getProperty("local.server.port")));
	producto.setPort(port);
	// Se está simulando un tiempo de espera para probarlo con Hystrix y ribbon
	try {
		Thread.sleep(2000);
	} catch (InterruptedException e) {
		throw new RuntimeException(e);
	}
	return productoService.findById(id);
}
```
