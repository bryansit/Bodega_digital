package com.BG.bodegadigital.bodega_digital_inventario.service;

import com.BG.bodegadigital.bodega_digital_inventario.model.Alerta;
import com.BG.bodegadigital.bodega_digital_inventario.model.Producto;
import com.BG.bodegadigital.bodega_digital_inventario.repository.AlertaRepository;
import com.BG.bodegadigital.bodega_digital_inventario.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Servicio que gestiona alertas con lógica inteligente basada en datos reales de la BD.
 * Usa IaInventarioService para cálculos de rotación y MLAdvancedService para predicciones.
 */
@Service
public class AlertaServiceImpl implements AlertaService {

    private final AlertaRepository alertaRepository;
    private final ProductoRepository productoRepository;
    private final MLAdvancedService mlService;
    private final IaInventarioService iaInventarioService;  // ← Nueva inyección clave

    public AlertaServiceImpl(
            AlertaRepository alertaRepository,
            ProductoRepository productoRepository,
            MLAdvancedService mlService,
            IaInventarioService iaInventarioService) {
        this.alertaRepository = alertaRepository;
        this.productoRepository = productoRepository;
        this.mlService = mlService;
        this.iaInventarioService = iaInventarioService;
    }

    @Override
    public List<Alerta> listarAlertasActivas() {
        return alertaRepository.findByResueltaFalse();
    }

