package cl.duoc.bn.ordenes.model;

/** Ciclo de vida de una orden. */
public enum EstadoOrden {
    CREADA,
    EN_PICKING,
    DESPACHADA,
    CANCELADA
}