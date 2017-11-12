package com.chy.storm.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.IMetricsContext;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.operation.BaseFunction;
import org.apache.storm.trident.operation.CombinerAggregator;
import org.apache.storm.trident.operation.TridentCollector;
import org.apache.storm.trident.state.OpaqueValue;
import org.apache.storm.trident.state.State;
import org.apache.storm.trident.state.StateFactory;
import org.apache.storm.trident.state.map.IBackingMap;
import org.apache.storm.trident.state.map.OpaqueMap;
import org.apache.storm.trident.tuple.TridentTuple;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

public class TridentApiPersistenAggregateTopology {

	public static class SentenceSpout extends BaseRichSpout{
		
		SpoutOutputCollector _collector;
		Random rand = new Random();

		@Override
		public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
			_collector = collector;
		}

		@Override
		public void nextTuple() {
			String[] strs = new String[] {
					"I learn java",
					"java and python",
					"I learn javascript"
			};
			_collector.emit(new Values(strs[rand.nextInt(strs.length)]));
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("sentence"));
		}
		
	}
	
	public static class Split extends BaseFunction{

		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			Arrays.stream(tuple.getStringByField("sentence").split(" "))
				.forEach(sentence -> collector.emit(new Values(sentence)));
		}
		
	}
	
	public static class MyStateFactory implements StateFactory{

		@Override
		public State makeState(Map conf, IMetricsContext metrics, int partitionIndex, int numPartitions) {
			// TODO Auto-generated method stub
			return OpaqueMap.build(new MyBackingMap<OpaqueValue>());
		}
		
	}
	
	public static class MyBackingMap<T> implements IBackingMap<T>{
		
		Map<Object, T> map = new HashMap<>();
		
		@SuppressWarnings("unchecked")
		@Override
		public List<T> multiGet(List<List<Object>> keys) {
			System.out.println("multiGet" + keys);
			List<T> result = new ArrayList<>();
			keys.stream().forEach(key -> {
				if(map.containsKey(key))
					result.add(map.get(key));
				else {
					result.add(null);
				}
			});
			System.out.println("iiiiiiii" + map);
			return result;
		}

		@Override
		public void multiPut(List<List<Object>> keys, List<T> vals) {
			System.out.println("multiPut");
			System.out.println(keys);
			System.out.println(vals);
			for(int i = 0; i < keys.size(); i++) {
				map.put(keys.get(i), vals.get(i));
				System.out.println(String.format("key=%s, val=%s", keys.get(i), vals.get(i)));
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
	
	public static void main(String[] args) throws InterruptedException {
		TridentTopology topology = new TridentTopology();
		topology.newStream("sentences", new SentenceSpout())
			.each(new Fields("sentence"), new Split(), new Fields("word"))
			.groupBy(new Fields("word"))
			.persistentAggregate(new MyStateFactory(), new MyCount(), new Fields("count"))
			;
		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology("test1", new Config(), topology.build());
		Thread.sleep(60 * 1000);
		cluster.killTopology("test1");
		cluster.shutdown();
		
		
	}
}
