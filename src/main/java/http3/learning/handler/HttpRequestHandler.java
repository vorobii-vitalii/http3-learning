package http3.learning.handler;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;

public class HttpRequestHandler extends SimpleChannelInboundHandler<DefaultHttpRequest> {
	private static final byte[] CONTENT = "Hey HTTP\r\n".getBytes(StandardCharsets.UTF_8);

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestHandler.class);

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, DefaultHttpRequest httpRequest) {
		LOGGER.info("Handling request = {}", httpRequest);
		DefaultHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(CONTENT));
		httpResponse.headers().add("X-Extra-Header", "Value");
		channelHandlerContext.writeAndFlush(httpResponse);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		LOGGER.error("Error occurred", cause);
		ctx.close();
	}
}
