
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



// "main"�߳�: 
StreamExecutionEnvironment.execute(jobName){
    StreamGraph streamGraph= getStreamGraph(jobName)
    
    return execute(streamGraph);{
        // ��Ҫ����1: ��ȡ�򴴽�?ִ�л���
        final JobClient jobClient = executeAsync(streamGraph);{
            final PipelineExecutorFactory executorFactory = executorServiceLoader.getExecutorFactory(configuration);
            CompletableFuture<? extends JobClient> jobClientFuture = executorFactory
                .getExecutor(configuration)
                .execute(streamGraph, configuration);{//LocalExecutor.execute()
                    final JobGraph jobGraph = getJobGraph(pipeline, effectiveConfig);
                    return PerJobMiniClusterFactory.createWithFactory(effectiveConfig, miniClusterFactory).submitJob(jobGraph);{// PerJobMiniClusterFactory.submitJob()
                        MiniCluster miniCluster = miniClusterFactory.apply(miniClusterConfig);
                        miniCluster.start();
                        
                        return miniCluster
                            .submitJob(jobGraph){//MiniCluster.submitJob()
                                final CompletableFuture<DispatcherGateway> dispatcherGatewayFuture = getDispatcherGatewayFuture();
                                final CompletableFuture<Void> jarUploadFuture = uploadAndSetJobFiles(blobServerAddressFuture, jobGraph);
                                final CompletableFuture<Acknowledge> acknowledgeCompletableFuture = jarUploadFuture
                                .thenCombine(dispatcherGatewayFuture,(Void ack, DispatcherGateway dispatcherGateway) -> dispatcherGateway.submitJob(jobGraph, rpcTimeout)){
                                    dispatcherGateway.submitJob(): ����Զ��Rpc����: ʵ��ִ�� Dispatcher.submitJob()
                                    Dispatcher.submitJob(){ //Զ��Rpc����,�����ؽ��;
                                        //������������:
                                    }
                                }
                                .thenCompose(Function.identity());
                                return acknowledgeCompletableFuture.thenApply((Acknowledge ignored) -> new JobSubmissionResult(jobGraph.getJobID()));
                                
                            }
                            .thenApply(result -> new PerJobMiniClusterJobClient(result.getJobID(), miniCluster))
                            .whenComplete((ignored, throwable) -> {
                                if (throwable != null) {
                                    // We failed to create the JobClient and must shutdown to ensure cleanup.
                                    shutDownCluster(miniCluster);
                                }
                            });
                            
                    }
                }
            jobListeners.forEach(jobListener -> jobListener.onJobSubmitted(jobClient, null));
        }
        
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
       

        
// flink-akka.actor.default-dispatcher-3 �߳�:
submitJob(){//Dispatcher.
    if (isDuplicateJob(jobGraph.getJobID())) {
        return FutureUtils.completedExceptionally(new DuplicateJobSubmissionException(jobGraph.getJobID()));
    }else if (isPartialResourceConfigured(jobGraph)) { //ƫ��/������ ��Դ����?
    }else{ //�����ظ���: ������������;
        return internalSubmitJob(jobGraph);{//Dispatcher.
            final CompletableFuture<Acknowledge> persistAndRunFuture = waitForTerminatingJobManager(jobGraph.getJobID(), jobGraph, this::persistAndRunJob).thenApply(ignored -> Acknowledge.get());{
                Dispatcher.waitForTerminatingJobManager(){
                    jobManagerTerminationFuture.thenComposeAsync((ignored) -> {
                        jobManagerTerminationFutures.remove(jobId);
                        return action.apply(jobGraph);{//�����action �� persistAndRunJob()����:{
                            persistAndRunJob(){// action = persistAndRunJob()
                                runJob(jobGraph); //����ϸ�������һ��:
                                
                            }
                        }
                    }):
                }
            }
    
            return persistAndRunFuture.handleAsync((acknowledge, throwable) -> {});
        }
    }
}



// flink-akka.actor.default-dispatcher-2 �߳�: 
persistAndRunJob(){//Dispatcher.
    final CompletableFuture<Void> runJobFuture = runJob(jobGraph);{//Dispatcher
        final CompletableFuture<JobManagerRunner> jobManagerRunnerFuture = createJobManagerRunner(jobGraph);{
            final RpcService rpcService = getRpcService();
            return CompletableFuture.supplyAsync();
        }
        
        return jobManagerRunnerFuture
            .thenApply(FunctionUtils.uncheckedFunction(this::startJobManagerRunner(){
                // ����� CompletableFuture.supplyAsync(); ִ�����, �ʹ�����startJobManagerRunner()ִ��;
                Dispatcher.startJobManagerRunner();{
                    jobManagerRunner.getResultFuture().handleAsync(()->{});
                    
                    jobManagerRunner.start();{// JobManagerRunnerImpl.start()
                        leaderElectionService.start(this);{//EmbeddedLeaderService.EmbeddedLeaderElectionService
                            addContender(this, contender);{
                                if (!allLeaderContenders.add(service)) throw new IllegalStateException();
                                
                                updateLeader().whenComplete((aVoid, throwable) -> {fatalError(throwable);});{
                                    EmbeddedLeaderService.updateLeader(){//
                                        EmbeddedLeaderElectionService leaderService = allLeaderContenders.iterator().next();
                                        
                                        return execute(new GrantLeadershipCall(leaderService.contender, leaderSessionId, LOG));{
                                            return CompletableFuture.runAsync(runnable, notificationExecutor);{
                                                GrantLeadershipCall.run(){
                                                    contender.grantLeadership(leaderSessionId);{//JobManagerRunnerImpl.
                                                        leadershipOperation = leadershipOperation.thenCompose((ignored) -> {
                                                            return verifyJobSchedulingStatusAndStartJobManager(leaderSessionID);{
                                                                //��������
                                                                final CompletableFuture<JobSchedulingStatus> jobSchedulingStatusFuture = getJobSchedulingStatus();
                                                                return jobSchedulingStatusFuture.thenCompose(()->{
                                                                    return startJobMaster(leaderSessionId);
                                                                })
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }))
            .thenApply(FunctionUtils.nullFn())
            .whenCompleteAsync(
                (ignored, throwable) -> {
                    if (throwable != null) jobManagerRunnerFutures.remove(jobGraph.getJobID());
                },
                getMainThreadExecutor());
    
    }
    
    return runJobFuture.whenComplete(()->{jobGraphWriter.removeJobGraph(jobGraph.getJobID());});
}
        
        
// mini-cluster-io-thread-2 �߳�: 
verifyJobSchedulingStatusAndStartJobManager(){//JobManagerRunnerImpl.verifyJobSchedulingStatusAndStartJobManager()
    final CompletableFuture<JobSchedulingStatus> jobSchedulingStatusFuture = getJobSchedulingStatus();
    return jobSchedulingStatusFuture.thenCompose(jobSchedulingStatus -> {
        if (jobSchedulingStatus == JobSchedulingStatus.DONE) {
            return jobAlreadyDone();
        } else {
            return startJobMaster(leaderSessionId);{//JobManagerRunnerImpl.startJobMaster()
                runningJobsRegistry.setJobRunning(jobGraph.getJobID());
                startFuture = jobMasterService.start(new JobMasterId(leaderSessionId));{//JobMaster.start()
                    start();
                    
                    return callAsyncWithoutFencing(() -> startJobExecution(newJobMasterId), RpcUtils.INF_TIMEOUT);{
                        // �м�һ�ѵ�װ��;
                        
                        JobMaster.startJobExecution(){
                            startJobMasterServices();{
                                startHeartbeatServices();
                                slotPool.start(getFencingToken(), getAddress(), getMainThreadExecutor());
                                scheduler.start(getMainThreadExecutor());
                                reconnectToResourceManager(new FlinkException("Starting JobMaster component."));
                                
                                resourceManagerLeaderRetriever.start(new ResourceManagerLeaderListener());{//
                                    EmbeddedLeaderService.EmbeddedLeaderRetrievalService.start(){
                                        addListener(this, listener);{//EmbeddedLeaderService.addListener()
                                            notifyListener(currentLeaderAddress, currentLeaderSessionId, listener);{//EmbeddedLeaderService.notifyListener()
                                                return CompletableFuture.runAsync(new NotifyOfLeaderCall(address, leaderSessionId, listener, LOG), notificationExecutor);{
                                                    NotifyOfLeaderCall.run(){
                                                        listener.notifyLeaderAddress(address, leaderSessionId);{
                                                            runAsync(() -> notifyOfNewResourceManagerLeader(){// �첽ִ�и� notifyOfNewResourceManagerLeader()����;
                                                                ResourceManagerLeaderListener.notifyOfNewResourceManagerLeader(){
                                                                    resourceManagerAddress = createResourceManagerAddress(newResourceManagerAddress, resourceManagerId);
                                                                    reconnectToResourceManager(); // Դ���������;
                                                                }
                                                            });
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            resetAndStartScheduler();
                        }
                        
                    }
                    
                }
                
                return startFuture.thenAcceptAsync((Acknowledge ack) -> confirmLeaderSessionIdIfStillLeader());
            }
        }
    });
}



// flink-akka.actor.default-dispatcher-3 �߳�:  ����������� ���� RM ��Դ������?
JobMaster.ResourceManagerLeaderListener.notifyLeaderAddress(){
    runAsync(() -> notifyOfNewResourceManagerLeader(){//JobMaster.notifyOfNewResourceManagerLeader
        resourceManagerAddress = createResourceManagerAddress(newResourceManagerAddress, resourceManagerId);
        reconnectToResourceManager(new FlinkException(String.format( resourceManagerAddress)));{
            closeResourceManagerConnection(cause);
            tryConnectToResourceManager();{//JobMaster.tryConnectToResourceManager()
                if (resourceManagerAddress != null) connectToResourceManager();{
                    resourceManagerConnection = new ResourceManagerConnection()
                    resourceManagerConnection.start();{//ResourceManagerConnection.start()
                        final RetryingRegistration<F, G, S> newRegistration = createNewRegistration();
                        
                        newRegistration.startRegistration();{
                            CompletableFuture<Void> rpcGatewayAcceptFuture = rpcGatewayFuture.thenAcceptAsync((G rpcGateway) -> {
                                
                                // �����첽�߳�,ִ�� register()����, ���� thenAcceptAsync()�����ж���ע��ɹ���Ķ���: 
                                register(rpcGateway, 1);{//RetryingRegistration.register()
                                    CompletableFuture<RegistrationResponse> registrationFuture = invokeRegistration(gateway, fencingToken, timeoutMillis);
                                    
                                    CompletableFuture<Void> registrationAcceptFuture = registrationFuture.thenAcceptAsync(
                                        (RegistrationResponse result) -> {
                                            if (result instanceof RegistrationResponse.Success) {
                                                completionFuture.complete(Tuple2.of(gateway, success));{//CompletableFuture.complete()
                                                    // ������ÿ�����첽ִ��;
                                                    CompletableFuture.postComplete() -> tryFire() -> uniWhenComplete(){//CompletableFuture.uniWhenComplete()
                                                        c.claim(){
                                                            e.execute(this);{//ScheduledThreadPoolExecutor.execute
                                                                ScheduledThreadPoolExecutor.schedule();{
                                                                    new FutureTask().run() -> Executors.RunnableAdapter.call() -> Completion.run() -> UniWhenComplete.tryFire() -> CompletableFuture.uniWhenComplete(){
                                                                        // ����ִ�� ���� RetryingRegistration.createNewRegistration() ������ future.whenCompleteAsync()�еķ�����:
                                                                        RetryingRegistration.createNewRegistration() -> future.whenCompleteAsync(()->{
                                                                            
                                                                            AkkaRpcActor.handleMessage -> handleRpcMessage() -> handleRunAsync() => runAsync.getRunnable().run(){
                                                                                // ���ﴥ�� ResourceManagerConnection.onRegistrationSuccess()
                                                                                onRegistrationSuccess(result.f1); //��������������;
                                                                            }
                                                                        })
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            
                                        }
                                    );
                                }
                                
                            })
                        }
                    }
                }
            }
        }
    });
}




// flink-akka.actor.default-dispatcher-3 �߳�: RMע��ɹ���Ĳ���: onRegistrationSuccess(), �� requestSlot()����Task���������Task ? 
AkkaRpcActor.handleMessage()-> handleRpcMessage(message);{
    if (expectedFencingToken == null){
        
    }else{
        super.handleRpcMessage(fencedMessage.getPayload());{//AkkaRpcActor.handleRpcMessage()
            if (message instanceof RunAsync) {
                handleRunAsync((RunAsync) message);{//AkkaRpcActor.handleRunAsync()
                    runAsync.getRunnable().run();{
                        
                        ResourceManagerConnection.onRegistrationSuccess(){
                            runAsync(() -> {
                                if (this == resourceManagerConnection) {
                                    JobMaster.establishResourceManagerConnection(success);{//
                                        final ResourceManagerGateway resourceManagerGateway = resourceManagerConnection.getTargetGateway();
                                        establishedResourceManagerConnection = new EstablishedResourceManagerConnection();
                                        slotPool.connectToResourceManager(resourceManagerGateway);{//SlotPoolImpl.
                                            for (PendingRequest pendingRequest : waitingForResourceManager.values()) {
                                                requestSlotFromResourceManager(resourceManagerGateway, pendingRequest);{
                                                    final AllocationID allocationId = new AllocationID();
                                                    CompletableFuture<Acknowledge> rmResponse = resourceManagerGateway.requestSlot();{ //Զ�̵���;
                                                        // ����Akka��Rpc����, Զ�� ִ�� ResourceManager.requestSlot()���������ؽ��;
                                                    }
                                                }
                                            }
                                        }
                                        
                                    }
                                }
                            });    
                        }
                    }
                    
                }
            }
        }
    }
}




// "flink-akka.actor.default-dispatcher-5" : ��TaskManager�˷��� ����Slot���� ?
ResourceManager.requestSlot(){
    checkInit();
    internalRequestSlot(pendingSlotRequest);{//SlotManagerImpl.internalRequestSlot()
        OptionalConsumer.of(findMatchingSlot(resourceProfile))
            .ifPresent(taskManagerSlot -> allocateSlot(taskManagerSlot, pendingSlotRequest))
            .ifNotPresent(() -> fulfillPendingSlotRequestWithPendingTaskManagerSlot(pendingSlotRequest));{
                SlotManagerImpl.allocateSlot(){
                    TaskExecutorGateway gateway = taskExecutorConnection.getTaskExecutorGateway();
                    //gateway ��ʲô? TaskExecutorGateway => AkkaInvocationHandler => TaskExecutor.requestSlot(), Ӧ���ǵ�Զ��Rpc�������ݷ���;
                    CompletableFuture<Acknowledge> requestFuture = gateway.requestSlot();{
                        // ��Ack����Rpc����,�����յ��� TaskExecutor��  �������ִ��;
                        
                        { //�߳�: flink-akka.actor.default-dispatcher-3
                            TaskExecutor.requestSlot()
                                ->jobLeaderService.addJob(jobId, targetAddress) -> leaderRetrievalService.start(jobManagerLeaderListener);
                                
                        }
                    }
                    
                    requestFuture.whenComplete();
                    
                    completableFuture.whenCompleteAsync();
                }
            }
    }
}



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




















# 3.1 TaskExecutor�ϵ� 



// Window(TumblingEventTimeWindows(3000), EventTimeTrigger, CoGroupWindowFunction) -> Map -> Filter -> Sink: Print to Std. Err (2/4) �߳�: 
Task.run(){
    doRun();{
        TaskKvStateRegistry kvStateRegistry = kvStateService.createKvStateTaskRegistry(jobId, getJobVertexId());
        Environment env = new RuntimeEnvironment();
        invokable = loadAndInstantiateInvokable(userCodeClassLoader, nameOfInvokableClass, env);
        
        // ����invokable��ʵ��������: OneInputStreamTask;��StreamTask�ļ̳���:
        // StreamTask�Ļ�������3���̳���: AbstractTwoInputStreamTask, SourceReaderStreamTask, SourceStreamTask; ?
        invokable.invoke();{// OneInputStreamTask.invoke() -> ���ø���StreamTask.invoke()����;
            StreamTask.invoke(){
                
                // ������������ǰ,�Ƚ��г�ʼ��
                beforeInvoke();
                
                // ����������ѭ��������Ϣ,������;
                runMailboxLoop();
                
                // �������Ѻʹ����,��Դ�ͷ�;
                afterInvoke();
                
            }
        }
    }
}



Task.run(){
    doRun();{
        TaskKvStateRegistry kvStateRegistry = kvStateService.createKvStateTaskRegistry(jobId, getJobVertexId());
        Environment env = new RuntimeEnvironment();
        invokable = loadAndInstantiateInvokable(userCodeClassLoader, nameOfInvokableClass, env);
        
        // ����invokable��ʵ��������: OneInputStreamTask;��StreamTask�ļ̳���:
        // StreamTask�Ļ�������3���̳���: AbstractTwoInputStreamTask, SourceReaderStreamTask, SourceStreamTask; ?
        invokable.invoke();{// OneInputStreamTask.invoke() -> ���ø���StreamTask.invoke()����;
            StreamTask.invoke(){
                runMailboxLoop();{
                    mailboxProcessor.runMailboxLoop();{//MailboxProcessor.runMailboxLoop()
                        final MailboxController defaultActionContext = new MailboxController(this);
                        while (processMail(localMailbox)) {
                            mailboxDefaultAction.runDefaultAction(defaultActionContext); {// ʵ����: StreamTask.runDefaultAction()
                                // ���mailboxDefaultAction()����,�� new MailboxProcessor(this::processInput, mailbox, actionExecutor) ������ this::processInput ����;
                                StreamTask.processInput(){
                                    InputStatus status = input.emitNext(output);{//StreamTaskNetworkInput.
                                        while (true) {
                                            DeserializationResult result = currentRecordDeserializer.getNextRecord(deserializationDelegate);
                                            if (result.isFullRecord()) {//isFullRecord�ֶ�Ϊtrue;
                                                processElement(deserializationDelegate.getInstance(), output);{//StreamTaskNetworkInput.
                                                    if (recordOrMark.isRecord()){ //return getClass() == StreamRecord.class;
                                                        output.emitRecord(recordOrMark.asRecord());
                                                    } else if (recordOrMark.isWatermark()) { // return getClass() == Watermark.class;
                                                        statusWatermarkValve.inputWatermark(recordOrMark.asWatermark(), lastChannel);{//StatusWatermarkValue.inputWatermark()
                                                            // ��Ҫ�߼�: ����ˮλ�Ĺ���?
                                                            if (watermark.getTimestamp() > channelStatuses[channelIndex].watermark) {
                                                                channelStatuses[channelIndex].watermark = watermarkMillis;
                                                                findAndOutputNewMinWatermarkAcrossAlignedChannels();{
                                                                    if (hasAlignedChannels && newMinWatermark > lastOutputWatermark) {
                                                                        lastOutputWatermark = newMinWatermark;
                                                                        output.emitWatermark(new Watermark(lastOutputWatermark));{//OneInputStreamTask.StreamTaskNetworkOutput 
                                                                            operator.processWatermark(watermark){//AbstractStreamOperator
                                                                                if (timeServiceManager != null) {
                                                                                    timeServiceManager.advanceWatermark(mark);{//InternalTimeServiceManager
                                                                                        for (InternalTimerServiceImpl<?, ?> service : timerServices.values()) {
                                                                                            service.advanceWatermark(watermark.getTimestamp());{//InternalTimeServiceManager
                                                                                                while ((timer = eventTimeTimersQueue.peek()) != null && timer.getTimestamp() <= time) {
                                                                                                    eventTimeTimersQueue.poll();
                                                                                                    triggerTarget.onEventTime(timer);{// WindowOperator.onEventTime
                                                                                                        TriggerResult triggerResult = triggerContext.onEventTime(timer.getTimestamp());
                                                                                                        if (triggerResult.isFire()) {
                                                                                                            emitWindowContents(triggerContext.window, contents);{//WindowOperator.
                                                                                                                timestampedCollector.setAbsoluteTimestamp(window.maxTimestamp());
                                                                                                                userFunction.process(triggerContext.key, window, processContext, contents, timestampedCollector);{//InternalIterableWindowFunction.
                                                                                                                    wrappedFunction.apply(key, window, input, out);
                                                                                                                }
                                                                                                            }
                                                                                                        }
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                                }
                                                                                output.emitWatermark(mark);
                                                                                
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else if (recordOrMark.isLatencyMarker()) { // return getClass() == LatencyMarker.class;
                                                        output.emitLatencyMarker(recordOrMark.asLatencyMarker());
                                                    } else if (recordOrMark.isStreamStatus()) {
                                                        statusWatermarkValve.inputStreamStatus(recordOrMark.asStreamStatus(), lastChannel);
                                                    } else {
                                                        throw new UnsupportedOperationException("Unknown type of StreamElement");
                                                    } 
                                                }
                                            }
                                        }
                                    }
                                }
                                
                            }
                        }
                    }
                }
            }
        }
    }
}

// ���� ֱ�Ӵ�Kafka���ѵ�����,����ֱ��fromElements ���ɵ�����: 

// ��ͬ�ĺ����߼�:  LegacySourceFunctionThread.run() -> StreamSource.run()-> userFunction.run(ctx);

// Legacy Source: ������Source: ����������Դ?
SourceStreamTask.LegacySourceFunctionThread.run(){
    headOperator.run(getCheckpointLock(), getStreamStatusMaintainer(), operatorChain);{//StreamSource
        run(lockingObject, streamStatusMaintainer, output, operatorChain);{
            this.ctx = StreamSourceContexts.getSourceContext();
            userFunction.run(ctx);{//FlinkKafkaConsumerBase.run()
                
                FlinkKafkaConsumerBase.run(){
                    this.kafkaFetcher = createFetcher();
                    kafkaFetcher.runFetchLoop();{
                        final Handover handover = this.handover;
                        // ����Kafka�����߳�, ������Kafka��������;
                        consumerThread.start();
                        
                        while (running) {
                            //consumerThread�߳���ȡ��kafka���ݴ����handover����м�����/������;
                            final ConsumerRecords<byte[], byte[]> records = handover.pollNext();
                            for (KafkaTopicPartitionState<TopicPartition> partition : subscribedPartitionStates()) {
                                List<ConsumerRecord<byte[], byte[]>> partitionRecords =records.records(partition.getKafkaPartitionHandle());
                                for (ConsumerRecord<byte[], byte[]> record : partitionRecords) {
                                    
                                    // �����Ǵ���ҵ���߼��ĺ��ķ���: 
                                    emitRecord(value, partition, record.offset(), record);{//KafkaFetcher.
                                        emitRecordWithTimestamp(record, partition, offset, consumerRecord.timestamp());{//AbstractFetcher.
                                            sourceContext.collectWithTimestamp(record, timestamp);{//StreamSourceContexts.
                                                collect(element);{//StreamSourceContexts.NonTimestampContext
                                                    output.collect(reuse.replace(element));{//AbstractStreamOperator.CountingOutput
                                                        numRecordsOut.inc();
                                                        output.collect(record);{// Operator.CopyingChainingOutput.
                                                            pushToOperator(record);{//
                                                                StreamRecord<T> castRecord = (StreamRecord<T>) record;
                                                                StreamRecord<T> copy = castRecord.copy(serializer.copy(castRecord.getValue()));
                                                                operator.processElement(copy);{//StreamMap.
                                                                    // ���element:StreamRecord(value:T,hasTimestamp:Boolean,timestamp:Long);
                                                                    // ��� userFunction ���û�����ĺ�����: �൱��Java�е� @Interface �ӿڵĺ���ʵ��; 
                                                                    // ���userFunction Ҳ������MapFunction��ʵ����: MyMapFuncImpl, ��ֱ�ӵ��� MyMapFuncImpl.map()���д���;
                                                                    X element = userFunction.map(element.getValue());{// 
                                                                        
                                                                    }
                                                                    
                                                                    output.collect(element.replace(element){//StreamRecord.replace() ��ԭ��.value�ֶ��滻���µ�ֵ,������new StreamRecord ��ת���ɱ�;
                                                                        this.value = (T) element; 
                                                                        return (StreamRecord<X>) this; //�����������װ����, ��Ϊԭ��������: value:String ���� => value:(String,String) 
                                                                    });{//AbstractStreamOperator.CountingOutput.collect()
                                                                        numRecordsOut.inc(); //��ν��CountingOutput,��������򵥶�StreamRecord����+1;
                                                                        output.collect(record);{//OperatorChain.WatermarkGaugeExposingOutput �ļ����ӿ�
                                                                            // outputs: ��addSink()����ӵ����; 
                                                                            OperatorChain.BroadcastingOutputCollector.collect(record){
                                                                                for (Output<StreamRecord<T>> output : outputs) {
                                                                                    output.collect(record);
                                                                                }    
                                                                            }
                                                                            
                                                                            // �������Ӻ��滹����һ��filter/map()/������ʱ,
                                                                            OperatorChain.CopyingChainingOutput.collect(){
                                                                                if (this.outputTag != null) return;
                                                                                //����Record���� Operator������;
                                                                                pushToOperator(record);{//CopyingChainingOutput.
                                                                                    StreamRecord<T> copy = castRecord.copy(serializer.copy(castRecord.getValue()));
                                                                                    operator.processElement(copy);{ //����FilterStream��������
                                                                                        // �����userFunction: FilterFunction<T>��ʵ���� MyFilterFuncImpl��ʵ������;
                                                                                        if (userFunction.filter(element.getValue())) {
                                                                                            output.collect(element);
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                            
                                                                            // Shuffle��д����,�������Output;
                                                                            RecordWriterOutput[implements WatermarkGaugeExposingOutput].collect(){
                                                                                
                                                                            }
                                                                            
                                                                            // DirectedOutput[implements WatermarkGaugeExposingOutput]: 
                                                                            
                                                                            // ChiningOutput [implements WatermarkGaugeExposingOutput]: 
                                                                            
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            partitionState.setOffset(offset);
                                        }
                                    }
                                }
                            }
                            
                        }
                    }
                    
                }
                
                FromElementsFunction.run(){ // 
                    ByteArrayInputStream bais = new ByteArrayInputStream(elementsSerialized);
                    // ѭ�������涨����(numElements)��Ԫ��, ���������Driver�������?
                    while (isRunning && numElementsEmitted < numElements) {
                        T next = serializer.deserialize(input);
                        // ��ν��collect()����һ���������洫;����Ҫshuffle���������,��ֹͣ;
                        ctx.collect(next);{//StreamSourceContexts.NonTimestampContext.collect()
                            //�м��׼��װ��, ����ǵ�: element.replace(userFunction.map()); ���� userFunction.xxx(): map(),filter(),sum(),reduce()..
                            output.collect(reuse.replace(element));-> output.collect(record);->pushToOperator(record);-> operator.processElement(copy);{
                                output.collect(element.replace(userFunction.map(element.getValue())));
                            }
                        }
                        numElementsEmitted++;
                    }
                }
                
            }
        }
    }
    completionFuture.complete(null);
}




// 3.2 ����KafkaConsumer���߼�: 

FlinkKafkaConsumerBase.run(){
    this.kafkaFetcher = createFetcher();
    kafkaFetcher.runFetchLoop();{
        final Handover handover = this.handover;
        // ����Kafka�����߳�, ������Kafka��������;
        consumerThread.start();
        
        while (running) {
            //consumerThread�߳���ȡ��kafka���ݴ����handover����м�����/������;
            final ConsumerRecords<byte[], byte[]> records = handover.pollNext();
            for (KafkaTopicPartitionState<TopicPartition> partition : subscribedPartitionStates()) {
                List<ConsumerRecord<byte[], byte[]>> partitionRecords =records.records(partition.getKafkaPartitionHandle());
                for (ConsumerRecord<byte[], byte[]> record : partitionRecords) {
                    
                    // �����Ǵ���ҵ���߼��ĺ��ķ���: 
                    emitRecord(value, partition, record.offset(), record);{//KafkaFetcher.
                        emitRecordWithTimestamp(record, partition, offset, consumerRecord.timestamp());{//AbstractFetcher.
                            sourceContext.collectWithTimestamp(record, timestamp);{//StreamSourceContexts.
                                collect(element);{//StreamSourceContexts.NonTimestampContext
                                    output.collect(reuse.replace(element));{//AbstractStreamOperator.CountingOutput
                                        numRecordsOut.inc();
                                        output.collect(record);{// Operator.CopyingChainingOutput.
                                            pushToOperator(record);{//
                                                StreamRecord<T> castRecord = (StreamRecord<T>) record;
                                                StreamRecord<T> copy = castRecord.copy(serializer.copy(castRecord.getValue()));
                                                operator.processElement(copy);{//StreamMap.
                                                    // ���element:StreamRecord(value:T,hasTimestamp:Boolean,timestamp:Long);
                                                    // ��� userFunction ���û�����ĺ�����: �൱��Java�е� @Interface �ӿڵĺ���ʵ��; 
                                                    // ���userFunction Ҳ������MapFunction��ʵ����: MyMapFuncImpl, ��ֱ�ӵ��� MyMapFuncImpl.map()���д���;
                                                    X element = userFunction.map(element.getValue());{// 
                                                        
                                                    }
                                                    
                                                    output.collect(element.replace(element){//StreamRecord.replace() ��ԭ��.value�ֶ��滻���µ�ֵ,������new StreamRecord ��ת���ɱ�;
                                                        this.value = (T) element; 
                                                        return (StreamRecord<X>) this; //�����������װ����, ��Ϊԭ��������: value:String ���� => value:(String,String) 
                                                    });{//AbstractStreamOperator.CountingOutput.collect()
                                                        numRecordsOut.inc(); //��ν��CountingOutput,��������򵥶�StreamRecord����+1;
                                                        output.collect(record);{//OperatorChain.WatermarkGaugeExposingOutput �ļ����ӿ�
                                                            // outputs: ��addSink()����ӵ����; 
                                                            OperatorChain.BroadcastingOutputCollector.collect(record){
                                                                for (Output<StreamRecord<T>> output : outputs) {
                                                                    output.collect(record);
                                                                }    
                                                            }
                                                            
                                                            // �������Ӻ��滹����һ��filter/map()/������ʱ,
                                                            OperatorChain.CopyingChainingOutput.collect(){
                                                                if (this.outputTag != null) return;
                                                                //����Record���� Operator������;
                                                                pushToOperator(record);{//CopyingChainingOutput.
                                                                    StreamRecord<T> copy = castRecord.copy(serializer.copy(castRecord.getValue()));
                                                                    operator.processElement(copy);{ //����FilterStream��������
                                                                        // �����userFunction: FilterFunction<T>��ʵ���� MyFilterFuncImpl��ʵ������;
                                                                        if (userFunction.filter(element.getValue())) {
                                                                            output.collect(element);
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            
                                                            // Shuffle��д����,�������Output;
                                                            RecordWriterOutput[implements WatermarkGaugeExposingOutput].collect(){
                                                                
                                                            }
                                                            
                                                            // DirectedOutput[implements WatermarkGaugeExposingOutput]: 
                                                            
                                                            // ChiningOutput [implements WatermarkGaugeExposingOutput]: 
                                                            
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            partitionState.setOffset(offset);
                        }
                    }
                }
            }
            
        }
    }
    
}



// 3.3 ���� FromElements�ļ����߼�: 
FromElementsFunction.run(){ // 
    ByteArrayInputStream bais = new ByteArrayInputStream(elementsSerialized);
    // ѭ�������涨����(numElements)��Ԫ��, ���������Driver�������?
    while (isRunning && numElementsEmitted < numElements) {
        T next = serializer.deserialize(input);
        // ��ν��collect()����һ���������洫;����Ҫshuffle���������,��ֹͣ;
        ctx.collect(next);{//StreamSourceContexts.NonTimestampContext.collect()
            //�м��׼��װ��, ����ǵ�: element.replace(userFunction.map()); ���� userFunction.xxx(): map(),filter(),sum(),reduce()..
            output.collect(reuse.replace(element));-> output.collect(record);{//RecordWriterOutput.collect()
                if (this.outputTag != null) return;
                pushToRecordWriter(record);{//RecordWriterOutput.
                    serializationDelegate.setInstance(record);
                    recordWriter.emit(serializationDelegate);{
                        int nextChannelToSendTo = channelSelector.selectChannel(record);{// RebalancePartitioner.selectChannel() ��������?
                            nextChannelToSendTo = (nextChannelToSendTo + 1) % numberOfChannels;
                            return nextChannelToSendTo;
                        }
                        emit(record, nextChannelToSendTo);{//ChannelSelectorRecordWriter.emit()
                            serializer.serializeRecord(record);
                            
                            boolean pruneTriggered = copyFromSerializerToTargetChannel(targetChannel);{// RecordWriter.copyFromSerializerToTargetChannel()
                                SerializationResult result = serializer.copyToBufferBuilder(bufferBuilder);
                                while (result.isFullBuffer()) {
                                    finishBufferBuilder(bufferBuilder);
                                }
                                if (flushAlways) flushTargetPartition(targetChannel);
                            }
                            
                            if (pruneTriggered) { //
                                serializer.prune();
                            }
                        }
                    }
                }
            }
        }
        numElementsEmitted++;
    }
}

    // �������߳�: Map -> Sink: Print to Std. Out

// Window(TumblingEventTimeWindows(3000), EventTimeTrigger, CoGroupWindowFunction) -> Map -> Filter -> Sink: Print to Std. Err (2/4) �߳�: 
Task.run(){
    doRun();{
        TaskKvStateRegistry kvStateRegistry = kvStateService.createKvStateTaskRegistry(jobId, getJobVertexId());
        Environment env = new RuntimeEnvironment();
        invokable = loadAndInstantiateInvokable(userCodeClassLoader, nameOfInvokableClass, env);
        
        // ����invokable��ʵ��������: OneInputStreamTask;��StreamTask�ļ̳���:
        // StreamTask�Ļ�������3���̳���: AbstractTwoInputStreamTask, SourceReaderStreamTask, SourceStreamTask; ?
        invokable.invoke();{// OneInputStreamTask.invoke() -> ���ø���StreamTask.invoke()����;
            StreamTask.invoke(){
                beforeInvoke(); // ������������ǰ,�Ƚ��г�ʼ��
                // ����������ѭ��������Ϣ,������;
                runMailboxLoop();{
                    mailboxProcessor.runMailboxLoop();{
                        boolean hasAction = processMail(localMailbox);{//Դ����while(processMail(localMailbox)); û��Mailʱ��һֱ�����ڴ�,����Ϣ��
                            if (!mailbox.createBatch(){// TaskMailboxImpl.createBatch()
                                if (!hasNewMail) {
                                    return !batch.isEmpty();
                                }
                                
                            }) {
                                return true;
                            }
                            
                            while (isDefaultActionUnavailable() && isMailboxLoopRunning()) { //ѭ�������ڴ�,�ȵ�Mail��Ϣ; ������Ϣ�Ż����while()ѭ���е� runDefaultAction();
                                // ��������, һֱ�ȵ�ֱ�� queue��Ϊ��,ȡ����һ�� headMail:Mail
                                mailbox.take(MIN_PRIORITY).run();{//TaskMailboxImpl.take(int priority)
                                    Mail head = takeOrNull(batch, priority);
                                    while ((headMail = takeOrNull(queue, priority)) == null) {
                                        // �����߳��ź�; �����ڴ�, һ��("OutputFlusher for Source")�̷߳��� notEmpty.signal()�ź�,�ͽ����ȴ�,������Ϣ;
                                        notEmpty.await();
                                    }
                                    hasNewMail = !queue.isEmpty(); // ���� createBatch()���ж�;
                                    return headMail;
                                }
                            }
                            return isMailboxLoopRunning();// return mailboxLoopRunning;
                        }
                        
                        while (hasAction = processMail(localMailbox)) {//�����������жϵķ�����, �жϻ�����Running״̬ʱ,���������� runDefaultAction()
                            mailboxDefaultAction.runDefaultAction(defaultActionContext); {
                                this.processInput();{
                                    StreamTask.processInput();{
                                        // ���ﲻͬ�� inputProcessor:StreamInputProcessor ʵ����,���в�ͬ����;
                                        InputStatus status = inputProcessor.processInput();{
                                            StreamoneInputProcessor.processInput();{}
                                        }
                                    }
                                
                                    SourceStreamTask.processInput(controller);{
                                        
                                    }
                                }
                            }
                        }
                    }
                }
                
                // �������Ѻʹ����,��Դ�ͷ�;
                afterInvoke();
                
            }
        }
    }
}
 
Task.run(){
    doRun();{
        TaskKvStateRegistry kvStateRegistry = kvStateService.createKvStateTaskRegistry(jobId, getJobVertexId());
        Environment env = new RuntimeEnvironment();
        invokable = loadAndInstantiateInvokable(userCodeClassLoader, nameOfInvokableClass, env);
        
        // ����invokable��ʵ��������: OneInputStreamTask;��StreamTask�ļ̳���:
        // StreamTask�Ļ�������3���̳���: AbstractTwoInputStreamTask, SourceReaderStreamTask, SourceStreamTask; ?
        invokable.invoke();{// OneInputStreamTask.invoke() -> ���ø���StreamTask.invoke()����;
            StreamTask.invoke(){
                runMailboxLoop();{
                    mailboxProcessor.runMailboxLoop();{//MailboxProcessor.runMailboxLoop()
                        final MailboxController defaultActionContext = new MailboxController(this);
                        while (processMail(localMailbox)) {
                            mailboxDefaultAction.runDefaultAction(defaultActionContext); {// ʵ����: StreamTask.runDefaultAction()
                                // ���mailboxDefaultAction()����,�� new MailboxProcessor(this::processInput, mailbox, actionExecutor) ������ this::processInput ����;
                                StreamTask.processInput(){
                                    InputStatus status = input.emitNext(output);{//StreamTaskNetworkInput.
                                        // ����ط�ѭ������, �ӻ��������ζ�ȡÿ��record�� �ֽ�����,�����л��󽻸�����operatorȡ����;
                                        while (true) {
                                            DeserializationResult result = currentRecordDeserializer.getNextRecord(deserializationDelegate);
                                            if (result.isFullRecord()) {//isFullRecord�ֶ�Ϊtrue;
                                                processElement(deserializationDelegate.getInstance(), output);{//StreamTaskNetworkInput.
                                                    if (recordOrMark.isRecord()){ //return getClass() == StreamRecord.class;
                                                        output.emitRecord(recordOrMark.asRecord());{//OneInputStreamTask.StreamTaskNetworkOutput.emitRecord()
                                                            operator.processElement(record);{ //StreamMap.
                                                                //����ִ���û����� �߼�: userFunction.map();
                                                                output.collect(element.replace( userFunction.map(element.getValue())));
                                                            }
                                                        }
                                                    } else if (recordOrMark.isWatermark()) { // return getClass() == Watermark.class;
                                                        statusWatermarkValve.inputWatermark(recordOrMark.asWatermark(), lastChannel);
                                                    } else if (recordOrMark.isLatencyMarker()) { // return getClass() == LatencyMarker.class;
                                                        output.emitLatencyMarker(recordOrMark.asLatencyMarker());
                                                    } else if (recordOrMark.isStreamStatus()) {
                                                        statusWatermarkValve.inputStreamStatus(recordOrMark.asStreamStatus(), lastChannel);
                                                    } else {
                                                        throw new UnsupportedOperationException("Unknown type of StreamElement");
                                                    } 
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



StreamTask MailboxController  InputStatus StreamRecord
AbstractStreamOperator 
// ѧϰĿ�� 
    - �˽� 1����ҵ/���� ������read -> writerд��, ���������̺���Ҫ��ʱ;
    

//














    
