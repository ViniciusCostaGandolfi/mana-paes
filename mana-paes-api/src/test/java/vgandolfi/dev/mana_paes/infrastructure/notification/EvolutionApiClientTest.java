package vgandolfi.dev.mana_paes.infrastructure.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import vgandolfi.dev.mana_paes.config.AppProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mapeamento HTTP do {@link EvolutionApiClient}: createInstance (parse do token
 * em "hash" string ou "hash.apikey"), connectInstance (QR em qrcode.base64 ou
 * base64 no topo, prefixado como data URI), connectionState (state/wuid),
 * logout e sendText. URL/chave em branco → {@link EvolutionApiNotConfiguredException}.
 */
@ExtendWith(MockitoExtension.class)
class EvolutionApiClientTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private RestClient restClient;
    @Mock
    private RestClient.RequestBodyUriSpec postSpec;
    @SuppressWarnings("rawtypes")
    @Mock
    private RestClient.RequestHeadersUriSpec getSpec;
    @SuppressWarnings("rawtypes")
    @Mock
    private RestClient.RequestHeadersUriSpec deleteSpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    private AppProperties props(String url, String globalKey) {
        return new AppProperties(
                new AppProperties.Jwt("test-secret-test-secret-test-secret-test-secret-1234", 3600000L, 86400000L),
                new AppProperties.Encryption("mana-paes-test-master-key-32chars!"),
                new AppProperties.Evolution(url, globalKey, 0L),
                new AppProperties.Backend(""),
                new AppProperties.Frontend("http://localhost"),
                new AppProperties.Mail(false),
                new AppProperties.Notifications(false, 2),
                new AppProperties.Scheduler(false));
    }

    private EvolutionApiClient client() {
        return new EvolutionApiClient(restClient, props("http://evolution:8080", "global-key"));
    }

    /**
     * Stub do encadeamento POST (uri com/sem varargs, header, contentType, body,
     * retrieve → responseSpec.body(JsonNode)). Marcações lenient cobrem os
     * overloads não usados em cada teste.
     */
    private void stubPost(JsonNode body) {
        when(restClient.post()).thenReturn(postSpec);
        lenient().when(postSpec.uri(anyString())).thenReturn(postSpec);
        lenient().when(postSpec.uri(anyString(), any(Object[].class))).thenReturn(postSpec);
        when(postSpec.header(anyString(), anyString())).thenReturn(postSpec);
        lenient().when(postSpec.contentType(any(MediaType.class))).thenReturn(postSpec);
        lenient().when(postSpec.body(any(Object.class))).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(body);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubGet(JsonNode body) {
        when(restClient.get()).thenReturn(getSpec);
        lenient().when(getSpec.uri(anyString())).thenReturn(getSpec);
        lenient().when(getSpec.uri(anyString(), any(Object[].class))).thenReturn(getSpec);
        when(getSpec.header(anyString(), anyString())).thenReturn(getSpec);
        when(getSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JsonNode.class)).thenReturn(body);
    }

    // ------------------------------------------------------------------
    // createInstance
    // ------------------------------------------------------------------

    @Test
    void createInstanceExtractsTokenFromHashString() throws Exception {
        stubPost(MAPPER.readTree("{\"instance\":{\"instanceName\":\"mana-paes\"},\"hash\":\"token-abc\"}"));

        String token = client().createInstance("mana-paes", "http://backend/api/v1/webhooks/evolution-api");

        assertThat(token).isEqualTo("token-abc");
        verify(restClient).post();
    }

    @Test
    void createInstanceExtractsTokenFromHashApikey() throws Exception {
        stubPost(MAPPER.readTree("{\"instance\":{\"instanceName\":\"mana-paes\"},\"hash\":{\"apikey\":\"token-def\"}}"));

        assertThat(client().createInstance("mana-paes", null)).isEqualTo("token-def");
    }

    @Test
    void createInstanceWithoutTokenThrows() throws Exception {
        stubPost(MAPPER.readTree("{\"instance\":{\"instanceName\":\"mana-paes\"}}"));

        assertThatThrownBy(() -> client().createInstance("mana-paes", null))
                .isInstanceOf(EvolutionApiException.class)
                .hasMessageContaining("token");
    }

    @Test
    void createInstanceWithoutUrlThrowsNotConfigured() {
        EvolutionApiClient notConfigured = new EvolutionApiClient(restClient, props("", "global-key"));

        assertThatThrownBy(() -> notConfigured.createInstance("mana-paes", null))
                .isInstanceOf(EvolutionApiNotConfiguredException.class);
    }

    @Test
    void createInstanceWithoutGlobalKeyThrowsNotConfigured() {
        EvolutionApiClient noKey = new EvolutionApiClient(restClient, props("http://evolution:8080", " "));

        assertThatThrownBy(() -> noKey.createInstance("mana-paes", null))
                .isInstanceOf(EvolutionApiNotConfiguredException.class);
    }

    // ------------------------------------------------------------------
    // connectInstance
    // ------------------------------------------------------------------

    @Test
    void connectInstanceExtractsQrFromQrcodeBase64() throws Exception {
        stubGet(MAPPER.readTree("{\"qrcode\":{\"base64\":\"QUJD\"}}"));

        String qr = client().connectInstance("mana-paes", "token");

        assertThat(qr).isEqualTo("data:image/png;base64,QUJD");
    }

    @Test
    void connectInstanceExtractsQrFromTopLevelBase64() throws Exception {
        stubGet(MAPPER.readTree("{\"base64\":\"QUJD\"}"));

        String qr = client().connectInstance("mana-paes", "token");

        assertThat(qr).isEqualTo("data:image/png;base64,QUJD");
    }

    @Test
    void connectInstanceKeepsDataUriPrefix() throws Exception {
        stubGet(MAPPER.readTree("{\"qrcode\":{\"base64\":\"data:image/png;base64,XYZ\"}}"));

        String qr = client().connectInstance("mana-paes", "token");

        assertThat(qr).isEqualTo("data:image/png;base64,XYZ");
    }

    @Test
    void connectInstanceWithoutQrThrows() throws Exception {
        stubGet(MAPPER.readTree("{\"instance\":{}}"));

        assertThatThrownBy(() -> client().connectInstance("mana-paes", "token"))
                .isInstanceOf(EvolutionApiException.class)
                .hasMessageContaining("QR code");
    }

    @Test
    void connectInstanceWithoutApiKeyThrowsNotConfigured() {
        assertThatThrownBy(() -> client().connectInstance("mana-paes", "  "))
                .isInstanceOf(EvolutionApiNotConfiguredException.class);
    }

    // ------------------------------------------------------------------
    // getConnectionState
    // ------------------------------------------------------------------

    @Test
    void getConnectionStateParsesTopLevelStateAndWuid() throws Exception {
        stubGet(MAPPER.readTree("{\"state\":\"open\",\"wuid\":\"5511999999999@s.whatsapp.net\"}"));

        EvolutionApiClient.ConnectionStateInfo info = client().getConnectionState("mana-paes", "token");

        assertThat(info.state()).isEqualTo("open");
        assertThat(info.wuid()).isEqualTo("5511999999999@s.whatsapp.net");
    }

    @Test
    void getConnectionStateFallsBackToNestedInstance() throws Exception {
        stubGet(MAPPER.readTree("{\"instance\":{\"state\":\"close\",\"wuid\":\"5511888888888@s.whatsapp.net\"}}"));

        EvolutionApiClient.ConnectionStateInfo info = client().getConnectionState("mana-paes", "token");

        assertThat(info.state()).isEqualTo("close");
        assertThat(info.wuid()).isEqualTo("5511888888888@s.whatsapp.net");
    }

    // ------------------------------------------------------------------
    // logoutInstance / sendText
    // ------------------------------------------------------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void logoutInstanceCallsDelete() {
        when(restClient.delete()).thenReturn(deleteSpec);
        lenient().when(deleteSpec.uri(anyString())).thenReturn(deleteSpec);
        when(deleteSpec.uri(anyString(), any(Object[].class))).thenReturn(deleteSpec);
        when(deleteSpec.header(anyString(), anyString())).thenReturn(deleteSpec);
        when(deleteSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        client().logoutInstance("mana-paes", "token");

        verify(restClient).delete();
    }

    @Test
    void sendTextSendsToInstanceEndpoint() {
        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.uri(anyString(), any(Object[].class))).thenReturn(postSpec);
        when(postSpec.header(anyString(), anyString())).thenReturn(postSpec);
        when(postSpec.contentType(any(MediaType.class))).thenReturn(postSpec);
        when(postSpec.body(any(Object.class))).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        client().sendText("mana-paes", "token", "5511999999999", "olá");

        verify(postSpec).uri("/message/sendText/{instance}", "mana-paes");
        verify(postSpec).header("apikey", "token");
    }

    @Test
    void sendTextWithoutUrlThrowsNotConfigured() {
        EvolutionApiClient notConfigured = new EvolutionApiClient(restClient, props("", "key"));

        assertThatThrownBy(() -> notConfigured.sendText("mana-paes", "token", "5511", "oi"))
                .isInstanceOf(EvolutionApiNotConfiguredException.class);
    }

    @Test
    void sendTextWithoutApiKeyThrowsNotConfigured() {
        assertThatThrownBy(() -> client().sendText("mana-paes", null, "5511", "oi"))
                .isInstanceOf(EvolutionApiNotConfiguredException.class);
    }

    @Test
    void sendTextWithoutInstanceThrowsNotConfigured() {
        assertThatThrownBy(() -> client().sendText(" ", "token", "5511", "oi"))
                .isInstanceOf(EvolutionApiNotConfiguredException.class);
    }
}