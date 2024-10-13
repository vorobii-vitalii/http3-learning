package http3.learning.initializer;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.incubator.codec.http3.DefaultHttp3DataFrame;
import io.netty.incubator.codec.http3.DefaultHttp3HeadersFrame;
import io.netty.incubator.codec.http3.Http3DataFrame;
import io.netty.incubator.codec.http3.Http3Headers;
import io.netty.incubator.codec.http3.Http3HeadersFrame;
import io.netty.incubator.codec.http3.Http3RequestStreamInboundHandler;
import io.netty.incubator.codec.http3.Http3UnknownFrame;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.ReferenceCountUtil;

public class QuicServerInitializer extends ChannelInitializer<QuicChannel> {
	private static final byte[] CONTENT = "Hello".getBytes(StandardCharsets.UTF_8);

	@Override
	protected void initChannel(QuicChannel ch) {
		ch.pipeline().addLast(new Http3RequestStreamInboundHandler() {

			@Override
			protected void channelRead(ChannelHandlerContext ctx, Http3UnknownFrame frame) {
				System.out.println("Unknown frame = " + frame);
				super.channelRead(ctx, frame);
			}

			@Override
			protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
				ReferenceCountUtil.release(frame);
			}

			@Override
			protected void channelRead(ChannelHandlerContext ctx, Http3DataFrame frame) {
				ReferenceCountUtil.release(frame);
			}

			@Override
			protected void channelInputClosed(ChannelHandlerContext ctx) {
				Http3HeadersFrame headersFrame = new DefaultHttp3HeadersFrame();
				Http3Headers headers = headersFrame.headers();
				headers.status("404");
				headersFrame.headers().add("server", "netty");
				headers.addInt("content-length", CONTENT.length);
				ctx.write(headersFrame);
				ctx.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.wrappedBuffer(CONTENT)))
						.addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
			}

			@Override
			public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
				super.exceptionCaught(ctx, cause);
			}
		});
//		ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
	}
}
