package com.kumarvv.vrest;

/**
 * (c) kappals.com
 * <p/>
 * User: Vijay
 * Date: 7/26/14
 * Time: 6:54 PM
 */
public class MyServer extends AbstractRestServer {
	public static void main(String[] args) {
		MyServer myServer = new MyServer();
		myServer.start(4001);
	}
}
