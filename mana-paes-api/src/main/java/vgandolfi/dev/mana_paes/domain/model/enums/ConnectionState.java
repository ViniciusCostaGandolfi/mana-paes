package vgandolfi.dev.mana_paes.domain.model.enums;

/**
 * Estado da conexão WhatsApp global (instância única "mana-paes" da Evolution API).
 *
 * <p>Os valores espelham os estados da Evolution API ({@code close},
 * {@code open}) mais o estado intermediário {@code CONNECTING} usado enquanto
 * um QR code está pendente de scan.</p>
 */
public enum ConnectionState {
    CLOSE,
    CONNECTING,
    OPEN
}