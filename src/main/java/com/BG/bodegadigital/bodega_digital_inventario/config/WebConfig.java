package com.BG.bodegadigital.bodega_digital_inventario.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración de recursos estáticos para servir imágenes subidas por los usuarios.
 * Asegura que las rutas /uploads/** apunten a la carpeta física "uploads/" en el proyecto.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Servir imágenes subidas desde la carpeta "uploads/" en el root del proyecto
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")   // Ruta relativa al proyecto
                .setCachePeriod(3600);  // Cache de 1 hora (mejora rendimiento)

        // Opcional: servir archivos estáticos adicionales si tienes más carpetas
        // Ejemplo: registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
    }
}