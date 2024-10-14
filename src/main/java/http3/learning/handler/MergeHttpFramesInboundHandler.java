package http3.learning.handler;

import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;

public class MergeHttpFramesInboundHandler extends SimpleChannelInboundHandler<HttpObject> {
	private static final Logger LOGGER = LoggerFactory.getLogger(MergeHttpFramesInboundHandler.class);
	private HttpRequest httpRequest;
	private final Map<Class<? extends HttpObject>, BiConsumer<HttpObject, ChannelHandlerContext>> httpObjectConsumerByType = Map.of(
			HttpContent.class, this::onHttpContentRead,
			HttpRequest.class, this::onHttpRequestRead
	);

	@Override
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpObject httpObject) throws Exception {
		for (var entry : httpObjectConsumerByType.entrySet()) {
			if (entry.getKey().isAssignableFrom(httpObject.getClass())) {
				entry.getValue().accept(httpObject, channelHandlerContext);
				return;
			}
		}
		LOGGER.warn("Unexpected HTTP object {}", httpObject);
	}

	private void onHttpContentRead(HttpObject httpObject, ChannelHandlerContext channelHandlerContext) {
		HttpContent httpContent = (HttpContent) httpObject;
		channelHandlerContext.fireChannelRead(new DefaultFullHttpRequest(
				httpRequest.protocolVersion(),
				httpRequest.method(),
				httpRequest.uri(),
				httpContent.content().copy(),
				httpRequest.headers(),
				EmptyHttpHeaders.INSTANCE
		));
		channelHandlerContext.fireChannelReadComplete();
	}

	private void onHttpRequestRead(HttpObject httpObject, ChannelHandlerContext channelHandlerContext) {
		HttpRequest httpRequest = (HttpRequest) httpObject;
		LOGGER.info("HTTP request = {}", httpRequest);
		this.httpRequest = httpRequest;
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

}
