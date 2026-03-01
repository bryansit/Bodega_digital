package com.BG.bodegadigital.bodega_digital_inventario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventas")
@Getter
@Setter
@NoArgsConstructor  // recomendado para JPA + Lombok
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;  // snapshot del precio en el momento de la venta

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;           // cantidad × precioUnitario

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 100)
    private String usuario;             // opcional: registrado por...
    
    // Constructor recomendado para crear ventas desde el service
    public Venta(Producto producto, Integer cantidad, BigDecimal precioUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.total = BigDecimal.valueOf(cantidad).multiply(precioUnitario);
        this.fecha = LocalDateTime.now();
        // this.usuario = ... (puedes pasarlo como parámetro o tomarlo de SecurityContext)
    }

    // Método para recalcular total (por si acaso se modifica cantidad o precio después)
    public void recalcularTotal() {
        if (this.cantidad != null && this.precioUnitario != null) {
            this.total = BigDecimal.valueOf(this.cantidad).multiply(this.precioUnitario);
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.fecha == null) {
            this.fecha = LocalDateTime.now();
        }
        if (this.total == null) {
            recalcularTotal();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Opcional: si permites modificar una venta después de creada
        recalcularTotal();
    }
}