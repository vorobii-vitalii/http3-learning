package http3.learning;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3;
import io.netty.incubator.codec.http3.Http3ClientConnectionHandler;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicSslContextBuilder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public class Http3Client {

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		NioEventLoopGroup group = new NioEventLoopGroup(1);

		try {
			Channel channel = createHttp3Channel(group);
			QuicChannel quicChannel = createQuicChannel(channel);
			QuicStreamChannel streamChannel = createQuicStream(quicChannel);

			sendHttpRequest(streamChannel);
			streamChannel.closeFuture().sync();
			quicChannel.close().sync();
			channel.close().sync();
		}
		finally {
			group.shutdownGracefully();
		}
	}

	private static void sendHttpRequest(QuicStreamChannel streamChannel) throws InterruptedException {
		byte[] content = "Hey!".getBytes(StandardCharsets.UTF_8);
		Http3HeadersFrame headersFrame = new DefaultHttp3HeadersFrame();
		headersFrame.headers()
				.method("POST")
				.path("/")
				.authority("test" + ":" + Http3Server.PORT)
				.addInt("content-length", content.length)
				.scheme("https");
		streamChannel.write(headersFrame);
		streamChannel.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.copiedBuffer(content)))
				.addListener(QuicStreamChannel.SHUTDOWN_OUTPUT)
				.sync();
	}

	private static QuicStreamChannel createQuicStream(QuicChannel quicChannel) throws InterruptedException {
		return Http3.newRequestStream(quicChannel,
				new Http3RequestStreamInboundHandler() {
					@Override
					protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
						System.out.println("HEADERS = " + frame.headers());
						ReferenceCountUtil.release(frame);
					}

					@Override
					protected void channelRead(ChannelHandlerContext ctx, Http3DataFrame frame) {
						System.out.print("DATA = " + frame.content().toString(CharsetUtil.US_ASCII));
						ReferenceCountUtil.release(frame);
					}

					@Override
					protected void channelInputClosed(ChannelHandlerContext ctx) {
						ctx.close();
					}
				}).sync().getNow();
	}

	private static QuicChannel createQuicChannel(Channel channel) throws InterruptedException, ExecutionException {
		return QuicChannel.newBootstrap(channel)
				.handler(new Http3ClientConnectionHandler())
				.remoteAddress(new InetSocketAddress("localhost", Http3Server.PORT))
				.connect()
				.get();
	}

	private static Channel createHttp3Channel(NioEventLoopGroup group) throws InterruptedException {
		return new Bootstrap().group(group)
				.channel(NioDatagramChannel.class)
				.handler(createHttp3ChannelHandler())
				.bind(0)
				.sync()
				.channel();
	}

	private static ChannelHandler createHttp3ChannelHandler() {
		return Http3.newQuicClientCodecBuilder()
				.sslContext(QuicSslContextBuilder.forClient()
						.trustManager(InsecureTrustManagerFactory.INSTANCE)
						.applicationProtocols(Http3.supportedApplicationProtocols())
						.build())
				.maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
				.initialMaxData(10000000)
				.initialMaxStreamDataBidirectionalLocal(1000000)
				.build();
	}

}
