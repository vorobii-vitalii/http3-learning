package http3.learning.initializer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.incubator.codec.http3.*;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.CharsetUtil;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class QuicServerInitializer extends ChannelInitializer<QuicChannel> {
    private static final byte[] CONTENT = "Hello123321\r\n".getBytes(StandardCharsets.UTF_8);

    @Override
    protected void initChannel(QuicChannel ch) {
        ch.pipeline().addLast(new Http3ServerConnectionHandler(new ChannelInitializer<QuicStreamChannel>() {
            @Override
            protected void initChannel(QuicStreamChannel quicStreamChannel) throws Exception {
                quicStreamChannel.pipeline().addLast(new Http3FrameToHttpObjectCodec(true, false));
                quicStreamChannel.pipeline().addLast(new CustomHttpServerHandler());
                quicStreamChannel.pipeline().remove(this);
            }
        }));
        ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
    }

    class CustomHttpServerHandler extends SimpleChannelInboundHandler {
        private HttpRequest request;
        StringBuilder responseData = new StringBuilder();

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
            DefaultHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(CONTENT));
            httpResponse.headers().add("extra-header", "hey!");
            ctx.writeAndFlush(httpResponse);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

    }


}