    @Override
    public void marcarComoResuelta(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));
        alerta.setResuelta(true);
        alertaRepository.save(alerta);
    }

    // ────────────────────────────────────────────────
    // Creación de alertas con IA basada en BD real
    // ────────────────────────────────────────────────

    @Override
    public void crearAlertaStockBajo(Producto p) {
        if (existeAlertaActiva(p, "STOCK_BAJO")) {
            return;
        }

        int cantidadSugerida;
        try {
            // Intentamos usar predicción ML (datos reales de BD)
            cantidadSugerida = calcularCantidadReposicionML(p);
        } catch (Exception e) {
            // Fallback: regla inteligente usando rotación real de BD
            cantidadSugerida = calcularCantidadReposicionInteligente(p);
        }

        Alerta alerta = new Alerta();
        alerta.setTipo("STOCK_BAJO");
        alerta.setMensaje("Stock bajo: solo " + p.getCantidad() + " unidades de " + p.getNombre()
                + ". 💡 IA sugiere reponer **" + cantidadSugerida + "** unidades.");
        alerta.setNivelUrgencia(asignarNivelUrgenciaEconomica(p, "STOCK_BAJO"));
        alerta.setFechaCreacion(LocalDateTime.now());
        alerta.setResuelta(false);
        alerta.setProducto(p);
        alertaRepository.save(alerta);
    }

    @Override
    public void crearAlertaPorVencer(Producto p, long diasRestantes) {
        if (existeAlertaActiva(p, "VENCIMIENTO_POR_VENCER")) {
            return;
        }

        // Usamos rotación REAL desde BD para estimar riesgo
        String riesgoIA = analizarRiesgoVencimientoReal(p, diasRestantes);

        Alerta alerta = new Alerta();
        alerta.setTipo("VENCIMIENTO_POR_VENCER");
        String mensaje = (diasRestantes > 0)
                ? "Quedan **" + diasRestantes + "** días para que venza " + p.getNombre()
                : "Producto por vencer: " + p.getNombre();

        if (!riesgoIA.isEmpty()) {
            mensaje += ". " + riesgoIA;
        }

        alerta.setMensaje(mensaje);
        alerta.setNivelUrgencia(asignarNivelUrgenciaEconomica(p, "VENCIMIENTO_POR_VENCER"));
        alerta.setFechaCreacion(LocalDateTime.now());
        alerta.setResuelta(false);
        alerta.setProducto(p);
        alertaRepository.save(alerta);
    }

    @Override
    public void crearAlertaVencido(Producto p) {
        if (existeAlertaActiva(p, "VENCIMIENTO_VENCIDO")) {
            return;
        }

        Alerta alerta = new Alerta();
        alerta.setTipo("VENCIMIENTO_VENCIDO");
        alerta.setMensaje("⚠️ **CRÍTICO**: Producto vencido: " + p.getNombre()
                + " (" + p.getCantidad() + " unidades). Acción inmediata requerida.");
        alerta.setNivelUrgencia("CRÍTICA");
        alerta.setFechaCreacion(LocalDateTime.now());
        alerta.setResuelta(false);
        alerta.setProducto(p);
        alertaRepository.save(alerta);
    }

    private boolean existeAlertaActiva(Producto p, String tipo) {
        return alertaRepository.existsByProductoAndTipoAndResueltaFalse(p, tipo);
    }

    // ────────────────────────────────────────────────
    // Funciones de IA mejoradas (usando datos de BD)
    // ────────────────────────────────────────────────

    /**
     * Regla inteligente de reposición usando rotación real de BD
     */
    private int calcularCantidadReposicionInteligente(Producto p) {
        double rotacionDiaria = iaInventarioService.obtenerRotacionPromedio(p.getId(), 30);

        if (rotacionDiaria <= 0) {
            // Sin ventas → sugerencia conservadora
            return 20;
        }

        // Sugerimos cubrir aprox. 10-14 días de ventas + margen
        int diasCobertura = (rotacionDiaria > 10) ? 10 : (rotacionDiaria > 5) ? 14 : 20;
        int sugerida = (int) Math.round(rotacionDiaria * diasCobertura);

        // Mínimo razonable según precio
        if (p.getPrecioUnidad() != null) {
            double precio = p.getPrecioUnidad().doubleValue();
            if (precio > 50) sugerida = Math.max(sugerida, 30);
            else if (precio < 5) sugerida = Math.max(sugerida, 50);
        }

        return Math.max(sugerida, 10);
    }

    /**
     * Versión ML (ya existente, pero ahora más integrada)
     */
    private int calcularCantidadReposicionML(Producto p) {
        Map<String, Object> prediccion = mlService.predecirVentas(p.getId());
        if (prediccion != null && prediccion.containsKey("total_predicho_7d")) {
            int ventas7d = (Integer) prediccion.get("total_predicho_7d");
            // Cubrir aprox. 10-14 días (más conservador que antes)
            int sugerida = (int) Math.round(ventas7d * 1.7);
            return Math.max(sugerida, 10);
        }
        // Fallback a la versión inteligente basada en BD
        return calcularCantidadReposicionInteligente(p);
    }

    /**
     * Análisis de riesgo de vencimiento usando rotación REAL de BD
     */
    private String analizarRiesgoVencimientoReal(Producto p, long diasRestantes) {
        if (diasRestantes <= 0) return "";

        // Rotación real de los últimos 30 días
        double ventasDiarias = iaInventarioService.obtenerRotacionPromedio(p.getId(), 30);

        if (ventasDiarias <= 0) {
            return "🟡 Sin ventas recientes. Considera promoción fuerte para evitar pérdida total.";
        }

        long unidadesQueSeVenderian = Math.round(ventasDiarias * diasRestantes);

        if (unidadesQueSeVenderian < p.getCantidad()) {
            long riesgo = p.getCantidad() - unidadesQueSeVenderian;
            return "🔴 **Riesgo alto**: ~" + riesgo + " unidades podrían vencer sin venderse. "
                    + "Considera bajar precio o promoción urgente.";
        } else if (unidadesQueSeVenderian <= p.getCantidad() * 1.3) {
            return "🟡 **Inventario justo**. Monitorea ventas diarias para no quedarte corto.";
        } else {
            return "🟢 **Buen margen**. Probablemente se venda todo antes del vencimiento.";
        }
    }

    private String asignarNivelUrgenciaEconomica(Producto p, String tipo) {
        if (p.getPrecioUnidad() == null || p.getCantidad() == null) {
            return "MEDIA";
        }
        double precio = p.getPrecioUnidad().doubleValue();
        double valorStock = p.getCantidad() * precio;

        if (valorStock > 5000 || "VENCIMIENTO_VENCIDO".equals(tipo)) {
            return "CRÍTICA";
        }
        if (valorStock > 1500 || "STOCK_BAJO".equals(tipo)) {
            return "ALTA";
        }
        if (valorStock > 500) {
            return "MEDIA";
        }
        return "BAJA";
    }

    @Override
    public String generarRecomendacionPrincipal() {
        List<Alerta> alertas = listarAlertasActivas();
        if (alertas.isEmpty()) {
            return "✅ Todo bajo control. El inventario está bien organizado.";
        }

        long criticas = alertas.stream().filter(a -> "CRÍTICA".equals(a.getNivelUrgencia())).count();
        long altas = alertas.stream().filter(a -> "ALTA".equals(a.getNivelUrgencia())).count();
        long vencidas = alertas.stream().filter(a -> "VENCIMIENTO_VENCIDO".equals(a.getTipo())).count();

        if (criticas > 0) {
            return "🔴 **Emergencia**: " + criticas + " productos en estado CRÍTICO. Atención inmediata.";
        }
        if (vencidas > 0) {
            return "⚠️ **Urgente**: " + vencidas + " productos ya vencidos. Retíralos ya.";
        }
        if (altas > 3) {
            return "🟠 **Atención alta**: " + altas + " alertas prioritarias. Revisa reposiciones y vencimientos.";
        }
        if (alertas.size() > 6) {
            return "🟡 Tienes " + alertas.size() + " alertas activas. Organiza por prioridad.";
        }
        return "🟢 " + alertas.size() + " alertas menores. Todo manejable.";
    }

    // Métodos de listado por tipo (sin cambios)
    @Override public List<Alerta> listarVencidos() { return alertaRepository.findByResueltaFalseAndTipo("VENCIMIENTO_VENCIDO"); }
    @Override public List<Alerta> listarPorVencer() { return alertaRepository.findByResueltaFalseAndTipo("VENCIMIENTO_POR_VENCER"); }
    @Override public List<Alerta> listarBajoStock() { return alertaRepository.findByResueltaFalseAndTipo("STOCK_BAJO"); }
}