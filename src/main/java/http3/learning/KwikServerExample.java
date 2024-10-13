package http3.learning;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

import http3.learning.example.MyApplicationProtocolConnectionFactory;
import net.luminis.quic.QuicConnection;
import net.luminis.quic.log.SysOutLogger;
import net.luminis.quic.server.ServerConnectionConfig;
import net.luminis.quic.server.ServerConnector;

public class KwikServerExample {

	public static void main(String[] args) throws Exception {
		File certChainFile = new File("/home/vitaliivorobii/fun/http3-learning/cert.jks");
		File keyFile = new File("/home/vitaliivorobii/fun/http3-learning/key.pem");
		final SysOutLogger log = new SysOutLogger();
		log.logPackets(true);
		final String keyStorePassword = "secret";
		KeyStore keyStore = KeyStore.getInstance(new File("/home/vitaliivorobii/fun/http3-learning/cert.jks"), keyStorePassword.toCharArray());

		List<QuicConnection.QuicVersion> supportedVersions = new ArrayList<>();
		supportedVersions.add(QuicConnection.QuicVersion.V1);
		supportedVersions.add(QuicConnection.QuicVersion.V2);
		final ServerConnector.Builder builder = ServerConnector.builder()
				.withPort(9998);
		String alias = keyStore.aliases().nextElement();
		System.out.println("Using certificate with alias " + alias + " from keystore");
		builder.withKeyStore(keyStore, alias, keyStorePassword.toCharArray());
		ServerConnector serverConnector = builder
//				.withKeyStore(keyStore, "servercert", "secret".toCharArray())
				.withSupportedVersions(supportedVersions)
				.withConfiguration(ServerConnectionConfig.builder()
						.maxOpenPeerInitiatedBidirectionalStreams(100)
						.build())
				.withLogger(log)
				.build();
		serverConnector.registerApplicationProtocol("echo", new MyApplicationProtocolConnectionFactory());
		serverConnector.start();
	}

}
