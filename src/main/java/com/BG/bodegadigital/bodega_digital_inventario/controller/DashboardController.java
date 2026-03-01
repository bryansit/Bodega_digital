package com.BG.bodegadigital.bodega_digital_inventario.controller;

import com.BG.bodegadigital.bodega_digital_inventario.service.AlertaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final AlertaService alertaService;

    public DashboardController(AlertaService alertaService) {
        this.alertaService = alertaService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Listar solo alertas activas
        var alertas = alertaService.listarAlertasActivas();
        
        // IA: Generar recomendación principal
        String recomendacionIA = alertaService.generarRecomendacionPrincipal();

        model.addAttribute("alertas", alertas);
        model.addAttribute("totalAlertas", alertas.size());
        model.addAttribute("recomendacionIA", recomendacionIA); // Nueva
        return "dashboard";
    }
}
