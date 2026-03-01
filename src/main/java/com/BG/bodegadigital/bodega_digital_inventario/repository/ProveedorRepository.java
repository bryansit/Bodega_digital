package com.BG.bodegadigital.bodega_digital_inventario.repository;

import com.BG.bodegadigital.bodega_digital_inventario.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

//ProveedorRepository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
 Optional<Proveedor> findByIdentificacion(String identificacion);
}
