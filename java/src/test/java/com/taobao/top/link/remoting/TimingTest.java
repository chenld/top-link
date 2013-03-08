package com.taobao.top.link.remoting;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import com.taobao.top.link.ChannelException;
import com.taobao.top.link.ClientChannel;
import com.taobao.top.link.DefaultLoggerFactory;
import com.taobao.top.link.Endpoint;
import com.taobao.top.link.websocket.WebSocketChannelSelectHandler;
import com.taobao.top.link.websocket.WebSocketServerChannel;

// rpc timing is important for overlay-io/reused-channel
public class TimingTest {
	private WebSocketChannelSelectHandler selectHandler = new WebSocketChannelSelectHandler(new DefaultLoggerFactory());

	@Test
	public void timing_test() throws URISyntaxException, RemotingException, FormatterException, ChannelException {
		final URI uri = new URI("ws://localhost:9010/link");
		this.runServer(uri);

		DynamicProxy proxy = RemotingService.connect(uri);
		for (int i = 0; i < 10; i++) {
			MethodCall methodCall = new MethodCall();
			methodCall.Args = new Object[] { i };
			assertEquals(i, proxy.invoke(methodCall).ReturnValue);
		}
	}

	@Test
	public void same_channel_timing_test() throws URISyntaxException, ChannelException, InterruptedException {
		final URI uri = new URI("ws://localhost:9011/link");
		this.runServer(uri);

		// proxy1/2 will share same channel
		ClientChannel channel = selectHandler.getClientChannel(uri);
		final DynamicProxy proxy1 = RemotingService.proxy(channel);
		final DynamicProxy proxy2 = RemotingService.proxy(channel);

		this.runThread(proxy1, uri, 0, 100);
		this.runThread(proxy2, uri, 100, 200);

		synchronized (uri) {
			uri.wait(10000);
		}
	}

	@Test
	public void different_channel_timing_test() throws URISyntaxException, ChannelException, InterruptedException {
		final URI uri = new URI("ws://localhost:9012/link");
		this.runServer(uri);

		// proxy1/2 use different channel but same remote server
		final DynamicProxy proxy1 = RemotingService.proxy(selectHandler.connect(uri, 2000, null));
		final DynamicProxy proxy2 = RemotingService.proxy(selectHandler.connect(uri, 2000, null));

		this.runThread(proxy1, uri, 0, 100);
		this.runThread(proxy2, uri, 100, 200);

		synchronized (uri) {
			uri.wait(10000);
		}
	}

	private void runThread(final DynamicProxy proxy, final Object sync, final int from, final int to) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					for (int i = from; i < to; i++) {
						MethodCall methodCall = new MethodCall();
						methodCall.Args = new Object[] { i };
						assertEquals(i, proxy.invoke(methodCall).ReturnValue);
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}

				synchronized (sync) {
					sync.notify();
				}
			}
		}).start();
	}

	private void runServer(URI uri) {
		WebSocketServerChannel serverChannel = new WebSocketServerChannel(uri.getHost(), uri.getPort());
		Endpoint server = new Endpoint();
		server.setChannelHandler(new RemotingServerChannelHandler() {
			@Override
			public MethodReturn onMethodCall(MethodCall methodCall) {
				MethodReturn methodReturn = new MethodReturn();
				methodReturn.ReturnValue = methodCall.Args[0];
				return methodReturn;
			}
		});
		server.bind(serverChannel);
	}
}