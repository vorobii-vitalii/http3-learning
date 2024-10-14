package http3.learning.handler;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

@ExtendWith(MockitoExtension.class)
class TestMergeHttpFramesInboundHandler {

	MergeHttpFramesInboundHandler mergeHttpFramesInboundHandler = new MergeHttpFramesInboundHandler();

	@Mock
	ChannelHandlerContext channelHandlerContext;

	@Test
	void onlyFullRequestIsSentToDownHandler() {
		// Given
		HttpVersion httpVersion = HttpVersion.HTTP_1_1;
		HttpMethod method = HttpMethod.GET;
		String uri = "/path";
		ByteBuf payload = Unpooled.copiedBuffer(new byte[] {1, 2, 3});
		DefaultHttpHeaders httpHeaders = new DefaultHttpHeaders();

		// When
		mergeHttpFramesInboundHandler.channelRead0(channelHandlerContext, new DefaultHttpRequest(httpVersion, method, uri, httpHeaders));
		mergeHttpFramesInboundHandler.channelRead0(channelHandlerContext, new DefaultHttpContent(payload));
		mergeHttpFramesInboundHandler.channelRead0(channelHandlerContext, LastHttpContent.EMPTY_LAST_CONTENT);

		// Then
		verify(channelHandlerContext).fireChannelRead(new DefaultFullHttpRequest(
				httpVersion,
				method,
				uri,
				payload,
				httpHeaders,
				EmptyHttpHeaders.INSTANCE
		));
	}
}
