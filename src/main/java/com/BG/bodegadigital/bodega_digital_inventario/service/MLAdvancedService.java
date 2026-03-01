package com.BG.bodegadigital.bodega_digital_inventario.service;

import com.BG.bodegadigital.bodega_digital_inventario.model.MovimientoInventario;
import com.BG.bodegadigital.bodega_digital_inventario.repository.MovimientoInventarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.regression.LinearModel;
import smile.regression.OLS;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio de predicción de ventas usando Smile 3.1.1.
 * Versión ultra-robusta: maneja todos los casos de fallo sin romper el reporte.
 */
@Service
public class MLAdvancedService {

    private static final Logger logger = LoggerFactory.getLogger(MLAdvancedService.class);

    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public MLAdvancedService(MovimientoInventarioRepository movimientoInventarioRepository) {
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    /**
     * Predice ventas para los próximos 7 días.
     * Si no hay datos suficientes o falla el modelo → retorna valores por defecto seguros.
     */
    public Map<String, Object> predecirVentas(Long productoId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. Obtener ventas históricas (cantidad < 0)
            List<MovimientoInventario> movimientos = movimientoInventarioRepository
                    .findByProductoIdAndCantidadLessThanOrderByFechaDesc(productoId, 0);

            // 2. Chequeo estricto: mínimo 4 ventas para tener algo de sentido estadístico
            if (movimientos.size() < 4) {
                logger.debug("Producto {} tiene solo {} ventas → sin predicción", productoId, movimientos.size());
                return crearResultadoSinDatos();
            }

            // 3. Preparar arrays (evitamos DataFrame.of(double[][]) que falla a veces)
            int n = movimientos.size();
            double[] x = new double[n]; // días atrás
            double[] y = new double[n]; // ventas

            LocalDate hoy = LocalDate.now();
            boolean tieneVarianza = false;
            double primeraVenta = Math.abs(movimientos.get(0).getCantidad());

            for (int i = 0; i < n; i++) {
                MovimientoInventario m = movimientos.get(i);
                x[i] = ChronoUnit.DAYS.between(m.getFecha(), hoy);
                y[i] = Math.abs(m.getCantidad());

                // Chequeo simple de varianza (si todas ventas iguales → no se puede ajustar modelo)
                if (Math.abs(y[i] - primeraVenta) > 0.001) {
                    tieneVarianza = true;
                }
            }

            if (!tieneVarianza) {
                logger.debug("Producto {} tiene ventas constantes → sin predicción lineal posible", productoId);
                return crearResultadoSinDatos();
            }

            // 4. Crear DataFrame de forma segura
            DataFrame df = DataFrame.of(
                    smile.data.vector.DoubleVector.of("dias", x),
                    smile.data.vector.DoubleVector.of("ventas", y)
            );

            // 5. Fórmula explícita y segura
            Formula formula = Formula.of("ventas ~ dias");

            // 6. Intentar ajustar modelo
            LinearModel model = OLS.fit(formula, df);

            // 7. Predecir (protegido contra NaN o valores locos)
            double totalPredicho = 0.0;
            for (int dia = 1; dia <= 7; dia++) {
                try {
                    double pred = model.predict(new double[]{dia});
                    if (!Double.isNaN(pred) && !Double.isInfinite(pred)) {
                        totalPredicho += Math.max(0, pred);
                    }
                } catch (Exception ex) {
                    logger.trace("Predicción falló para día {} del producto {}: {}", dia, productoId, ex.getMessage());
                }
            }

            // 8. Categoría basada en promedio diario
            double promedio = totalPredicho / 7;
            String categoria = promedio < 5 ? "Baja demanda" :
                               promedio < 20 ? "Demanda media" : "Alta demanda";

            result.put("total_predicho_7d", String.format("%.0f", totalPredicho));
            result.put("categoria_ml", categoria);

        } catch (Throwable t) {  // Captura TODO (Error, Exception, RuntimeException)
            logger.warn("Fallo total en predicción para producto {}: {}", productoId, t.getMessage(), t);
            return crearResultadoSinDatos();
        }

        return result;
    }

    private Map<String, Object> crearResultadoSinDatos() {
        Map<String, Object> result = new HashMap<>();
        result.put("total_predicho_7d", "Sin datos suficientes");
        result.put("categoria_ml", "N/A");
        return result;
    }
}