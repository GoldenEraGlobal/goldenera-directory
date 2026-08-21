package global.goldenera.directory;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import global.goldenera.cryptoj.enums.Network;

class ConstantsTest {

	@Test
	void bothNetworksSwitchVersionAtTheirMiningEconomicsActivationHeight() {
		assertThat(Constants.requiredSoftwareVersion(Network.MAINNET, 731_502L)).isEqualTo("0.0.1");
		assertThat(Constants.requiredSoftwareVersion(Network.MAINNET, 731_503L)).isEqualTo("0.1.1");
		assertThat(Constants.requiredSoftwareVersion(Network.TESTNET, 716_823L)).isEqualTo("0.0.1");
		assertThat(Constants.requiredSoftwareVersion(Network.TESTNET, 716_824L)).isEqualTo("0.1.1");
	}

	@Test
	void acceptsOldReleaseBeforeForkAndRejectsItAtTheExactForkBlock() {
		assertThat(Constants.shouldRejectNodeVersion(Network.TESTNET, 716_823L, "0.1.0")).isFalse();
		assertThat(Constants.shouldRejectNodeVersion(Network.TESTNET, 716_824L, "0.1.0")).isTrue();
		assertThat(Constants.shouldRejectNodeVersion(Network.TESTNET, 716_824L, "not-a-version")).isTrue();
		assertThat(Constants.shouldRejectNodeVersion(Network.TESTNET, 716_824L, null)).isTrue();
	}

    @Test
    void acceptsMinimumAndNewerVersions() {
		assertThat(Constants.shouldRejectNodeVersion(Network.MAINNET, 731_503L, "0.1.1")).isFalse();
		assertThat(Constants.shouldRejectNodeVersion(Network.TESTNET, 716_824L, "0.2.0")).isFalse();
    }
}
