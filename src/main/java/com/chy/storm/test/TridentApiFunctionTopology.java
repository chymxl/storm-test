package com.chy.storm.test;

import java.util.Map;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.operation.BaseFunction;
import org.apache.storm.trident.operation.TridentCollector;
import org.apache.storm.trident.tuple.TridentTuple;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;

public class TridentApiFunctionTopology {
	
	public static class NumbersProduce extends BaseRichSpout{
		
		SpoutOutputCollector _collector;
		int i = 0;
		int j = 0;
		
		@Override
		public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
			_collector = collector;
		}

		@Override
		public void nextTuple() {
			if(j==10)
				Utils.sleep(100 * 1000);
			int[][] ints = {
			                {1,2,3},
			                {4,1,6},
			                {3,0,8}
			                };
			int[] nums = ints[i++];
			j++;
			_collector.emit(new Values(nums[0], nums[1], nums[2]));
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			declarer.declare(new Fields("a","b","c"));
		}
		
	}
	
	public static class MyFunction extends BaseFunction{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			System.out.println("the integer 0 is " + tuple.getInteger(0));
			for(int i = 0; i < 10; i++) {
				collector.emit(new Values(i));
				System.out.println("emit num is " + i);
			}
		}
		
	}
	
	public static class PrintFunction extends BaseFunction{
		
		@Override
		public void execute(TridentTuple input, TridentCollector collector) {
			System.out.println(String.format("receive %s,%s,%s,%s", input.getInteger(0),
					input.getInteger(1), input.getInteger(2), input.getInteger(3)));
			collector.emit(new Values(input.getInteger(0),
					input.getInteger(1), input.getInteger(2), input.getInteger(3)));
		}
		
	}
	
	public static void main(String[] args) throws InterruptedException {
		TridentTopology topo = new TridentTopology();
		topo.newStream("ints", new NumbersProduce())
			.each(new Fields("b"), new MyFunction(), new Fields("d"))
			.each(new Fields("a","b","c","d"), new PrintFunction(), new Fields(""));
		LocalCluster cluster = new LocalCluster();
		Config conf = new Config();
		conf.setDebug(false);
		cluster.submitTopology("testfunction", conf, topo.build());
		Thread.sleep(10000);
		cluster.killTopology("testfunction");
		cluster.shutdown();
	}

}
