package cl.duoc.bn.ordenes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Un registro del historial (tracking) de una orden: cada cambio de estado deja una fila. */
@Entity
@Table(name = "seguimientos_orden")
public class SeguimientoOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private EstadoOrden estado;

    private LocalDateTime fecha;

    private String comentario;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "orden_id")
    private Orden orden;

    public SeguimientoOrden() {
    }

    public SeguimientoOrden(Orden orden, EstadoOrden estado, String comentario) {
        this.orden = orden;
        this.estado = estado;
        this.comentario = comentario;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EstadoOrden getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrden estado) {
        this.estado = estado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public Orden getOrden() {
        return orden;
    }

    public void setOrden(Orden orden) {
        this.orden = orden;
    }
}