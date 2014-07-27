package com.kumarvv.vrest;

import org.json.simple.JSONValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.kumarvv.vrest.AbstractResource.GET;
import static com.kumarvv.vrest.AbstractResource.Path;

/**
* Created with IntelliJ IDEA.
* User: kmarvv
* Date: 7/25/14
* Time: 5:08 PM
* To change this template use File | Settings | File Templates.
*/
public abstract class AbstractRestServer {

	private static final String NEW_LINE = "\n";
	private static final String HDR_HTTP = "HTTP/1.0 200";
	private static final String HDR_CONTENT_TYPE = "Content-type: application/json";
	private static final String HDR_SERVER_NAME = "Server-name: VjRestServer";
	private static final String HDR_CONTENT_LENGTH = "Content-length: %d";

	private static final String SLASH = "/";

	private static final int DEFAULT_NUM_THREADS = 100;
	private static final Executor _threadPool = Executors.newFixedThreadPool(DEFAULT_NUM_THREADS);

	private final Map<String, Object> _resourcesMap = new HashMap<String, Object>();

	protected AbstractRestServer() {
		_resourcesMap.put("GET", new HashMap<String, Object>());
		_resourcesMap.put("POST", new HashMap<String, Object>());
		_resourcesMap.put("PUT", new HashMap<String, Object>());
		_resourcesMap.put("DELETE", new HashMap<String, Object>());
	}

