package com.taobao.top.link;

import com.taobao.top.link.handler.ReceiveHandler;

public class EndpointProxy {

	private Identity identity;
	private ClientChannel channel;

	protected void using(ClientChannel channel) {
		this.channel = channel;
	}

	public Identity getIdentity() {
		return this.identity;
	}

	public void send(byte[] data, int offset, int length) {
		this.channel.send(data, offset, length);
	}

	// special once-handle
	public void send(byte[] data, int offset, int length, ReceiveHandler handler) {
		
	}
}
