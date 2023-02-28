package com.formacionbdi.springboot.app.oauth.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;

import java.util.Arrays;

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
