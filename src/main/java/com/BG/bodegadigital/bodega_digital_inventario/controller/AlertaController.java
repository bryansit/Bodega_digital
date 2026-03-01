package com.BG.bodegadigital.bodega_digital_inventario.controller;

import com.BG.bodegadigital.bodega_digital_inventario.service.AlertaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/alertas")
public class AlertaController {

    private final AlertaService alertaService;

    public AlertaController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    // Muestra todas las alertas activas (bajo stock, por vencer, vencido)
    @GetMapping
    public String listarAlertas(Model model) {
        model.addAttribute("alertasVencidas", alertaService.listarVencidos());
        model.addAttribute("alertasPorVencer", alertaService.listarPorVencer());
        model.addAttribute("alertasBajoStock", alertaService.listarBajoStock());
        return "alertas";
    }





    // Marca una alerta como resuelta
    @GetMapping("/resolver/{id}")
    public String resolverAlerta(@PathVariable Long id) {
        alertaService.marcarComoResuelta(id);
        return "redirect:/alertas";
    }

    // Genera nuevamente las alertas a partir de los productos
 
}
