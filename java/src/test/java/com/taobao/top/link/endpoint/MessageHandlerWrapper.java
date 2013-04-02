package com.taobao.top.link.endpoint;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

public class MessageHandlerWrapper implements MessageHandler {
	public Object sync = new Object();
	public AtomicInteger receive = new AtomicInteger();
	public HashMap<String, String> lastMessage;

	public boolean doReply;

	@Override
	public void onMessage(EndpointContext context) throws Exception {
		lastMessage = context.getMessage();
		receive.incrementAndGet();
		System.out.println("onMessage: " + context.getMessage());
		if (doReply)
			context.reply(context.getMessage());
		this.notifyHandler();
	}

	public void waitHandler() throws InterruptedException {
		this.waitHandler(0);
	}

	public void waitHandler(int timeout) throws InterruptedException {
		synchronized (this.sync) {
			if (timeout > 0)
				this.sync.wait(timeout);
			else
				this.sync.wait();
		}
	}

	public void clear() {
		lastMessage = null;
		receive = new AtomicInteger();
	}

	public void notifyHandler() {
		synchronized (this.sync) {
			this.sync.notify();
		}
	}

	public void assertHandler(int receive) {
		Assert.assertEquals(receive, this.receive.get());
	}
}