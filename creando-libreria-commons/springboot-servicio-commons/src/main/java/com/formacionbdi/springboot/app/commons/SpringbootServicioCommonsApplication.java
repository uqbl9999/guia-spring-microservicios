package com.formacionbdi.springboot.app.commons;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// Excluimos la autoconfiguración con el datasource ya que no esta incluida en el proyecto ninguna base de datos
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SpringbootServicioCommonsApplication {
    // Se quita el metodo principal de arranque
}
