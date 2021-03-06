package com.chy.storm.drpc.test;

import org.apache.storm.Config;
import org.apache.storm.utils.DRPCClient;

public class DRPCClientTest {
	
	public static void main(String[] args) throws Exception {
		Config conf = new Config();
		conf.setDebug(true);
        conf.put("storm.thrift.transport", "org.apache.storm.security.auth.SimpleTransportPlugin");
        conf.put(Config.STORM_NIMBUS_RETRY_TIMES, 3);
        conf.put(Config.STORM_NIMBUS_RETRY_INTERVAL, 10);
        conf.put(Config.STORM_NIMBUS_RETRY_INTERVAL_CEILING, 20);
        conf.put(Config.DRPC_MAX_BUFFER_SIZE, 1048576);
//		DRPCClient client = new DRPCClient(conf,"yzb-centos-10.cs1cloud.internal", 3772);;
		DRPCClient client = new DRPCClient(conf,"172.20.4.224", 3772);;
//		String result = client.execute("exclamation", "abc efg");
		String result = client.execute("word-count", "hello data big");
		System.out.println(result);
	}

}
