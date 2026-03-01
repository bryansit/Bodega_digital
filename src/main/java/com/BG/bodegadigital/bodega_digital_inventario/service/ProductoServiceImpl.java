package com.BG.bodegadigital.bodega_digital_inventario.service;

import com.BG.bodegadigital.bodega_digital_inventario.model.EstadoProducto;
import com.BG.bodegadigital.bodega_digital_inventario.model.MovimientoInventario;
import com.BG.bodegadigital.bodega_digital_inventario.model.Producto;
import com.BG.bodegadigital.bodega_digital_inventario.model.Venta;
import com.BG.bodegadigital.bodega_digital_inventario.repository.MovimientoInventarioRepository;
import com.BG.bodegadigital.bodega_digital_inventario.repository.ProductoRepository;
import com.BG.bodegadigital.bodega_digital_inventario.repository.VentaRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioService movimientoInventarioService;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final AlertaService alertaService;
    private final IaInventarioService iaInventarioService;
    private final MLAdvancedService mlAdvancedService;
    private final VentaRepository ventaRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               MovimientoInventarioService movimientoInventarioService,
                               MovimientoInventarioRepository movimientoInventarioRepository,
                               AlertaService alertaService,
                               IaInventarioService iaInventarioService,
                               MLAdvancedService mlAdvancedService,
                               VentaRepository ventaRepository) {
        this.productoRepository = productoRepository;
        this.movimientoInventarioService = movimientoInventarioService;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.alertaService = alertaService;
        this.iaInventarioService = iaInventarioService;
        this.mlAdvancedService = mlAdvancedService;
        this.ventaRepository = ventaRepository;
    }
    @Override
    public Page<Venta> buscarVentasConFiltros(LocalDate desde, LocalDate hasta, Long productoId, Pageable pageable) {
        if (productoId != null && productoId > 0) {
            return ventaRepository.findByFechaBetweenAndProductoId(desde, hasta, productoId, pageable);
        }
        return ventaRepository.findByFechaBetween(desde, hasta, pageable);
    }
    @Override
    public List<Producto> listarTodos() {
        actualizarEstadosPorStockYVencimiento();
        return productoRepository.findAll();
    }

    @Override
    public Page<Producto> listarTodosPaginado(Pageable pageable) {
        return productoRepository.findAll(pageable);
    }

    @Override
    public Producto guardar(Producto producto) {
        long total = productoRepository.count();
        if (total >= 400 && producto.getId() == null) {
            throw new IllegalStateException("Límite de 400 productos alcanzado");
        }
        Producto saved = productoRepository.save(producto);
        actualizarEstadoYAlertasDeUnProducto(saved);
        return saved;
    }

    @Override
    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    @Override
    @Transactional
    public void eliminarPorId(Long id) {
        movimientoInventarioRepository.deleteByProductoId(id);
        productoRepository.deleteById(id);
    }

    @Override
    public Producto buscarPorCodigo(String codigo) {
        return productoRepository.findByCodigo(codigo).orElse(null);
    }

    @Override
    public void actualizarEstadosPorStockYVencimiento() {
        List<Producto> productos = productoRepository.findAll();
        for (Producto p : productos) {
            actualizarEstadoYAlertasDeUnProducto(p);
        }
    }

    @Override
    public void actualizarEstadoYAlertasDeUnProducto(Producto p) {
        LocalDate hoy = LocalDate.now();
        EstadoProducto estadoAnterior = p.getEstado();

        double promedioDiario = movimientoInventarioService.calcularPromedioVentaDiaria(p.getId(), 30);
        int diasAlerta = iaInventarioService.recomendarDiasAlerta(p.getId());

        p.setDiasAlertaVencimiento(diasAlerta);
        p.setEstado(EstadoProducto.NORMAL);

        // Bajo stock
        boolean esBajoStock = p.getCantidad() != null && p.getCantidad() <= 0;
        if (esBajoStock) {
            p.setEstado(EstadoProducto.BAJO_STOCK);
            if (estadoAnterior != EstadoProducto.BAJO_STOCK) {
                alertaService.crearAlertaStockBajo(p);
            }
        }

        // Vencimiento
        if (p.getFechaVencimiento() != null) {
            LocalDate umbral = hoy.plusDays(diasAlerta);
            if (p.getFechaVencimiento().isBefore(hoy)) {
                p.setEstado(EstadoProducto.VENCIDO);
                if (estadoAnterior != EstadoProducto.VENCIDO) {
                    alertaService.crearAlertaVencido(p);
                }
            } else if (!p.getFechaVencimiento().isAfter(umbral)) {
                p.setEstado(EstadoProducto.POR_VENCER);
                long diasRestantes = ChronoUnit.DAYS.between(hoy, p.getFechaVencimiento());
                if (estadoAnterior != EstadoProducto.POR_VENCER) {
                    alertaService.crearAlertaPorVencer(p, diasRestantes);
                }
            }
        }

        productoRepository.save(p);
    }

    @Override
    @Transactional
    public void registrarVenta(Long productoId, Integer cantidadVendida) {
        if (cantidadVendida == null || cantidadVendida <= 0) {
            throw new IllegalArgumentException("La cantidad vendida debe ser mayor a 0");
        }
        Producto producto = buscarPorId(productoId);
        if (producto.getCantidad() < cantidadVendida) {
            throw new IllegalStateException("Stock insuficiente: solo hay " + producto.getCantidad() + " unidades disponibles");
        }
        int nuevoStock = producto.getCantidad() - cantidadVendida;
        producto.setCantidad(nuevoStock);
        productoRepository.save(producto);
        actualizarEstadoYAlertasDeUnProducto(producto);

        // Guardar movimiento como negativo (venta)
        MovimientoInventario mov = new MovimientoInventario();
        mov.setProducto(producto);
        mov.setFecha(LocalDate.now());
        mov.setCantidad(-cantidadVendida); // Negativo = venta
        mov.setStockDespuesMovimiento(nuevoStock);
        movimientoInventarioRepository.save(mov);
    }

    @Override
    public List<Producto> buscarConFiltros(String busqueda, String estado) {
        String busquedaNormalizada = normalizarBusqueda(busqueda);
        EstadoProducto estadoEnum = parseEstado(estado);
        return productoRepository.buscarConFiltros(busquedaNormalizada, estadoEnum);
    }

    @Override
    public Page<Producto> buscarConFiltrosPaginado(String busqueda, String estado, Pageable pageable) {
        String busquedaNormalizada = normalizarBusqueda(busqueda);
        EstadoProducto estadoEnum = parseEstado(estado);
        return productoRepository.buscarConFiltrosPaginado(busquedaNormalizada, estadoEnum, pageable);
    }

    private String normalizarBusqueda(String busqueda) {
        if (busqueda == null) return null;
        String trimmed = busqueda.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.toLowerCase()
                .replaceAll("[áàäâã]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöôõ]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("[ñ]", "n");
    }

    private EstadoProducto parseEstado(String estado) {
        if (estado == null || estado.trim().isEmpty()) return null;
        try {
            return EstadoProducto.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public long contarTotal() {
        return productoRepository.count();
    }

    @Override
    public long contarBajoStock() {
        return productoRepository.countByEstado(EstadoProducto.BAJO_STOCK);
    }

    @Override
    public long contarProximosAVencer() {
        return productoRepository.countByEstado(EstadoProducto.POR_VENCER);
    }

    @Override
    public long contarVencidos() {
        return productoRepository.countByEstado(EstadoProducto.VENCIDO);
    }

    /**
     * Genera reporte Excel de ventas totales con integración de IA (predicción y categoría)
     */
    /**
     * Genera reporte Excel de ventas totales con integración de IA (predicción y categoría)
     * @return bytes del archivo Excel
     * @throws IOException si hay error al generar el archivo
     */
    @Override
    public byte[] generarReporteVentasExcel() throws IOException {
        // 1. Obtener TODAS las ventas registradas
        List<Venta> ventas = ventaRepository.findAll();
        
        if (ventas.isEmpty()) {
            // Opcional: retornar un Excel vacío o con mensaje
            Workbook wb = new XSSFWorkbook();
            Sheet sheet = wb.createSheet("Reporte Ventas Totales");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("No hay ventas registradas aún");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            wb.close();
            return out.toByteArray();
        }

        // 2. Agrupar por producto
        Map<Producto, Integer> cantidadesVendidas = new HashMap<>();
        Map<Producto, BigDecimal> totalesVendidos = new HashMap<>();
        BigDecimal totalGeneral = BigDecimal.ZERO;
        int totalUnidadesVendidas = 0;

        for (Venta v : ventas) {
            Producto p = v.getProducto();
            int cantidad = v.getCantidad();           // ya es positiva
            BigDecimal subtotal = v.getTotal();       // ya está calculado en la entidad

            cantidadesVendidas.merge(p, cantidad, Integer::sum);
            totalesVendidos.merge(p, subtotal, BigDecimal::add);

            totalGeneral = totalGeneral.add(subtotal);
            totalUnidadesVendidas += cantidad;
        }

        // 3. Crear Excel con Apache POI
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte Ventas Totales");

            // Estilo encabezados
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Encabezados
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "Producto", 
                    "Cantidad Vendida", 
                    "Precio Unitario (S/)", 
                    "Total Vendido (S/)",
                    "Predicción 7 días (IA)", 
                    "Categoría IA (ML)"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos por producto + IA
            int rowNum = 1;
            for (Map.Entry<Producto, Integer> entry : cantidadesVendidas.entrySet()) {
                Producto p = entry.getKey();
                int cantidad = entry.getValue();
                BigDecimal total = totalesVendidos.get(p);
                BigDecimal precioUnitario = p.getPrecioUnidad();

                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(p.getNombre());
                row.createCell(1).setCellValue(cantidad);
                row.createCell(2).setCellValue(precioUnitario.doubleValue());
                row.createCell(3).setCellValue(total.doubleValue());

                // Integración de IA (predicción y clustering)
                Map<String, Object> pred = mlAdvancedService.predecirVentas(p.getId());
                
                if (pred.containsKey("error") || "Sin datos".equals(pred.get("total_predicho_7d"))) {
                    row.createCell(4).setCellValue("Sin datos suficientes");
                    row.createCell(5).setCellValue("N/A");
                } else {
                    row.createCell(4).setCellValue(String.valueOf(pred.get("total_predicho_7d")));
                    row.createCell(5).setCellValue(String.valueOf(pred.get("categoria_ml")));
                }
            }

            // Fila de totales generales
            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(0).setCellValue("TOTAL GENERAL");
            totalRow.createCell(1).setCellValue(totalUnidadesVendidas);
            totalRow.createCell(3).setCellValue(totalGeneral.doubleValue());

            CellStyle totalStyle = workbook.createCellStyle();
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);

            totalRow.getCell(0).setCellStyle(totalStyle);
            totalRow.getCell(1).setCellStyle(totalStyle);
            totalRow.getCell(3).setCellStyle(totalStyle);

            // Autoajuste de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convertir a bytes
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                workbook.write(outputStream);
                return outputStream.toByteArray();
            }
        }
    }
	@Override
	public BigDecimal calcularTotalVentasPeriodo(LocalDate desde, LocalDate hasta, Long productoId) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Object listarUltimos(int i) {
		// TODO Auto-generated method stub
		return null;
	}
}