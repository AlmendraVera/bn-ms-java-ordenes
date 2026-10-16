package cl.duoc.bn.ordenes.repository;

import cl.duoc.bn.ordenes.model.EstadoOrden;
import cl.duoc.bn.ordenes.model.Orden;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdenRepository extends JpaRepository<Orden, Long> {

    Optional<Orden> findByCodigoExterno(String codigoExterno);

    boolean existsByCodigoExterno(String codigoExterno);

    List<Orden> findByEstado(EstadoOrden estado);
}