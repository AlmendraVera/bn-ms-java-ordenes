package cl.duoc.bn.ordenes.client;

import cl.duoc.bn.ordenes.exception.ApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Cliente HTTP hacia bn-ms-java-inventario (valida stock y obtiene el precio). */
@Component
public class InventarioClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductoDto(Long id, String sku, String nombre, Integer precio, Integer stock) {
    }

    private final RestClient restClient;

    public InventarioClient(@Value("${servicios.inventario.url}") String baseUrl) {
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(fabrica).build();
    }

    public ProductoDto obtenerProducto(String sku) {
        try {
            return restClient.get()
                    .uri("/api/productos/sku/{sku}", sku)
                    .retrieve()
                    .body(ProductoDto.class);
        } catch (RestClientResponseException e) {
            throw ErroresRemotos.traducir(e, "inventario");
        } catch (ResourceAccessException e) {
            throw ApiException.servicioNoDisponible("El microservicio de inventario no responde");
        }
    }
}