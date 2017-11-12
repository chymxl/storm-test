package com.chy.storm.test;

import java.util.Map;
import java.util.Random;

import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

public class KafkaTopology {
	
	public static class RandomWordSpout extends BaseRichSpout{
		
		SpoutOutputCollector _collector;
		Random rand = new Random();

		@Override
		public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
			_collector = collector;
		}

		@Override
		public void nextTuple() {
			String[] words = new String[] {"java", "python", "javascript", "scala", "shell"};
			_collector.emit(new Values(words[rand.nextInt(words.length)]));
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("word"));
		}
		
	}

}
