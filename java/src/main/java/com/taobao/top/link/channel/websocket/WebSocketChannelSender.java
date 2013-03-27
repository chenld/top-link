package com.taobao.top.link.channel.websocket;

import java.nio.ByteBuffer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import com.taobao.top.link.channel.ChannelException;
import com.taobao.top.link.channel.ChannelSender;

public class WebSocketChannelSender implements ChannelSender {
	private ChannelHandlerContext ctx;

	public WebSocketChannelSender(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void send(byte[] data, int offset, int length) throws ChannelException {
		ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(data, offset, length);
		BinaryWebSocketFrame frame = new BinaryWebSocketFrame(buffer);
		frame.setFinalFragment(true);
		ctx.getChannel().write(frame);
	}

	@Override
	public void send(ByteBuffer dataBuffer, final SendHandler sendHandler) throws ChannelException {
		ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(dataBuffer);
		BinaryWebSocketFrame frame = new BinaryWebSocketFrame(buffer);
		frame.setFinalFragment(true);
		ctx.getChannel().write(frame).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture arg0) throws Exception {
				if (sendHandler != null)
					sendHandler.onSendComplete();
			}
		});
	}
}
