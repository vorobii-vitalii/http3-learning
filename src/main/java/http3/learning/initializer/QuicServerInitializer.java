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
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.incubator.codec.http3.Http3ConnectionHandler;
import io.netty.incubator.codec.http3.Http3FrameToHttpObjectCodec;
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.util.CharsetUtil;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class QuicServerInitializer extends ChannelInitializer<QuicChannel> {
    private static final byte[] CONTENT = "Hello\r\n".getBytes(StandardCharsets.UTF_8);

    @Override
    protected void initChannel(QuicChannel ch) {
        ch.pipeline().addLast(new Http3ServerConnectionHandler(new ChannelInitializer<QuicStreamChannel>() {
            @Override
            protected void initChannel(QuicStreamChannel quicStreamChannel) throws Exception {
                quicStreamChannel.pipeline().addLast(new Http3FrameToHttpObjectCodec(true));
                quicStreamChannel.pipeline().addLast(new CustomHttpServerHandler());
                quicStreamChannel.pipeline().remove(this);
                quicStreamChannel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
            }
        }));

//        ch.pipeline().addLast(new Http3ServerConnectionHandler(new Http3FrameToHttpObjectCodec(true)));


//		ch.pipeline().addLast(new Http3RequestStreamInboundHandler() {
//
//			@Override
//			protected void channelRead(ChannelHandlerContext ctx, Http3UnknownFrame frame) {
//				System.out.println("Unknown frame = " + frame);
//				super.channelRead(ctx, frame);
//			}
//
//			@Override
//			protected void channelRead(ChannelHandlerContext ctx, Http3HeadersFrame frame) {
//				ReferenceCountUtil.release(frame);
//			}
//
//			@Override
//			protected void channelRead(ChannelHandlerContext ctx, Http3DataFrame frame) {
//				ReferenceCountUtil.release(frame);
//			}
//
//			@Override
//			protected void channelInputClosed(ChannelHandlerContext ctx) {
//				Http3HeadersFrame headersFrame = new DefaultHttp3HeadersFrame();
//				Http3Headers headers = headersFrame.headers();
//				headers.status("404");
//				headersFrame.headers().add("server", "netty");
//				headers.addInt("content-length", CONTENT.length);
//				ctx.write(headersFrame);
//				ctx.writeAndFlush(new DefaultHttp3DataFrame(Unpooled.wrappedBuffer(CONTENT)))
//						.addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
//			}
//
//			@Override
//			public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//				super.exceptionCaught(ctx, cause);
//			}
//		});
//        ch.pipeline().addLast(new CustomHttpServerHandler());
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
            // implementation to follow
            System.out.println("msg = " + msg);
            if (msg instanceof HttpRequest) {
                HttpRequest request = this.request = (HttpRequest) msg;

                if (HttpUtil.is100ContinueExpected(request)) {
                    writeResponse(ctx);
                }
                responseData.setLength(0);
                responseData.append(formatParams(request));
            }
            responseData.append(evaluateDecoderResult(request));

            if (msg instanceof HttpContent) {
                HttpContent httpContent = (HttpContent) msg;
                responseData.append(formatBody(httpContent));
                responseData.append(evaluateDecoderResult(request));

                if (msg instanceof LastHttpContent) {
                    LastHttpContent trailer = (LastHttpContent) msg;
                    responseData.append(prepareLastResponse(request, trailer));
                    writeResponse(ctx, trailer, responseData);
                }
            }
        }

        private void writeResponse(ChannelHandlerContext ctx) {
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE, Unpooled.EMPTY_BUFFER);
            ctx.write(response);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

        StringBuilder formatParams(HttpRequest request) {
            StringBuilder responseData = new StringBuilder();
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
            Map<String, List<String>> params = queryStringDecoder.parameters();
            if (!params.isEmpty()) {
                for (Map.Entry<String, List<String>> p : params.entrySet()) {
                    String key = p.getKey();
                    List<String> vals = p.getValue();
                    for (String val : vals) {
                        responseData.append("Parameter: ").append(key.toUpperCase()).append(" = ")
                                .append(val.toUpperCase()).append("\r\n");
                    }
                }
                responseData.append("\r\n");
            }
            return responseData;
        }

        StringBuilder formatBody(HttpContent httpContent) {
            StringBuilder responseData = new StringBuilder();
            ByteBuf content = httpContent.content();
            if (content.isReadable()) {
                responseData.append(content.toString(CharsetUtil.UTF_8).toUpperCase())
                        .append("\r\n");
            }
            return responseData;
        }

        StringBuilder prepareLastResponse(HttpRequest request, LastHttpContent trailer) {
            StringBuilder responseData = new StringBuilder();
            responseData.append("Good Bye!\r\n");

            if (!trailer.trailingHeaders().isEmpty()) {
                responseData.append("\r\n");
                for (CharSequence name : trailer.trailingHeaders().names()) {
                    for (CharSequence value : trailer.trailingHeaders().getAll(name)) {
                        responseData.append("P.S. Trailing Header: ");
                        responseData.append(name).append(" = ").append(value).append("\r\n");
                    }
                }
                responseData.append("\r\n");
            }
            return responseData;
        }

        private void writeResponse(ChannelHandlerContext ctx, LastHttpContent trailer,
                                   StringBuilder responseData) {
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1,
                    ((HttpObject) trailer).decoderResult().isSuccess() ? OK : BAD_REQUEST,
                    Unpooled.copiedBuffer(responseData.toString(), CharsetUtil.UTF_8));

            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

            if (keepAlive) {
                httpResponse.headers().setInt(HttpHeaderNames.CONTENT_LENGTH,
                        httpResponse.content().readableBytes());
                httpResponse.headers().set(HttpHeaderNames.CONNECTION,
                        HttpHeaderValues.KEEP_ALIVE);
            }
            ctx.write(httpResponse);

            if (!keepAlive) {
                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        }

        static StringBuilder evaluateDecoderResult(HttpObject o) {
            StringBuilder responseData = new StringBuilder();
            DecoderResult result = o.decoderResult();

            if (!result.isSuccess()) {
                responseData.append("..Decoder Failure: ");
                responseData.append(result.cause());
                responseData.append("\r\n");
            }

            return responseData;
        }

    }


}
