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

### En el application.properties
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
### En el controlador
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

# Cómo usar Zuul Gateway

## Zuul server
### En el pom.xml
```xml
<!-- Se utiliza esta dependencia para zuul -->
<!-- Recordar que Zuul no se puede usar en versiones de spring 2.4 en adelante -->
<dependency>
	<groupId>org.springframework.cloud</groupId>
	<artifactId>spring-cloud-starter-netflix-zuul</artifactId>
</dependency>
```

### En el application.properties
```yml
spring.application.name=servicio-zuul-server
server.port=8090

eureka.client.service-url.defaultZone=http://localhost:8761/eureka/

## La siguiente configuración es para definir el nombre del microservicio al que se conectará y las rutas base
## por ejemplo si se quiere consultar el listado de items ahora será así
## http://localhost:8090/api/items/listar
## anteponiendo el path definido en este properties
zuul.routes.productos.service-id=servicio-productos
zuul.routes.productos.path=/api/productos/**

zuul.routes.items.service-id=servicio-items
zuul.routes.items.path=/api/items/**

```


### En la clase del filtro Pre
```java
// Esta clase se está definiendo como un filtro de Zuul
// Para ello se anotará con @Component y luego extendará de ZuulFilter
// La clase padre pedirá implementar 4 métodos
// filterType() sirve para definir si el filtro será "pre" o "post" cambiando el retorno
// filterOrder() sirve para indicar el orden del filtro
// shouldFilter() sirve para indicar si el filtro debe ejecutarse,
// ahi dentro se puede poner alguna lógica para indicar en que momento ejecutar el filtro
// En caso de que se cumpla con esa lógica interna retornar un booleano,
// si se quiere ejecutar siempre el filtro entonces retornar true
// run() sirve para colocar la lógica principal del filtro con ayuda de un RequestContext
// Para el filtro POST simplemente retornar en el filterType() "post"
@Component
public class PreTiempoTranscurridoFilter extends ZuulFilter {

    private static Logger log = LoggerFactory.getLogger(PreTiempoTranscurridoFilter.class);

    @Override
    public String filterType() {
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {

        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        log.info(String.format("%s request enrutado a %s", request.getMethod(), request.getRequestURL().toString()));
        Long tiempoInicio = System.currentTimeMillis();
        request.setAttribute("tiempoInicio", tiempoInicio);

        return null;
    }
}
```


# Cómo usar Zuul Gateway con un timeout (de ribbon)

## Zuul server
### En el application.properties
```yml
## Se deberá colocar la configuración que estaba en el application.properties del microservicio items
hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 20000
ribbon.ConnectTimeout: 3000
ribbon.ReadTimeout: 10000
```


## Microservicio Items
### En el application.properties
```yml
## Se deberá descomentar la configuración que estaba ahi y mostrarse así
hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 20000
ribbon.ConnectTimeout: 3000
ribbon.ReadTimeout: 10000
```

## Microservicio Productos
### Controlador
```java
@GetMapping("/ver/{id}")
public Producto detalle(@PathVariable Long id) {
	Producto producto = productoService.findById(id);
	producto.setPort(Integer.parseInt(env.getProperty("local.server.port")));
	//producto.setPort(port);

	// Se ha descomentado las siguientes lineas para poder realizar la prueba con el timeout
	try {
		Thread.sleep(2000);
	} catch (InterruptedException e) {
		throw new RuntimeException(e);
	}
	return productoService.findById(id);

}
```

# Cómo usar Spring Cloud Gateway

## Uso de gateway con filtro global
### En el pom.xml
```xml
<!-- Importar la dependencia de spring cloud gateway -->
<!-- Recordar que spring cloud gateway a diferencia de zuul funciona reactivamente -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

### En el application.yml
```yml
#  spring: es la raíz de la configuración de Spring.
#  cloud: indica que es una configuración específica para aplicaciones en la nube.
#  gateway: indica que se está configurando Spring Cloud Gateway.
#  routes: especifica las rutas que se deben configurar.
#  En las siguientes líneas se definen dos rutas:
#
#  servicio-productos: es el identificador de la ruta. Se utilizará para hacer referencia a esta ruta en otras partes de la configuración.
#  uri: indica la dirección del servicio al que se está redirigiendo. lb://servicio-productos significa que se está utilizando un balanceador de carga para redirigir la solicitud al servicio "servicio-productos".
#  predicates: se utiliza para definir las condiciones que deben cumplirse para que una solicitud sea enrutada a esta ruta. En este caso, las condiciones son:
#  Path=/api/productos/**: la solicitud debe tener la ruta /api/productos seguido de cualquier número de sub-rutas.
#  Header=token, \d+: el encabezado de la solicitud debe tener una clave llamada token y su valor debe ser un número de uno o más dígitos.
#  Header=Content-Type,application/json: el encabezado de la solicitud debe tener una clave llamada Content-Type y su valor debe ser application/json.
#  Method=GET, POST: la solicitud debe ser una solicitud GET o POST.
#  Query=color, verde: el parámetro de consulta debe ser color y su valor debe ser verde.
#  Cookie=color, azul: la cookie debe tener una clave llamada color y su valor debe ser azul.
#  filters: se utiliza para definir los filtros que se deben aplicar a la solicitud antes de que se enrutada a la ruta. En este caso, los filtros son:
#  StripPrefix=2: elimina los dos primeros segmentos de la ruta (/api/productos).
#  EjemploCookie=Hola mi mensaje personalizado, usuario, Burandori: agrega una cookie llamada EjemploCookie con un valor de Hola mi mensaje personalizado y dos atributos adicionales usuario y Burandori.
#  La segunda ruta (servicio-items) es similar, pero tiene diferentes condiciones y filtros. En este caso, solo hay una condición (Path=/api/items/**) y cuatro filtros:
#
#  StripPrefix=2: elimina los dos primeros segmentos de la ruta (/api/items).
#  AddRequestHeader=token-request, 123456: agrega un encabezado de solicitud llamado token-request con un valor de 123456.
#  AddResponseHeader=token-response, 12345678: agrega un encabezado de respuesta llamado token-response con un valor de 12345678.
#  SetResponseHeader=Content-Type, text/plain: establece el encabezado de respuesta Content-Type a text/plain.
#  AddRequestParameter=nombre, burandori: agrega un parámetro de consulta llamado nombre con un valor de burandori.

