package com.kumarvv.vrest;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.Method;
import java.net.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Simple REST server with just one class for complete processing.
 *
 * - No HTTP server / servlet container required.
 * - Uses Java SE socket to listen and process client requests.
 * - Multi-threaded request processing (100 threads by default).
 * - Uses jackson mapper (org.codehaus.jackson) for JSON processing
 *
 */
public class RESTServer {

	private static final String HDR_HTTP = "HTTP/1.0 200";
	private static final String HDR_CONTENT_TYPE = "Content-type: application/json";
	private static final String HDR_SERVER_NAME = "Server-name: VjRestServer";
	private static final String HDR_CONTENT_LENGTH = "Content-length: %d";

	private static final String SLASH = "/";
	private static final String NEW_LINE = "\n";

	private static final int DEFAULT_NUM_THREADS = 100;
	private static final Executor _threadPool = Executors.newFixedThreadPool(DEFAULT_NUM_THREADS);
	private final Map<String, Object> _resourcesMap = new HashMap<String, Object>();

	/**
	 * initialize supported HTTP methods
	 */
	protected RESTServer() {
		_resourcesMap.put("GET", new HashMap<String, Object>());
		_resourcesMap.put("POST", new HashMap<String, Object>());
		_resourcesMap.put("PUT", new HashMap<String, Object>());
		_resourcesMap.put("DELETE", new HashMap<String, Object>());
	}

