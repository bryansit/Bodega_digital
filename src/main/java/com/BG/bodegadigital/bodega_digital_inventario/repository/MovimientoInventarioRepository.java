package com.BG.bodegadigital.bodega_digital_inventario.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.BG.bodegadigital.bodega_digital_inventario.model.MovimientoInventario;

public interface MovimientoInventarioRepository
        extends JpaRepository<MovimientoInventario, Long> {

    List<MovimientoInventario> findByProductoIdAndFechaBetween(
            Long productoId, LocalDate inicio, LocalDate fin);
    

    @Modifying
    @Transactional
    @Query("DELETE FROM MovimientoInventario m WHERE m.producto.id = :productoId")
    void deleteByProductoId(@Param("productoId") Long productoId);

	List<MovimientoInventario> findByProductoIdAndCantidadLessThanOrderByFechaDesc(Long productoId, int i);
	// En MovimientoInventarioRepository
	Page<MovimientoInventario> findByCantidadLessThanAndFechaBetween(int cantidadMax, LocalDate desde, LocalDate hasta, Pageable pageable);

	Page<MovimientoInventario> findByCantidadLessThanAndFechaBetweenAndProductoId(int cantidadMax, LocalDate desde, LocalDate hasta, Long productoId, Pageable pageable);
}
