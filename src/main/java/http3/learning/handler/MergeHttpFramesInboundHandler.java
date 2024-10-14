package http3.learning.handler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;

public class MergeHttpFramesInboundHandler extends SimpleChannelInboundHandler<HttpObject> {
	private static final Logger LOGGER = LoggerFactory.getLogger(MergeHttpFramesInboundHandler.class);
	private final Map<Class<? extends HttpObject>, BiConsumer<HttpObject, ChannelHandlerContext>> httpObjectConsumerByType =
			new LinkedHashMap<>();
	{
		httpObjectConsumerByType.put(LastHttpContent.class, this::onFinalHttpObject);
		httpObjectConsumerByType.put(HttpContent.class, this::onHttpContentRead);
		httpObjectConsumerByType.put(HttpRequest.class, this::onHttpRequestRead);
	}
	private HttpRequest httpRequest;
	private ByteBuf payload;

	@Override
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, HttpObject httpObject) {
		for (var entry : httpObjectConsumerByType.entrySet()) {
			if (entry.getKey().isAssignableFrom(httpObject.getClass())) {
				entry.getValue().accept(httpObject, channelHandlerContext);
				return;
			}
		}
		LOGGER.warn("Unexpected HTTP object {}", httpObject);
	}

	private void onFinalHttpObject(HttpObject httpObject, ChannelHandlerContext channelHandlerContext) {
		channelHandlerContext.fireChannelRead(new DefaultFullHttpRequest(
				httpRequest.protocolVersion(),
				httpRequest.method(),
				httpRequest.uri(),
				payload == null ? Unpooled.EMPTY_BUFFER : payload,
				httpRequest.headers(),
				EmptyHttpHeaders.INSTANCE
		));
		channelHandlerContext.fireChannelReadComplete();
	}

	private void onHttpContentRead(HttpObject httpObject, ChannelHandlerContext channelHandlerContext) {
		HttpContent httpContent = (HttpContent) httpObject;
		LOGGER.info("HTTP content {}", httpContent);
		this.payload = httpContent.content().copy();
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
