package http3.learning;

import java.net.InetSocketAddress;

import http3.learning.handler.EchoServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class EchoServer {
	private static final int PORT = 8080;

	public static void main(String[] args) throws InterruptedException {
		EchoServerHandler echoServerHandler = new EchoServerHandler();
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(group)
					.channel(NioServerSocketChannel.class)
					.localAddress(new InetSocketAddress(PORT))
					.childHandler(new ChannelInitializer<SocketChannel>(){
						@Override
						public void initChannel(SocketChannel ch) {
							ch.pipeline().addLast(echoServerHandler);
						}
					});
			ChannelFuture f = b.bind().sync();
			f.channel().closeFuture().sync();
		} finally {
			group.shutdownGracefully().sync();
		}
	}

}
