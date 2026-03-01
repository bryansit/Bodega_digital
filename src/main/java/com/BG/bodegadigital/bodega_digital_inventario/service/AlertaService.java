package com.BG.bodegadigital.bodega_digital_inventario.service;

import com.BG.bodegadigital.bodega_digital_inventario.model.Alerta;
import com.BG.bodegadigital.bodega_digital_inventario.model.Producto;

import java.util.List;

public interface AlertaService {

    List<Alerta> listarAlertasActivas();

    void marcarComoResuelta(Long id);

    List<Alerta> listarVencidos();
    List<Alerta> listarPorVencer();
    List<Alerta> listarBajoStock();

    // Métodos específicos para crear alertas desde ProductoService
    void crearAlertaStockBajo(Producto producto);
    void crearAlertaPorVencer(Producto producto, long diasRestantes);
    void crearAlertaVencido(Producto producto);

    // Método IA para generar recomendación principal del dashboard
    String generarRecomendacionPrincipal();

    
}
