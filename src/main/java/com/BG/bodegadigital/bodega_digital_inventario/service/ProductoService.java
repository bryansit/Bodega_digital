package com.BG.bodegadigital.bodega_digital_inventario.service;

import com.BG.bodegadigital.bodega_digital_inventario.model.EstadoProducto;
import com.BG.bodegadigital.bodega_digital_inventario.model.Producto;
import com.BG.bodegadigital.bodega_digital_inventario.model.Venta;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ProductoService {

    // Todos tus métodos existentes...
    List<Producto> listarTodos();
    Producto guardar(Producto producto);
    Producto buscarPorId(Long id);
    void eliminarPorId(Long id);
    Producto buscarPorCodigo(String codigo);
    void actualizarEstadosPorStockYVencimiento();
    void actualizarEstadoYAlertasDeUnProducto(Producto producto);
    void registrarVenta(Long productoId, Integer cantidadVendida);
    List<Producto> buscarConFiltros(String busqueda, String estado);
    long contarTotal();
    long contarBajoStock();
    long contarProximosAVencer();
    long contarVencidos();
    Page<Producto> listarTodosPaginado(Pageable pageable);
    Page<Producto> buscarConFiltrosPaginado(String busqueda, String estado, Pageable pageable);

    // ← MÉTODO QUE FALTABA: declaración obligatoria en la interfaz
  
    Page<Venta> buscarVentasConFiltros(LocalDate desde, LocalDate hasta, Long productoId, Pageable pageable);

    BigDecimal calcularTotalVentasPeriodo(LocalDate desde, LocalDate hasta, Long productoId);

    byte[] generarReporteVentasExcel() throws IOException;   // ya lo tienes, se puede mantener
	Object listarUltimos(int i);
}