package com.BG.bodegadigital.bodega_digital_inventario.controller;

import com.BG.bodegadigital.bodega_digital_inventario.model.EstadoProducto;
import com.BG.bodegadigital.bodega_digital_inventario.model.Producto;
import com.BG.bodegadigital.bodega_digital_inventario.model.Venta;
import com.BG.bodegadigital.bodega_digital_inventario.service.ProductoService;
import com.BG.bodegadigital.bodega_digital_inventario.service.ProveedorService;
import com.BG.bodegadigital.bodega_digital_inventario.service.VentaService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private static final Logger logger = LoggerFactory.getLogger(ProductoController.class);
   
    private final ProductoService productoService;
    private final ProveedorService proveedorService;
    private final VentaService ventaService;
    private final ObjectMapper objectMapper;

    public ProductoController(ProductoService productoService, VentaService ventaService, ProveedorService proveedorService, ObjectMapper objectMapper) {
        this.productoService = productoService;
        this.proveedorService = proveedorService;
        this.objectMapper = objectMapper;
        this.ventaService = ventaService;
    }

    @GetMapping
    public String listarProductos(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado,
            @PageableDefault(size = 12, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable,
            Model model) {

        productoService.actualizarEstadosPorStockYVencimiento();

        String busquedaLimpia = (busqueda != null) ? busqueda.trim() : null;
        if (busquedaLimpia != null && busquedaLimpia.isEmpty()) {
            busquedaLimpia = null;
        }

        EstadoProducto estadoEnum = null;
        if (estado != null && !estado.trim().isEmpty()) {
            try {
                estadoEnum = EstadoProducto.valueOf(estado.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Estado inválido recibido: {}", estado);
            }
        }

        Page<Producto> pageProductos;
        if (busquedaLimpia != null || estadoEnum != null) {
            pageProductos = productoService.buscarConFiltrosPaginado(
                    busquedaLimpia, estadoEnum != null ? estadoEnum.name() : null, pageable);
        } else {
            pageProductos = productoService.listarTodosPaginado(pageable);
        }

        model.addAttribute("productos", pageProductos.getContent());
        model.addAttribute("page", pageProductos);
        model.addAttribute("filtroBusqueda", busquedaLimpia);
        model.addAttribute("filtroEstado", estadoEnum != null ? estadoEnum.name() : null);

        // KPIs para dashboard
        model.addAttribute("totalProductos", productoService.contarTotal());
        model.addAttribute("totalBajoStock", productoService.contarBajoStock());
        model.addAttribute("totalProximosAVencer", productoService.contarProximosAVencer());
        model.addAttribute("totalVencidos", productoService.contarVencidos());

        return "productos";
    }
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        productoService.actualizarEstadosPorStockYVencimiento();
        
        model.addAttribute("totalProductos", productoService.contarTotal());
        model.addAttribute("totalBajoStock", productoService.contarBajoStock());
        model.addAttribute("totalProximosAVencer", productoService.contarProximosAVencer());
        model.addAttribute("totalVencidos", productoService.contarVencidos());
        model.addAttribute("totalVentasHoy", ventaService.contarVentasHoy()); // si tienes este método
        
        // Opcional: últimos productos o ventas
        model.addAttribute("ultimosProductos", productoService.listarUltimos(5));
        
        return "dashboard"; // crea una vista dashboard.html si quieres algo más visual
    }

    @GetMapping("/nuevo")
    public String mostrarFormularioNuevoProducto(Model model) {
        model.addAttribute("producto", new Producto());
        model.addAttribute("proveedores", proveedorService.listarTodos());
        return "producto-form";
    }

    @PostMapping("/guardar")
    public String guardarProducto(
            @ModelAttribute("producto") Producto producto,
            @RequestParam(value = "imagenArchivo", required = false) MultipartFile imagenArchivo,
            RedirectAttributes redirectAttributes) {

        // Mantener imagen existente si no se sube una nueva en edición
        if (producto.getId() != null && (imagenArchivo == null || imagenArchivo.isEmpty())) {
            Producto original = productoService.buscarPorId(producto.getId());
            producto.setImagenUrl(original.getImagenUrl());
        }

        // Subir imagen si se proporciona
        if (imagenArchivo != null && !imagenArchivo.isEmpty()) {
            try {
                String nombreArchivo = System.currentTimeMillis() + "_" + imagenArchivo.getOriginalFilename();
                Path ruta = Paths.get("uploads/productos");
                Files.createDirectories(ruta);
                Path rutaCompleta = ruta.resolve(nombreArchivo);
                Files.write(rutaCompleta, imagenArchivo.getBytes());
                producto.setImagenUrl("productos/" + nombreArchivo);
            } catch (IOException e) {
                logger.error("Error al subir imagen", e);
                redirectAttributes.addFlashAttribute("mensaje", "Producto guardado, pero error al subir imagen");
                redirectAttributes.addFlashAttribute("tipo", "warning");
            }
        }

        try {
            Producto savedProducto = productoService.guardar(producto);
            productoService.actualizarEstadoYAlertasDeUnProducto(savedProducto);
            redirectAttributes.addFlashAttribute("mensaje", "Producto guardado correctamente");
            redirectAttributes.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            logger.error("Error al guardar producto", e);
            redirectAttributes.addFlashAttribute("mensaje", "Error al guardar el producto: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipo", "danger");
        }

        return "redirect:/productos";
    }

    @GetMapping("/editar/{id}")
    public String editarProducto(@PathVariable Long id, Model model) {
        Producto producto = productoService.buscarPorId(id);
        model.addAttribute("producto", producto);
        model.addAttribute("proveedores", proveedorService.listarTodos());
        return "producto-form";
    }

    @GetMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productoService.eliminarPorId(id);
            redirectAttributes.addFlashAttribute("mensaje", "Producto eliminado correctamente");
            redirectAttributes.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            logger.error("Error al eliminar producto {}", id, e);
            redirectAttributes.addFlashAttribute("mensaje", "Error al eliminar el producto");
            redirectAttributes.addFlashAttribute("tipo", "danger");
        }
        return "redirect:/productos";
    }

    @GetMapping("/venta")
    public String mostrarFormularioVenta(Model model) {
        model.addAttribute("productos", productoService.listarTodos());
        return "venta-form";
    }

    // Venta individual (mantiene compatibilidad)
    @PostMapping("/venta")
    public String registrarVenta(
            @RequestParam Long productoId,
            @RequestParam Integer cantidadVendida,
            RedirectAttributes redirect) {
        try {
            productoService.registrarVenta(productoId, cantidadVendida);
            redirect.addFlashAttribute("mensaje", "Venta registrada correctamente");
            redirect.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirect.addFlashAttribute("mensaje", e.getMessage());
            redirect.addFlashAttribute("tipo", "danger");
        }
        return "redirect:/productos/venta";
    }
 // Agrega o corrige este método
    @GetMapping("/ventas")   // ← ruta completa: /productos/ventas
    public String listarVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long productoId,
            @PageableDefault(size = 15, sort = "fecha", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {

        LocalDateTime desdeDT = (desde != null) ? desde.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime hastaDT = (hasta != null) ? hasta.atTime(23, 59, 59, 999999999) : LocalDate.now().atTime(23, 59, 59, 999999999);

        Page<Venta> page = ventaService.buscarVentas(desdeDT, hastaDT, productoId, pageable);
        BigDecimal total = ventaService.calcularTotalVentasPeriodo(desdeDT, hastaDT, productoId);

        model.addAttribute("ventas", page.getContent());
        model.addAttribute("page", page);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);
        model.addAttribute("productoId", productoId);
        model.addAttribute("totalPeriodo", total);
        model.addAttribute("productos", productoService.listarTodos());

        return "ventas";   // ← debe devolver el nombre de la plantilla: ventas.html
    }

    // Venta múltiple → cambiar a ventaService si ya migraste
    @PostMapping("/venta/multiple")
    public String registrarVentaMultiple(
            @RequestParam String carritoData,
            RedirectAttributes redirect) {
        try {
            List<Map<String, Object>> carrito = objectMapper.readValue(
                    carritoData,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            for (Map<String, Object> item : carrito) {
                Long productoId = Long.parseLong(item.get("productoId").toString());
                Integer cantidad = Integer.parseInt(item.get("cantidad").toString());
                ventaService.registrarVenta(productoId, cantidad);  // ← cambiar a ventaService
            }
            redirect.addFlashAttribute("mensaje", "Compra múltiple registrada correctamente (" + carrito.size() + " productos)");
            redirect.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            logger.error("Error al procesar venta múltiple", e);
            redirect.addFlashAttribute("mensaje", "Error al registrar la compra: " + e.getMessage());
            redirect.addFlashAttribute("tipo", "danger");
        }
        return "redirect:/productos/venta";
    }

    @GetMapping("/exportar-ventas")
    public ResponseEntity<?> exportarVentasTotales() {
        try {
            byte[] excelBytes = productoService.generarReporteVentasExcel();

            if (excelBytes == null || excelBytes.length == 0) {
                HttpHeaders headers = new HttpHeaders();
                headers.setLocation(URI.create("/productos"));
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            }

            ByteArrayResource resource = new ByteArrayResource(excelBytes);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"reporte_ventas_" + LocalDate.now() + ".xlsx\"");
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(excelBytes.length)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error al generar reporte de ventas", e);
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create("/productos"));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
    }




    @GetMapping("/escaneo")
    public String vistaEscaneoProducto() {
        return "producto-escaneo";
    }

    @GetMapping("/buscar-codigo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> buscarPorCodigoAjax(@RequestParam String codigo) {
        Producto producto = productoService.buscarPorCodigo(codigo);
        if (producto != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("id", producto.getId());
            response.put("codigo", producto.getCodigo());
            response.put("nombre", producto.getNombre());
            response.put("cantidad", producto.getCantidad());
            response.put("precioUnidad", producto.getPrecioUnidad());
            response.put("estado", producto.getEstado().name());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }
}