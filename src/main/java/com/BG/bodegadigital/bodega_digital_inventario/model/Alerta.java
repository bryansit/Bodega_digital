package com.BG.bodegadigital.bodega_digital_inventario.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // STOCK / VENCIMIENTO u otros tipos que quieras agregar
    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "mensaje", nullable = false, length = 255)
    private String mensaje;

    // Nivel de urgencia: BAJA, MEDIA, ALTA, por ejemplo
    @Column(name = "nivel_urgencia", length = 20)
    private String nivelUrgencia;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "resuelta", nullable = false)
    private boolean resuelta = false;

    // Relación opcional con el producto al que hace referencia la alerta
    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    public Alerta() {
    }

    // Getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getNivelUrgencia() {
        return nivelUrgencia;
    }

    public void setNivelUrgencia(String nivelUrgencia) {
        this.nivelUrgencia = nivelUrgencia;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public boolean isResuelta() {
        return resuelta;
    }

    public void setResuelta(boolean resuelta) {
        this.resuelta = resuelta;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }
}
