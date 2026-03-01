package com.BG.bodegadigital.bodega_digital_inventario.repository;

import com.BG.bodegadigital.bodega_digital_inventario.model.EstadoProducto;
import com.BG.bodegadigital.bodega_digital_inventario.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad {@link Producto}.
 */
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Búsquedas básicas
    Optional<Producto> findByCodigo(String codigo);

    // Por estado
    List<Producto> findByEstado(EstadoProducto estado);
    long countByEstado(EstadoProducto estado);

    // Por vencimiento
    List<Producto> findByFechaVencimientoBefore(LocalDate fecha);
    List<Producto> findByFechaVencimientoBetween(LocalDate desde, LocalDate hasta);

    // Conteos múltiples estados (útil para dashboard)
    long countByEstadoIn(List<EstadoProducto> estados);

    // -------------------------------------------------------------------------
    // Búsqueda avanzada con filtro por nombre/código (sin acentos) + estado
    // Versión lista completa (para compatibilidad con código antiguo)
    // -------------------------------------------------------------------------
    @Query("""
        SELECT p FROM Producto p
        WHERE (:busqueda IS NULL OR (
            LOWER(
                REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    p.nombre, 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'), 'ñ','n')
            ) LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR
            LOWER(
                REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    p.codigo, 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'), 'ñ','n')
            ) LIKE LOWER(CONCAT('%', :busqueda, '%'))
        ))
        AND (:estado IS NULL OR p.estado = :estado)
        """)
    List<Producto> buscarConFiltros(
            @Param("busqueda") String busqueda,
            @Param("estado") EstadoProducto estado
    );

    // Versión paginada (la que usarás desde el controlador)
    @Query("""
        SELECT p FROM Producto p
        WHERE (:busqueda IS NULL OR (
            LOWER(
                REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    p.nombre, 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'), 'ñ','n')
            ) LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR
            LOWER(
                REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    p.codigo, 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'), 'ñ','n')
            ) LIKE LOWER(CONCAT('%', :busqueda, '%'))
        ))
        AND (:estado IS NULL OR p.estado = :estado)
        """)
    Page<Producto> buscarConFiltrosPaginado(
            @Param("busqueda") String busqueda,
            @Param("estado") EstadoProducto estado,
            Pageable pageable
    );

    // Versión con ordenamiento fijo por nombre (si la necesitas en algún lugar)
    @Query("""
        SELECT p FROM Producto p
        WHERE (:busqueda IS NULL OR (
            LOWER(
                REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    p.nombre, 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'), 'ñ','n')
            ) LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR
            LOWER(
                REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    p.codigo, 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'), 'ñ','n')
            ) LIKE LOWER(CONCAT('%', :busqueda, '%'))
        ))
        AND (:estado IS NULL OR p.estado = :estado)
        ORDER BY p.nombre ASC
        """)
    List<Producto> buscarConFiltrosOrdenadoPorNombre(
            @Param("busqueda") String busqueda,
            @Param("estado") EstadoProducto estado
    );

    // Conteo con filtros (útil para validaciones o estadísticas)
    @Query("""
        SELECT COUNT(p) FROM Producto p
        WHERE (:busqueda IS NULL OR (
            LOWER(
                REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    p.nombre, 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'), 'ñ','n')
            ) LIKE LOWER(CONCAT('%', :busqueda, '%'))
            OR
            LOWER(
                REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
                    p.codigo, 'á','a'), 'é','e'), 'í','i'), 'ó','o'), 'ú','u'), 'ñ','n')
            ) LIKE LOWER(CONCAT('%', :busqueda, '%'))
        ))
        AND (:estado IS NULL OR p.estado = :estado)
        """)
    long countByFiltros(
            @Param("busqueda") String busqueda,
            @Param("estado") EstadoProducto estado
    );

    // Productos con stock bajo (opcional, umbral fijo)
    @Query("SELECT p FROM Producto p WHERE p.cantidad <= :umbralStock")
    List<Producto> findConStockBajo(@Param("umbralStock") int umbralStock);

    // Paginación básica (ya heredado de JpaRepository, pero lo dejamos explícito)
    Page<Producto> findAll(Pageable pageable);
}