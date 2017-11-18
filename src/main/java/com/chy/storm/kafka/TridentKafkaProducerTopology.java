package com.chy.storm.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.kafka.trident.TridentKafkaStateFactory;
import org.apache.storm.kafka.trident.TridentKafkaUpdater;
import org.apache.storm.kafka.trident.mapper.FieldNameBasedTupleToKafkaMapper;
import org.apache.storm.kafka.trident.selector.DefaultTopicSelector;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.trident.Stream;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.operation.TridentCollector;
import org.apache.storm.trident.spout.IBatchSpout;
import org.apache.storm.trident.testing.FixedBatchSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

public class TridentKafkaProducerTopology {
	
	public static class RandomWordsSpout extends BaseRichSpout{
		
		SpoutOutputCollector _collector;
		
		static String[] words = new String[]{
				"abc", "xyz", "hello world", "hello storm", "abc xyz",
				"big data"
		};
		static Random rand = new Random();
		
		@Override
		public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
			_collector = collector;
		}

		@Override
		public void nextTuple() {
			try {
				Thread.sleep(5 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			String word = words[rand.nextInt(words.length)];
			System.out.println(word);
			_collector.emit(new Values(word.hashCode() + "", word));
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("key", "words"));
		}
		
	}
	
	public static class BatchSpout implements IBatchSpout{

		ConcurrentHashMap<Long, List<Object>> map = new ConcurrentHashMap<>();
		String[] words = new String[]{
				"abc", "xyz", "hello world", "hello storm", "abc xyz",
				"big data"
		};
		Random rand = new Random();
		
		@Override
		public void open(Map conf, TopologyContext context) {

		}

		@Override
		public void emitBatch(long batchId, TridentCollector collector) {
			List<Object> result = map.get(batchId);
			if(result == null) {
				result = new ArrayList<>();
				result.add(new Values("1", words[rand.nextInt(words.length)]));
				result.add(new Values("2", words[rand.nextInt(words.length)]));
				//result.add(new Values("3", words[rand.nextInt(words.length)]));
				map.put(batchId, result);
			}
			collector.emit(new Values("1", words[rand.nextInt(words.length)]));
			
		}

		@Override
		public void ack(long batchId) {
			map.remove(batchId);
		}

		@Override
		public void close() {

		}

		@Override
		public Map<String, Object> getComponentConfiguration() {
			return new Config();
		}

		@Override
		public Fields getOutputFields() {
			// TODO Auto-generated method stub
			return new Fields("key", "words");
		}
		
	}
	
	public static void main(String[] args) {
		TridentTopology topology = new TridentTopology();
		RandomWordsSpout spout1 = new RandomWordsSpout();
		FixedBatchSpout spout = new FixedBatchSpout(new Fields("key","words"), 4,
                new Values("storm", "1"),
                new Values("trident", "1"),
                new Values("needs", "1"),
                new Values("javadoc", "1")
        );
		BatchSpout spout2 = new BatchSpout();
		//spout.setCycle(true);
		Stream stream = topology.newStream("spout1", spout2);
		
		Properties props = new Properties();
        props.put("bootstrap.servers", "172.20.4.224:9092,172.20.4.223:9092,172.20.4.221:9092,172.20.4.220:9092,172.20.4.219:9092");
//        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "1");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        
        TridentKafkaStateFactory factory = new TridentKafkaStateFactory()
        		.withProducerProperties(props)
        		.withKafkaTopicSelector(new DefaultTopicSelector("storm-kafka-test"))
        		.withTridentTupleToKafkaMapper(new FieldNameBasedTupleToKafkaMapper<String, String>("key","words"));
        
        stream
        .partitionPersist(factory, new Fields("key", "words"), new TridentKafkaUpdater(), new Fields());
//        KafkaBolt<String,String> bolt = new KafkaBolt<String,String>()
//        		.withProducerProperties(props)
//        		.withTopicSelector("storm-kafka-test")
//        		.withTupleToKafkaMapper(new FieldNameBasedTupleToKafkaMapper<String,String>("key", "word"));
        
        try {
        	StormSubmitter.submitTopology("words-producer", new Config(), topology.build());
//        	LocalCluster cluster = new LocalCluster();
//        	Config conf = new Config();
//        	conf.setDebug(true);
//    		cluster.submitTopology("words-producer", conf, topology.build());
//    		Thread.sleep(600 * 1000);
//    		cluster.killTopology("words-producer");
//    		cluster.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
        
	}

}
