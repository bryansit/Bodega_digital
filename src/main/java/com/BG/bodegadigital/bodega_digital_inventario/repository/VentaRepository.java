package com.BG.bodegadigital.bodega_digital_inventario.repository;

import com.BG.bodegadigital.bodega_digital_inventario.model.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // Versión con JOIN FETCH para cargar producto inmediatamente
    @Query("SELECT v FROM Venta v JOIN FETCH v.producto " +
           "WHERE v.fecha BETWEEN :desde AND :hasta " +
           "ORDER BY v.fecha DESC")
    Page<Venta> findByFechaBetweenWithProducto(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    // Si filtras por productoId también
    @Query("SELECT v FROM Venta v JOIN FETCH v.producto " +
           "WHERE v.fecha BETWEEN :desde AND :hasta " +
           "AND (:productoId IS NULL OR v.producto.id = :productoId) " +
           "ORDER BY v.fecha DESC")
    Page<Venta> findByFechaBetweenAndProductoIdWithProducto(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("productoId") Long productoId,
            Pageable pageable);

    // El cálculo de total se mantiene igual (no necesita fetch)
    @Query("SELECT COALESCE(SUM(v.total), 0) " +
           "FROM Venta v " +
           "WHERE v.fecha BETWEEN :desde AND :hasta " +
           "AND (:productoId IS NULL OR v.producto.id = :productoId)")
    BigDecimal calcularTotalPeriodo(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("productoId") Long productoId);

	Page<Venta> findByFechaBetweenAndProductoId(LocalDate desde, LocalDate hasta, Long productoId, Pageable pageable);

	Page<Venta> findByFechaBetween(LocalDate desde, LocalDate hasta, Pageable pageable);
}