	/**
	 * scans for REST resources, starts server and listens for request
	 *
	 * @param port
	 * @throws java.io.IOException
	 */
	public void start(int port) {
		scanResources();

		try {
			ServerSocket socket = new ServerSocket(port);
			while (true) {
				final Socket connection = socket.accept();
				Runnable task = new Runnable() {
					@Override
					public void run() {
						HandleRequest(connection);
					}
				};
				log("listening on http://localhost:" + port);
				_threadPool.execute(task);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * scan all resources
	 */
	private void scanResources() {
		log("scanning resources...");
		Class[] clazzes = new Class[] { TestResource.class };
		// temp code
		for (Class clazz : clazzes) {
			if (clazz.isAnnotationPresent(Path.class)) {
				Path pathAnn = (Path) clazz.getAnnotation(Path.class);

				for (Method m : clazz.getMethods()) {
					if (m.isAnnotationPresent(GET.class)) {
						GET getAnn = m.getAnnotation(GET.class);
						registerApi("GET", pathAnn.value(), getAnn.value(), new API(clazz, m));
					}
				}
			}
		}
	}

	/**
	 * normalize method path considering class
	 * if method path starts with SLASH, then method path is the root path
	 * otherwise inherits the path from class
	 *
	 * @param cPath
	 * @param mPath
	 * @return
	 */
	private boolean registerApi(String type, String cPath, String mPath, API api) {
		String path;
		if (cPath == null || cPath.trim().isEmpty()) {
			path = SLASH;
		} else {
			path = cPath.trim();
		}
		if (mPath != null && !mPath.trim().isEmpty()) {
			if (mPath.charAt(0) == '/') { // becomes root path
				path = mPath;
			} else {
				path = (cPath.charAt(cPath.length()-1) == '/' ? "" : SLASH) + mPath.trim();
			}
		}
		api.setPath(path);

		String ps[] = path.split(SLASH);
		for (int i = 0; i < ps.length; i++) {
			if (ps[i].length() > 0) ps[i] = ps[i].charAt(0) == ':' ? "*" : ps[i];
		}

		Map<String, Object> resMap = (Map<String, Object>)_resourcesMap.get(type);
		if (resMap == null) {
			log("Unknown method: " + type);
			return false;
		}

		if (ps == null || ps.length == 0) {
			resMap.put(SLASH, api);
		} else {
			for (int i = 0; i < ps.length; i++) {
				if (ps[i] == null || ps[i].isEmpty()) {
					continue;
				}
				resMap = registerMap(resMap, ps[i], null);
			}
			registerMap(resMap, SLASH, api);
		}
		return true;
	}

	private Map<String, Object> registerMap(Map<String, Object> resMap, String path, API api) {
		Map<String, Object> childMap = new HashMap<String, Object>();
		if (api != null) {
			resMap.put(SLASH, api);
		} else {
			resMap.put(path, childMap);
		}
		return childMap;
	}

	private API getRequestAPI(String path) {
		String[] ps = path.split(SLASH);
		if (ps != null && ps.length > 0) {
			Map<String, Object> parent = (Map<String, Object>) _resourcesMap.get(ps[0]);
			for (int i = 1; i < ps.length; i++) {
				Map<String, Object> map = (Map<String, Object>) parent.get(ps[i]);
				if (map == null) {
					map = (Map<String, Object>) parent.get("*");
				}
				if (i == ps.length -1) { // last
					Object o = map.get("/");
					if (o != null && o instanceof API) {
						return (API) o;
					}
				}
				parent = map;
			}
		}
		return null;
	}

	/**
	 * handle request
	 * @param s
	 */
	private void HandleRequest(Socket s) {
		PrintWriter out;
		try {
			String webServerAddress = s.getInetAddress().toString();
			System.out.println("New Connection:" + webServerAddress);

			Map<String, String> params = processRequest(s);

			String response = prepareResponse(params);

			out = new PrintWriter(s.getOutputStream(), true);
			out.println(prepareResponseHeader(response.length()));
			out.println(response);
			out.flush();
			out.close();
			s.close();
		} catch (IOException e) {
			System.out.println("Failed respond to client request: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return;
	}

	/**
	 * parse request
	 * @param socket
	 * @return
	 */
	private Map<String, String> processRequest(Socket socket) {
		BufferedReader in;
		Map<String, String> params = new HashMap<String, String>();
		try {
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String s = in.readLine();
			String[] p = s.split(" ");
			if (p.length > 0) {
				params.put("Http-Method", p[0]);
				if (p.length > 1) {
					params.put("Context-Path", p[1]);
					params.put("Action", p[0] + p[1]);
					if (p.length > 2) {
						params.put("Http-Version", p[2]);
					}
				}
			}
			while (true) {
				s = in.readLine();
				if (s == null || s.isEmpty()) {
					break;
				}
				if (s.indexOf(":") > 0) {
					params.put(s.substring(0, s.indexOf(":")).trim(), s.substring(s.indexOf(":")+1).trim());
				}
			}

			String lenStr = params.get("Content-Length");
			if (lenStr != null && !lenStr.isEmpty()) {
				int len = Integer.valueOf(lenStr);
				String data = "";
				for (int i = 0; i < len; i++) {
					char c = (char) in.read();
					data += c;
				}
				params.put("Payload", data);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return params;
	}

	/**
	 * build and return http response header
	 *
	 * @param contentLength
	 * @return
	 */
	private String prepareResponseHeader(long contentLength) {
		StringBuilder sb = new StringBuilder("");
		sb.append(HDR_HTTP).append(NEW_LINE);
		sb.append(HDR_CONTENT_TYPE).append(NEW_LINE);
		sb.append(HDR_SERVER_NAME).append(NEW_LINE);
		sb.append(String.format(HDR_CONTENT_LENGTH, contentLength +1)).append(NEW_LINE);
		sb.append(NEW_LINE);

		return sb.toString();
	}

	/**
	 * prepare response
	 *
	 * @param requestParams
	 * @return
	 */
	private String prepareResponse(Map<String, String> requestParams) {
		API api = getRequestAPI(requestParams.get("Action"));
		if (api != null) {
			return api.execute(requestParams);
		} else {
			return getDefaultResponse(requestParams);
		}
	}

	/**
	 * default response
	 * @param params
	 * @return
	 */
	private String getDefaultResponse(Map<String,String> params) {
		return JSONValue.toJSONString(params);
	}

	/**
	 * log -- implement sl4j
	 * @param msg
	 */
	private void log(String msg) {
		System.out.println("INFO: " + msg);
	}

	/**
	 * supporting API
	 */
	private static class API {
		public API(Class<?> clazz, Method method) {
			this.clazz = clazz;
			this.method = method;
		}

		private Class<?> clazz;
		public Class<?> getClazz() {
			return clazz;
		}

		private Method method;
		public Method getMethod() {
			return method;
		}

		private String path;
		public String getPath() {
			return path;
		}
		public void setPath(String path) {
			this.path = path;
			String[] ps = path.split(SLASH);
			for (int i = 0; i < ps.length; i++) {
				if (ps[i] != null && !ps[i].isEmpty() && ps[i].charAt(0) == ':') {
					paramsDef.put(i, ps[i].substring(1));
				}
			}
		}

		private Map<Integer, String> paramsDef = new HashMap<Integer, String>();

		public String execute(Map<String, String> requestParams) {
			Object rc = null;
			try {
				Object ob = clazz.newInstance();
				if (ob instanceof AbstractResource) {
					AbstractResource ar = ((AbstractResource) ob);
					String[] ps = requestParams.get("Context-Path").split("/");
					Map<String, String> urlParams = new HashMap<String, String>();
					for (Integer i : paramsDef.keySet()) {
						if (i < ps.length) {
							urlParams.put(paramsDef.get(i), ps[i]);
						}
					}
					ar.initParams(requestParams, urlParams);
					rc = method.invoke(ar);
				} else {
					rc = method.invoke(ob);
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			return JSONValue.toJSONString(rc);
		}

		@Override
		public String toString() {
			return clazz.getName() + "." + method.getName();
		}
	}
}