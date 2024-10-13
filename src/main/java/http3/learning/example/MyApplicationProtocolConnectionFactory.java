package http3.learning.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import net.luminis.quic.QuicConnection;
import net.luminis.quic.QuicStream;
import net.luminis.quic.server.ApplicationProtocolConnection;
import net.luminis.quic.server.ApplicationProtocolConnectionFactory;

public class MyApplicationProtocolConnectionFactory implements ApplicationProtocolConnectionFactory {
	@Override
	public ApplicationProtocolConnection createConnection(String protocol, QuicConnection quicConnection) {
		return new ApplicationProtocolConnection() {
			@Override
			public void acceptPeerInitiatedStream(QuicStream stream) {
				try {
					// Note that this implementation is not safe to use in the wild, as attackers can crash the server by sending arbitrary large requests.
					byte[] bytesRead = stream.getInputStream().readAllBytes();
					System.out.println("Read echo request with " + bytesRead.length + " bytes of data.");
					stream.getOutputStream().write(bytesRead);
					stream.getOutputStream().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
	}

	public int maxConcurrentPeerInitiatedUnidirectionalStreams() {
		return 50;
	}

	public int maxConcurrentPeerInitiatedBidirectionalStreams() {
		return 50;
	}
}
