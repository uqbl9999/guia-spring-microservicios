package com.formacionbdi.springboot.app.zuul.filters;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
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
