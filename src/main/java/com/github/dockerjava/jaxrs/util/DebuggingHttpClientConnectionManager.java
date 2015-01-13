package com.github.dockerjava.jaxrs.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HttpContext;

public class DebuggingHttpClientConnectionManager implements
		HttpClientConnectionManager {
	private final HttpClientConnectionManager manager;
	private final IdentityHashMap<HttpClientConnection, StackTraceElement[]> map =
			new IdentityHashMap<HttpClientConnection, StackTraceElement[]>();

	private final File debuglog;
			
	public DebuggingHttpClientConnectionManager(
			HttpClientConnectionManager manager) {
		this.manager = manager;
		int i=0;
		File locLog = null;
		do {
			locLog = new File("debuglog"+i);
			i++;
		} while(locLog.exists());
		debuglog = locLog;
	}

	@Override
	public ConnectionRequest requestConnection(HttpRoute route, Object state) {
		// let's see were it comes from
		return new DebugConnectionRequest(this, manager.requestConnection(route, state));
	}

	@Override
	public void releaseConnection(HttpClientConnection conn, Object newState,
			long validDuration, TimeUnit timeUnit) {
		// and note that it is not occupied any longer
		map.put(conn, null);
		
		FileWriter fw = null;
		try {
			fw = new FileWriter(debuglog, true);
			fw.write("returned ");
			fw.write(Integer.toHexString(System.identityHashCode(conn)));
			fw.write("\n");
		} catch(IOException ex) {
			throw new IllegalStateException("Could not write debuglog. ", ex);
		} finally {
			try {
				fw.close();
			} catch (IOException ex) {
				throw new IllegalStateException("Could not close debuglog. ", ex);
			}
		}
		
		manager.releaseConnection(conn, newState, validDuration, timeUnit);
	}

	@Override
	public void connect(HttpClientConnection conn, HttpRoute route,
			int connectTimeout, HttpContext context) throws IOException {
		manager.connect(conn, route, connectTimeout, context);
	}

	@Override
	public void upgrade(HttpClientConnection conn, HttpRoute route,
			HttpContext context) throws IOException {
		manager.upgrade(conn, route, context);
	}

	@Override
	public void routeComplete(HttpClientConnection conn, HttpRoute route,
			HttpContext context) throws IOException {
		manager.routeComplete(conn, route, context);
	}

	@Override
	public void closeIdleConnections(long idletime, TimeUnit tunit) {
		manager.closeIdleConnections(idletime, tunit);
	}

	@Override
	public void closeExpiredConnections() {
		manager.closeExpiredConnections();
	}

	@Override
	public void shutdown() {
		manager.shutdown();
	}

	public void register(StackTraceElement[] elems, HttpClientConnection conn) {
		FileWriter fw = null;
		try {
			fw = new FileWriter(debuglog, true);
			fw.write("--- ");
			fw.write(Integer.toHexString(System.identityHashCode(conn)));
			fw.write("\n");
			
			for(StackTraceElement elem:elems) {
				fw.write(" at ");
				fw.write(elem.toString());
				fw.write("\n");
			}
		} catch(IOException ex) {
			throw new IllegalStateException("Could not write debuglog. ", ex);
		} finally {
			try {
				fw.close();
			} catch (IOException ex) {
				throw new IllegalStateException("Could not close debuglog. ", ex);
			}
		}
		map.put(conn, elems);
	}

}
