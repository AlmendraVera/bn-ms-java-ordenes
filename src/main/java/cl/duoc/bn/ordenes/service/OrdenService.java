package cl.duoc.bn.ordenes.service;

import cl.duoc.bn.ordenes.client.InventarioClient;
import cl.duoc.bn.ordenes.client.InventarioClient.ProductoDto;
import cl.duoc.bn.ordenes.exception.ApiException;
import cl.duoc.bn.ordenes.model.DetalleOrden;
import cl.duoc.bn.ordenes.model.EstadoOrden;
import cl.duoc.bn.ordenes.model.Orden;
import cl.duoc.bn.ordenes.model.SeguimientoOrden;
import cl.duoc.bn.ordenes.repository.OrdenRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdenService {

    /** Respuesta del endpoint de tracking. */
    public record Tracking(Long ordenId, String codigoExterno, EstadoOrden estadoActual,
            List<SeguimientoOrden> historial) {
    }

    private final OrdenRepository ordenRepository;
    private final InventarioClient inventarioClient;

    public OrdenService(OrdenRepository ordenRepository, InventarioClient inventarioClient) {
        this.ordenRepository = ordenRepository;
        this.inventarioClient = inventarioClient;
    }

    public List<Orden> listar(EstadoOrden estado) {
        if (estado != null) {
            return ordenRepository.findByEstado(estado);
        }
        return ordenRepository.findAll();
    }

    public Orden buscarPorId(Long id) {
        return ordenRepository.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("No existe una orden con id " + id));
    }

    public Orden buscarPorCodigoExterno(String codigo) {
        return ordenRepository.findByCodigoExterno(codigo)
                .orElseThrow(() -> ApiException.noEncontrado("No existe una orden con código externo " + codigo));
    }

    /**
     * Crea una orden: evita duplicados por código externo (webhooks repetidos),
     * valida el stock de cada SKU en inventario y toma el precio actual.
     */
    @Transactional
    public Orden crear(Orden orden) {
        validar(orden);
        String codigo = orden.getCodigoExterno().trim();
        if (ordenRepository.existsByCodigoExterno(codigo)) {
            throw ApiException.conflicto("Ya existe una orden con el código externo " + codigo
                    + " (posible webhook duplicado)");
        }

        Set<String> skusVistos = new HashSet<>();
        int total = 0;
        for (DetalleOrden d : orden.getDetalles()) {
            String sku = d.getSku().trim().toUpperCase();
            if (!skusVistos.add(sku)) {
                throw ApiException.solicitudInvalida("El SKU " + sku + " está repetido en la orden");
            }
            ProductoDto producto = inventarioClient.obtenerProducto(sku);
            if (producto.stock() < d.getCantidad()) {
                throw ApiException.conflicto("Stock insuficiente para el SKU " + sku
                        + ": disponible " + producto.stock() + ", solicitado " + d.getCantidad());
            }
            d.setId(null);
            d.setSku(sku);
            d.setPrecioUnitario(producto.precio());
            d.setOrden(orden);
            total += producto.precio() * d.getCantidad();
        }

        orden.setId(null);
        orden.setCodigoExterno(codigo);
        orden.setCliente(orden.getCliente().trim());
        if (orden.getCanal() == null || orden.getCanal().isBlank()) {
            orden.setCanal("DIRECTO");
        }
        orden.setEstado(EstadoOrden.CREADA);
        orden.setFechaCreacion(LocalDateTime.now());
        orden.setTotal(total);
        orden.getSeguimientos().add(new SeguimientoOrden(orden, EstadoOrden.CREADA, "Orden recibida"));
        return ordenRepository.save(orden);
    }

    /** Solo se pueden editar los datos del cliente mientras la orden está CREADA. */
    public Orden actualizar(Long id, Orden datos) {
        if (datos.getCliente() == null || datos.getCliente().isBlank()) {
            throw ApiException.solicitudInvalida("El cliente es obligatorio");
        }
        Orden orden = buscarPorId(id);
        if (orden.getEstado() != EstadoOrden.CREADA) {
            throw ApiException.conflicto("Solo se puede modificar una orden en estado CREADA (actual: "
                    + orden.getEstado() + ")");
        }
        orden.setCliente(datos.getCliente().trim());
        if (datos.getCanal() != null && !datos.getCanal().isBlank()) {
            orden.setCanal(datos.getCanal());
        }
        return ordenRepository.save(orden);
    }

    @Transactional
    public Orden cambiarEstado(Long id, EstadoOrden nuevo, String comentario) {
        if (nuevo == null) {
            throw ApiException.solicitudInvalida("El estado es obligatorio (CREADA, EN_PICKING, DESPACHADA, CANCELADA)");
        }
        Orden orden = buscarPorId(id);
        if (!transicionValida(orden.getEstado(), nuevo)) {
            throw ApiException.conflicto("Transición no permitida: " + orden.getEstado() + " -> " + nuevo);
        }
        orden.setEstado(nuevo);
        String texto = (comentario == null || comentario.isBlank()) ? "Cambio de estado a " + nuevo : comentario;
        orden.getSeguimientos().add(new SeguimientoOrden(orden, nuevo, texto));
        return ordenRepository.save(orden);
    }

    public Tracking tracking(Long id) {
        Orden orden = buscarPorId(id);
        return new Tracking(orden.getId(), orden.getCodigoExterno(), orden.getEstado(), orden.getSeguimientos());
    }

    public void eliminar(Long id) {
        Orden orden = buscarPorId(id);
        if (orden.getEstado() == EstadoOrden.EN_PICKING || orden.getEstado() == EstadoOrden.DESPACHADA) {
            throw ApiException.conflicto("No se puede eliminar una orden en estado " + orden.getEstado());
        }
        ordenRepository.delete(orden);
    }

    private boolean transicionValida(EstadoOrden actual, EstadoOrden nuevo) {
        return switch (actual) {
            case CREADA -> nuevo == EstadoOrden.EN_PICKING || nuevo == EstadoOrden.CANCELADA;
            case EN_PICKING -> nuevo == EstadoOrden.DESPACHADA || nuevo == EstadoOrden.CANCELADA;
            case DESPACHADA, CANCELADA -> false;
        };
    }

    private void validar(Orden orden) {
        if (orden.getCodigoExterno() == null || orden.getCodigoExterno().isBlank()) {
            throw ApiException.solicitudInvalida("El código externo de la orden es obligatorio");
        }
        if (orden.getCliente() == null || orden.getCliente().isBlank()) {
            throw ApiException.solicitudInvalida("El cliente es obligatorio");
        }
        if (orden.getDetalles() == null || orden.getDetalles().isEmpty()) {
            throw ApiException.solicitudInvalida("La orden debe tener al menos un producto");
        }
        for (DetalleOrden d : orden.getDetalles()) {
            if (d.getSku() == null || d.getSku().isBlank()) {
                throw ApiException.solicitudInvalida("Cada producto debe indicar su SKU");
            }
            if (d.getCantidad() == null || d.getCantidad() <= 0) {
                throw ApiException.solicitudInvalida("La cantidad del SKU " + d.getSku() + " debe ser mayor a 0");
            }
        }
    }
}