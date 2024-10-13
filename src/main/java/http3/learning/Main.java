package http3.learning;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.Http3SslContextSpec;
import reactor.netty.http.HttpProtocol;
import reactor.netty.http.server.HttpServer;

import java.io.File;
import java.time.Duration;

public class Main {

	public static void main(String[] args) {
		File certChainFile = new File("/home/vitalii/projects/http3-learning/cert.pem");
		File keyFile = new File("/home/vitalii/projects/http3-learning/key.pem");

		Http3SslContextSpec serverCtx = Http3SslContextSpec.forServer(keyFile, null, certChainFile);

		DisposableServer server =
				HttpServer.create()
						.port(8080)
						.protocol(HttpProtocol.HTTP3)
						.secure(spec -> spec.sslContext(serverCtx))
						.http3Settings(spec -> spec.idleTimeout(Duration.ofSeconds(5))
								.maxData(10000000)
								.maxStreamDataBidirectionalLocal(1000000)
								.maxStreamDataBidirectionalRemote(1000000)
								.maxStreamsBidirectional(100))
						.handle((request, response) -> response.header("server", "reactor-netty")
								.sendString(Mono.just("hello")))
						.bindNow();

		server.onDispose()
				.block();
	}
}