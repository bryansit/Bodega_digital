package com.BG.bodegadigital.bodega_digital_inventario.repository;

import com.BG.bodegadigital.bodega_digital_inventario.model.Alerta;
import com.BG.bodegadigital.bodega_digital_inventario.model.Producto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
//AlertaRepository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {

 List<Alerta> findByResueltaFalse();
 List<Alerta> findByResuelta(boolean resuelta);
 List<Alerta> findByResueltaFalseAndTipo(String tipo);
 boolean existsByProductoAndTipoAndResueltaFalse(Producto producto, String tipo);
}
