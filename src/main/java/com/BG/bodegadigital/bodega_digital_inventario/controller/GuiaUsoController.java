package com.BG.bodegadigital.bodega_digital_inventario.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/guia")
public class GuiaUsoController {

    @GetMapping
    public String mostrarGuiaUso() {
        // Devuelve la página donde insertarás el video tutorial
        return "guia-uso";
    }
}
