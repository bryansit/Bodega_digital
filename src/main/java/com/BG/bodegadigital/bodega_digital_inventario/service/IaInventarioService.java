package com.BG.bodegadigital.bodega_digital_inventario.service;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Servicio de IA para inventario que calcula métricas en tiempo real desde la base de datos.
 * Ya no usa CSV estático.
 */
@Service
public class IaInventarioService {

    private final MovimientoInventarioService movimientoInventarioService;

    public IaInventarioService(MovimientoInventarioService movimientoInventarioService) {
        this.movimientoInventarioService = movimientoInventarioService;
    }

    /**
     * Recomienda cuántos días antes del vencimiento debe activarse la alerta,
     * según la velocidad real de rotación (ventas promedio diarias) de los últimos 30 días.
     *
     * @param productoId ID del producto
     * @return número de días recomendados para alerta (7, 15 o 30)
     */
    public int recomendarDiasAlerta(Long productoId) {
        if (productoId == null) {
            return 30; // valor por defecto si no hay ID
        }

        // Obtenemos el promedio real de ventas diarias de los últimos 30 días
        double promedioVentasDiarias = movimientoInventarioService
                .calcularPromedioVentaDiaria(productoId, 30);

        // Lógica de recomendación basada en rotación real
        if (promedioVentasDiarias >= 10.0) {
            return 7;   // productos que se venden muy rápido → alerta cercana al vencimiento
        } 
        else if (promedioVentasDiarias >= 5.0) {
            return 15;  // rotación media
        } 
        else {
            return 30;  // rotación lenta → más margen de seguridad
        }
    }

    /**
     * Versión alternativa: calcula la rotación considerando también el stock actual
     * y el valor económico aproximado del producto (opcional para futura mejora).
     */
    public int recomendarDiasAlertaAvanzado(Long productoId, Integer stockActual, Double precioUnitario) {
        double rotacionDiaria = movimientoInventarioService.calcularPromedioVentaDiaria(productoId, 30);

        if (rotacionDiaria <= 0) {
            return 30; // sin ventas → máximo margen
        }

        // Días estimados para agotar stock actual
        double diasParaAgotar = stockActual != null && stockActual > 0 
                ? stockActual / rotacionDiaria 
                : 999;

        // Si el producto es caro, damos más margen de alerta
        double factorValor = (precioUnitario != null && precioUnitario > 20) ? 1.5 : 1.0;

        if (rotacionDiaria >= 10 || diasParaAgotar <= 10) {
            return (int) Math.max(5, 7 * factorValor);
        } else if (rotacionDiaria >= 5 || diasParaAgotar <= 30) {
            return (int) Math.max(10, 15 * factorValor);
        } else {
            return 30;
        }
    }

    /**
     * Método auxiliar para obtener la rotación diaria promedio (útil para dashboard o reportes)
     */
    public double obtenerRotacionPromedio(Long productoId, int diasVentana) {
        return movimientoInventarioService.calcularPromedioVentaDiaria(productoId, diasVentana);
    }
}