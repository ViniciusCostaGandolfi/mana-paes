package vgandolfi.dev.mana_paes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

/**
 * Estabiliza a serialização de {@code Page} (evita o warning de
 * "Serializing PageImpl instances as-is is not supported"): os metadados de
 * paginação passam a ser serializados como {@code page} (DTO estável) em vez de
 * campos top-level instáveis. {@code content} permanece como antes.
 */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class WebDataSupportConfig {
}