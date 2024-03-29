/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


//--port 9000 --checkpoint 5000
//-c com.skhillare.flink_avg.StreamingJob

package com.skhillare.flink_avg;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.ConfigConstants;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.QueryableStateOptions;
import org.apache.flink.runtime.jobgraph.JobGraph;
import org.apache.flink.runtime.minicluster.FlinkMiniCluster;
import org.apache.flink.runtime.minicluster.LocalFlinkMiniCluster;
import org.apache.flink.shaded.curator.org.apache.curator.shaded.com.google.common.collect.Lists;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.flink.api.common.time.Time;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

//@SuppressWarnings("serial")
class QuitValueState extends Exception{
	QuitValueState(String m1,String inetAddress,int port) throws IOException {
		super(m1);
		Socket socket = new Socket();
		SocketAddress socketAddress=new InetSocketAddress(inetAddress, port);
		socket.bind(socketAddress);
		socket.close();

	}
}



public class StreamingJob extends RichFlatMapFunction<Tuple2<Long, Long>, Tuple2<Long, String>>  {
private transient ValueState<Tuple2<Long, String>> sum;

	@Override
	public void flatMap(Tuple2<Long, Long> input, Collector<Tuple2<Long, String>> out) throws Exception {
		if (input.f1==-1){
			sum.clear();
			return;
		}
		Tuple2<Long, String> currentSum = sum.value();
//		currentSum.f0 += 1;
		currentSum.f0 += input.f1;
		//Throw arithmatic exception for checkpoint restarting
		if (input.f1==155){
			throw new ArithmeticException("not valid");
		}
		sum.update(currentSum);
		System.out.println("Current Sum: "+(sum.value().f1)+"\nCurrent Count: "+(sum.value().f0));
//		if (sum.value().f0>=2) {
			out.collect(new Tuple2<>(input.f1, "avg"));
//			for two number avg
//			sum.clear();
//		}
	}

	@Override
	public void open(Configuration config) {
		ValueStateDescriptor<Tuple2<Long, String>> descriptor =
				new ValueStateDescriptor<>(
						"average",// the state name
						TypeInformation.of(new TypeHint<Tuple2<Long, String>>() {}), // type information
						Tuple2.of(0L, "avg")
				); // default value of the state, if nothing was set
//		descriptor.setQueryable("query-name");
		sum = getRuntimeContext().getState(descriptor);

	}



	public static void main(String[] args) throws Exception {
//		Configuration config = new Configuration();
//		config.setInteger(ConfigConstants.LOCAL_NUMBER_TASK_MANAGER, 1);
//		config.setInteger(QueryableStateOptions.CLIENT_NETWORK_THREADS, 1);
//		config.setInteger(QueryableStateOptions.PROXY_NETWORK_THREADS, 1);
//		config.setInteger(QueryableStateOptions.SERVER_NETWORK_THREADS, 1);
//		config.setBoolean(QueryableStateOptions.SERVER_ENABLE, true);

		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		final String hostname;
		final Integer port;
		try {
			final ParameterTool params = ParameterTool.fromArgs(args);
			hostname = params.has("hostname") ? params.get("hostname") : "localhost";
			port = params.getInt("port");
//			long cpInterval = params.getLong("checkpoint", TimeUnit.MINUTES.toMillis(1));
//			if (cpInterval > 0) {
//				CheckpointConfig checkpointConf = env.getCheckpointConfig();
//				checkpointConf.setCheckpointInterval(cpInterval);
//				checkpointConf.setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
//				checkpointConf.setCheckpointTimeout(TimeUnit.HOURS.toMillis(1));
//				checkpointConf.enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
//				env.getConfig().setUseSnapshotCompression(true);
//			}
		}catch (Exception e) {
			System.err.println(
					"No port specified. Please run 'SocketWindowWordCount "
							+ "--hostname <hostname> --port <port>', where hostname (localhost by default) "
							+ "and port is the address of the text server");
			System.err.println(
					"To start a simple text server, run 'netcat -l <port>' and "
							+ "type the input text into the command line");
			return;
		}

		//restart attempts after time delay
//		env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3,10000));

		//restart attempts in fixed time
//		env.setRestartStrategy(RestartStrategies.failureRateRestart(3, Time.of(5, TimeUnit.MINUTES),Time.of(10, TimeUnit.SECONDS)	));
		ValueStateDescriptor<Tuple2<Long, String>> descriptor =
				new ValueStateDescriptor<>(
						"average",// the state name
						TypeInformation.of(new TypeHint<Tuple2<Long, String>>() {}), // type information
						Tuple2.of(0L, "avg")
				);

		DataStreamSource<String> inp = env.socketTextStream(hostname, port, "\n");

//		DataStream<Tuple2<Long,Double>> ValueStateRes=
				inp.flatMap(new FlatMapFunction<String, Tuple2<Long, Long>>() {
			@Override
			public void flatMap(String inpstr, Collector<Tuple2<Long, Long>> out) throws Exception{

				for (String word : inpstr.split("\\s")) {
					try {
						if(word.equals("quit")){
							throw new QuitValueState( "Stoppping!!!",hostname,port);
						}
						if(word.equals("clear")){
							word="-1";
						}
						out.collect(Tuple2.of(1L, Long.valueOf(word)));
					}
					catch ( NumberFormatException e) {
						System.out.println("Enter valid number: "+e.getMessage());
					}catch (QuitValueState ex){
						System.out.println("Quitting!!!");
					}
				}
			}
		}).keyBy(0).flatMap(new StreamingJob())
						.keyBy(1).asQueryableState("query-name",descriptor);
//			ValueStateRes.print().setParallelism(1);

		JobGraph jobGraph = env.getStreamGraph().getJobGraph();

		System.out.println("[info] Job ID: " + jobGraph.getJobID());
		System.out.println();
			env.execute("Average with ValueState");


//		env.fromElements(Tuple2.of(1L, 3L), Tuple2.of(1L, 5L), Tuple2.of(1L, 7L), Tuple2.of(1L, 4L), Tuple2.of(1L, 2L))
//				.keyBy(0).flatMap(new StreamingJob()).keyBy(0)
//				.print();
//		env.execute("running!");
	}

}







