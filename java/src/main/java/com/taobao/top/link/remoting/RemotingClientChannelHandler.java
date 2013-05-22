package com.taobao.top.link.remoting;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import remoting.protocol.NotSupportedException;
import remoting.protocol.tcp.TcpContentDelimiter;
import remoting.protocol.tcp.TcpOperations;
import remoting.protocol.tcp.TcpTransportHeader;

import com.taobao.top.link.BufferManager;
import com.taobao.top.link.Logger;
import com.taobao.top.link.LoggerFactory;
import com.taobao.top.link.Text;
import com.taobao.top.link.channel.ChannelContext;
import com.taobao.top.link.channel.ChannelHandler;
import com.taobao.top.link.remoting.protocol.RemotingTcpProtocolHandle;
import com.taobao.top.link.remoting.protocol.RemotingTransportHeader;

public class RemotingClientChannelHandler implements ChannelHandler {
	private Logger logger;
	private AtomicInteger flagAtomic;
	private HashMap<String, RemotingCallback> callbacks = new HashMap<String, RemotingCallback>();
	private Serializer serializer = new DefaultSerializer();

	public RemotingClientChannelHandler(LoggerFactory loggerFactory, AtomicInteger flagAtomic) {
		this.logger = loggerFactory.create(this);
		this.flagAtomic = flagAtomic;
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	public ByteBuffer pending(RemotingCallback handler,
			short operation,
			HashMap<String, Object> transportHeaders,
			MethodCall methodCall)
			throws FormatterException {
		byte[] data = this.serializer.serializeMethodCall(methodCall);
		return this.pending(handler, operation, transportHeaders, data, 0, data.length);
	}

	// act as formatter sink
	public ByteBuffer pending(RemotingCallback handler,
			short operation,
			HashMap<String, Object> transportHeaders,
			byte[] data,
			int dataOffset,
			int dataLength) {
		String flag = Integer.toString(this.flagAtomic.incrementAndGet());

		ByteBuffer requestBuffer = BufferManager.getBuffer();
		RemotingTcpProtocolHandle handle = new RemotingTcpProtocolHandle(requestBuffer);
		handle.WritePreamble();
		handle.WriteMajorVersion();
		handle.WriteMinorVersion();
		handle.WriteOperation(TcpOperations.Request);
		handle.WriteContentDelimiter(TcpContentDelimiter.ContentLength);
		handle.WriteContentLength(data.length);
		transportHeaders.put(RemotingTransportHeader.Flag, flag);
		transportHeaders.put(RemotingTransportHeader.Format, this.serializer.getName());
		handle.WriteTransportHeaders(transportHeaders);
		handle.WriteContent(data);

		handler.flag = flag;
		this.callbacks.put(handler.flag, handler);// concurrent?
		if (this.logger.isDebugEnabled())
			this.logger.debug(Text.RPC_PENDING_CALL, flag);

		return requestBuffer;
	}

	public void cancel(RemotingCallback callback) {
		this.callbacks.remove(callback.flag);
	}

	@Override
	public void onConnect(ChannelContext context) {
	}

	@Override
	public void onMessage(ChannelContext context) {
		RemotingTcpProtocolHandle protocol = new RemotingTcpProtocolHandle((ByteBuffer) context.getMessage());
		protocol.ReadPreamble();
		protocol.ReadMajorVersion();
		protocol.ReadMinorVersion();

		short operation = protocol.ReadOperation();
		if (operation != TcpOperations.Reply)
			return;

		protocol.ReadContentDelimiter();
		protocol.ReadContentLength();

		HashMap<String, Object> transportHeaders = null;
		try {
			transportHeaders = protocol.ReadTransportHeaders();
		} catch (NotSupportedException e) {
			this.logger.error(e);
		}
		Object flag;
		if (transportHeaders == null ||
				(flag = transportHeaders.get(RemotingTransportHeader.Flag)) == null)
			return;

		if (this.logger.isDebugEnabled())
			this.logger.debug(Text.RPC_GET_RETURN, flag);

		RemotingCallback callback = this.callbacks.remove(flag);
		if (callback == null)
			return;

		Object statusCode = transportHeaders.get(TcpTransportHeader.StatusCode);
		Object statusPhrase = transportHeaders.get(TcpTransportHeader.StatusPhrase);
		if (statusCode != null &&
				Integer.parseInt(statusCode.toString()) > 0) {
			callback.onException(new Exception(String.format(
					Text.RPC_RETURN_ERROR, statusCode, statusPhrase)));
			return;
		}

		MethodReturn methodReturn = null;
		try {
			methodReturn = this.serializer.deserializeMethodReturn(protocol.ReadContent(), callback.returnType);
		} catch (FormatterException e) {
			callback.onException(e);
			return;
		}

		try {
			callback.onMethodReturn(methodReturn);
		} catch (Exception e) {
			this.logger.error(e);
		}
	}

	@Override
	public void onError(ChannelContext context) {
		// all is fail!
		for (Entry<String, RemotingCallback> i : this.callbacks.entrySet()) {
			try {
				i.getValue().onException(context.getError());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.callbacks = new HashMap<String, RemotingCallback>();
	}
}
