package http3.learning.initializer;

import http3.learning.handler.HttpRequestHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.incubator.codec.http3.Http3FrameToHttpObjectCodec;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;

public class Http3ServerInitializer extends ChannelInitializer<QuicChannel> {
	private final HttpRequestHandler httpRequestHandler;

	public Http3ServerInitializer(HttpRequestHandler httpRequestHandler) {
		this.httpRequestHandler = httpRequestHandler;
	}

	@Override
	protected void initChannel(QuicChannel ch) {
		ch.pipeline().addLast(new Http3ServerConnectionHandler(new ChannelInitializer<QuicStreamChannel>() {
			@Override
			protected void initChannel(QuicStreamChannel streamChannel) {
				streamChannel.pipeline().addLast(new Http3FrameToHttpObjectCodec(true, false));
				streamChannel.pipeline().addLast(httpRequestHandler);
				streamChannel.pipeline().remove(this);
			}
		}));
		ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
	}

}
