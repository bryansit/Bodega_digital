package com.BG.bodegadigital.bodega_digital_inventario.service;

import com.BG.bodegadigital.bodega_digital_inventario.model.Proveedor;

import java.util.List;

public interface ProveedorService {

    List<Proveedor> listarTodos();

    Proveedor guardar(Proveedor proveedor);

    Proveedor buscarPorId(Long id);

    void eliminar(Long id);

    Proveedor buscarPorIdentificacion(String identificacion);
}
