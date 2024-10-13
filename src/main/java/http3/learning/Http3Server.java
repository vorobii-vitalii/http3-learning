package http3.learning;

import java.io.File;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import http3.learning.initializer.QuicServerInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.quic.InsecureQuicTokenHandler;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;

public class Http3Server {
	public static final int PORT = 9999;

	public static void main(String[] args) throws InterruptedException, CertificateException {
		NioEventLoopGroup group = new NioEventLoopGroup(1);
		File certChainFile = new File("/home/vitaliivorobii/fun/http3-learning/cert.pem");
		File keyFile = new File("/home/vitaliivorobii/fun/http3-learning/key.pem");
//		SelfSignedCertificate cert = new OpenJdkSelfSignedCertGenerator();
//		QuicSslContext sslContext = QuicSslContextBuilder.forServer(cert.key(), null, cert.cert())
//				.applicationProtocols(Http3.supportedApplicationProtocols()).build();

//		QuicSslContextBuilder.forServer()

		QuicSslContext sslContext = QuicSslContextBuilder.forServer(keyFile, null, certChainFile)
				.applicationProtocols(Http3.supportedApplicationProtocols()).build();
		ChannelHandler codec = Http3.newQuicServerCodecBuilder()
				.sslContext(sslContext)
				.maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
				.initialMaxData(10000000)
				.initialMaxStreamDataBidirectionalLocal(1000000)
				.initialMaxStreamDataBidirectionalRemote(1000000)
				.initialMaxStreamsBidirectional(100)
				.tokenHandler(InsecureQuicTokenHandler.INSTANCE)
				.handler(new QuicServerInitializer())
				.build();
		try {
			Bootstrap bs = new Bootstrap();
			Channel channel = bs.group(group)
					.channel(NioDatagramChannel.class)
					.handler(codec)
					.bind(new InetSocketAddress(PORT)).sync().channel();
			channel.closeFuture().sync();
		}
		finally {
			group.shutdownGracefully();
		}
	}

}
