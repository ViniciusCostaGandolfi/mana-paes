package vgandolfi.dev.mana_paes.infrastructure.evolution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiException;
import vgandolfi.dev.mana_paes.infrastructure.notification.EvolutionApiNotConfiguredException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mapeamento HTTP do {@link EvolutionClient} (TODOS os endpoints da Evolution
 * API v2.3.7): createInstance (token em hash/hash.apikey/instance.apikey,
 * instanceName/instanceId/status), connectInstance (QR), connectionState,
 * fetchInstances, restart/logout/delete/setPresence, sendText/sendMedia/
 * sendButtons/sendList. URL/chave em branco → {@link EvolutionApiNotConfiguredException}.
 */
@ExtendWith(MockitoExtension.class)
class EvolutionClientTest {

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
                new AppProperties.Evolution(url, globalKey, 0L, "mana-paes", false),
                new AppProperties.Backend(""),
                new AppProperties.Frontend("http://localhost"),
                new AppProperties.Mail(false),
                new AppProperties.Notifications(false, 2),
                new AppProperties.Scheduler(false),
                new AppProperties.SystemAdmin("vinicius@vgandolfi.dev", "admin123"));
    }

    private EvolutionClient client() {
        return new EvolutionClient(restClient, props("http://evolution:8080", "global-key"));
    }

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
    private void stubPostBodiless() {
        when(restClient.post()).thenReturn(postSpec);
        lenient().when(postSpec.uri(anyString())).thenReturn(postSpec);
        lenient().when(postSpec.uri(anyString(), any(Object[].class))).thenReturn(postSpec);
        when(postSpec.header(anyString(), anyString())).thenReturn(postSpec);
        lenient().when(postSpec.contentType(any(MediaType.class))).thenReturn(postSpec);
        lenient().when(postSpec.body(any(Object.class))).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void stubDelete() {
        when(restClient.delete()).thenReturn(deleteSpec);
        lenient().when(deleteSpec.uri(anyString())).thenReturn(deleteSpec);
        when(deleteSpec.uri(anyString(), any(Object[].class))).thenReturn(deleteSpec);
        when(deleteSpec.header(anyString(), anyString())).thenReturn(deleteSpec);
        when(deleteSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());
    }

    // ------------------------------------------------------------------
    // createInstance
    // ------------------------------------------------------------------

    @Test
    void createInstanceParsesTokenAndInstanceMetadata() throws Exception {
        stubPost(MAPPER.readTree(
                "{\"instance\":{\"instanceName\":\"VGandolfi Dev-1234\",\"id\":\"inst-9\",\"status\":\"open\"},\"hash\":\"token-abc\"}"));

        EvolutionClient.CreateInstanceResult result = client().createInstance("VGandolfi Dev-1234", null);

        assertThat(result.instanceName()).isEqualTo("VGandolfi Dev-1234");
        assertThat(result.instanceId()).isEqualTo("inst-9");
        assertThat(result.status()).isEqualTo("open");
        assertThat(result.token()).isEqualTo("token-abc");
        verify(postSpec).uri("/instance/create");
        verify(postSpec).header("apikey", "global-key");
    }

    @Test
    void createInstanceExtractsTokenFromHashApikey() throws Exception {
        stubPost(MAPPER.readTree("{\"instance\":{\"instanceName\":\"x\"},\"hash\":{\"apikey\":\"token-def\"}}"));

        EvolutionClient.CreateInstanceResult result = client().createInstance("x", null);

        assertThat(result.token()).isEqualTo("token-def");
    }

    @Test
    void createInstanceExtractsTokenFromInstanceApikey() throws Exception {
        stubPost(MAPPER.readTree("{\"instance\":{\"instanceName\":\"x\",\"apiKey\":\"token-ghi\"}}"));

        assertThat(client().createInstance("x", null).token()).isEqualTo("token-ghi");
    }

    @Test
    void createInstanceWithoutTokenThrows() throws Exception {
        stubPost(MAPPER.readTree("{\"instance\":{\"instanceName\":\"x\"}}"));

        assertThatThrownBy(() -> client().createInstance("x", null))
                .isInstanceOf(EvolutionApiException.class)
                .hasMessageContaining("token");
    }

    @Test
    void createInstanceWithoutUrlThrowsNotConfigured() {
        EvolutionClient notConfigured = new EvolutionClient(restClient, props("", "global-key"));

        assertThatThrownBy(() -> notConfigured.createInstance("x", null))
                .isInstanceOf(EvolutionApiNotConfiguredException.class);
    }

    @Test
    void createInstanceWithoutGlobalKeyThrowsNotConfigured() {
        EvolutionClient noKey = new EvolutionClient(restClient, props("http://evolution:8080", " "));

        assertThatThrownBy(() -> noKey.createInstance("x", null))
                .isInstanceOf(EvolutionApiNotConfiguredException.class);
    }

    // ------------------------------------------------------------------
    // connectInstance / getConnectionState / fetchInstances
    // ------------------------------------------------------------------

    @Test
    void connectInstanceExtractsQrFromQrcodeBase64() throws Exception {
        stubGet(MAPPER.readTree("{\"qrcode\":{\"base64\":\"QUJD\"}}"));

        assertThat(client().connectInstance("x", "token")).isEqualTo("data:image/png;base64,QUJD");
        verify(getSpec).uri("/instance/connect/{instance}", "x");
        verify(getSpec).header("apikey", "token");
    }

    @Test
    void getConnectionStateParsesStateAndWuid() throws Exception {
        stubGet(MAPPER.readTree("{\"state\":\"open\",\"wuid\":\"5511999999999@s.whatsapp.net\"}"));

        EvolutionClient.ConnectionStateInfo info = client().getConnectionState("x", "token");

        assertThat(info.state()).isEqualTo("open");
        assertThat(info.wuid()).isEqualTo("5511999999999@s.whatsapp.net");
    }

    @Test
    void fetchInstancesParsesList() throws Exception {
        stubGet(MAPPER.readTree(
                "[{\"instanceName\":\"a\",\"status\":\"open\",\"connectionStatus\":\"open\"},"
                        + "{\"instanceName\":\"b\",\"status\":\"close\",\"connectionStatus\":\"close\"}]"));

        List<EvolutionClient.InstanceInfo> instances = client().fetchInstances();

        assertThat(instances).hasSize(2);
        assertThat(instances.get(0).instanceName()).isEqualTo("a");
        assertThat(instances.get(1).connectionStatus()).isEqualTo("close");
        verify(getSpec).uri("/instance/fetchInstances");
    }

    @Test
    void fetchInstancesReturnsEmptyOnNonArray() throws Exception {
        stubGet(MAPPER.readTree("{\"error\":\"x\"}"));

        assertThat(client().fetchInstances()).isEmpty();
    }

    // ------------------------------------------------------------------
    // restart / logout / delete / setPresence
    // ------------------------------------------------------------------

    @Test
    void restartInstanceCallsPost() {
        stubPostBodiless();

        client().restartInstance("x", "token");

        verify(postSpec).uri("/instance/restart/{instance}", "x");
        verify(postSpec).header("apikey", "token");
    }

    @Test
    void logoutInstanceCallsDelete() {
        stubDelete();

        client().logoutInstance("x", "token");

        verify(deleteSpec).uri("/instance/logout/{instance}", "x");
        verify(deleteSpec).header("apikey", "token");
    }

    @Test
    void deleteInstanceCallsDelete() {
        stubDelete();

        client().deleteInstance("x", "token");

        verify(deleteSpec).uri("/instance/delete/{instance}", "x");
        verify(deleteSpec).header("apikey", "token");
    }

    @Test
    void setPresenceSendsPresenceBody() {
        stubPostBodiless();

        client().setPresence("x", "token", "available");

        verify(postSpec).uri("/instance/setPresence/{instance}", "x");
        verify(postSpec).header("apikey", "token");
        verify(postSpec).body(Map.of("presence", "available"));
    }

    // ------------------------------------------------------------------
    // sendText / sendMedia / sendButtons / sendList
    // ------------------------------------------------------------------

    @Test
    void sendTextUsesNumberAndTextBody() {
        stubPostBodiless();

        client().sendText("x", "token", "5511999999999", "olá");

        verify(postSpec).uri("/message/sendText/{instance}", "x");
        verify(postSpec).header("apikey", "token");
        verify(postSpec).body(Map.of("number", "5511999999999", "text", "olá"));
    }

    @Test
    void sendMediaIncludesCaptionWhenPresent() {
        stubPostBodiless();

        client().sendMedia("x", "token", "5511999999999", "image", "data:image/png;base64,x", "legenda");

        verify(postSpec).uri("/message/sendMedia/{instance}", "x");
        verify(postSpec).body(any(Object.class));
        verify(postSpec).header("apikey", "token");
    }

    @Test
    void sendButtonsSendsButtonsList() {
        stubPostBodiless();

        client().sendButtons("x", "token", "5511999999999", "Título", "Descrição", null,
                List.of(Map.of("text", "Sim")));

        verify(postSpec).uri("/message/sendButtons/{instance}", "x");
        verify(postSpec).body(any(Object.class));
        verify(postSpec).header("apikey", "token");
    }

    @Test
    void sendListSendsSections() {
        stubPostBodiless();

        client().sendList("x", "token", "5511999999999", "Título", "Descrição", "Ver mais",
                List.of(Map.of("title", "Seção")));

        verify(postSpec).uri("/message/sendList/{instance}", "x");
        verify(postSpec).body(any(Object.class));
        verify(postSpec).header("apikey", "token");
    }

    @Test
    void sendTextWithoutUrlThrowsNotConfigured() {
        EvolutionClient notConfigured = new EvolutionClient(restClient, props("", "key"));

        assertThatThrownBy(() -> notConfigured.sendText("x", "token", "5511", "oi"))
                .isInstanceOf(EvolutionApiNotConfiguredException.class);
    }

    @Test
    void sendTextWithoutApiKeyThrowsNotConfigured() {
        assertThatThrownBy(() -> client().sendText("x", null, "5511", "oi"))
                .isInstanceOf(EvolutionApiNotConfiguredException.class);
    }

    @Test
    void sendTextWithoutInstanceThrowsNotConfigured() {
        assertThatThrownBy(() -> client().sendText(" ", "token", "5511", "oi"))
                .isInstanceOf(EvolutionApiNotConfiguredException.class);
    }
}