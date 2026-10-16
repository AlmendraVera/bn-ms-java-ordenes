package cl.duoc.bn.ordenes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Orden de venta. Tiene muchos detalles (productos) y muchos registros de seguimiento. */
@Entity
@Table(name = "ordenes")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador de la venta en el canal externo (ej. id del pedido de Shopify). Evita duplicados. */
    @Column(name = "codigo_externo", nullable = false, unique = true)
    private String codigoExterno;

    @Column(nullable = false)
    private String cliente;

    /** Canal de venta: SHOPIFY, PANEL_COMERCIO, etc. */
    private String canal;

    @Enumerated(EnumType.STRING)
    private EstadoOrden estado;

    private LocalDateTime fechaCreacion;

    private Integer total;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleOrden> detalles = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC, id ASC")
    private List<SeguimientoOrden> seguimientos = new ArrayList<>();

    public Orden() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoExterno() {
        return codigoExterno;
    }

    public void setCodigoExterno(String codigoExterno) {
        this.codigoExterno = codigoExterno;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public EstadoOrden getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrden estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<DetalleOrden> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleOrden> detalles) {
        this.detalles = detalles;
    }

    public List<SeguimientoOrden> getSeguimientos() {
        return seguimientos;
    }

    public void setSeguimientos(List<SeguimientoOrden> seguimientos) {
        this.seguimientos = seguimientos;
    }
}