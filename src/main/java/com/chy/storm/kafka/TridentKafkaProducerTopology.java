package com.chy.storm.kafka;

import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.kafka.trident.TridentKafkaStateFactory;
import org.apache.storm.kafka.trident.TridentKafkaStateUpdater;
import org.apache.storm.kafka.trident.mapper.FieldNameBasedTupleToKafkaMapper;
import org.apache.storm.kafka.trident.selector.DefaultTopicSelector;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.trident.Stream;
import org.apache.storm.trident.TridentTopology;
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
			_collector.emit(new Values(words[rand.nextInt(words.length)]));
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("words"));
		}
		
	}
	
	public static void main(String[] args) {
		TridentTopology topology = new TridentTopology();
		Stream stream = topology.newStream("spout", new RandomWordsSpout());
		
		Properties props = new Properties();
//        props.put("bootstrap.servers", "172.20.4.224:9092,172.20.4.223:9092,172.20.4.221:9092,172.20.4.220:9092,172.20.4.219:9092");
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "1");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        
        TridentKafkaStateFactory factory = new TridentKafkaStateFactory()
        		.withProducerProperties(props)
        		.withKafkaTopicSelector(new DefaultTopicSelector("storm-kafka-test"))
        		.withTridentTupleToKafkaMapper(new FieldNameBasedTupleToKafkaMapper<String, String>("words","words"));
        
        stream.partitionPersist(factory, new Fields("words"), new TridentKafkaStateUpdater(), new Fields());
        
        try {
        	LocalCluster cluster = new LocalCluster();
    		cluster.submitTopology("words-producer", new Config(), topology.build());
    		Thread.sleep(60 * 1000);
    		cluster.killTopology("words-producer");
    		cluster.shutdown();
//			StormSubmitter.submitTopology("words-producer", new Config(), topology.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
        
	}

}
