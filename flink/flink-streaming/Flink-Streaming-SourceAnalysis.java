
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



Flink������͸���:

TaskSlot: TaskSlot��Flink����С����Դ����Ԫ����������TaskManager�ϵ��ڴ棬��������CPU
    - ÿ��TaskManager������3��TaskSlot����ô��ζ��ÿ��TaskSlot������֮һTaskManage�ڴ�
    - Flink��Job�߼��ᱻ��Ϊһ��һ����Task������Դ�����ϣ�Flink��֤Task��ÿ�����ж�һ���и�TaskSlotʹ��


//main�߳� -> flink-akka.actor.default-dispatcher-3 �߳�
env.executor() -> MiniCluster.submitJob(): Զ��Rpc����,�����ؽ�� 
    // flink-akka.actor.default-dispatcher-3 �߳� -> flink-akka.actor.default-dispatcher-2 �߳�
    Dispatcher.submitJob() -> Dispatcher.runJob(jobGraph): 
    
        // flink-akka.actor.default-dispatcher-2 �߳� ->  mini-cluster-io-thread-2 �߳�: 
        runJob(jobGraph) -> JobManagerRunnerImpl.verifyJobSchedulingStatusAndStartJobManager()
            
            // mini-cluster-io-thread-2 �߳�:  -> flink-akka.actor.default-dispatcher-3
            verifyJobSchedulingStatusAndStartJobManager -> JobMaster.notifyOfNewResourceManagerLeader
            
                // flink-akka.actor.default-dispatcher-3�߳�  -> "flink-akka.actor.default-dispatcher-5"
                notifyOfNewResourceManagerLeader() -> ResourceManager.requestSlot()
                
                    
                    // "flink-akka.actor.default-dispatcher-5" -> Զ���߳�;Rpc���� TaskExecutor.requestSlot()
                    resourceManagerGateway.requestSlot() -> TaskExecutor.requestSlot() 
                    
                    //? ��� �� TaskExecutor.requestSlot ��Task.run() ?
                    
                        // TaskManager�����е���Task��ִ��;
                        Task.run() -> StreamTask.invoke(){
                            beforeInvoke();
                            runMailboxLoop();
                            afterInvoke();
                        }
                    


//main�߳� -> flink-akka.actor.default-dispatcher-3 �߳�
env.executor() -> MiniCluster.submitJob(): Զ��Rpc����,�����ؽ�� 
    // flink-akka.actor.default-dispatcher-3 �߳� -> flink-akka.actor.default-dispatcher-2 �߳�
    Dispatcher.submitJob() -> Dispatcher.runJob(jobGraph): 
        // flink-akka.actor.default-dispatcher-2 �߳� ->  mini-cluster-io-thread-2 �߳�: 
        runJob(jobGraph) -> JobManagerRunnerImpl.verifyJobSchedulingStatusAndStartJobManager()
            // mini-cluster-io-thread-2 �߳�:  -> flink-akka.actor.default-dispatcher-3
            verifyJobSchedulingStatusAndStartJobManager -> JobMaster.notifyOfNewResourceManagerLeader
                // flink-akka.actor.default-dispatcher-3�߳�  -> "flink-akka.actor.default-dispatcher-5"
                notifyOfNewResourceManagerLeader() -> ResourceManager.requestSlot()
                    // "flink-akka.actor.default-dispatcher-5" -> Զ���߳�;Rpc���� TaskExecutor.requestSlot()
                    resourceManagerGateway.requestSlot() -> TaskExecutor.requestSlot() 
                    //? ��� �� TaskExecutor.requestSlot ��Task.run() ?
                        // TaskManager�����е���Task��ִ��;
                        Task.run() -> StreamTask.invoke(){
                            beforeInvoke();
                            runMailboxLoop();
                            afterInvoke();
                        }
                    

                    
    

"main"�߳���Ҫ����:

// 1. ����env �жϲ��ж��Ǵ���Local, StreamPlan����StreamContext��ִ�л���;
StreamExecutionEnvironment.createStreamExecutionEnvironment(){ 
    if (env instanceof ContextEnvironment) {
        return new StreamContextEnvironment((ContextEnvironment) env);
    } else if (env instanceof OptimizerPlanEnvironment) {
        return new StreamPlanEnvironment(env);
    } else {
        return createLocalEnvironment();
    }
}









FlinkStreaming ִ�����: StreamExecutionEnvironment.execute(jobName) 

StreamExecutionEnvironment.execute(jobName){
    StreamGraph streamGraph= getStreamGraph(jobName)
    
    return execute(streamGraph);{
        // ��Ҫ����1: ��ȡ�򴴽�?ִ�л���
        final JobClient jobClient = executeAsync(streamGraph);
        
        // ��Ҫ����2: ִ��job���ȴ����?
        if (configuration.getBoolean(DeploymentOptions.ATTACHED)) { //localģʽ, �������true, LocalStreamEnvironment.validateAndGetConfiguration()���趨��;
            jobExecutionResult = jobClient.getJobExecutionResult(userClassloader).get();
            
        } else {
            jobExecutionResult = new DetachedJobExecutionResult(jobClient.getJobID());
        }

        jobListeners.forEach(jobListener -> jobListener.onJobExecuted(jobExecutionResult, null));
        return jobExecutionResult;
    }
}




















# 1.2 Flink Local Դ�����: ��ǰ׷��;



// "flink-akka.actor.default-dispatcher-2" �߳�:  ��δ���Ĺ���,������Ǵ���һ�� JobManagerLeaderListener,����ӵ� listeners��Set��;

    - �����̺߳ͷ���: "flink-akka.actor.default-dispatcher-5"  �߳�;
        - ResourceManager.requestSlot() -> 
            -> SlotManagerImpl.internalRequestSlot() -> allocateSlot()
                -> gateway.requestSlot() : Զ��Rpc���� TaskExecutor.requestSlot()

TaskExecutor.requestSlot(){
    if (jobManagerTable.contains(jobId)) {
        offerSlotsToJobManager(jobId);
    }else{ //��һ�ν�������: 
        jobLeaderService.addJob(jobId, targetAddress);{
            JobLeaderService.JobManagerLeaderListener jobManagerLeaderListener = new JobManagerLeaderListener(jobId);
            leaderRetrievalService.start(jobManagerLeaderListener);{//EmbeddedLeaderService.start()
                // Set<EmbeddedLeaderRetrievalService> listeners: ��Set()�д��� Service? 
                if (!listeners.add(service)) throw new IllegalStateException
                
            }
        }
    }
    return CompletableFuture.completedFuture(Acknowledge.get());
}















    
