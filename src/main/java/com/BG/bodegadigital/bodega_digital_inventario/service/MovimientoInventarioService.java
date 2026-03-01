package com.BG.bodegadigital.bodega_digital_inventario.service;

import com.BG.bodegadigital.bodega_digital_inventario.model.MovimientoInventario;
import com.BG.bodegadigital.bodega_digital_inventario.repository.MovimientoInventarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoRepo;

    public MovimientoInventarioService(MovimientoInventarioRepository movimientoRepo) {
        this.movimientoRepo = movimientoRepo;
    }

    // Promedio venta diaria últimos N días
    public double calcularPromedioVentaDiaria(Long productoId, int diasVentana) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.minusDays(diasVentana);

        List<MovimientoInventario> movimientos =
                movimientoRepo.findByProductoIdAndFechaBetween(productoId, inicio, hoy);

        int totalVendidas = movimientos.stream()
                .mapToInt(m -> m.getCantidad() != null ? m.getCantidad() : 0)
                .sum();

        return diasVentana > 0 ? (double) totalVendidas / diasVentana : 0.0;
    }

    // Histórico diario para futura IA/ML
    public List<Integer> obtenerVentasUltimosDias(Long productoId, int dias) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.minusDays(dias);

        List<MovimientoInventario> movimientos =
                movimientoRepo.findByProductoIdAndFechaBetween(productoId, inicio, hoy);

        // Agrupar ventas por día (ventas positivas)
        Map<LocalDate, Integer> ventasPorDia = new TreeMap<>();
        movimientos.stream()
                .filter(m -> m.getCantidad() != null)
                .forEach(m -> ventasPorDia.merge(
                        m.getFecha(),
                        m.getCantidad(),      // ya es positiva
                        Integer::sum
                ));

        // Rellenar días sin ventas con 0
        List<Integer> resultado = new ArrayList<>();
        for (int i = 0; i < dias; i++) {
            LocalDate fecha = inicio.plusDays(i);
            resultado.add(ventasPorDia.getOrDefault(fecha, 0));
        }
        return resultado;
    }
}
