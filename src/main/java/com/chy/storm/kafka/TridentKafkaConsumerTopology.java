package com.chy.storm.kafka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.kafka.StringScheme;
import org.apache.storm.kafka.ZkHosts;
import org.apache.storm.kafka.trident.TransactionalTridentKafkaSpout;
import org.apache.storm.kafka.trident.TridentKafkaConfig;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.task.IMetricsContext;
import org.apache.storm.trident.Stream;
import org.apache.storm.trident.TridentState;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.operation.BaseFunction;
import org.apache.storm.trident.operation.CombinerAggregator;
import org.apache.storm.trident.operation.TridentCollector;
import org.apache.storm.trident.operation.builtin.MapGet;
import org.apache.storm.trident.state.OpaqueValue;
import org.apache.storm.trident.state.State;
import org.apache.storm.trident.state.StateFactory;
import org.apache.storm.trident.state.map.IBackingMap;
import org.apache.storm.trident.state.map.OpaqueMap;
import org.apache.storm.trident.tuple.TridentTuple;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

public class TridentKafkaConsumerTopology {
	
	public static class Split extends BaseFunction{

		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			String str = tuple.getString(0);
			if(str != null) {
				Arrays.stream(str.split(" "))
					.forEach(word -> collector.emit(new Values(word)));
				
			}
		}
		
	}
	
	public static class MyStateFactory implements StateFactory{

		@Override
		public State makeState(Map conf, IMetricsContext metrics, int partitionIndex, int numPartitions) {
			return OpaqueMap.build(new MyBackingMap<OpaqueValue>());
		}
		
	}
	
	public static class MyBackingMap<T> implements IBackingMap<T>{

		ConcurrentHashMap<Object,  T> map = new ConcurrentHashMap<>();
		
		@Override
		public List<T> multiGet(List<List<Object>> keys) {
			List<T> result = new ArrayList<>();
			if(keys != null) {
				keys.stream().forEach(key -> 
						result.add(map.get(key) == null ? null : map.get(key))
					);
			}
			return result;
		}

		@Override
		public void multiPut(List<List<Object>> keys, List<T> vals) {
			for(int i = 0; i < keys.size(); i++) {
				map.put(keys.get(i), vals.get(i));
			}
		}
		
	}
	
	public static class MyCount implements CombinerAggregator<Long>{

		@Override
		public Long init(TridentTuple tuple) {
			return 1L;
		}

		@Override
		public Long combine(Long val1, Long val2) {
			return val1 + val2;
		}

		@Override
		public Long zero() {
			return 0L;
		}

		
	}
	
	public static void main(String[] args) throws Exception {
//		ZkHosts hosts = new ZkHosts("localhost:2181");
		ZkHosts hosts = new ZkHosts("172.20.4.224:2381,172.20.4.223:2381,172.20.4.221:2381,172.20.4.220:2381,172.20.4.219:2381/chroot/kafka");
		TridentKafkaConfig config = new TridentKafkaConfig(hosts, "storm-kafka-test");
		config.scheme = new SchemeAsMultiScheme(new StringScheme());
		config.startOffsetTime = kafka.api.OffsetRequest.LatestTime();
		TransactionalTridentKafkaSpout spout = new TransactionalTridentKafkaSpout(config);
		TridentTopology topology = new TridentTopology();
		Stream stream = topology.newStream("kafkaspout", spout);
		TridentState state = stream.each(new Fields("str"), new Split(), new Fields("word"))
			.groupBy(new Fields("word"))
			.persistentAggregate(new MyStateFactory(), new MyCount(), new Fields("count"));
		
		topology.newDRPCStream("word-count")
			.project(new Fields("args"))
			.each(new Fields("args"), new Split(), new Fields("word"))
			.groupBy(new Fields("word"))
			.stateQuery(state, new Fields("word"), new MapGet(), new Fields("count"))
			.project(new Fields("word", "count"));
		
//		LocalCluster cluster = new LocalCluster();
//		cluster.submitTopology("word-count-drpc", new Config(), topology.build());
//		Thread.sleep(600 * 1000);
		StormSubmitter.submitTopology("word-count-drpc", new Config(), topology.build());
	}

}