spring:
  cloud:
    gateway:
      routes:
        - id: servicio-productos
          uri: lb://servicio-productos
          predicates:
            - Path=/api/productos/**
#            - Header=token, \d+
#            - Header=Content-Type,application/json
#            - Method=GET, POST
#            - Query=color, verde
#            - Cookie=color, azul
          filters:
            - StripPrefix=2
            - EjemploCookie=Hola mi mensaje personalizado, usuario, Burandori
        - id: servicio-items
          uri: lb://servicio-items
          predicates:
            - Path=/api/items/**
          filters:
            - StripPrefix=2
            # Otras cabeceras predeterminadas
            - AddRequestHeader=token-request, 123456
            - AddResponseHeader=token-response, 12345678
            - SetResponseHeader=Content-Type, text/plain
            - AddRequestParameter=nombre, burandori

## OTRA FORMA DE DEFINIR EL mensaje, cookieValor, cookieNombre
#spring:
#  cloud:
#    gateway:
#      routes:
#        - id: servicio-productos
#          uri: lb://servicio-productos
#          predicates:
#            - Path=/api/productos/**
#          filters:
#            - StripPrefix=2
#            - name: Ejemplo
#              args:
#                mensaje: Hola mi mensaje personalizado
#                cookieValor: usuario
#                cookieNombre: BrandonLee
#        - id: servicio-items
#          uri: lb://servicio-items
#          predicates:
#            - Path=/api/items/**
#          filters:
#            - StripPrefix=2
```


### En la clase principal de spring
```java
@SpringBootApplication
// Se deberá anotar también como cliente de eureka con @EnableEurekaClient
@EnableEurekaClient
public class SpringbootServicioGatewayServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootServicioGatewayServerApplication.class, args);
	}

}
```


### En la clase del filtro global
```java
//    Se define la anotación @Component para que Spring lo reconozca
//    como un componente que debe ser administrado por el contenedor de Spring.
//    Se define la clase EjemploGlobalFilter que implementa la interfaz GlobalFilter
//    y la interfaz Ordered. GlobalFilter es una interfaz que proporciona un punto de
//    extensión para el procesamiento de solicitudes y respuestas en Spring Cloud Gateway,
//    y Ordered es una interfaz que se utiliza para definir la orden de los filtros.
@Component
public class EjemploGlobalFilter implements GlobalFilter, Ordered {

    private final Logger logger = LoggerFactory.getLogger(EjemploGlobalFilter.class);

    @Override
    // Este método básicamente se define el filtro pre y post
    // lo que está antes del retorno del método es el filtro pre
    // lo que está en el retorno del método es el filtro post
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("Ejecutando filtro pre");
        // exchange representa la petición que llega al gateway y la respuesta que se enviará de regreso
        // así como modificar sus atributos y headers
        // Se utiliza mutate para modificar los headers ya que son inmutables
        exchange.getRequest().mutate().headers(httpHeaders -> httpHeaders.add("token", "123456"));

        // chain representa la cadena de filtros que se ejecutarán en orden para procesar la petición
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            logger.info("Ejecutando filtro post");
            // Aca se comprueba si el header token está presente en la solicitud.
            // En caso afirmativo, se agrega el mismo header a la respuesta
            Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("token")).ifPresent(valor -> {
                exchange.getResponse().getHeaders().add("token", valor);
            });
            // se agrega una cookie llamada color con el valor rojo a la respuesta
            exchange.getResponse().getCookies().add("color", ResponseCookie.from("color", "rojo").build());
            //exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
        }));
    }

    // Aqui se define el orden de ejecución del filtro respecto a otros
    @Override
    public int getOrder() {
        return 1;
    }
}
```

## Uso de gateway con filtro global
### Clase del filtro personalizado, generalmente sirven para filtros orientados a un servicio en especifico
```java
// Se anota con @Component para indicar a spring que será un componente dentro del contenedor
// extiende AbstractGatewayFilterFactory para crear fábricas de filtros personalizados
@Component
public class EjemploGatewayFilterFactory extends AbstractGatewayFilterFactory<EjemploGatewayFilterFactory.Configuracion> {

    private Logger logger = LoggerFactory.getLogger(EjemploGatewayFilterFactory.class);

    // En el constructor pasar por el super la clase abstracta de configuración
    public EjemploGatewayFilterFactory() {
        super(Configuracion.class);
    }

    // Aqui se indica el orden de los campos en la configuración
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("mensaje", "cookieNombre", "cookieValor");
    }

    // con el metodo name() se indica el nombre que tendrá el filtro
    // Generalmente los nombrese se colocan asi <Nombre>GatewayFilterFactory
    // tomara lo que este dentro del diamante
    // si se quiere cambiar dicho valor entonces usar el método name()
    @Override
    public String name() {
        return "EjemploCookie";
    }

    // El método apply implementa una lógica personalizada en el pre y post-procesamiento de la solicitud entrante
    @Override
    public GatewayFilter apply(Configuracion config) {
        return (exchange, chain) -> {
            // En esta parte se define el preprocesamiento del filtro
            logger.info("ejecutando pre gateway filter factory: " + config.mensaje);

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                // En esta parte se define el postprocesamiento del filtro
                Optional.ofNullable(config.cookieValor).ifPresent(cookie -> {
                    exchange.getResponse().addCookie(ResponseCookie.from(config.cookieNombre, cookie).build());
                });

                logger.info("ejecutando post gateway filter factory: " + config.mensaje);
            }));
        };



//        return new OrderedGatewayFilter((exchange, chain) -> {
//            logger.info("ejecutando pre gateway filter factory: " + config.mensaje);
//            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
//
//                Optional.ofNullable(config.cookieValor).ifPresent(cookie -> {
//                    exchange.getResponse().addCookie(ResponseCookie.from(config.cookieNombre, cookie).build());
//                });
//
//                logger.info("ejecutando post gateway filter factory: " + config.mensaje);
//            }));
//        },2);
    }

    // Se define una subclase estática Configuracion,
    // que se utiliza para almacenar la configuración específica del filtro.
    // En este caso, la clase Configuracion tiene tres propiedades: mensaje, cookieValor y cookieNombre
    public static class Configuracion {
        private String mensaje;
        private String cookieValor;
        private String cookieNombre;

        /**SETS AND GETS**/
    }
}

```

# Cómo usar Resilience4j

## Microservicio productos 
### En la clase controller
```java
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
```


## Microservicio items 
### En el pom.xml
```xml
<!--	Se ha está importando circuitbreaker-resilience4j	-->
<!--	Recordar que este es compatible desde la version 2.4 en	adelante	-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

