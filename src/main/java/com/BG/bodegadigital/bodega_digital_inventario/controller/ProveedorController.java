package com.BG.bodegadigital.bodega_digital_inventario.controller;

import com.BG.bodegadigital.bodega_digital_inventario.model.Proveedor;
import com.BG.bodegadigital.bodega_digital_inventario.service.ProveedorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    // Listar proveedores
    @GetMapping
    public String listarProveedores(Model model) {
        model.addAttribute("proveedores", proveedorService.listarTodos());
        return "proveedores";
    }

    // Formulario nuevo proveedor
    @GetMapping("/nuevo")
    public String mostrarFormularioNuevoProveedor(Model model) {
        model.addAttribute("proveedor", new Proveedor());
        return "proveedor-form";
    }

    // Guardar proveedor
    @PostMapping("/guardar")
    public String guardarProveedor(@ModelAttribute("proveedor") Proveedor proveedor) {
        proveedorService.guardar(proveedor);
        return "redirect:/proveedores";
    }

    // Editar proveedor
    @GetMapping("/editar/{id}")
    public String editarProveedor(@PathVariable Long id, Model model) {
        Proveedor proveedor = proveedorService.buscarPorId(id);
        model.addAttribute("proveedor", proveedor);
        return "proveedor-form";
    }

    // Eliminar proveedor
    @GetMapping("/eliminar/{id}")
    public String eliminarProveedor(@PathVariable Long id) {
        proveedorService.eliminar(id);
        return "redirect:/proveedores";
    }
}
