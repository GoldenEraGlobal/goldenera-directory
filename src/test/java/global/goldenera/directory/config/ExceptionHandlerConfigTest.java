package global.goldenera.directory.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import global.goldenera.cryptoj.enums.Network;
import global.goldenera.directory.exceptions.NodeVersionUnsupportedException;

class ExceptionHandlerConfigTest {

    @Test
    void unsupportedVersionResponseIsMachineReadableAndUsesUpgradeRequired() {
        ExceptionHandlerConfig handler = new ExceptionHandlerConfig();

        var response = handler.handleNodeVersionUnsupportedException(
                new NodeVersionUnsupportedException(Network.TESTNET, "0.1.0", "0.1.1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UPGRADE_REQUIRED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NODE_VERSION_UNSUPPORTED");
        assertThat(response.getBody().network()).isEqualTo("TESTNET");
        assertThat(response.getBody().currentVersion()).isEqualTo("0.1.0");
        assertThat(response.getBody().minimumVersion()).isEqualTo("0.1.1");
    }
}
