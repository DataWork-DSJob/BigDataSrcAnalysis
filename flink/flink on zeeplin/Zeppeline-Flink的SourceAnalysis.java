

*. �����߳�: 
    - "flink-scheduler-1"
    - "FlinkCompletableFutureDelayScheduler-thread-1"
    
1. netty ͨ�ſ��: SelectorImpl.select(), NioEventLoop.run()
    - "flink-rest-server-netty-worker-thread-n":  SingleThreadEventExecutor.run() -> SelectedSelectionKeySetSelector.select();
    - "flink-rest-server-netty-boss-thread-1": 
    - "flink-rest-client-netty-thread-n"
    - "Flink Netty Server (0) Thread 0": ͨ����������?

    

2. dispatcher: ��Ϣת��: Dispatcher, 
	- "flink-akka.actor.default-dispatcher-15"
 	- "Flink-DispatcherRestEndpoint-thread-n"

    

3. metric���:
    - "flink-metrics-akka.remote.default-remote-dispatcher-13"�߳�:
    - "flink-metrics-akka.remote.default-remote-dispatcher-4" 
    - "flink-metrics-scheduler-n": ThreadRenamingRunnable.run() -> NioEventLoop.select() -> SelectedSelectionKeySetSelector.select()
    - "flink-metrics-13"
    - "Flink-MetricRegistry-thread-1" : ע��Metric?


"Reduce (SUM(1)) (1/1)" #534 prio=5 os_prio=0 tid=0x00007f02e4370800 nid=0xdf1 runnable [0x00007f02ac7f1000]
   java.lang.Thread.State: RUNNABLE    
   
// Client?
- "Flink-RestClusterClient-IO-thread-n"

4. ����?
- "jobmanager-future-thread-2"
- "IOManager reader thread #1"
- "IOManager writer thread #1"
- "Timer-n" ?

"FIFOScheduler-inter": Flink����ִ�����;


// Zeppelin�� Flink���: 

Job.run()-> interpreter.interpret(script, context); -> FlinkInterpreter.interpret()


    "flink-rest-server-netty-worker-thread-n":  NioEventLoop.processSelectedKey() -> AbstractChannelHandlerContext.fireChannelRead()-> LeaderRetrievalHandler.channelRead0() -> 
        -> JobSubmitHandler.handleRequest() -> gateway.submitJob() -> ������һ�߳�
            => "flink-akka.actor.default-dispatcher-15"�߳�:  Dispatcher.submitJob() -> internalSubmitJob()
                Dispatcher.waitForTerminatingJobManager()
                => "flink-akka.actor.default-dispatcher-642"�߳�: action.apply(jobGraph) -> Dispatcher.persistAndRunJob() -> runJob()












