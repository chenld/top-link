package com.taobao.top.link.remoting;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.FactoryBean;

import com.taobao.top.link.BufferManager;
import com.taobao.top.link.LoggerFactory;
import com.taobao.top.link.channel.ClientChannelSelector;
import com.taobao.top.link.channel.ClientChannelSharedSelector;

// easy support spring bean
public class SpringServiceProxyBean implements FactoryBean {
	private static SerializationFactory serializationFactory;
	private static ClientChannelSelector channelSelector;
	private static RemotingClientChannelHandler channelHandler;
	static {
		// default set 2M max message size for client
		// TODO:change to growing buffer
		BufferManager.setBufferSize(1024 * 1024 * 2);
		LoggerFactory loggerFactory = Util.getLoggerFactory(new Object());
		serializationFactory = new CrossLanguageSerializationFactory();
		channelSelector = new ClientChannelSharedSelector(loggerFactory);
		channelHandler = new RemotingClientChannelHandler(loggerFactory, new AtomicInteger(0));
	}

	private URI uri;
	private Class<?> interfaceType;
	private int executionTimeout;
	private String format;

	public void setInterfaceName(String interfaceName) throws ClassNotFoundException {
		this.interfaceType = Class.forName(interfaceName);
	}

	public void setUri(String uri) throws URISyntaxException {
		this.uri = new URI(uri);
	}

	public void setExecutionTimeout(String executionTimeout) {
		this.executionTimeout = Integer.parseInt(executionTimeout);
	}

	public void setHeaders(HandshakingHeadersBean headersBean) {
		headersBean.setUri(this.uri);
	}

	public void setSerialization(String format) {
		this.format = format;
	}

	@Override
	public Object getObject() throws Exception {
		DynamicProxy proxy = new DynamicProxy(this.uri, channelSelector, channelHandler);
		if (this.executionTimeout > 0) {
			proxy.setExecutionTimeout(this.executionTimeout);
		}
		return proxy.create(this.interfaceType, this.uri);
	}

	@Override
	public Class<?> getObjectType() {
		return this.interfaceType;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
