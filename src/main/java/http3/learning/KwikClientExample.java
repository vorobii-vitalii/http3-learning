package http3.learning;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import net.luminis.quic.QuicClientConnection;
import net.luminis.quic.QuicConnection;
import net.luminis.quic.QuicStream;
import net.luminis.quic.log.SysOutLogger;

public class KwikClientExample {

	public static void main(String[] args) throws IOException {
		String applicationProtocolId = "echo";
		final SysOutLogger log = new SysOutLogger();
		log.logPackets(true);
		QuicClientConnection connection = QuicClientConnection.newBuilder()
				.uri(URI.create("echo://localhost:" + 9998))
				.applicationProtocol(applicationProtocolId)
				.logger(log)
				.version(QuicConnection.QuicVersion.V1)
				.noServerCertificateCheck()
				.build();
		connection.connect();
		QuicStream quicStream = connection.createStream(true);
		OutputStream output = quicStream.getOutputStream();
		output.write("Hey".getBytes(StandardCharsets.UTF_8));
		output.close();
		InputStream input = quicStream.getInputStream();
		System.out.println("Response = " + new String(input.readAllBytes()));
		connection.closeAndWait();
	}

}
