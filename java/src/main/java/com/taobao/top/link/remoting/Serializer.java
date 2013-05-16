package com.taobao.top.link.remoting;

public interface Serializer {
	public byte[] serializeMethodCall(MethodCall methodCall) throws FormatterException;
	public MethodReturn deserializeMethodReturn(byte[] input) throws FormatterException;
	public byte[] serializeMethodReturn(MethodReturn methodReturn) throws FormatterException;
	public MethodCall deserializeMethodCall(byte[] input) throws FormatterException;
}