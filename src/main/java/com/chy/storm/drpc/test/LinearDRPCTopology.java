package com.chy.storm.drpc.test;

import java.util.Arrays;

import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.operation.BaseFunction;
import org.apache.storm.trident.operation.TridentCollector;
import org.apache.storm.trident.tuple.TridentTuple;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

public class LinearDRPCTopology {
	
	public static class SplitFunction extends BaseFunction{

		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			String args = tuple.getString(0);
			String[] arr = args.split(" ");
			Arrays.stream(arr).forEach(word -> 
					collector.emit(new Values(word)));
		}
		
	}
	
	public static class ExclamationFunction extends BaseFunction{

		@Override
		public void execute(TridentTuple tuple, TridentCollector collector) {
			
		}
		
	}
	
	public static void main(String[] args) {
		TridentTopology topology = new TridentTopology();
		topology.newDRPCStream("exclamation")
			.each(new Fields("args"), new SplitFunction(), new Fields("word"))
			.project(new Fields("word"));
		try {
			StormSubmitter.submitTopology("test", new Config(), topology.build());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
