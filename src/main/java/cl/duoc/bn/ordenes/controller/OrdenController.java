package cl.duoc.bn.ordenes.controller;

import cl.duoc.bn.ordenes.model.EstadoOrden;
import cl.duoc.bn.ordenes.model.Orden;
import cl.duoc.bn.ordenes.service.OrdenService;
import cl.duoc.bn.ordenes.service.OrdenService.Tracking;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    /** Cuerpo de PUT /api/ordenes/{id}/estado */
    public record CambioEstadoRequest(EstadoOrden estado, String comentario) {
    }

    private final OrdenService ordenService;

    public OrdenController(OrdenService ordenService) {
        this.ordenService = ordenService;
    }

    /** POST /api/ordenes : también es el destino del aviso (webhook) de venta. */
    @PostMapping
    public ResponseEntity<Orden> crear(@RequestBody Orden orden) {
        Orden creada = ordenService.crear(orden);
        return ResponseEntity.created(URI.create("/api/ordenes/" + creada.getId())).body(creada);
    }

    /** GET /api/ordenes  o  GET /api/ordenes?estado=CREADA */
    @GetMapping
    public List<Orden> listar(@RequestParam(required = false) EstadoOrden estado) {
        return ordenService.listar(estado);
    }

    @GetMapping("/{id}")
    public Orden buscarPorId(@PathVariable Long id) {
        return ordenService.buscarPorId(id);
    }

    @GetMapping("/codigo/{codigoExterno}")
    public Orden buscarPorCodigoExterno(@PathVariable String codigoExterno) {
        return ordenService.buscarPorCodigoExterno(codigoExterno);
    }

    @GetMapping("/{id}/tracking")
    public Tracking tracking(@PathVariable Long id) {
        return ordenService.tracking(id);
    }

    @PutMapping("/{id}")
    public Orden actualizar(@PathVariable Long id, @RequestBody Orden orden) {
        return ordenService.actualizar(id, orden);
    }

    @PutMapping("/{id}/estado")
    public Orden cambiarEstado(@PathVariable Long id, @RequestBody CambioEstadoRequest peticion) {
        return ordenService.cambiarEstado(id, peticion.estado(), peticion.comentario());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ordenService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}