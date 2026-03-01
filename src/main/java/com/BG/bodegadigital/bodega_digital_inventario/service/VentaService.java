package com.BG.bodegadigital.bodega_digital_inventario.service;

import com.BG.bodegadigital.bodega_digital_inventario.model.Producto;
import com.BG.bodegadigital.bodega_digital_inventario.model.Venta;
import com.BG.bodegadigital.bodega_digital_inventario.repository.VentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoService productoService;

    // Constructor con inyección de dependencias (recomendado en Spring)
    public VentaService(VentaRepository ventaRepository, ProductoService productoService) {
        this.ventaRepository = ventaRepository;
        this.productoService = productoService;
    }
 // Cambiar a LocalDateTime
    public Page<Venta> buscarVentas(LocalDateTime desde, LocalDateTime hasta, Long productoId, Pageable pageable) {
        if (productoId != null && productoId > 0) {
            return ventaRepository.findByFechaBetweenAndProductoIdWithProducto(desde, hasta, productoId, pageable);
        }
        return ventaRepository.findByFechaBetweenWithProducto(desde, hasta, pageable);
    }
 // Cambiar a LocalDateTime
    public BigDecimal calcularTotalVentasPeriodo(LocalDateTime desde, LocalDateTime hasta, Long productoId) {
        return ventaRepository.calcularTotalPeriodo(desde, hasta, productoId);
    }

    @Transactional
    public void registrarVenta(Long productoId, Integer cantidad) {
        Producto p = productoService.buscarPorId(productoId);
        if (p == null) {
            throw new IllegalArgumentException("Producto no encontrado con ID: " + productoId);
        }
        if (p.getCantidad() < cantidad) {
            throw new IllegalStateException("Stock insuficiente. Disponible: " + p.getCantidad() + ", solicitado: " + cantidad);
        }

        Venta venta = new Venta();
        venta.setProducto(p);
        venta.setCantidad(cantidad);
        venta.setPrecioUnitario(p.getPrecioUnidad());
        venta.setTotal(p.getPrecioUnidad().multiply(BigDecimal.valueOf(cantidad)));

        // Actualizar stock del producto
        p.setCantidad(p.getCantidad() - cantidad);
        productoService.guardar(p);

        // Guardar la venta
        ventaRepository.save(venta);
    }
	public Object contarVentasHoy() {
		// TODO Auto-generated method stub
		return null;
	}
}