### Configuración 1 en la clase AppConfig
```java
// Se crea un @Bean para utilizar la configuración de resilience
// Recordar que la configuracion en bean es de menor precedencia que un archivo properties
@Bean
public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer(){
//	factory.configureDefault(id -> new Resilience4JConfigBuilder(id): Configura el CircuitBreakerFactory con una nueva instancia de Resilience4JConfigBuilder.
//	El parámetro "id" es un identificador único para el CircuitBreaker.
//	por ahi es donde se pasa el nombre de la instancia por ejemplo "items".
    return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
            // .circuitBreakerConfig(CircuitBreakerConfig.custom(): Configura la configuración del CircuitBreaker
            .circuitBreakerConfig(CircuitBreakerConfig.custom()
                    // Establece que analizara cada grupo de 10 peticiones como muestra
                    .slidingWindowSize(10)
                    // Establece un umbral de taza de falla del 50% para el circuit breaker
                    // Si falla mas del 50% de una muestra de 10 peticiones, entonces el circuito se abre
                    .failureRateThreshold(50)
                    // Establece la duración de espera en el estado abierto del CircuitBreaker en 10 segundos.
                    .waitDurationInOpenState(Duration.ofSeconds(10))
                    // Establece el número de llamadas permitidas en el estado semi-abierto del CircuitBreaker en 5.
                    .permittedNumberOfCallsInHalfOpenState(5)
                    // Establece el umbral de tasa de llamadas lentas en 50%.
                    // Si más del 50% de una muestra de 10 llamadas se ponen lentas, entonces el circuito se abre
                    .slowCallRateThreshold(50)
                    // Establece la duración máxima de una las llamadas lentas en 2 segundos
                    .slowCallDurationThreshold(Duration.ofSeconds(2L))
                    .build())
            // Configura el TimeLimiter del CircuitBreaker.
            // Aquí se establece el tiempo máximo de espera para una llamada en 3 segundos
            // El TimeLimiter se utiliza para definir el tiempo máximo que una llamada
            // puede tardar antes de que se agote el tiempo y se interrumpa, es decir (timeout)
            .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(3L)).build())
            .build());
}
```

### Clase controller 
```java
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
```

### Configuración 2 en la clase application.yml (Esta tiene mayor prioridad que una configuracion en una clase)
```yml
resilience4j:
  # circuitbreaker: sección define la configuración de CircuitBreaker.
  circuitbreaker:
    configs:
      # defecto: define la configuración del CircuitBreaker llamada "defecto".
      defecto:
        # Establece que analizara cada grupo de 6 peticiones como muestra
        sliding-window-size: 6
        # Establece un umbral de taza de falla del 50% para el circuit breaker
        # Si falla mas del 50% de una muestra de 10 peticiones, entonces el circuito se abre
        failure-rate-threshold: 50
        # Establece la duración de espera en el estado abierto del CircuitBreaker en 20 segundos.
        wait-duration-in-open-state: 20s
        # Establece el número de llamadas permitidas en el estado semi-abierto del CircuitBreaker en 4.
        permitted-number-of-calls-in-half-open-state: 4
        # Establece el umbral de tasa de llamadas lentas en 50%.
        # Si más del 50% de una muestra de 6 llamadas se ponen lentas, entonces el circuito se abre
        slow-call-rate-threshold: 50
        # Establece la duración máxima de una las llamadas lentas en 2 segundos
        slow-call-duration-threshold: 2s
    # instances: sección define las instancias de CircuitBreaker.
    # En este caso, hay una instancia llamada "items" que se basa en la configuración "defecto".
    # Recordar que si se quiere probar la llamada lenta entonces, el valor de la llamada lenta deberá ser menor al del timelimiter
    instances:
      items:
        base-config: defecto
  # timelimiter: sección define la configuración del TimeLimiter.
  timelimiter:
    configs:
      # defecto: define la configuración del TimeLimiter llamada "defecto"
      defecto:
        # timeout-duration: 2s establece la duración máxima permitida para una llamada en 2 segundos.
        timeout-duration: 2s
    # instances: sección define las instancias de TimeLimiter. 
    # En este caso, hay una instancia llamada "items" que se basa en la configuración "defecto".
    # Recordar que si se quiere probar la llamada lenta entonces, el valor de la llamada lenta deberá ser menor al del timelimiter
    instances:
      items:
        base-config: defecto
```


### Clase controller, para método con anotación @CircuitBreaker
```java
// Si se van a usar anotaciones,
// quien aplica la configuración de las anotaciones es el applitacion.yml
// En la anotación @CircuitBreaker se indica que se creará una instancia items
// Si falla entonces se ejecutará el metodoAlternativo
@CircuitBreaker(name = "items", fallbackMethod = "metodoAlternativo")
@GetMapping("/ver2/{id}/cantidad/{cantidad}")
public Item detalle2(@PathVariable Long id, @PathVariable Integer cantidad) {
    return itemService.findById(id, cantidad);
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
```

### Clase controller, para método con anotación @CircuitBreaker y @TimeLimiter
```java
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
```

# Cómo usar Resilience4j y spring cloud gateway

## Gateway 
### En el pom.xml
```xml
<!--	Para ello se necesita la dependencia de resilience4j pero reactiva, es decir reactor-resilience4j	-->
<!--	ya que spring cloud gateway es reactivo		-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

### En el application.yml
```yml
# El ejemplo se realizará con el microservicio productos
# Se debe pasar crear en el gateway la configuracion para el circuitbreaker
resilience4j:
  circuitbreaker:
    configs:
      defecto:
        sliding-window-size: 6
        failure-rate-threshold: 50
        wait-duration-in-open-state: 20s
        permitted-number-of-calls-in-half-open-state: 4
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
    instances:
      productos:
        base-config: defecto
  timelimiter:
    configs:
      defecto:
        timeout-duration: 2s
    instances:
      productos:
        base-config: defecto


