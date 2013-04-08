package com.taobao.top.link;

// special log/error text here, easy support culture
public class Text {
	public static String WS_REACH_MAX_IDLE = "reach max idle time";
	public static String WS_REACH_MAX_IDLE_AND_CLOSE = "reach maxIdleTimeSeconds=%s, close client channel";

	public static String WS_HANDSHAKE_ERROR = "handshake error";
	public static String WS_HANDSHAKE_INVALID = "Invalid handshake response";

	public static String WS_CONNECT_ERROR = "connect error";
	public static String WS_CONNECT_FAIL = "connect fail";
	public static String WS_CONNECT_TIMEOUT = "connect timeout";

	public static String WS_CHANNEL_CLOSED = "websocket channel closed";
	public static String WS_CONNECTION_CLOSED_BY = "connection closed: %s|%s";
	public static String WS_NOT_FINAL = "received a frame that not final fragment, not support!";

	public static String WS_ERROR_AT_CLIENT = "exceptionCaught at client";
	public static String WS_ERROR_AT_SERVER = "exceptionCaught at server";

	public static String WS_SERVER_RUN = "server channel bind at %s";
	public static String WS_SERVER_STOP = "server channel shutdown";

	public static String E_UNBIND_ERROR = "unbind error";
	public static String E_ID_DUPLICATE = "target identity can not equal itself";
	public static String E_CREATE_NEW = "create new EndpointProxy by identity";
	public static String E_UNKNOWN_MSG_FROM = "uknown message from";
	public static String E_GOT_ERROR = "got error: %s|%s";
	public static String E_CHANNEL_ERROR = "channel error";
	public static String E_ACCEPT = "%s accept a connect-in endpoint#%s and assign token#%s";
	public static String E_REFUSE = "refuse a connect-in endpoint";
	public static String E_CONNECT_SUCCESS = "sucessfully connect to endpoint#%s, and got token#%s";
	public static String E_NO_CALLBACK = "receive CONNECTACK, but no callback to handle it";
	public static String E_NO_SENDER = "do not have any valid channel to send";
	public static String E_EXECUTE_TIMEOUT = "execution timeout";
	public static String E_UNKNOWN_ERROR = "uknown error";
	
	public static String RPC_POOL_BUSY = "channel pool is busy, retry later";
	public static String RPC_CAN_NOT_GET_CHANNEL = "can not get channel";
	public static String RPC_SEND_ERROR = "send error";
	public static String RPC_CALL_ERROR = "remoting call error";
	public static String RPC_EXECUTE_TIMEOUT = "remoting execution timeout";
	public static String RPC_CHANNEL_BROKEN = "channel broken with unknown error";
	public static String RPC_WAIT_INTERRUPTED = "waiting callback interrupted";
	public static String RPC_PENDING_CALL = "pending methodCall#%s";
	public static String RPC_GET_RETURN = "receive methodReturn of methodCall#%s";
	public static String RPC_RETURN_ERROR = "remote reutrn error#%s: %s";
}