package com.chy.storm.test;

import java.util.Map;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

public class ExclamationTopology {
	
	public static class ExclamationSpout extends BaseRichSpout{

		SpoutOutputCollector _collector;
		
		@Override
		public void nextTuple() {
			Utils.sleep(100);
			_collector.emit(new Values("abc"));
		}

		@Override
		public void open(Map arg0, TopologyContext arg1, SpoutOutputCollector collector) {
			_collector = collector;
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("word"));
		}
		
	}
	
	public static class ExclamationBolt extends BaseRichBolt{

		OutputCollector _collector;
		
		@Override
		public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
			_collector = collector;
		}

		@Override
		public void execute(Tuple input) {
			String abc = input.getStringByField("word");
			_collector.emit(new Values(abc + "!!!"));
			_collector.ack(input);
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("word"));
		}
		
	}
	
	public static void main(String... args) {
		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("words", new ExclamationSpout(), 3);
		builder.setBolt("exclam1", new ExclamationBolt(), 2)
			.shuffleGrouping("words");
		builder.setBolt("exclam2", new ExclamationBolt(), 1)
			.shuffleGrouping("words")
			.shuffleGrouping("exclam1");
		
		Config conf = new Config();
		conf.setDebug(true);
		
		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology("test", conf, builder.createTopology());
		Utils.sleep(10000);
		cluster.killTopology("test");
		cluster.shutdown();
	}
}