spring:
  cloud:
    gateway:
      routes:
        - id: servicio-productos
          uri: lb://servicio-productos
          predicates:
            - Path=/api/productos/**
          filters:
            # Se indica que se usara en el filtro el CircuitBreaker
            # con argumentos como nombre de la instancia del circuibreaker que es productos
            # que cuando se de un error 500, entonces se redirigirá a la ruta que se especifica en el fallbackuri
            - name: CircuitBreaker
              args:
                name: productos
                statusCodes: 500
                fallbackUri: forward:/api/items/ver/9/cantidad/5
            - StripPrefix=2
            - EjemploCookie=Hola mi mensaje personalizado, usuario, Burandori
        - id: servicio-items
          uri: lb://servicio-items
          predicates:
            - Path=/api/items/**
          filters:
            - StripPrefix=2
            - AddRequestHeader=token-request, 123456
            - AddResponseHeader=token-response, 12345678
            - SetResponseHeader=Content-Type, text/plain
            - AddRequestParameter=nombre, burandori
```

# Cómo usar Spring cloud config server

## Config Server 
### En el pom.xml
```xml
<!--	Agregar la siguiente dependencia al proyecto de spring cloud server config		-->
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-config-server</artifactId>
</dependency>
```

### En el application.properties
```yml
spring.application.name=config-server
server.port:8888

# De esta manera se puede configurar de manera local el servidor de configuración
spring.cloud.config.server.git.uri=file:///C:/Users/bluq1/Desktop/config

# De esta manera se puede configurar de manera online el servidor de configuración
#spring.cloud.config.server.git.uri=https://github.com/andresguzf/servicio-items-config.git
```

### En la clase principal de spring
```java
// Se anotará con @EnableConfigServer en la clase principal de spring
@EnableConfigServer
@SpringBootApplication
public class SpringbootServicioConfigServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootServicioConfigServerApplication.class, args);
	}

}
```

### La ruta C:/Users/bluq1/Desktop/config se debe inicializar como un repositorio git, además se crearon los siguientes archivos con el siguiente contenido
```
# En servicio-items.properties
server.port=8005
configuracion.texto=Configurando ambiente por defecto

# En servicio-items-dev.properties
configuracion.texto=Configurando ambiente de desarrollo
configuracion.autor.nombre=Brandon
configuracion.autor.email=brandon@correo.com

# En servicio-items-prod.properties
server.port=8007
configuracion.texto=Configurando ambiente de Producción
```




## Microservicio Items 
### En el pom.xml
```xml
<!--	Con esta dependencia se indica que el microservicio será un cliente de spring cloud config server 	-->
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
<!--	Con esta dependencia se trae la dependencia de actuator 	-->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### En el bootstrap.properties
```yml
# Se debe crear un archivo bootstra.properties
# Especificando el nombre de este microservicio
spring.application.name= servicio-items
# spring.profiles.active: esta propiedad se utiliza para especificar los perfiles activos de la aplicación.
# Los perfiles activos se utilizan para cargar diferentes configuraciones de
# la aplicación en función del entorno de ejecución.
# En este caso, el perfil activo es dev.
spring.profiles.active=dev
# spring.cloud.config.uri: esta propiedad se utiliza para especificar la URL del servidor de configuración remoto.
# Spring Cloud Config es un servidor de configuración remoto que se utiliza para centralizar
# la configuración de varias aplicaciones y servicios.
# En este caso, el servidor de configuración está en la URL http://localhost:8888.
spring.cloud.config.uri=http://localhost:8888
# management.endpoints.web.exposure.include:
# esta propiedad se utiliza para especificar los puntos finales que se deben exponer en la aplicación.
# Spring Actuator proporciona varios puntos finales para monitorear y administrar una aplicación,
# como la información de estado, las métricas y más.
# En este caso, se están exponiendo todos los puntos finales con el asterisco (*).
management.endpoints.web.exposure.include=*
```


### En el controlador de items
```java
// La anotación @RefreshScope de Spring se utiliza para habilitar
// la actualización dinámica de las propiedades de configuración de una aplicación
// en tiempo de ejecución. Esto significa que, si se realiza un cambio en la configuración
// en el servidor de configuración y se envía una solicitud POST al punto final /actuator/refresh,
// las instancias de los beans se actualizan automáticamente con las nuevas
// propiedades de configuración sin necesidad de reiniciar la aplicación.
// Por lo tanto, se recomienda utilizar la anotación @RefreshScope en los componentes
// de la aplicación que dependen de las propiedades de configuración y que deben
// actualizarse automáticamente en caso de que se realice un cambio en la configuración.
@RefreshScope
@RestController
public class ItemController {
	
	private static Logger log = LoggerFactory.getLogger(ItemController.class);
	
	@Autowired
	private Environment env;
	
	@Value("${configuracion.texto}")
	private String texto;

	// Con este método se realiza la prueba del servidor de configuración por ejemplo comunicandose al
	// http://localhost:8005/obtener-config
	@GetMapping("/obtener-config")
	public ResponseEntity<?> obtenerConfig(@Value("${server.port}") String puerto){
		
		log.info(texto);
		
		Map<String, String> json = new HashMap<>();
		json.put("texto", texto);
		json.put("puerto", puerto);
		
		if(env.getActiveProfiles().length>0 && env.getActiveProfiles()[0].equals("dev")) {
			json.put("autor.nombre", env.getProperty("configuracion.autor.nombre"));
			json.put("autor.email", env.getProperty("configuracion.autor.email"));
		}
		
		return new ResponseEntity<Map<String, String>>(json, HttpStatus.OK);
	}
}

```

# Creando una libreria commons

## Libreria Usuario Commons 

##### En esta librería se considera las entidades para la seguridad

### En el pom.xml
```xml
<!--	Agregar la dependencia JPA ya que se usará para las entidades 	-->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```


### En la clase principal de spring
```java
// exclude = {DataSourceAutoConfiguration.class} excluye la configuración
// automática del origen de datos (DataSourceAutoConfiguration)
// de Spring Boot utilizando la anotación @SpringBootApplication.
// Esto indica que la aplicación no necesita una conexión a una base de datos y se puede ejecutar sin una
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SpringbootServicioUsuariosCommonsApplication {
	// Se quita lo que está en el interior del main
	public static void main(String[] args) {
	}
}
```

### Se crea la clase entidad usuario
```java
@Entity
@Table(name = "usuarios")
public class Usuario implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String username;

    @Column(length = 60)
    private String password;

    private Boolean enabled;
    private String nombre;
    private String apellido;

    @Column(unique = true, length = 100)
    private String email;

    private Integer intentos;

    // @ManyToMany(fetch = FetchType.LAZY): indica que la relación entre la entidad Usuario
    // y la entidad Role es de muchos a muchos, y que la carga de los datos se hará de forma lazy (perezosa).
    // @JoinTable(name = "usuarios_to_roles", joinColumns = @JoinColumn(name = "user_id"),
    // inverseJoinColumns = @JoinColumn(name = "role_id"), uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "role_id"})}):
    // especifica la tabla de unión que se utilizará para almacenar la relación entre las entidades Usuario y Role,
    // así como las columnas de unión y la restricción UNIQUE.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuarios_to_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"),
            uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "role_id"})}
    )
    private List<Role> roles;


    private static final long serialVersionUID = 4002221912401133094L;

}
```

### Se crea la clase entidad role
```java
@Entity
@Table(name = "roles")
public class Role implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(unique = true, length = 30)
    private String nombre;

    private static final long serialVersionUID = 4467531611801172710L;
}
```



## Microservicio usuarios 
#### En este microservicio se define el mecanismo de recuperación de usuarios mediante un dao
### En el pom.xml
```xml
<!--		Importamos la dependencia commons que tiene las clases de usuarios y roles		-->
<dependency>
  <groupId>com.formacionbdi.springboot.app.commons.usuarios</groupId>
  <artifactId>springboot-servicio-usuarios-commons</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <scope>runtime</scope>
  <optional>true</optional>
</dependency>
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-rest</artifactId>
</dependency>
```


### En el application.properties
```yml
spring.application.name=servicio-usuarios
server.port=${PORT:0}
eureka.instance.instance-id=${spring.application.name}:${spring.application.instance_id:${random.value}}
eureka.client.service-url.defaultZone=http://localhost:8761/eureka
logging.level.org.hibernate.SQL=debug
```


### En la clase principal de spring 
```java
@SpringBootApplication
// Utilizamos la anotación @EntityScan que se utiliza para especificar el paquete donde se encuentran las entidades de JPA.
// De la libreria que estamos importando en maven que hemos hecho
@EntityScan({"com.formacionbdi.springboot.app.commons.usuarios.models.entity"})
public class SpringbootServicioUsuarioApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringbootServicioUsuarioApplication.class, args);
	}

}
```

### En la clase dao de usuarios 
```java
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
```



### En la clase RepositoryConfig
#### Aquí se busca recuperar exponer los identificadores de Usuario y Role en el cuerpo de la respuesta  
```java
@Configuration
// RepositoryRestConfigurer: es una interfaz de Spring Framework que proporciona métodos de configuración
// para personalizar la exposición de los recursos REST generados por Spring Data REST
public class RepositoryConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        // Cuando se envía una solicitud HTTP para obtener un objeto Usuario o Role, la respuesta incluirá el identificador único de ese objeto en la base de datos.
        config.exposeIdsFor(Usuario.class, Role.class);
    }
}
```



## Microservicio Oauth 

### En el pom.xml
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <scope>runtime</scope>
  <optional>true</optional>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
<!--	Importamos la dependencia commons que tiene las clases de usuarios y roles		-->
<!--	Además exluimos la dependencia de JPA ya que se autoconfigurando por la libreria commons	-->
<!--	Otra forma de hacerlo como @SpringBootApplication(exclude = {DataSourceAutoConfiguration.class}) es como se muestra -->
<dependency>
  <groupId>com.formacionbdi.springboot.app.commons.usuarios</groupId>
  <artifactId>springboot-servicio-usuarios-commons</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <exclusions>
    <exclusion>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </exclusion>
  </exclusions>
</dependency>
<!--		Se importa de forma manual oauth2 con la finalidad de especificar la version.		-->
<!--		Ya que si se escoge la version 2.4 o mayor, entonces pedirá utilizar una libreria externa		-->
<!--		para el token en el servidor de autorización 		-->
<dependency>
  <groupId>org.springframework.security.oauth</groupId>
  <artifactId>spring-security-oauth2</artifactId>
  <version>2.3.8.RELEASE</version>
</dependency>
<!--		También se necesita importar la libreria de jwt con la siguiente version -->
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-jwt</artifactId>
  <version>1.1.1.RELEASE</version>
</dependency>
<!-- Es importante importar jaxb-runtime no se incluye para versiones de java 11 en adelante -->
<!-- Para versiones inferiores a la version 11, como la 8 por ejemplo, entonces ya no es necesario importar jaxb -->
<dependency>
  <groupId>org.glassfish.jaxb</groupId>
  <artifactId>jaxb-runtime</artifactId>
</dependency>
<!-- Agregamos la dependencia para volver cliente este microservicio con spring cloud config client -->
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```


### En el application.properties
```yml
spring.application.name=servicio-oauth
server.port=9101
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
## Esta configuración es necesaria para versiones de spring 2.4 en adelante para que pueda leer el servidor de configuracion
spring.config.import=optional:configserver:
```

### Se crea un bootstrap.properties
```yml
spring.application.name=servicio-oauth
spring.cloud.config.uri=http://localhost:8888
management.endpoints.web.exposure.include=*
```


### En la clase principal de spring
```java
@SpringBootApplication
@EnableEurekaClient
// Se importa de Feign para poderse comunicar con el microservicio de usuarios
@EnableFeignClients
public class SpringbootServicioOauthApplication implements CommandLineRunner {

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	public static void main(String[] args) {
		SpringApplication.run(SpringbootServicioOauthApplication.class, args);
	}

	// Aqui se está generando las contraseñas con BCrypt
	// para realizar las pruebas para ello se necesita implementar de implements CommandLineRunner
	@Override
	public void run(String... args) throws Exception {
		String password = "12345";

		for (int i = 0; i < 4; i++) {
			String passwordBCrypt = passwordEncoder.encode(password);
			System.out.println(passwordBCrypt);
		}

	}
}
```


### Se crea una clase UsuarioFeignClient
#### Para comunicarse con el Microservicio Usuarios
```java
@FeignClient(name = "servicio-usuarios")
public interface UsuarioFeignClient {
    @GetMapping("/usuarios/search/buscar-username")
    public Usuario findByUsername(@RequestParam String username);

    @PutMapping("/usuarios/{id}")
    public Usuario update(@RequestBody Usuario usuario, @PathVariable Long id);
}
```


### Se crea una interfaz con su implementacion UsuarioService
```java
public interface IUsuarioService {
    public Usuario findByUsername(String username);
    public Usuario update(Usuario usuario, Long id);
}



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
```


### Se crea una clase de configuración SpringSecurityConfig
```java
// Esta clase buscará definir tres cosas
// 1. Establecer el método de encriptación,
// 2. Establecer el userService que se usará
// 3. Establecer el manejador de error y éxito en la autenticación mediante el authenticationEventPublisher
@Configuration
public class SpringSecurityConfig {

    @Autowired
    private UserDetailsService usuarioService;

    @Autowired
    private AuthenticationEventPublisher eventPublisher;

    // El método authenticationManager es un método de configuración
    // que define cómo se manejarán las autenticaciones en la aplicación
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationManagerBuilder auth) throws Exception {
        // Aquí se construye con una instancia de AuthenticationManager
        // Se pasa una instancia del UserDetailsService
        // Se especifica un codificador de contraseña para encriptar y comparar las contraseñas de los usuarios
        return auth.userDetailsService(this.usuarioService)
                    .passwordEncoder(passwordEncoder())
                    .and()
                    .authenticationEventPublisher(eventPublisher)
                    .build();
    }

    // Se crea un bean ESTATICO para la encriptación de las contraseñas, recordar que debe ser estático el BCrypt
    @Bean
    public static BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```


### Se crea una clase para el servidor de configuracion de seguridad
```java
// Aquí se define una clase que será el servidor de configuración de Spring,
// extiende la clase AuthorizationServerConfigurerAdapter
// para proporcionar métodos para configurar el servidor de autorización
// se puede testear en la siguiente ruta localhost:8090/api/security/oauth/token con el gateway Zuul
// EnableAuthorizationServer se utiliza para habilitar la funcionalidad del servidor de autorización OAuth2 en una aplicación
// @RefreshScope se utiliza para permitir la recarga de propiedades de forma dinámica sin necesidad de reiniciar la aplicación
// cuando se realice una petición POST a la URL /actuator/refresh
@RefreshScope
@Configuration
@EnableAuthorizationServer
public class AuthorizationConfigServer extends AuthorizationServerConfigurerAdapter {

    @Autowired
    public Environment env;

    @Autowired
    public BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public AuthenticationManager authenticationManager;

    @Autowired
    public InfoAdicionalToken infoAdicionalToken;


    // Este método se encarga de configurar la seguridad del servidor de autorización de OAuth2,
    // permitiendo el acceso a la clave pública para cualquier cliente y
    // restringiendo el acceso al endpoint oauth/check_token a los clientes autenticados
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        // Con tokenKeyAccess("permitAll()") la clave pública se hace pública y está disponible para todos los clientes
        // Con checkTokenAccess("isAuthenticated()") indica que el acceso al endpoint oauth/check_token
        // debe estar restringido a los clientes autenticados.
        // Este endpoint se utiliza para verificar si un token de acceso es válido o no
        security.tokenKeyAccess("permitAll()")
                .checkTokenAccess("isAuthenticated()");
    }


    // Este método configura los clientes que pueden solicitar tokens de acceso
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        // Primero se indica que el servicio de clientes se configura para utilizar una memoria
        // en lugar de una base de datos u otro sistema de almacenamiento permanente
        // Se agrega un cliente con el identificador "frontendapp", puede ser una aplicacion angular por ejemplo
        // Luego se establece la clave secreta del cliente
        // Se establece los alcances de autorización para el cliente como lectura y escritura. Esto significa que
        // el cliente "Angular" por ejemplo, no podrá llamar metodos de tipo PUT o DELETE, metodos que modifican la data existente
        // mientras que si podria ejecutar un GET o POST, este último porque permite agregar nueva información, mas no modificar
        // El valor "password" permite al cliente solicitar un token de acceso utilizando el nombre de usuario y contraseña del usuario
        // El valor "refresh_token" permite al cliente solicitar un token de acceso actualizado después de que el token original expire
        // Luego se establece la duración de tiempo de vida del token de acceso y la duración de tiempo de vida del token de actualización
        // El refresh token debe tener un tiempo mucho mayor que el access token para que este no venza,
        // por ejemplo el access_token 1 dia y el refresh token 30 días
        // Nota: LA CONFIGURACION LA ESTA SACANDO DEL SERVIDOR DE CONFIGURACION!
        clients.inMemory()
                .withClient(env.getProperty("config.security.oauth.client.id"))
                .secret(passwordEncoder.encode(env.getProperty("config.security.oauth.client.secret")))
                .scopes("read", "write")
                .authorizedGrantTypes("password", "refresh_token")
                .accessTokenValiditySeconds(3600)
                .refreshTokenValiditySeconds(7200);
    }

    // Este método permite agregar información adicional al token y
    // configurar el token en los endpoints
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        TokenEnhancerChain tokenEnhancerChain = addAdditionalInfoToToken();
        configurateTokenInEndpoints(endpoints, tokenEnhancerChain);
    }

    // Este método permite agregar información adicional al token con TokenEnhancerChain
    // además de definir el tipo de key que usará para validar el token,
    private TokenEnhancerChain addAdditionalInfoToToken(){
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(Arrays.asList(infoAdicionalToken, accessTokenConverter()));
        return tokenEnhancerChain;
    }

    // Este metodo permite definir que tipo de autenticador tendran los endpoints
    // como se almacenaran los tokens ademas de especificar como se convertirán con la key
    // por ultimo establecer la información adicional
    private void configurateTokenInEndpoints(AuthorizationServerEndpointsConfigurer endpoints, TokenEnhancerChain tokenWithAditionalInfo){
        endpoints.authenticationManager(authenticationManager)
                .tokenStore(tokenStorage())
                .accessTokenConverter(accessTokenConverter())
                .tokenEnhancer(tokenWithAditionalInfo);
    }

    // JwtAccessTokenConverter se utiliza para convertir los tokens de acceso OAuth2 en tokens JWT (JSON Web Tokens) y viceversa
    // luego se configura su clave de firma (signing key) utilizando la propiedad config.security.oauth.client.jwt
    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter tokenConverter = new JwtAccessTokenConverter();
        tokenConverter.setSigningKey(env.getProperty("config.security.oauth.client.jwt"));
        return tokenConverter;
    }

    // El token store se inicializa con el accessTokenConverter,
    // que se configura con una clave secreta utilizada para firmar el token de acceso
    @Bean
    public JwtTokenStore tokenStorage() {
        return new JwtTokenStore(accessTokenConverter());
    }

}
```


### Se crea una clase para agregar mas información al token 
```java
@Component
public class InfoAdicionalToken implements TokenEnhancer {

    @Autowired
    private IUsuarioService usuarioService;

    // El método enhance es implementado de la interfaz TokenEnhancer
    // y es utilizado para añadir información adicional al token de acceso.
    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        Map<String, Object> info = new HashMap<>();
        // Se recupera los datos del usuario
        // Luego se agrega esos datos a un map
        // por ultimo se agrega la información del map al accessToken
        Usuario usuario = usuarioService.findByUsername(authentication.getName());
        info.put("nombre", usuario.getNombre());
        info.put("apellido", usuario.getApellido());
        info.put("correo", usuario.getEmail());
        ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(info);
        return accessToken;
    }
}
```

### Se crea una clase para verificar el éxito o error al momento de autenticarse, es un handler
```java
@Component
public class AuthenticationSuccessErrorHandler implements AuthenticationEventPublisher {

    private Logger log = LoggerFactory.getLogger(AuthenticationSuccessErrorHandler.class);

    @Autowired
    private IUsuarioService usuarioService;

    // Este método se ejecuta cuando el usuario se autentica correctamente
    // Permite hacer una verificación a los detalles del authentication
    // con la finalidad de que no se procesen peticiones que sean instancias de WebAuthenticationDetails
    // ya que esta ocurriendo que se está entrando a este método dos veces al intentar logearse
    // Luego se busca busca al usuario por id para resetear los intentos en caso haya mas de cero intentos
    // y que luego de esos haya conseguido logearse exitosamente
    @Override
    public void publishAuthenticationSuccess(Authentication authentication) {
        if(authentication.getDetails() instanceof WebAuthenticationDetails){
            return;
        }
        logUser(authentication);
        Usuario usuario = usuarioService.findByUsername(authentication.getName());
        resetAttempts(usuario);
    }

    private void logUser(Authentication authentication){
        UserDetails user = (UserDetails) authentication.getPrincipal();
        String mensaje = "Success Login: " + user.getUsername();
        System.out.println(mensaje);
        log.info(mensaje);
    }

    private void resetAttempts(Usuario usuario){
        if (usuario.getIntentos() != null && usuario.getIntentos() > 0) {
            usuario.setIntentos(0);
            log.info("Los intentos actual es de: " + usuario.getIntentos());
            usuarioService.update(usuario, usuario.getId());
        }
    }

    // Este método se ejecuta cuando el usuario no se autentica correctamente,
    // pero solo cuando se equivoca de contraseña es que puede entrar aqui
    // Dentro del método se verifica si el usuario es nulo
    // Luego se verifica el máximo de intentos,
    // si es mayor o igual a 3 entonces el usuario se deshabilita
    // por último se actualizan los datos en la base de datos del usuario
    // En caso haya algun error como que el usuario ingresa su contraseña incorrecta, se lanzara una FeignException
    @Override
    public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
        logMessageLogin(exception);
        try {
            Usuario usuario = usuarioService.findByUsername(authentication.getName());
            checkUserNull(usuario);
            logAttempts(usuario);
            checkMaxAttempts(usuario);
            usuarioService.update(usuario, usuario.getId());
        }catch (FeignException e){
            log.error(String.format("El usuario %s no existe en el sistema", authentication.getName()));
        }
    }

    private void logMessageLogin(AuthenticationException exception){
        String mensaje = "Error en el login: " + exception.getMessage();
        log.error(mensaje);
        System.out.println(mensaje);
    }

    private void checkUserNull(Usuario usuario){
        if (usuario.getIntentos() == null) {
            usuario.setIntentos(0);
        }
    }

    private void logAttempts(Usuario usuario){
        log.info("Los intentos actual es de: " + usuario.getIntentos());
        usuario.setIntentos(usuario.getIntentos() + 1);
        log.info("Los intentos después es de: " + usuario.getIntentos());
    }

    private void checkMaxAttempts(Usuario usuario){
        if (usuario.getIntentos() >= 3) {
            log.error(String.format("El usuario %s deshabilitado por maximos intentos.", usuario.getUsername()));
            usuario.setEnabled(false);
        }
    }
}
```



### Se crea un application.properties al git
```yml
config.security.oauth.client.id=frontendapp
config.security.oauth.client.secret=12345
config.security.oauth.client.jwt=algun_codigo_secreto_aeiou
```


## En el Gateway de Zuul
### Se crea un application.properties al git
```xml
<!--		Se importa de forma manual oauth2 con la finalidad de especificar la version.		-->
<!--		Ya que si se escoge la version 2.4 o mayor, entonces pedirá utilizar una libreria externa		-->
<!--		para el token en el servidor de autorización 		-->
<dependency>
  <groupId>org.springframework.security.oauth</groupId>
  <artifactId>spring-security-oauth2</artifactId>
  <version>2.3.8.RELEASE</version>
</dependency>
<!--		También se necesita importar la libreria de jwt con la siguiente version -->
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-jwt</artifactId>
  <version>1.1.1.RELEASE</version>
</dependency>
<!-- Es importante importar jaxb-runtime no se incluye para versiones de java 11 en adelante -->
<!-- Para versiones inferiores a la version 11, como la 8 por ejemplo, entonces ya no es necesario importar jaxb -->
<dependency>
  <groupId>org.glassfish.jaxb</groupId>
  <artifactId>jaxb-runtime</artifactId>
</dependency>
<!-- Agregamos la dependencia para volver cliente este microservicio con spring cloud config client -->
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-config</artifactId>
</dependency>
```


### En el application.properties se agrega
```yml
zuul.routes.security.service-id=servicio-oauth
zuul.routes.security.path=/api/security/**
zuul.routes.security.sensitive-headers=Cookie,Set-Cookie
```


### En la clase ResourceServerConfig
```java
@RefreshScope
@Configuration
// @EnableResourceServer configura automáticamente un filtro de Spring Security
// para validar los tokens de acceso y garantizar que el usuario tenga los permisos
// necesarios para acceder a los recursos protegidos
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

    @Value("${config.security.oauth.client.jwt}")
    public String jwtKey;

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.tokenStore(tokenStorage());
    }

    // En este metodo se definen los permisos a cada recursos y se establece la configuracion del CORS
    // Es importante que al especificar que rol puede acceder a algun recurso no se anteponga la palabra ROLE
    // ya que por detrás se concatena al rol
    // Recordar que al dar los permisos a los recursos es necesario comenzar desde una ruta especifica hasta una ruta más génerica
    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/api/security/oauth/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/productos/listar", "/api/items/listar/", "/api/usuarios/usuarios").permitAll()
                .antMatchers(HttpMethod.GET, "/api/productos/ver/{id}", "/api/items/ver/{id}/cantidad/{cantidad}", "/api/usuarios/usuarios/{id}").hasAnyRole("ADMIN", "USER")
                .antMatchers("/api/productos/**", "/api/items/**", "/api/usuarios/**").hasRole("ADMIN")
                .anyRequest().authenticated()
                .and().cors().configurationSource(corsConfigurationSource());
    }

    // Se establece la politica de seguridad en el CORS
    // en el setAllowedOrigins se puede indicar que clientes pueden conectarse al servidor de recursos,
    // en este caso es un asterisco, asi que puede ser cualquiera
    // si hubiera un cliente angular se especificaria el nombre del cliente
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(Arrays.asList("*"));
        corsConfig.setAllowedMethods(Arrays.asList("POST", "GET", "PUT", "DELETE", "OPTIONS"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(){
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(corsConfigurationSource()));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    // El token store se inicializa con el accessTokenConverter,
    // que se configura con una clave secreta utilizada para firmar el token de acceso
    @Bean
    public JwtTokenStore tokenStorage() {
        return new JwtTokenStore(accessTokenConverter());
    }


    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter tokenConverter = new JwtAccessTokenConverter();
        tokenConverter.setSigningKey(jwtKey);
        return tokenConverter;
    }
}
```

# Zipkin, sleuth y rabbitmq

## Microservicio Oauth 

### En el pom.xml de cada microservicio a usar zipkin, sleuth y rabbitmq / Instalación de cada software
```xml
<!--	Sleuth es una biblioteca de rastreo distribuido que se utiliza en aplicaciones basadas
        en Spring Boot. Es parte del ecosistema Spring Cloud y se integra perfectamente
        con otras herramientas de este ecosistema, como Zipkin	-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<!--	Zipkin es una herramienta de rastreo de solicitudes y trazabilidad de microservicios. Permite
monitorear y diagnosticar problemas en sistemas distribuidos, proporcionando una visión detallada de cómo
interactúan los diferentes componentes en un flujo de solicitud.	-->
<!--	El cliente .jar se puede descargar de https://zipkin.io/	-->
<!--	Se puede ejecutar ingresando el comando java -jar zipkin-server-x.xx.x-exec.jar	-->
<!--	Si aparece un error relacionado a Armeria Server, probablemente el puerto esté ocupado	-->
<!--	Se puede cambiar indicando el puerto con java -jar zipkin-server-2.18.3-exec.jar (Estos son guiones->) &#45;&#45;server.port=9512	-->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
<!--	RabbitMQ es un software de mensajería de código abierto que funciona
como un intermediario (broker) de mensajes. Permite que diferentes aplicaciones o componentes
se comuniquen entre sí de manera eficiente y confiable, mediante el intercambio de mensajes a
través de canales o colas (queues) gestionados por el broker	-->
<!--	El cliente se puede descargar de https://www.rabbitmq.com/#getstarted	-->
<!--	Al terminar de descargar se puede solicitar actualizar Erlang	-->
<!--	Al terminar de instalar, ejecutar el comando rabbitmq-plugins enable rabbitmq_management	-->
<!--	Luego intentar abrir la UI de rabbitMQ entrando a http://localhost:15672/ -->
<!--	Con el usuario "guest" y contraseña "guest"	-->
<!--	En caso que no se pueda abrir el http://localhost:15672/  -->
<!--	Ejecutar lo siguiente:   -->
<!--	1.- rabbitmq-service remove		-->
<!--	2.- rabbitmq-service install	-->
<!--	3.- rabbitmq-server restart		-->
<!--	Luego intentar entrar nuevamente	-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### Vinculando BD MySQL a zipkin y sleuth 
```
En lugar de ejecutar directamente zipkin con java -jar zipkin-server-2.18.3-exec.jar
se creará un archivo zipkin.cmd con la siguiente configuración

@echo off
set RABBIT_ADDRESSES=localhost:5672
set STORAGE_TYPE=mysql
set MYSQL_USER=zipkin
SET MYSQL_PASS=zipkin
java -jar ./zipkin-server-2.24.0-exec.jar

Indicando que zipkin se conectará con el servidor de rabbitMQ en el puerto que el servidor de rabbitmq está levantado
Ademas indicará que usara la base de datos mysql
y el usuario y contraseña para poder tener acceso al esquema que crearemos después
A partir de aqui Zipkin se ejecutará arrancando el .cmd 
```

### Configurando la base de datos
```
https://github.com/openzipkin/zipkin#mysql
Usando el workbench de mysql: 
Se necesitará crear un esquema de base de datos, por ejemplo en la conexion por defecto de mysql se creará el esquema con el nombre zipkin
Luego ir a User and privileges para configurar el usuario y password
agregar una nueva cuenta e indicar como login name: zipkin, limit to host matching: localhost, password: zipkin y por ultimo aplicar
en esa misma ventana, dirigirse a la pestaña Schemas Privileges, agregar una entrada con "add entry"
Luego marcar Selected schema y seleccionar la base de datos de zipkin
Por último, marcar los privilegios que para este caso solo será SELECT, INSERT, UPDATE, DELETE, EXECUTE, SHOW VIEW
Aplicar cambios
```


### Creando tablas
```sql
-- https://github.com/openzipkin/zipkin/blob/master/zipkin-storage/mysql-v1/src/main/resources/mysql.sql
-- Ejecutar el siguiente script en el esquema creado para zipkin

CREATE TABLE IF NOT EXISTS zipkin_spans (
  `trace_id_high` BIGINT NOT NULL DEFAULT 0 COMMENT 'If non zero, this means the trace uses 128 bit traceIds instead of 64 bit',
  `trace_id` BIGINT NOT NULL,
  `id` BIGINT NOT NULL,
  `name` VARCHAR(255) NOT NULL,
  `remote_service_name` VARCHAR(255),
  `parent_id` BIGINT,
  `debug` BIT(1),
  `start_ts` BIGINT COMMENT 'Span.timestamp(): epoch micros used for endTs query and to implement TTL',
  `duration` BIGINT COMMENT 'Span.duration(): micros used for minDuration and maxDuration query',
  PRIMARY KEY (`trace_id_high`, `trace_id`, `id`)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED CHARACTER SET=utf8 COLLATE utf8_general_ci;

ALTER TABLE zipkin_spans ADD INDEX(`trace_id_high`, `trace_id`) COMMENT 'for getTracesByIds';
ALTER TABLE zipkin_spans ADD INDEX(`name`) COMMENT 'for getTraces and getSpanNames';
ALTER TABLE zipkin_spans ADD INDEX(`remote_service_name`) COMMENT 'for getTraces and getRemoteServiceNames';
ALTER TABLE zipkin_spans ADD INDEX(`start_ts`) COMMENT 'for getTraces ordering and range';

CREATE TABLE IF NOT EXISTS zipkin_annotations (
  `trace_id_high` BIGINT NOT NULL DEFAULT 0 COMMENT 'If non zero, this means the trace uses 128 bit traceIds instead of 64 bit',
  `trace_id` BIGINT NOT NULL COMMENT 'coincides with zipkin_spans.trace_id',
  `span_id` BIGINT NOT NULL COMMENT 'coincides with zipkin_spans.id',
  `a_key` VARCHAR(255) NOT NULL COMMENT 'BinaryAnnotation.key or Annotation.value if type == -1',
  `a_value` BLOB COMMENT 'BinaryAnnotation.value(), which must be smaller than 64KB',
  `a_type` INT NOT NULL COMMENT 'BinaryAnnotation.type() or -1 if Annotation',
  `a_timestamp` BIGINT COMMENT 'Used to implement TTL; Annotation.timestamp or zipkin_spans.timestamp',
  `endpoint_ipv4` INT COMMENT 'Null when Binary/Annotation.endpoint is null',
  `endpoint_ipv6` BINARY(16) COMMENT 'Null when Binary/Annotation.endpoint is null, or no IPv6 address',
  `endpoint_port` SMALLINT COMMENT 'Null when Binary/Annotation.endpoint is null',
  `endpoint_service_name` VARCHAR(255) COMMENT 'Null when Binary/Annotation.endpoint is null'
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED CHARACTER SET=utf8 COLLATE utf8_general_ci;

ALTER TABLE zipkin_annotations ADD UNIQUE KEY(`trace_id_high`, `trace_id`, `span_id`, `a_key`, `a_timestamp`) COMMENT 'Ignore insert on duplicate';
ALTER TABLE zipkin_annotations ADD INDEX(`trace_id_high`, `trace_id`, `span_id`) COMMENT 'for joining with zipkin_spans';
ALTER TABLE zipkin_annotations ADD INDEX(`trace_id_high`, `trace_id`) COMMENT 'for getTraces/ByIds';
ALTER TABLE zipkin_annotations ADD INDEX(`endpoint_service_name`) COMMENT 'for getTraces and getServiceNames';
ALTER TABLE zipkin_annotations ADD INDEX(`a_type`) COMMENT 'for getTraces and autocomplete values';
ALTER TABLE zipkin_annotations ADD INDEX(`a_key`) COMMENT 'for getTraces and autocomplete values';
ALTER TABLE zipkin_annotations ADD INDEX(`trace_id`, `span_id`, `a_key`) COMMENT 'for dependencies job';

CREATE TABLE IF NOT EXISTS zipkin_dependencies (
  `day` DATE NOT NULL,
  `parent` VARCHAR(255) NOT NULL,
  `child` VARCHAR(255) NOT NULL,
  `call_count` BIGINT,
  `error_count` BIGINT,
  PRIMARY KEY (`day`, `parent`, `child`)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED CHARACTER SET=utf8 COLLATE utf8_general_ci;
```


### Configurando la base de datos
```
https://github.com/openzipkin/zipkin#mysql
Se necesitará crear un esquema de base de datos, por ejemplo en la conexion por defecto de mysql se creará el esquema con el nombre zipkin
Luego ir a User and privileges para configurar el usuario y password
agregar una nueva cuenta e indicar como login name: zipkin, limit to host matching: localhost, password: zipkin y por ultimo aplicar
en esa misma ventana, dirigirse a la pestaña
```


