package com.BG.bodegadigital.bodega_digital_inventario.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import jakarta.servlet.http.HttpServletRequest;

// Solo aplica a controladores de tu app
@ControllerAdvice(basePackages = "com.BG.bodegadigital.bodega_digital_inventario.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, HttpServletRequest request, Model model) {

        // Errores de rutas de tu app
        if (e instanceof NoHandlerFoundException ||
            (e.getMessage() != null && e.getMessage().contains("No static resource"))) {
            return "forward:/";
        }

        model.addAttribute("mensaje", "Error inesperado: " + e.getMessage());
        model.addAttribute("tipo", "danger");
        return "error-general";
    }
}
