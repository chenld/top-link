package com.taobao.top.link;

import java.net.URI;

import com.taobao.top.link.handler.ChannelHandler;
import com.taobao.top.link.handler.ChannelSelectHandler;
import com.taobao.top.link.websocket.WebSocketChannelSelectHandler;

// just an sample api gateway, upper layer app can use serverChannel/channelSelect directly
public class Endpoint {
	private ServerChannel serverChannel;
	private ChannelSelectHandler channelSelectHandler;
	private ChannelHandler channelHandler;

	public Endpoint() {
		this(new DefaultLoggerFactory());
	}

	public Endpoint(LoggerFactory loggerFactory) {
		this.channelSelectHandler = new WebSocketChannelSelectHandler(loggerFactory);
	}

	public void setChannelHandler(ChannelHandler handler) {
		this.channelHandler = handler;
	}

	public void bind(ServerChannel channel) {
		this.serverChannel = channel;
		this.serverChannel.run(this);
	}
	
	public void unbind() {
		if(this.serverChannel!=null)
			this.serverChannel.stop();
	}

	public EndpointProxy getEndpoint(URI uri) throws ChannelException {
		return this.getEndpoint(this.channelSelectHandler.getClientChannel(uri));
	}

	protected ChannelHandler getChannelHandler() {
		return this.channelHandler;
	}

	protected EndpointProxy getEndpoint(ClientChannel channel) {
		EndpointProxy proxy = new EndpointProxy();
		channel.setChannelHandler(this.channelHandler);
		proxy.using(channel);
		return proxy;
	}
}
