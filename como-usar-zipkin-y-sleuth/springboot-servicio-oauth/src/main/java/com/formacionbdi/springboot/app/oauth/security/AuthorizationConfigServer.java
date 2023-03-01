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
import java.util.Base64;

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


    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security.tokenKeyAccess("permitAll()")
                .checkTokenAccess("isAuthenticated()");
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.inMemory()
                .withClient(env.getProperty("config.security.oauth.client.id"))
                .secret(passwordEncoder.encode(env.getProperty("config.security.oauth.client.secret")))
                .scopes("read", "write")
                .authorizedGrantTypes("password", "refresh_token")
                .accessTokenValiditySeconds(3600)
                .refreshTokenValiditySeconds(7200);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        TokenEnhancerChain tokenEnhancerChain = addAdditionalInfoToToken();
        configurateTokenInEndpoints(endpoints, tokenEnhancerChain);
    }

    private TokenEnhancerChain addAdditionalInfoToToken(){
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(Arrays.asList(infoAdicionalToken, accessTokenConverter()));
        return tokenEnhancerChain;
    }

    private void configurateTokenInEndpoints(AuthorizationServerEndpointsConfigurer endpoints, TokenEnhancerChain tokenWithAditionalInfo){
        endpoints.authenticationManager(authenticationManager)
                .tokenStore(tokenStorage())
                .accessTokenConverter(accessTokenConverter())
                .tokenEnhancer(tokenWithAditionalInfo);
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter tokenConverter = new JwtAccessTokenConverter();
        tokenConverter.setSigningKey(Base64.getEncoder().encodeToString(env.getProperty("config.security.oauth.client.jwt").getBytes()));
        return tokenConverter;
    }

    @Bean
    public JwtTokenStore tokenStorage() {
        return new JwtTokenStore(accessTokenConverter());
    }

}
