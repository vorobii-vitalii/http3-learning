package http3.learning;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import http3.learning.handler.HttpRequestHandler;
import http3.learning.initializer.Http3ServerInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.quic.InsecureQuicTokenHandler;
import io.netty.incubator.codec.quic.QuicSslContext;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;

public class Http3Server {
	public static final int PORT = 9999;

	public static void main(String[] args) throws InterruptedException {
		NioEventLoopGroup group = new NioEventLoopGroup();
		File certChainFile = new File("cert.pem");
		File keyFile = new File("key.pem");

		ChannelHandler http3ServerChannelHandler = createHttp3ServerChannelHandler(keyFile, certChainFile);
		try {
			Bootstrap bs = new Bootstrap();
			Channel channel = bs.group(group)
					.channel(NioDatagramChannel.class)
					.handler(http3ServerChannelHandler)
					.bind(new InetSocketAddress(PORT)).sync().channel();
			channel.closeFuture().sync();
		}
		finally {
			group.shutdownGracefully();
		}
	}

	private static ChannelHandler createHttp3ServerChannelHandler(File keyFile, File certChainFile) {
		return Http3.newQuicServerCodecBuilder()
				.sslContext(QuicSslContextBuilder.forServer(keyFile, null, certChainFile)
						.applicationProtocols(Http3.supportedApplicationProtocols())
						.build())
				.maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
				.initialMaxData(10000000)
				.initialMaxStreamDataBidirectionalLocal(1000000)
				.initialMaxStreamDataBidirectionalRemote(1000000)
				.initialMaxStreamsBidirectional(100)
				.tokenHandler(InsecureQuicTokenHandler.INSTANCE)
				.handler(new Http3ServerInitializer(new HttpRequestHandler()))
				.build();
	}

}
