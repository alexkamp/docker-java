package com.github.dockerjava.jaxrs.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ConnectionRequest;

public class DebugConnectionRequest implements ConnectionRequest {
	private final DebuggingHttpClientConnectionManager parent;
	private final ConnectionRequest delegate;

	public DebugConnectionRequest(DebuggingHttpClientConnectionManager parent,
			ConnectionRequest delegate) {
		this.parent = parent;
		this.delegate = delegate;
	}

	@Override
	public boolean cancel() {
		return delegate.cancel();
	}

	@Override
	public HttpClientConnection get(long timeout, TimeUnit tunit)
			throws InterruptedException, ExecutionException,
			ConnectionPoolTimeoutException {
		HttpClientConnection conn = delegate.get(timeout, tunit);
		StackTraceElement[] elems = Thread.currentThread().getStackTrace();
		parent.register(elems, conn);
		return conn;
	}

}