	/**
	 * Starts REST server and also scans for REST resources in provided
	 * classes.
	 *
	 * @param port
	 * @throws java.io.IOException
	 */
	public void start(int port) {
		scanResources();

		try {
			log("starting server...");
			ServerSocket socket = new ServerSocket(port);
			log("listening on http://localhost:" + port);
			while (true) {
				final Socket connection = socket.accept();
				Runnable task = new Runnable() {
					@Override
					public void run() {
						HandleRequest(connection);
					}
				};
				_threadPool.execute(task);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * starts REST server with default port 4001
	 */
	public void start() {
		start(4001);
	}

	/**
	 * scan provided classes annotated wih @Path and supported HTTP methods
	 *
	 * @see com.kumarvv.vrest.RESTServer.Path
	 * @see com.kumarvv.vrest.RESTServer.GET
	 */
	private void scanResources() {
		log("scanning resources...");
		Set<Class<?>> clazzes = getResourceClasses(
				  RESTServer.class.getProtectionDomain().getCodeSource().getLocation().getPath(),
				  "", null);

		// temp code
		for (Class clazz : clazzes) {
			if (clazz.isAnnotationPresent(Path.class)) {
				Path pathAnn = (Path) clazz.getAnnotation(Path.class);

				for (Method m : clazz.getMethods()) {
					checkMethod(pathAnn.value(), clazz, m);
				}
			}
		}
		log("scanning completed.");
	}

	/**
	 * finds resource classes from current directory (recursively)
	 *
	 * @param path
	 * @return
	 */
	private Set<Class<?>> getResourceClasses(String path, String pkg, Set<Class<?>> clazzes) {
		if (clazzes == null) {
			clazzes = new HashSet<Class<?>>();
		}
		File dir = new File(path);
		if (dir.isDirectory() && dir.exists()) {
			for (String cn : dir.list()) {
				if (cn.equals(".") || cn.equals("..")) {
					continue;
				}
				String cpath = path + (path.charAt(path.length()-1) == '/' ? "" : "/") + cn;
				File child = new File(cpath);
				if (child.isDirectory()) {
					clazzes = getResourceClasses(child.getPath(), (pkg.length() > 0 ? pkg + "." : "") + cn, clazzes);
				} else {
					if (child.exists() && cn.endsWith(".class")) {
						String fqcn = (pkg.length() > 0 ? pkg + "." : "") + cn.substring(0, cn.length()-6);
						try {
							URL url = child.toURI().toURL();
							ClassLoader cl = new URLClassLoader(new URL[]{url});
							Class<?> clazz = cl.loadClass(fqcn);
							if (clazz.isAnnotationPresent(Path.class)) {
								clazzes.add(clazz);
							}
						} catch (MalformedURLException e) {
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							log("Could not load class: " + fqcn);
							//e.printStackTrace();
						}
					}
				}
			}
		}
		return clazzes;
	}

	/**
	 * checks if method qualifies for REST API and registers API
	 *
	 * @param root
	 * @param m
	 * @return
	 */
	private boolean checkMethod(String root, Class<?> clazz, Method m) {
		String mPath = "";
		String httpMethod = "";
		if (m.isAnnotationPresent(GET.class)) {
			httpMethod = "GET";
			mPath = m.getAnnotation(GET.class).value();
		} else if (m.isAnnotationPresent(POST.class)) {
			httpMethod = "POST";
			mPath = m.getAnnotation(POST.class).value();
		} else if (m.isAnnotationPresent(PUT.class)) {
			httpMethod = "PUT";
			mPath = m.getAnnotation(PUT.class).value();
		} else if (m.isAnnotationPresent(DELETE.class)) {
			httpMethod = "DELETE";
			mPath = m.getAnnotation(DELETE.class).value();
		} else {
			return false;
		}
		String path = normalizeApiPath(root, mPath);

		return registerApi(httpMethod, path, new API(clazz, m, path));
	}

	/**
	 * normalizes method path for API registration
	 *
	 * @param cPath
	 * @param mPath
	 * @return
	 */
	private String normalizeApiPath(String cPath, String mPath) {
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
				path = cPath + (cPath.charAt(cPath.length()-1) == '/' ? "" : SLASH) + mPath.trim();
			}
		}
		return path;
	}

	/**
	 * register API for given method / path
	 *
	 * @param httpMethod
	 * @param path
	 * @param api
	 * @return
	 */
	private boolean registerApi(String httpMethod, String path, API api) {
		Map<String, Object> resMap = (Map<String, Object>)_resourcesMap.get(httpMethod);
		if (resMap == null) {
			log("Unsupported http method: " + httpMethod, true);
			return false;
		}

		String ps[] = path.split(SLASH);
		for (int i = 0; i < ps.length; i++) {
			if (ps[i].length() > 0) ps[i] = ps[i].charAt(0) == ':' ? "*" : ps[i];
		}

		if (ps == null || ps.length == 0) {
			resMap.put(SLASH, api);
		} else {
			for (int i = 0; i < ps.length; i++) {
				if (ps[i] == null || ps[i].isEmpty()) {
					continue;
				}
				resMap = buildApiMap(resMap, ps[i], null);
			}
			buildApiMap(resMap, SLASH, api);
		}
		log("Found api: " + api.toString() + " => " + path);
		return true;
	}

	/**
	 * supports registerApi method by initializing nested childMap for resource path
	 *
	 * @param resMap
	 * @param path
	 * @param api
	 * @return
	 */
	private Map<String, Object> buildApiMap(Map<String, Object> resMap, String path, API api) {
		Object o = resMap.get(path);
		Map<String, Object> childMap;
		if (o != null) {
			childMap = (Map<String, Object>) o;
		} else {
			childMap = new HashMap<String, Object>();
		}
		if (api != null) {
			resMap.put(SLASH, api);
		} else {
			resMap.put(path, childMap);
		}
		return childMap;
	}

	/**
	 * handles incoming REST request.
	 *
	 * Following steps are performed:
	 * - process request params
	 * - process request body (JSON only supported at this time)
	 * - prepares response headers
	 * - prepares response body using registered API
	 *
	 * @param s
	 */
	private void HandleRequest(Socket s) {
		PrintWriter out;
		String requestId = "request-" + Thread.currentThread().toString();
		try {
			String webServerAddress = s.getInetAddress().toString();
			log("Incoming Request [" + requestId + "]: " + webServerAddress);

			Map<String, String> params = processRequestParams(s);
			params.put("RequestId", requestId);

			String response = prepareResponse(params);
			out = new PrintWriter(s.getOutputStream(), true);
			out.println(prepareResponseHeader(response.length()));
			out.println(response);
			out.flush();
			out.close();
			s.close();

			log("Request [" + requestId + "] completed.");
		} catch (IOException e) {
			log("Failed respond to client request [" + requestId + "]: " + e.getMessage(), true);
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
	 * parse request and creates request parameters map (includes request headers also)
	 *
	 * @param socket
	 * @return request parameter map
	 */
	private Map<String, String> processRequestParams(Socket socket) {
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
	 * prepares response header
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
	 * Identify and execute API for the request action, then prepare response JSON.
	 *
	 * Default response (requestParams in JSON format) when no API found for the request.
	 *
	 * @param requestParams
	 * @return
	 */
	private String prepareResponse(Map<String, String> requestParams) {
		API api = getRequestAPI(requestParams.get("Action"));
		if (api != null) {
			return api.execute(requestParams);
		} else {
			return toJSON(requestParams);
		}
	}

	/**
	 * parses request context path and returns API (class method) registered
	 * for the path if available
	 *
	 * @param path
	 * @return API
	 */
	private API getRequestAPI(String path) {
		String[] ps = path.split(SLASH);
		if (ps != null && ps.length > 0) {
			Map<String, Object> parent = (Map<String, Object>) _resourcesMap.get(ps[0]);
			for (int i = 1; i < ps.length; i++) {
				Map<String, Object> map = (Map<String, Object>) parent.get(ps[i]);
				if (map == null) {
					map = (Map<String, Object>) parent.get("*");
					if (map == null) {
						return null; // no api registered
					}
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
	 * supporting API
	 */
	private static class API {
		public API(Class<?> clazz, Method method, String path) {
			this.clazz = clazz;
			this.method = method;
			this.setPath(path);
		}

		private Class<?> clazz;
		public Class<?> getClazz() {
			return clazz;
		}

		private Method method;

		public void setPath(String path) {
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
				if (method.getParameterTypes().length > 0) {
					String[] ps = requestParams.get("Context-Path").split("/");
					Map<String, String> urlParams = new HashMap<String, String>();
					for (Integer i : paramsDef.keySet()) {
						if (i < ps.length) {
							urlParams.put(paramsDef.get(i), ps[i]);
						}
					}
					Object[] mParams = new Object[method.getParameterTypes().length];
					int i = 0, j = 0;
					for (Annotation[] anns : method.getParameterAnnotations()) {
						for (Annotation ann : anns) {
							if (ann instanceof Param) {
								String key = ((Param) ann).value();
								mParams[i] = urlParams.containsKey(key) ? urlParams.get(key) : requestParams.get(key);
							}
							if (ann instanceof Data) {
								mParams[i] = toObject(requestParams.get("Payload"), method.getParameterTypes()[i]);
							}
							j++;
						}
						i++;
					}
					rc = method.invoke(ob, mParams);
				} else {
					rc = method.invoke(ob);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return toJSON(rc);
		}

		@Override
		public String toString() {
			return clazz.getName() + "." + method.getName();
		}
	}

	@Target({ElementType.TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Path {
		public String value();
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface GET {
		public String value() default "";
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface POST {
		public String value() default "";
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface PUT {
		public String value() default "";
	}

	@Target({ElementType.METHOD})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface DELETE {
		public String value() default "";
	}

	@Target({ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Param {
		public String value();
	}

	@Target({ElementType.PARAMETER})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Data {
	}

	/**
	 * log -- implement sl4j
	 * @param msg
	 */
	public static void log(String msg, boolean error) {
		System.out.println((error ? "ERROR: " : "INFO: ") + msg);
	}
	public static void log(String msg) {
		log(msg, false);
	}

	/**
	 * to JSON (using jackson)
	 *
	 * @param o
	 * @return
	 */
	public static String toJSON(Object o) {
		String rc = "";
		try {
			ObjectMapper mapper = new ObjectMapper();
			rc = mapper.writeValueAsString(o);
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rc;
	}

	/**
	 * to Object (using jackson)
	 *
	 * @param json
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	public static <T> T toObject(String json, Class<T> clazz) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			T o = mapper.readValue(json, clazz);
			return o;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * starts server with given configurations
	 */
	public static void main(String[] args) throws IOException {
		new RESTServer().start(4001);
	}

}
