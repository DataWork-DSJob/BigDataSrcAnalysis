/**
Checkpoint Coordinator ������ source �ڵ� trigger Checkpoint. Ȼ��Source Task�����������а���CheckPoint barrier
	source �������д���Barrier�����յ�Barrier�Ľڵ�ͻᱣ����գ�����source��
	
	source �ڵ������ι㲥 barrier����� barrier ����ʵ�� Chandy-Lamport �ֲ�ʽ�����㷨�ĺ��ģ����ε� task ֻ���յ����н����� barrier �Ż�ִ����Ӧ�� Checkpoint

���ε� sink �ڵ��ռ����������� input �� barrier ֮�󣬻�ִ�б��ؿ��գ������ص�չʾ�� RocksDB incremental Checkpoint �����̣����� RocksDB ��ȫ��ˢ���ݵ������ϣ���ɫ�����Ǳ�ʾ����Ȼ�� Flink ��ܻ����ѡ��û���ϴ����ļ����г־û�����
sink �ڵ�������Լ��� Checkpoint ֮�󣬻Ὣ state handle ����֪ͨ Coordinator

�� Checkpoint coordinator �ռ������� task �� state handle������Ϊ��һ�ε� Checkpoint ȫ������ˣ���־û��洢���ٱ���һ�� Checkpoint meta �ļ�





*/


// Checkpoint������: 
JobManager����: 
	CheckpointCoordinator
	ScheduledTrigger
	StateBackend
	
TaskExecutor����:
	StreamTask
	CheckponitBarrier
	
	


Checkpoint�ĺ������� CheckpointCoordinator: ����Э����
    - coordinates the distributed snapshots of operators and state: �����Ƕ�Operators����(�������л�?)��״̬���г־û�; 
    - It triggers the checkpoint by sending the messages to the relevant tasks and collects the checkpoint acknowledgements; 
        ͨ��CheckpointCoordinator.timer:ScheduledExecutor �����ʱ�߳�����ʱ����Ϣ(��?)��ÿ��Task,������ack��Ϣ�����Checkpoint;
    - ִ��Checkpoint���ʾ��ǵ���һ�� ScheduledTrigger.triggerCheckpoint()����; 
    

    �첽���Ͽ���(asynchronous barrier snapshotting, ABS)�㷨
    
����1�� ScheduledTrigger.triggerCheckpoint()������ִ���߼�;
    - �ȼ���Ƿ����㴥������, ����Ļ�����1�� PendingCheckpoint ����1����ʱ���Ϊkey��Map��, �ȴ����͸�Tasks;
    - ��Task���յ� ? ��Ϣ, ����ִ��TaskExecutor��triggerCheckpoint()����, ����������շ�4����:        TaskExecutor.triggerCheckpoint()
        * ����(���TaskExecutor���)Source����, ���ܵ� ��������(CheckpointBarrier)ʱ�����Լ���snapshot����(��Ҫ��offset��Ϣ), �����������������Ӵ�;      SourceStreamTask.triggerCheckpointAsync();
        * ���: ÿ��Operator�����ڽ����� ���������εļ�������(CheckponitBarrier)��, ���������Լ���snapshot����(��Ҫ�����ӵĳ�Ա����?��state״̬����), �������ϼ��������δ�,���ѭ��
        * ���: Sink��������snapshot��,�ͷ��� ack��Ϣ�� JobManager��CheckpointCoordinatorЭ����, ���checkpont����ɹ���; 


 StreamTask.triggerCheckpoint() -> performCheckpoint() 
    operatorChain.prepareSnapshotPreBarrier(checkpointId);
    operatorChain.broadcastCheckpointBarrier();
    checkpointState(checkpointMetaData);
        // ������ͬ��ִ��Checkpoint;
        * for (StreamOperator<?> op : allOperators) 
            op[AbstractStreamOperator].snapshotState();
        // �첽ִ��Checkpoint
        owner.asyncOperationsThreadPool.execute(new AsyncCheckpointRunnable());


 
        
checkpoint:        
- �����е�source����������ע��checkpointBarrier��TaskManager���յ��������ι㲥��CheckpointBarrier �󣬴���checkpoint��
- ������DAGͼ���������checkpoint������֮�󣬻�㱨��JobManager    
    
    
    prepareSnapshotPreBarrier(checkpointId);
                                    operatorChain.broadcastCheckpointBarrier();
                                    // 3. ������ 
                                    checkpointState




// ckp1. JobManager �г�ʼ�� State Checkpoint �����;
// Checkpoint Coordinator


����1: Checkpoint �����̺߳ͷ���:
    - �߳�: flink-akka.actor.default-sispatcher-3;
    - �������� JobMaster.startScheduling() -> DefaultScheduler.startSchedulingInternal()
        -> executionGraph.transitionToRunning() -> ExecutionGraph.notifyJobStatusChange()
            -> CheckpointCoordinatorDeActivator.jobStatusChanges() ->  CheckpointCoordinator.scheduleTriggerWithDelay()



JobMaster.resetAndStartScheduler(){
    validateRunsInMainThread();
    if (schedulerNG.requestJobStatus() == JobStatus.CREATED) {
        schedulerAssignedFuture = CompletableFuture.completedFuture(null);
        schedulerNG.setMainThreadExecutor(getMainThreadExecutor());
    }else{
        final SchedulerNG newScheduler = createScheduler(newJobManagerJobMetricGroup);
        schedulerAssignedFuture = schedulerNG.getTerminationFuture().handle(
            assignScheduler(newScheduler, newJobManagerJobMetricGroup);
        );
    }

    //��������½� schedulerNG,��ִ���� assignScheduler�̺߳�,��ִ�� startScheduling()�̺߳ͷ���;
    schedulerAssignedFuture.thenRun(this::startScheduling);{

        JobMaster.startScheduling(){
            schedulerNG.registerJobStatusListener(new JobManagerJobStatusListener());
            schedulerNG.startScheduling();{//JobMaster.startScheduling()
                registerJobMetrics();
                startSchedulingInternal();{//DefaultScheduler.startSchedulingInternal()
                    prepareExecutionGraphForNgScheduling();{//SchedulerBase.
                        executionGraph.transitionToRunning();{
                            if (!transitionState(JobStatus.CREATED, JobStatus.RUNNING){//ExecutionGraph.transitionState()
                                assertRunningInJobMasterMainThread();
                                notifyJobStatusChange(newState, error);{//ExecutionGraph.notifyJobStatusChange()
                                    for (JobStatusListener listener : jobStatusListeners) {
                                        listener.jobStatusChanges(getJobID(), newState, timestamp, serializedError);{

                                            /** �����Checkpoint��ص�Listener ��������job��,�����˶�ʱcheckpiontScheculer�����߳�;
                                            *
                                            */
                                            CheckpointCoordinatorDeActivator.jobStatusChanges(){//CheckpointCoordinatorDeActivator.jobStatusChanges()
                                                coordinator.startCheckpointScheduler();{
                                                    currentPeriodicTrigger = scheduleTriggerWithDelay(getRandomInitDelay());{//CheckpointCoordinator.scheduleTriggerWithDelay()
                                                        return timer.scheduleAtFixedRate(new ScheduledTrigger(),initDelay, baseInterval, TimeUnit.MILLISECONDS);{//ScheduledExecutorServiceAdapter.
                                                            // ��������� Checkpoint.duration��ʱƵ�ʵ��߳�,ȡ��checkpoint;
                                                            return scheduledExecutorService.scheduleAtFixedRate(command, initialDelay, period, unit);
                                                        }
                                                    }
                                                }
                                            }

                                            JobManagerJobStatusListener.jobStatusChanges(){}
                                        }
                                    }
                                }
                            }) {
                                throw new IllegalStateException("Job may only be scheduled from state " + JobStatus.CREATED);
                            }
                        }
                    }
                    schedulingStrategy.startScheduling();
                }
            }
        }
    }
}



// ckp2. JobManager�� �̶���� ������� Checkpoint�� ��ϸ����; 
// �⻹���� JobMaster������; 

run:1841, CheckpointCoordinator$ScheduledTrigger (org.apache.flink.runtime.checkpoint)
call:511, Executors$RunnableAdapter (java.util.concurrent)
runAndReset:308, FutureTask (java.util.concurrent)
access$301:180, ScheduledThreadPoolExecutor$ScheduledFutureTask (java.util.concurrent)
run:294, ScheduledThreadPoolExecutor$ScheduledFutureTask (java.util.concurrent)
runWorker:1149, ThreadPoolExecutor (java.util.concurrent)
run:624, ThreadPoolExecutor$Worker (java.util.concurrent)
run:748, Thread (java.lang)


// CheckpointCoordinator.ScheduledTrigger.triggerCheckpoint() �̶�Ƶ�� checkpointId �¼�,

timer.scheduleAtFixedRate(new ScheduledTrigger(),initDelay, baseInterval, TimeUnit.MILLISECONDS);{// ScheduledExecutorServiceAdapter.scheduleAtFixedRate()
    // ��������� Checkpoint.duration��ʱƵ�ʵ��߳�,ȡ��checkpoint;
    return scheduledExecutorService.scheduleAtFixedRate(command, initialDelay, period, unit);{//Executors$DelegatedScheduledExecutorService.
        
        CheckpointCoordinator.ScheduledTrigger.run(){
            triggerCheckpoint(System.currentTimeMillis(), true);{//ScheduledTrigger.triggerCheckpoint()
                return triggerCheckpoint(timestamp, checkpointProperties, null, isPeriodic, false);{//CheckpointCoordinator.triggerCheckpoint()
                    for (Execution execution: executions) {
                        execution.triggerCheckpoint(checkpointID, timestamp, checkpointOptions);{// Execution.triggerCheckpoint()
                            triggerCheckpointHelper(checkpointId, timestamp, checkpointOptions, false);{
                                taskManagerGateway.triggerCheckpoint(attemptId, getVertex().getJobId(), checkpointId, timestamp, 
                                checkpointOptions, advanceToEndOfEventTime);{//RpcTaskManagerGateway.triggerCheckpoint()
                                    // ͨ��akka ��̬�������: triggerCheckpoint:-1, $Proxy15  -> invoke:129, AkkaInvocationHandler ..
                                    
                                    // ����һ������,��miniCluster�е�TM�߳���:-> TaskExecutor.triggerCheckpoint()
                                    TaskExecutor.triggerCheckpoint() -> Task.triggerCheckpointBarrier() -> SourceStreamTask.triggerCheckpointAsync()
                                        -> StreamTask.triggerCheckpoint() -> StreamTask.performCheckpoint() -> StreamTask.checkpointState()
                                            -> AbstractStreamOperator.snapshotState() 
                                                -> userFunction.snapshotState() 
                                            // ��ϸԴ�� �μ����Ĵ���;
                                    
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

	// flink-1.12.2 src: 
	// ckp2.2 ÿ�δ��� ckp�����: CheckpointCoordinator.triggerCheckpoint()
	CheckpointCoordinator.triggerCheckpoint(props,externalSaveLocation,isPeriodic){
		CheckpointTriggerRequest request = new CheckpointTriggerRequest(props, externalSavepointLocation, isPeriodic);
        Optional<CheckpointTriggerRequest> requestOp = chooseRequestToExecute(request);
		requestOp.ifPresent(this::startTriggeringCheckpoint);{//CheckpointCoordinator.startTriggeringCheckpoint()
			preCheckGlobalState(request.isPeriodic);
			final long timestamp = System.currentTimeMillis();
			
			CompletableFuture<CheckpointIdAndStorageLocation> initedCkpStorage = initializeCheckpoint(request.props, request.externalSavepointLocation);{
				return CompletableFuture.supplyAsync(()->{
					long checkpointID = checkpointIdCounter.getAndIncrement();
					CheckpointStorageLocation checkpointStorageLocation =  props.isSavepoint() 
						? checkpointStorage.initializeLocationForSavepoint()
                        : checkpointStorage.initializeLocationForCheckpoint();
					return new CheckpointIdAndStorageLocation(checkpointID, checkpointStorageLocation);
										
				});
			}
			final CompletableFuture<PendingCheckpoint> pendingCheckpointCompletableFuture = initedCkpStorage.thenApplyAsync((checkpointIdAndStorageLocation) -> {
								// ckp2.2.1	����1��Ckp�¼�����ض���;
								createPendingCheckpoint();// Դ��ϸ�� ���� CheckpointCoordinator.createPendingCheckpoint()���
							},timer);
			
			final CompletableFuture<?> coordinatorCheckpointsComplete = pendingCheckpointCompletableFuture.thenComposeAsync(
                            (pendingCheckpoint) ->
                                    OperatorCoordinatorCheckpoints
                                            .triggerAndAcknowledgeAllCoordinatorCheckpointsWithCompletion(
                                                    coordinatorsToCheckpoint,
                                                    pendingCheckpoint,
                                                    timer),
                            timer);
			
			
			final CompletableFuture<?> masterStatesComplete = coordinatorCheckpointsComplete.thenComposeAsync(
                            ignored -> {
                                PendingCheckpoint checkpoint =
                                        FutureUtils.getWithoutException(
                                                pendingCheckpointCompletableFuture);
                                return snapshotMasterState(checkpoint);
                            },
                            timer);
							
			CompletableFuture.allOf(masterStatesComplete, coordinatorCheckpointsComplete)
					.handleAsync((ignored, throwable) -> {
							final PendingCheckpoint checkpoint =FutureUtils.getWithoutException(pendingCheckpointCompletableFuture);
							if (throwable != null) {
								onTriggerFailure(request, throwable);
							}else{
								if (checkpoint.isDisposed()) {
									onTriggerFailure()
								}else{
									final long checkpointId =checkpoint.getCheckpointId();
									// �־û� snapshot�����?
									snapshotTaskState();// Դ��ϸ�� ���� CheckpointCoordinator.snapshotTaskState()���
									
									coordinatorsToCheckpoint.forEach();
									onTriggerSuccess();
								}
							}
							return null;
						}, timer)
                            .exceptionally(
                                    error -> {
                                        if (!isShutdown()) {
                                            throw new CompletionException(error);
                                        } else if (findThrowable(
                                                        error, RejectedExecutionException.class)
                                                .isPresent()) {
                                            LOG.debug("Execution rejected during shutdown");
                                        } else {
                                            LOG.warn("Error encountered during shutdown", error);
                                        }
                                        return null;
                                    }));
			
		}
        return request.onCompletionPromise;
	}

	// ckp2.2.1	����1��Ckp����:PendingCheckpoint,���ӵ�pending�б��еȴ�����; 
	// JobManger: CheckpointCoordinator.ScheduledTrigger.triggerCheckpoint() -> CheckpointCoordinator.triggerCheckpoint() �� ��һ�������첽 createPendingCheckpoint()
	// ��־:  Triggering checkpoint 818 (type=CHECKPOINT) @ 1642768539241 for job 345db01b21c61fb4e441287a7bb77daf.
	CheckpointCoordinator.createPendingCheckpoint(timestamp,checkpointID,checkpointStorageLocation){
		preCheckGlobalState(isPeriodic);
		PendingCheckpoint checkpoint =new PendingCheckpoint();
		pendingCheckpoints.put(checkpointID, checkpoint);
		ScheduledFuture<?> cancellerHandle = timer.schedule();
		// ��־:  Triggering checkpoint 818 (type=CHECKPOINT) @ 1642768539241 for job 345db01b21c61fb4e441287a7bb77daf.
		LOG.info("Triggering checkpoint {} (type={}) @ {} for job {}.",
					checkpointID,//818
					checkpoint.getProps().getCheckpointType(),	//CHECKPOINT
					timestamp,// 1642768539241
					job);//345db01b21c61fb4e441287a7bb77daf
		return checkpoint;
	}
	

	// ckp2.2.2 ��� ckp׼��������, �첽���� CheckpointCoordinator.snapshotTaskState()

	CheckpointCoordinator.snapshotTaskState(timestamp,checkpointID,checkpointStorageLocation,props,executions){
		final CheckpointOptions checkpointOptions =CheckpointOptions.create();
        for (Execution execution : executions) {
            if (props.isSynchronous()) {// postCheckpointAction != PostCheckpointAction.NONE; 
				
                execution.triggerSynchronousSavepoint(checkpointID, timestamp, checkpointOptions);{
					// ����ʵ��, ͨ����һ��; �ο�����;
					triggerCheckpointHelper(checkpointId, timestamp, checkpointOptions);
				}
            } else {
                execution.triggerCheckpoint(checkpointID, timestamp, checkpointOptions);{// Execution.triggerCheckpoint()
					triggerCheckpointHelper(checkpointId, timestamp, checkpointOptions, false);{
						taskManagerGateway.triggerCheckpoint(attemptId, getVertex().getJobId(), checkpointId, timestamp, 
						checkpointOptions, advanceToEndOfEventTime);{//RpcTaskManagerGateway.triggerCheckpoint()
							// ͨ��akka ��̬�������: triggerCheckpoint:-1, $Proxy15  -> invoke:129, AkkaInvocationHandler ..
							
							// ����һ������,��miniCluster�е�TM�߳���:-> TaskExecutor.triggerCheckpoint()
							TaskExecutor.triggerCheckpoint() -> Task.triggerCheckpointBarrier() -> SourceStreamTask.triggerCheckpointAsync()
								-> StreamTask.triggerCheckpoint() -> StreamTask.performCheckpoint() -> StreamTask.checkpointState()
									-> AbstractStreamOperator.snapshotState() 
										-> userFunction.snapshotState() 
									// ��ϸԴ�� �μ����Ĵ���;
							
						}
					}
				}
            }
        }
	}





// ckp3. TaskExecutor�� ���� source ���ܵ�ckp�����ź�, ���� Barrier 
//   TaskExecutor ���յ� ckp�����ź�, ִ������ ���ӵ�ckp 

// ��Task Executor ��������
// ����һ������,��miniCluster�е�TM�߳���:-> TaskExecutor.triggerCheckpoint()


// ckp3. �ϸ�����ģ��: ��JobManger�� CheckpointCoordinator.ScheduledTrigger.triggerCheckpoint() -> Execution.triggerCheckpointHelper() �д���Rpc����; 
// ckp3. ���߳���
// Mailbox.run() -> ActorCell.receiveMessage() -> AkkaRpcActor.handleRpcMessage() -> Method.invoke()
// 
TaskExecutor.triggerCheckpoint(){
    task.triggerCheckpointBarrier(checkpointId, checkpointTimestamp, checkpointOptions, advanceToEndOfEventTime);{//Task.
        invokable.triggerCheckpointAsync(checkpointMetaData, checkpointOptions, advanceToEndOfEventTime);{
            SourceStreamTask.triggerCheckpointAsync(){
                return super.triggerCheckpointAsync(checkpointMetaData, checkpointOptions, advanceToEndOfEventTime);{
					// �����߳�, �첽ִ�� StreamTask.triggerCheckpoint()
                    return mailboxProcessor.getMainMailboxExecutor().submit(() -> triggerCheckpoint(checkpointMetaData, checkpointOptions,checkpointMetaData,checkpointOptions);{
                        MailboxExecutor.submit() -> StreamTask.triggerCheckpoint(){
                            boolean success = performCheckpoint(checkpointMetaData, checkpointOptions, checkpointMetrics, advanceToEndOfEventTime);{//StreamTask.
                                final long checkpointId = checkpointMetaData.getCheckpointId();
                                actionExecutor.runThrowing(() -> {
									
									subtaskCheckpointCoordinator.checkpointState(checkpointMetaData,checkpointOptions);{
									// ��׼��1��Barrier,����ת���������� Operators;
                                    operatorChain.prepareSnapshotPreBarrier(checkpointId);
                                    operatorChain.broadcastCheckpointBarrier();
                                    // 3. ������ 
                                    checkpointState(checkpointMetaData, checkpointOptions, checkpointMetrics);{//StreamTask.checkpointState()
                                        CheckpointingOperation checkpointingOperation = new CheckpointingOperation();
                                        checkpointingOperation.executeCheckpointing();{
                                            // ͬ��ִ�и����ӵ�checkpoint 
                                            for (StreamOperator<?> op : allOperators) {
                                                checkpointStreamOperator(op);{// StreamTask$CheckpointingOperation.checkpointStreamOperator()
                                                    OperatorSnapshotFutures snapshotInProgress = op.snapshotState();{//AbstractStreamOperator.snapshotState()
                                                        OperatorSnapshotFutures snapshotInProgress = new OperatorSnapshotFutures();
                                                        StateSnapshotContextSynchronousImpl snapshotContext = new StateSnapshotContextSynchronousImpl();
                                                        snapshotState(snapshotContext);{
                                                            StreamingFunctionUtils.snapshotFunctionState(context, getOperatorStateBackend(), userFunction);{
                                                                while (true) {
                                                                    if (trySnapshotFunctionState(context, backend, userFunction){
                                                                        if (userFunction instanceof CheckpointedFunction) {
                                                                            ((CheckpointedFunction)userFunction).snapshotState(context);
                                                                            return true;
                                                                        }
                                                                        
                                                                        if (userFunction instanceof ListCheckpointed) {
                                                                            List<Serializable> partitionableState = ((ListCheckpointed)userFunction).snapshotState(context.getCheckpointId(), context.getCheckpointTimestamp());{
                                                                                //�û���ʵ��Operator�� ������սӿڷ���;
                                                                                ExampleIntegerSource.snapshotState();
                                                                            }
                                                                            ListState listState = backend.getSerializableListState();
                                                                            listState.clear();
                                                                        }
                                                                    }) {
                                                                        break;
                                                                    }
                                                                }
                                                            } 
                                                        }
                                                    }
                                                }
                                            }
                                            
                                            // �����첽Checkpoint,���̳߳�ִ��
                                            AsyncCheckpointRunnable asyncCheckpointRunnable = new AsyncCheckpointRunnable();
                                            owner.cancelables.registerCloseable(asyncCheckpointRunnable);
                                            owner.asyncOperationsThreadPool.execute(asyncCheckpointRunnable);
                                        }
                                    }
									
									}
                                    
                                });
                            }
                        }
                    }

                }
            }
            
            // ��������Task ����?
			}
        }
    }
}

Task.run().doRun(){
	StreamTask.invoke()
	StreamTask.runMailboxLoop()
	StreamTask.processInput()
	StreamTaskNetworkInput.emitNext(){
		while (true) {
			if (currentRecordDeserializer != null) {
				DeserializationResult result = currentRecordDeserializer.getNextRecord(deserializationDelegate);
				if (result.isBufferConsumed()) {
					currentRecordDeserializer = null;
				}
				if (result.isFullRecord()) {
					processElement(deserializationDelegate.getInstance(), output);
					return InputStatus.MORE_AVAILABLE;
				}
			}
			
			Optional<BufferOrEvent> bufferOrEvent = checkpointedInputGate.pollNext();{//CheckpointedInputGate
				Optional<BufferOrEvent> next = inputGate.pollNext();
				if (!next.isPresent()) { // ʲô״̬ ���� ��buffer ? io��û��? ckp��û������ʱ�� ? 
					return handleEmptyBuffer();
				}
				
				// 
				BufferOrEvent bufferOrEvent = next.get();
				if (bufferOrEvent.isEvent()) { // ʲô�����, 
					return handleEvent(bufferOrEvent);{// CheckpointedInputGate.handleEvent
						Class<? extends AbstractEvent> eventClass = bufferOrEvent.getEvent().getClass();
						if (eventClass == CheckpointBarrier.class) {
							barrierHandler.processBarrier(checkpointBarrier, bufferOrEvent.getChannelInfo());{
								// while reading the first barrier , It can handle/track just single checkpoint at a time.
								SingleCheckpointBarrierHandler.processBarrier(barrier,channelInfo){
									if (currentCheckpointId > barrierId || (currentCheckpointId == barrierId && !isCheckpointPending())) {
										return;
									}
									if (currentCheckpointId < barrierId) {
										currentCheckpointId = barrier.getId();
										if (controller.preProcessFirstBarrier(channelInfo, barrier)) {
											notifyCheckpoint(barrier);{//CheckpointBarrierHandler.notifyCheckpoint
												CheckpointMetaData checkpointMetaData =new CheckpointMetaData(checkpointBarrier.getId(), checkpointBarrier.getTimestamp());
												toNotifyOnCheckpoint.triggerCheckpointOnBarrier(checkpointMetaData, checkpointBarrier.getCheckpointOptions(), checkpointMetrics);{//StreamTask.
													boolean isCheckpointOk = performCheckpoint(checkpointMetaData, checkpointOptions, checkpointMetrics);{//StreamTask.performCheckpoint()
														if (isRunning) {
															actionExecutor.runThrowing(()->{
																subtaskCheckpointCoordinator.checkpointState();{//SubtaskCheckpointCoordinatorImpl.checkpointState()
																	
																}
															});
															return true;
														}else{
															
															return false;
														}
													}
													if (isCheckpointOk) {
														if (isSynchronousSavepointId(checkpointMetaData.getCheckpointId())) {
															runSynchronousSavepointMailboxLoop();
														}
													}
												}
											}
										}
									}
								}
								// Once it has observed all checkpoint barriers for a checkpoint ID, notifies its listener of a completed checkpoint.
								CheckpointBarrierTracker.processBarrier(CheckpointBarrier receivedBarrier, InputChannelInfo channelInfo);
								
							}
						}else if (eventClass == CancelCheckpointMarker.class) {
							barrierHandler.processCancellationBarrier((CancelCheckpointMarker) bufferOrEvent.getEvent());
						}else if (eventClass == EndOfPartitionEvent.class) {
							barrierHandler.processEndOfPartition();
						} else if (eventClass == EventAnnouncement.class) {
							barrierHandler.processBarrierAnnouncement();
						} else if (bufferOrEvent.getEvent().getClass() == EndOfChannelStateEvent.class) {
							upstreamRecoveryTracker.handleEndOfRecovery(bufferOrEvent.getChannelInfo());
						}
						
					}
				}else if (bufferOrEvent.isBuffer()) {
					barrierHandler.addProcessedBytes(bufferOrEvent.getBuffer().getSize());
				}
				return next;
			}
			
		}
	}
}



logCompletedInternal:67, AbstractSnapshotStrategy (org.apache.flink.runtime.state)
logSyncCompleted:54, AbstractSnapshotStrategy (org.apache.flink.runtime.state)
snapshot:237, DefaultOperatorStateBackend (org.apache.flink.runtime.state)
snapshotState:213, StreamOperatorStateHandler (org.apache.flink.streaming.api.operators)
snapshotState:162, StreamOperatorStateHandler (org.apache.flink.streaming.api.operators)
snapshotState:371, AbstractStreamOperator (org.apache.flink.streaming.api.operators)
checkpointStreamOperator:686, SubtaskCheckpointCoordinatorImpl (org.apache.flink.streaming.runtime.tasks)
buildOperatorSnapshotFutures:607, SubtaskCheckpointCoordinatorImpl (org.apache.flink.streaming.runtime.tasks)
takeSnapshotSync:572, SubtaskCheckpointCoordinatorImpl (org.apache.flink.streaming.runtime.tasks)
checkpointState:298, SubtaskCheckpointCoordinatorImpl (org.apache.flink.streaming.runtime.tasks)
lambda$performCheckpoint$9:1004, StreamTask (org.apache.flink.streaming.runtime.tasks)




// ckp4. TaskExecutor ��, operatorChain������׼�� CheckpointBarrier���㲥��ȥ;
	// TaskExecutor.triggerCheckpoint() -> SourceStreamTask.triggerCheckpointAsync() �������߳�ִ�� StreamTask.triggerCheckpoint()
	// StreamTask.triggerCheckpoint() -> StreamTask.performCheckpoint() -> SubtaskCheckpointCoordinatorImpl.checkpointState()
	
	// Task����� checkpoint, ���ľ���: ����operatorChain ִ��ÿ��Operator.snapshotState()
	SubtaskCheckpointCoordinatorImpl.checkpointState(metadata,operatorChain){
		if (lastCheckpointId >= metadata.getCheckpointId()) {
			channelStateWriter.abort(metadata.getCheckpointId(), new CancellationException(), true);
			return;
		}
		// Step (1): Prepare the checkpoint, allow operators to do some pre-barrier work.
		// ��������δclosed�� Operator , ��ִ���� prepareSnapshotPreBarrier
		operatorChain.prepareSnapshotPreBarrier(metadata.getCheckpointId());{
			for (StreamOperatorWrapper<?, ?> operatorWrapper : getAllOperators()) {
				if (!operatorWrapper.isClosed()) {
					operatorWrapper.getStreamOperator().prepareSnapshotPreBarrier(checkpointId);{
						// �����Ӽ��ɵ� 
						StreamSource.prepareSnapshotPreBarrier()
						StreamFilter.prepareSnapshotPreBarrier()
						ProcessOperator.prepareSnapshotPreBarrier()
						StreamMap.prepareSnapshotPreBarrier()
					}
				}
			}
		}
		// Step (2): Send the checkpoint barrier downstream
		operatorChain.broadcastEvent(new CheckpointBarrier(id,timestamp,options));{
			// streamOutputs:RecordWriterOutput<?>[], 
			for (RecordWriterOutput<?> streamOutput : streamOutputs) {
				streamOutput.broadcastEvent(event, isPriorityEvent);{// RecordWriterOutput.broadcastEvent
					recordWriter.broadcastEvent(event, isPriorityEvent);{//RecordWriter.broadcastEvent()
						targetPartition.broadcastEvent(event, isPriorityEvent);{// BufferWritingResultPartition.broadcastEvent
							checkInProduceState();
							finishBroadcastBufferBuilder();
							finishUnicastBufferBuilders();
							
						}
						if (flushAlways) {
							flushAll();
						}
					}
				}
			}
		}
		
		// Step (3): Prepare to spill the in-flight buffers for input and output
		if (options.isUnalignedCheckpoint()) {
			channelStateWriter.finishOutput(metadata.getCheckpointId());
		}
		
		// Step (4): Take the state snapshot. This should be largely asynchronous, to not impact
		// �������� ִ�и� Operator.snapshotState()������ȡ����;
		boolean snapshotSyncOk = takeSnapshotSync(snapshotFutures, metadata, metrics, options, operatorChain, isRunning);{//SubtaskCheckpointCoordinatorImpl.takeSnapshotSync
			
			CheckpointStreamFactory storage =checkpointStorage.resolveCheckpointStorageLocation(checkpointId, checkpointOptions.getTargetLocation());
			for (StreamOperatorWrapper<?, ?> operatorWrapper : operatorChain.getAllOperators(true)) {
				if (!operatorWrapper.isClosed()) {
					OperatorSnapshotFutures snapshotFuture = buildOperatorSnapshotFutures();{// SubtaskCheckpointCoordinatorImpl.buildOperatorSnapshotFutures()
						OperatorSnapshotFutures snapshotInProgress =checkpointStreamOperator(op,checkpointMetaData,checkpointOptions);{
							return op.snapshotState(checkpointId,timestamp,checkpointOptions,factory);{// StreamOperator.snapshotState() ���� AbstractStreamOperator.snapshotState
								return stateHandler.snapshotState();{//StreamOperatorStateHandler.snapshotState()
									KeyGroupRange keyGroupRange = null != keyedStateBackend? keyedStateBackend.getKeyGroupRange(): KeyGroupRange.EMPTY_KEY_GROUP_RANGE;
									StateSnapshotContextSynchronousImpl snapshotContext = new StateSnapshotContextSynchronousImpl();
									// �����ڲ��ڶ����� inProgress,context �� ���շ���
									snapshotState(snapshotInProgress,snapshotContext);// Դ��ʵ������: StreamOperatorStateHandler.snapshotState()
									return snapshotInProgress;
								}
							}
						}
						if (op == operatorChain.getMainOperator()) {
							snapshotInProgress.setInputChannelStateFuture();
						}
						if (op == operatorChain.getTailOperator()) {
							snapshotInProgress.setResultSubpartitionStateFuture();
						}
						
						return snapshotInProgress;
					}
					operatorSnapshotsInProgress.put(operatorID,snapshotFuture);
				}
			}
		}
		
		if (snapshotSyncOk) {// �����ɹ���������; 
			finishAndReportAsync(snapshotFutures, metadata, metrics, isRunning);{//SubtaskCheckpointCoordinatorImpl.
				asyncOperationsThreadPool.execute(new AsyncCheckpointRunnable(snapshotFutures));{
					AsyncCheckpointRunnable.run(){// Դ��������� AsyncCheckpointRunnable.run, ��Ҫ���������Ӳ�ִ��callInternal(),����� reportCompletedSnapshot������ɵ�; 
						for (Map.Entry<OperatorID, OperatorSnapshotFutures> entry :operatorSnapshotsInProgress.entrySet()) {
							callInternal();
							logAsyncSnapshotComplete(startTime);
						}
						reportCompletedSnapshotStates();
					}
				}
			}
		}else{
			cleanup(snapshotFutures, metadata, metrics, new Exception("Checkpoint declined"));
		}
	}

	// ckp4.2 Task�е�Stream Operator��snapshotState ״̬����
	// ������Ϊ�� ckpId��ÿһ������, ����һ��AsyncSnapshotCallable,�������������ڲ�����: callInternal(),���ں��� finishAndReportAsync()ʱ�첽��ɳ־û�; 
	// ����־û�������·��: SubtaskCheckpointCoordinatorImpl.checkpointState() -> StreamOperatorStateHandler.snapshotState() -DefaultOperatorStateBackend.snapshotState(): new AsyncSnapshotCallable(){}
	StreamOperatorStateHandler.snapshotState(snapshotInProgress,snapshotContext){// StreamOperatorStateHandler.snapshotState()
		try {
			// CheckpointedStreamOperator �ӿڵ�ʵ����
			streamOperator.snapshotState(snapshotContext);{//CheckpointedStreamOperator �ӿڵ�ʵ������� 
				
			}
			
			snapshotInProgress.setKeyedStateRawFuture(snapshotContext.getKeyedStateStreamFuture());
			snapshotInProgress.setOperatorStateManagedFuture();
			if (null != operatorStateBackend) {
				// �־û�
				RunnableFuture<SnapshotResult> operatorStateManagedFuture =operatorStateBackend.snapshot(checkpointId, timestamp, factory, checkpointOptions);{
					BatchExecutionKeyedStateBackend
					AbstractKeyedStateBackend
					DefaultOperatorStateBackendSnapshotStrategy
					
					// Ĭ�ϲ���, hdfs, rackdb ?
					DefaultOperatorStateBackend.snapshotState(ckpId,time,streamFactory,ckpOptions){
						RunnableFuture<SnapshotResult> snapshotRunner =snapshotStrategy.snapshot(checkpointId, timestamp,);{//DefaultOperatorStateBackendSnapshotStrategy.
							// ��Ϊ��,����Ҫ�־û�;
							if (registeredOperatorStates.isEmpty() && registeredBroadcastStates.isEmpty()) {
								return DoneFuture.of(SnapshotResult.empty());
							}
							
							Thread.currentThread().setContextClassLoader(userClassLoader);
							if (!registeredOperatorStates.isEmpty()) {
								for (Map.Entry<String, PartitionableListState<?>> entry :registeredOperatorStates.entrySet()) {
									PartitionableListState<?> listState = entry.getValue();
									if (null != listState) {
										listState = listState.deepCopy();// ��copy һ������? �����޸ĺ��ڴ��Ӱ��? 
									}
									registeredOperatorStatesDeepCopies.put(entry.getKey(), listState);
								}
							}
							
							AsyncSnapshotCallable<SnapshotResult> snapshotCallable = new AsyncSnapshotCallable<SnapshotResult<OperatorStateHandle>>() {
								SnapshotResult<OperatorStateHandle> callInternal() {
									
								}
								
								void logAsyncSnapshotComplete(long startTime) {
									
								}
							}
							
							FutureTask<SnapshotResult> task =snapshotCallable.toAsyncSnapshotFutureTask(closeStreamOnCancelRegistry);{
								return new AsyncSnapshotTask(taskRegistry);
							}
							
							if (!asynchronousSnapshots) {// Ĭ��asynchronousSnapshots=true,�첽,���������� task.run()
								task.run();{//AsyncSnapshotTask.run()
									
								}
							}
							return task;
						}
						snapshotStrategy.logSyncCompleted(streamFactory, syncStartTime);{//AbstractSnapshotStrategy.logSyncCompleted
							logCompletedInternal(LOG_SYNC_COMPLETED_TEMPLATE, checkpointOutDescription, startTime);{
								long duration = (System.currentTimeMillis() - startTime);
								LOG.debug(template, description, checkpointOutDescription, Thread.currentThread(), duration);
							}
						}
						return snapshotRunner;
					}
					
					// ���� keyedState 
					HeapKeyedStateBackend.snapshotState(ckpId,time,streamFactory,ckpOptions){
						
					}
					

					
				}
				snapshotInProgress.setKeyedStateManagedFuture(operatorStateManagedFuture);
			}
			
			
		}catch (Exception snapshotException) {
			snapshotInProgress.cancel();
			snapshotContext.closeExceptionally();
			throw new CheckpointException(snapshotFailMessage);
		}
	}


	StreamOperator.snapshotState(){
		
		// ����ִ��ĳ�� ckp
		StreamOperator.snapshotState(checkpointId,timestamp,checkpointOptions,factory);{
			//CDC2Hudi�� StreamMap, StreamFilter, StreamSource, ProcessOperator ���ǵ��� ���෽��
			AbstractStreamOperator.snapshotState(checkpointId,timestamp,checkpointOptions,factory){
				return stateHandler.snapshotState(this,getOperatorName(), checkpointId,timestamp);{// StreamOperatorStateHandler.snapshotState()
					KeyGroupRange keyGroupRange = null != keyedStateBackend? keyedStateBackend.getKeyGroupRange() : KeyGroupRange.EMPTY_KEY_GROUP_RANGE;
					
					StateSnapshotContextSynchronousImpl snapshotContext = new StateSnapshotContextSynchronousImpl();
					snapshotState();{// StreamOperatorStateHandler.snapshotState()
						try {
							streamOperator.snapshotState(snapshotContext);// ��������� AbstractUdfStreamOperator.snapshotState(context) ����;
						}catch (Exception snapshotException) {
							snapshotInProgress.cancel();
							snapshotContext.closeExceptionally();
							throw new CheckpointException(snapshotFailMessage);
						}
					}
					return snapshotInProgress;
									
				}
			}
		
		}
		
		// ����ִ�� context�����ĵ� ckp; 
		StreamOperator.snapshotState(StateSnapshotContext context){
			
			// checkpoint 
			CheckpointedStreamOperator.snapshotState(StateSnapshotContext context){
				
				// �Զ���� udf�� ʵ����
				AbstractUdfStreamOperator.snapshotState(context){// extends AbstractStreamOperator [implements StreamOperator,CheckpointedStreamOperator]
					super.snapshotState(context);
					StreamingFunctionUtils.snapshotFunctionState(context, getOperatorStateBackend(), userFunction);{
						while (true) {
							boolean snapshotOk = trySnapshotFunctionState(context, backend, userFunction);{//StreamingFunctionUtils.trySnapshotFunctionState()
								if (userFunction instanceof CheckpointedFunction) {
									((CheckpointedFunction) userFunction).snapshotState(context);{
										// ��ͬ��ʵ����? 
										
										//Hudi �� Clean����: Sink function that cleans the old commits.
										hudi.sink.CleanFunction.snapshotState(){
											if (conf.getBoolean(FlinkOptions.CLEAN_ASYNC_ENABLED) && !isCleaning) {
												this.writeClient.startAsyncCleaning();{//HoodieFlinkWriteClient.startAsyncCleaning
													this.asyncCleanerService = AsyncCleanerService.startAsyncCleaningIfEnabled(this);{
														if (writeClient.getConfig().isAutoClean() && writeClient.getConfig().isAsyncClean()) {
															asyncCleanerService = new AsyncCleanerService(writeClient, instantTime);
															asyncCleanerService.start(null);{//AsyncCleanerService.start
																Pair<CompletableFuture, ExecutorService> res = startService();{//AsyncCleanerService.startService()
																	return Pair.of(CompletableFuture.supplyAsync(() -> {// �첽ִ�� writeClient.clean() ������
																		writeClient.clean(cleanInstantTime);{//AbstractHoodieWriteClient.clean
																			return clean(cleanInstantTime, true);{//AbstractHoodieWriteClient.clean()
																				LOG.info("Cleaner started");
																				HoodieCleanMetadata metadata = createTable(config, hadoopConf).clean(context, cleanInstantTime);
																				if (timerContext != null && metadata != null) {
																					long durationMs = metrics.getDurationInMs(timerContext.stop());
																					metrics.updateCleanMetrics(durationMs, metadata.getTotalFilesDeleted());
																				}
																				return metadata;
																			}
																		}
																		return true;
																	}), executor);
																}
																future = res.getKey();// �������� future,���� furtion.get()
																executor = res.getValue();
																// ���������߳� futrue.get()�� shutdown
																monitorThreads(onShutdownCallback);{ThreadExecutor.submit(() -> {
																	try {
																		future.get();
																	}finally { // ������ shutdown �ر�ʲô����? 
																		shutdown = true;
																		shutdown(false);
																	}
																});}
															}
														}
														return asyncCleanerService;
													}
												}
												this.isCleaning = true;
											}
										}
										
									}
									return true;
								}
								if (userFunction instanceof ListCheckpointed) {
									ListState<Serializable> listState = backend.getListState(listStateDescriptor);
									listState.clear();
								}
								return true;
							}
							if (snapshotOk) {
								break;
							}
						}
					}
				}
			}
			
		}
		
	}

	// ckp4.3  �Զ���� udf�� ���Operator�� snapshot����
	AbstractUdfStreamOperator.snapshotState(context){// extends AbstractStreamOperator [implements StreamOperator,CheckpointedStreamOperator]
		super.snapshotState(context);
		StreamingFunctionUtils.snapshotFunctionState(context, getOperatorStateBackend(), userFunction);{
			while (true) {
				boolean snapshotOk = trySnapshotFunctionState(context, backend, userFunction);{//StreamingFunctionUtils.trySnapshotFunctionState()
					if (userFunction instanceof CheckpointedFunction) {
						// ִ�� udf�� snapshotState() 
						((CheckpointedFunction) userFunction).snapshotState(context);{
							// ��ͬ��ʵ����? 
							
							//Hudi �� Clean����: Sink function that cleans the old commits.
							CompactionCommitSink.snapshotState() ���ø����� CleanFunction.snapshotState(),��������;
							hudi.sink.CleanFunction.snapshotState(){
								if (conf.getBoolean(FlinkOptions.CLEAN_ASYNC_ENABLED) && !isCleaning) {
									this.writeClient.startAsyncCleaning();{//HoodieFlinkWriteClient.startAsyncCleaning
										this.asyncCleanerService = AsyncCleanerService.startAsyncCleaningIfEnabled(this);{
											if (writeClient.getConfig().isAutoClean() && writeClient.getConfig().isAsyncClean()) {
												asyncCleanerService = new AsyncCleanerService(writeClient, instantTime);
												asyncCleanerService.start(null);{//AsyncCleanerService.start
													Pair<CompletableFuture, ExecutorService> res = startService();{//AsyncCleanerService.startService()
														return Pair.of(CompletableFuture.supplyAsync(() -> {// �첽ִ�� writeClient.clean() ������
															writeClient.clean(cleanInstantTime);{//AbstractHoodieWriteClient.clean
																return clean(cleanInstantTime, true);{//AbstractHoodieWriteClient.clean()
																	LOG.info("Cleaner started");
																	HoodieCleanMetadata metadata = createTable(config, hadoopConf).clean(context, cleanInstantTime);
																	if (timerContext != null && metadata != null) {
																		long durationMs = metrics.getDurationInMs(timerContext.stop());
																		metrics.updateCleanMetrics(durationMs, metadata.getTotalFilesDeleted());
																	}
																	return metadata;
																}
															}
															return true;
														}), executor);
													}
													future = res.getKey();// �������� future,���� furtion.get()
													executor = res.getValue();
													// ���������߳� futrue.get()�� shutdown
													monitorThreads(onShutdownCallback);{ThreadExecutor.submit(() -> {
														try {
															future.get();
														}finally { // ������ shutdown �ر�ʲô����? 
															shutdown = true;
															shutdown(false);
														}
													});}
												}
											}
											return asyncCleanerService;
										}
									}
									this.isCleaning = true;
								}
							}
							
							StreamWriterFunction.snapshotState(FunctionSnapshotContext functionSnapshotContext){
								
								flushRemaining(false);{
									this.currentInstant = instantToWrite(hasData());
									if (buckets.size() > 0) {
										this.buckets.values()
											.forEach(bucket -> {
												List<HoodieRecord> records = bucket.writeBuffer();
												bucket.preWrite(records);
												writeStatus.addAll(writeFunction.apply(records, currentInstant));
												records.clear();
												bucket.reset();
											});
									}
									
									final WriteMetadataEvent event = WriteMetadataEvent.builder()
												.writeStatus(writeStatus)
												.lastBatch(true)
												.endInput(endInput)
												.build();
									this.eventGateway.sendEventToCoordinator(event);
									this.writeClient.cleanHandles();
									this.writeStatuses.addAll(writeStatus);
									
								}
								
								reloadWriteMetaState();{// StreamWriterFunction.reloadWriteMetaState()
									this.writeMetadataState.clear();
									WriteMetadataEvent event = WriteMetadataEvent.builder()
										.writeStatus(new ArrayList<>(writeStatuses))
										.bootstrap(true)
										.build();
									this.writeMetadataState.add(event);
									writeStatuses.clear();
								}
							
							}
							
							BucketAssignFunction.snapshotState(FunctionSnapshotContext context){
								this.bucketAssigner.reset();{
									bucketInfoMap.clear();
									newFileAssignStates.clear();
								}
							}
							
						}
						return true;
					}
					if (userFunction instanceof ListCheckpointed) {
						ListState<Serializable> listState = backend.getListState(listStateDescriptor);
						listState.clear();
						return true;
					}
					
					// �� Checkpointed �Զ��庯��, û�� snapshotState����, ����false
					return false;
				}
				if (snapshotOk) {
					break;
				}
			}
		}
	}
	

	// ckp4.5 �첽��ɸ�ckp�ĳ־û�,������������; 
	// SubtaskCheckpointCoordinatorImpl.checkpointState() -> finishAndReportAsync() - asyncOperationsThreadPool.execute() �����߳� �첽 checkpoint�� report�� TM/JM; 
	AsyncCheckpointRunnable.run(){
		final long asyncStartDelayMillis = (asyncStartNanos - asyncConstructionNanos) / 1_000_000L;
		for (Map.Entry<OperatorID, OperatorSnapshotFutures> entry :operatorSnapshotsInProgress.entrySet()) {
			OperatorSnapshotFinalizer finalizedSnapshots =new OperatorSnapshotFinalizer(snapshotInProgress);{
				SnapshotResult<OperatorStateHandle> operatorManaged =FutureUtils.runIfNotDoneAndGet(snapshotFutures.getOperatorStateManagedFuture());{//FutureUtils.runIfNotDoneAndGet()
					FutureTask.run()-> call(); {//AsyncSnapshotCallable.call()
						final long startTime = System.currentTimeMillis();
						// ��DefaultOperatorStateBackendSnapshotStrategy$1 �������ڲ���ʵ��
						// SubtaskCheckpointCoordinatorImpl.checkpointState()->StreamOperatorStateHandler.snapshotState()->DefaultOperatorStateBackend.snapshotState(): new AsyncSnapshotCallable(){}
						T result = callInternal();{
							DefaultOperatorStateBackendSnapshotStrategy$1.callInternal(){
								CheckpointStateOutputStream localOut =streamFactory.createCheckpointStateOutputStream();
								snapshotCloseableRegistry.registerCloseable(localOut);
								List<StateMetaInfoSnapshot> broadcastMetaInfoSnapshots =new ArrayList<>(registeredBroadcastStatesDeepCopies.size());
								
								// ... write them all in the checkpoint stream ...
								DataOutputView dov = new DataOutputViewStreamWrapper(localOut);
								OperatorBackendSerializationProxy backendSerializationProxy =new OperatorBackendSerializationProxy(operatorMetaInfoSnapshots, broadcastMetaInfoSnapshots);
								backendSerializationProxy.write(dov);{//OperatorBackendSerializationProxy.write(dov)
									super.write(out);{//VersionedIOReadableWritable.write
										out.writeInt(getVersion());{//DataOutputStream.writeInt()
											out.write((v >>> 24) & 0xFF);{//java.io.OutputStream
												// ���� hdfs checkpoint��ʽ: �ӵ� writeBuffer:byte[] �ֽ�����,����ˢ��; 
												FsCheckpointStreamFactory.FsCheckpointStateOutputStream.write(int b){
													if (pos >= writeBuffer.length) {
														flushToFile();
													}
													writeBuffer[pos++] = (byte) b;
												}
											}
											out.write((v >>> 16) & 0xFF);
											out.write((v >>>  8) & 0xFF);
											out.write((v >>>  0) & 0xFF);
											incCount(4);
										}
									}
									
									writeStateMetaInfoSnapshots(operatorStateMetaInfoSnapshots, out);{
										out.writeShort(snapshots.size());{//DataOutputViewStreamWrapper ���෽�� DataOutputStream.writeShort()
											out.write((v >>> 8) & 0xFF);
											out.write((v >>> 0) & 0xFF);
											incCount(2);
										}
										
										for (StateMetaInfoSnapshot state : snapshots) {
											StateMetaInfoSnapshotReadersWriters
												.getWriter(){return CurrentWriterImpl.INSTANCE;}
												.writeStateMetaInfoSnapshot(state, out);{// StateMetaInfoSnapshotReadersWriters.CurrentWriterImpl.writeStateMetaInfoSnapshot()
													final Map<String, String> optionsMap = snapshot.getOptionsImmutable();
													outputView.writeUTF(snapshot.getName());
													outputView.writeInt(snapshot.getBackendStateType().ordinal());
													outputView.writeInt(optionsMap.size());
													
													outputView.writeInt(serializerConfigSnapshotsMap.size());
													for (Map.Entry<String, TypeSerializerSnapshot<?>> entry : serializerConfigSnapshotsMap.entrySet()) {
														final String key = entry.getKey();
														outputView.writeUTF(entry.getKey());{// DataOutputViewStreamWrapper ���෽�� DataOutputStream.writeUTF()
															writeUTF(str, this);{//DataOutputStream.writeUTF(str,out)
																int strlen = str.length();
																int utflen = 0;
																for (int i = 0; i < strlen; i++) {
																	c = str.charAt(i);
																	utflen++;
																}
																
																out.write(bytearr, 0, utflen+2);{// 
																	FsCheckpointStreamFactory.FsCheckpointStateOutputStream.write(byte[] b, int off, int len){
																		if (len < writeBuffer.length) {
																			final int remaining = writeBuffer.length - pos;
																			
																			System.arraycopy(b, off, writeBuffer, pos, len);
																			pos += len;
																		}else{
																			flushToFile();
																			outStream.write(b, off, len);
																		}
																	}
																}
															}
														}
														TypeSerializerSnapshotSerializationUtil.writeSerializerSnapshot(outputView,entry.getValue(),);
													}
													
												}
										}
									}
									
									writeStateMetaInfoSnapshots(broadcastStateMetaInfoSnapshots, out);
								}
							}
						}
						// ��DefaultOperatorStateBackendSnapshotStrategy$1 �������ڲ���ʵ��
						logAsyncSnapshotComplete(startTime);
						return result;
					}
				}
				SnapshotResult<OperatorStateHandle> operatorRaw =FutureUtils.runIfNotDoneAndGet(snapshotFutures.getOperatorStateRawFuture());
				SnapshotResult<StateObjectCollection<InputChannelStateHandle>> inputChannel =snapshotFutures.getInputChannelStateFuture().get();
				
				SnapshotResult<StateObjectCollection<ResultSubpartitionStateHandle>> resultSubpartition =snapshotFutures.getResultSubpartitionStateFuture().get();
				jobManagerOwnedState =OperatorSubtaskState.builder()
						.setManagedOperatorState(singletonOrEmpty(operatorManaged.getJobManagerOwnedSnapshot()))
						.setRawOperatorState(singletonOrEmpty(operatorRaw.getJobManagerOwnedSnapshot()))
						.setManagedKeyedState(singletonOrEmpty(keyedManaged.getJobManagerOwnedSnapshot()))
						.setRawKeyedState(singletonOrEmpty(keyedRaw.getJobManagerOwnedSnapshot()))
						.setInputChannelState(emptyIfNull(inputChannel.getJobManagerOwnedSnapshot()))
						.setResultSubpartitionState(emptyIfNull(resultSubpartition.getJobManagerOwnedSnapshot()))
						.build();

			}
			
			bytesPersistedDuringAlignment +=finalizedSnapshots.getJobManagerOwnedState().getResultSubpartitionState().getStateSize();
			bytesPersistedDuringAlignment += finalizedSnapshots.getJobManagerOwnedState().getInputChannelState().getStateSize();
			jobManagerTaskOperatorSubtaskStates.putSubtaskStateByOperatorID(operatorID, finalizedSnapshots.getJobManagerOwnedState());
		}
		checkpointMetrics.setAsyncDurationMillis(asyncDurationMillis);
		if (asyncCheckpointState.compareAndSet(AsyncCheckpointState.RUNNING, AsyncCheckpointState.COMPLETED)) {
			reportCompletedSnapshotStates();
		}
	}


write:218, FsCheckpointStreamFactory$FsCheckpointStateOutputStream (org.apache.flink.runtime.state.filesystem)
write:42, ForwardingOutputStream (org.apache.flink.runtime.util)
write:88, DataOutputStream (java.io)
writeString:837, StringValue (org.apache.flink.types)
serialize:68, StringSerializer (org.apache.flink.api.common.typeutils.base)
serialize:31, StringSerializer (org.apache.flink.api.common.typeutils.base)
serialize:349, PojoSerializer (org.apache.flink.api.java.typeutils.runtime)
serialize:147, CompositeSerializer (org.apache.flink.api.common.typeutils)
writeState:136, CopyOnWriteStateMapSnapshot (org.apache.flink.runtime.state.heap)
writeStateInKeyGroup:105, AbstractStateTableSnapshot (org.apache.flink.runtime.state.heap)
writeStateInKeyGroup:38, CopyOnWriteStateTableSnapshot (org.apache.flink.runtime.state.heap)
callInternal:204, HeapSnapshotStrategy$1 (org.apache.flink.runtime.state.heap)
callInternal:167, HeapSnapshotStrategy$1 (org.apache.flink.runtime.state.heap)
call:78, AsyncSnapshotCallable (org.apache.flink.runtime.state)
run:266, FutureTask (java.util.concurrent)
runIfNotDoneAndGet:618, FutureUtils (org.apache.flink.runtime.concurrent)
<init>:54, OperatorSnapshotFinalizer (org.apache.flink.streaming.api.operators)
run:127, AsyncCheckpointRunnable (org.apache.flink.streaming.runtime.tasks)
runWorker:1149, ThreadPoolExecutor (java.util.concurrent)
run:624, ThreadPoolExecutor$Worker (java.util.concurrent)
run:748, Thread (java.lang)


write:218, FsCheckpointStreamFactory$FsCheckpointStateOutputStream (org.apache.flink.runtime.state.filesystem)
writeBoolean:139, DataOutputStream (java.io)
write:123, KeyedBackendSerializationProxy (org.apache.flink.runtime.state)
callInternal:181, HeapSnapshotStrategy$1 (org.apache.flink.runtime.state.heap)
callInternal:167, HeapSnapshotStrategy$1 (org.apache.flink.runtime.state.heap)
call:78, AsyncSnapshotCallable (org.apache.flink.runtime.state)
run:266, FutureTask (java.util.concurrent)
runIfNotDoneAndGet:618, FutureUtils (org.apache.flink.runtime.concurrent)
<init>:54, OperatorSnapshotFinalizer (org.apache.flink.streaming.api.operators)
run:127, AsyncCheckpointRunnable (org.apache.flink.streaming.runtime.tasks)
runWorker:1149, ThreadPoolExecutor (java.util.concurrent)
run:624, ThreadPoolExecutor$Worker (java.util.concurrent)
run:748, Thread (java.lang)


write:218, FsCheckpointStreamFactory$FsCheckpointStateOutputStream (org.apache.flink.runtime.state.filesystem)
writeInt:198, DataOutputStream (java.io)
write:41, VersionedIOReadableWritable (org.apache.flink.core.io)
write:120, KeyedBackendSerializationProxy (org.apache.flink.runtime.state)
callInternal:181, HeapSnapshotStrategy$1 (org.apache.flink.runtime.state.heap)
callInternal:167, HeapSnapshotStrategy$1 (org.apache.flink.runtime.state.heap)
call:78, AsyncSnapshotCallable (org.apache.flink.runtime.state)
run:266, FutureTask (java.util.concurrent)
runIfNotDoneAndGet:618, FutureUtils (org.apache.flink.runtime.concurrent)
<init>:54, OperatorSnapshotFinalizer (org.apache.flink.streaming.api.operators)
run:127, AsyncCheckpointRunnable (org.apache.flink.streaming.runtime.tasks)
runWorker:1149, ThreadPoolExecutor (java.util.concurrent)
run:624, ThreadPoolExecutor$Worker (java.util.concurrent)
run:748, Thread (java.lang)



// ckp5. Operator ���ӵ� ckp����;

// ��ͬ���ӵ� snapshotState ״̬���� ʵ�ַ��� 
ListCheckpointed.snapshotState();
CheckpointedFunction.snapshotState(FunctionSnapshotContext context){
	 //�û���ʵ��Operator�� ������սӿڷ���;
    ExampleIntegerSource.snapshotState();
	
	
	AbstractUdfStreamOperator.snapshotState();{
		
	}
	
	
}



// ckp6. Sink ���ӵ� ckp���� �� ���� JM





// ckp7. JobManager �յ� sink���ckp�ź�, ����ckp�־û��ļ�;


	// ckp7.1 JobMaster: �յ� sink���ckp�ź�, ����ckp�־û��ļ�;
	// ���δ���: �յ�TaskExecutor ? �� checkpointCoordinatorGateway.acknowledgeCheckpoint() Rpc���ú� SchedulerBase.acknowledgeCheckpoint() -> CheckpointCoordinator.receiveAcknowledgeMessage()
	// ��ӡ��־: CheckpointCoordinator [] Completed checkpoint 818 for job 345db01b21c61fb4e441287a7bb77daf (4126714 bytes in 674 ms).

	JobMaster.acknowledgeCheckpoint(jobID,executionAttemptID,checkpointId,checkpointState){// ʵ����CheckpointCoordinatorGateway�ӿ�
		schedulerNG.acknowledgeCheckpoint(jobID, executionAttemptID, checkpointId, checkpointMetrics, checkpointState);{//SchedulerBase.acknowledgeCheckpoint()
			mainThreadExecutor.assertRunningInMainThread();
			AcknowledgeCheckpoint ackMessage =new AcknowledgeCheckpoint();
			String taskManagerLocationInfo = retrieveTaskManagerLocation(executionAttemptID);
			if (checkpointCoordinator != null) {
				ioExecutor.execute(()->{
					checkpointCoordinator.receiveAcknowledgeMessage(ackMessage, taskManagerLocationInfo);{//CheckpointCoordinator.receiveAcknowledgeMessage()
						long checkpointId = message.getCheckpointId();
						final PendingCheckpoint checkpoint = pendingCheckpoints.get(checkpointId);
						
						if (checkpoint != null && !checkpoint.isDisposed()) {
							TaskAcknowledgeResult ackResult = checkpoint.acknowledgeTask();
							switch (ackResult){
								case SUCCESS:
									if (checkpoint.isFullyAcknowledged()) {
										completePendingCheckpoint(checkpoint);{
											Map<OperatorID, OperatorState> operatorStates = pendingCheckpoint.getOperatorStates();
											sharedStateRegistry.registerAll(operatorStates.values());
											
											completedCheckpoint = pendingCheckpoint.finalizeCheckpoint(checkpointsCleaner, this::scheduleTriggerRequest, executor);
											failureManager.handleCheckpointSuccess(pendingCheckpoint.getCheckpointId());
											
											try{
												completedCheckpointStore.addCheckpoint(completedCheckpoint, checkpointsCleaner, this::scheduleTriggerRequest);
											}finally {
												pendingCheckpoints.remove(checkpointId);
												scheduleTriggerRequest();
											}
											
											dropSubsumedCheckpoints(checkpointId);
											// ��־: Completed checkpoint 818 for job 345db01b21c61fb4e441287a7bb77daf (4126714 bytes in 674 ms).
											LOG.info("Completed checkpoint {} for job {} ({} bytes in {} ms).",checkpointId,job,);
											sendAcknowledgeMessages(checkpointId, completedCheckpoint.getTimestamp());
										}
									}
									break;
								case DUPLICATE: break;
								case UNKNOWN: 
									discardSubtaskState();
									break;
								case DISCARDED:
									discardSubtaskState();
									break;
							}
							return true;
						}
						
					}
				});
			}else{
				log.error(errorMessage, jobGraph.getJobID());
			}
		}
	}




	// ckp7.2 JobMaster: �� ����task��master ckp����ɺ�, ֪ͨhudi commit;
	// OperatorCoordinatorHolder.notifyCheckpointComplete() -> StreamWriteOperatorCoordinator.commitInstant() doCommit()
	// �� Task Executor �з����� RPC���� notifyCheckpointComplete()? 
	// LOG: StreamWriteOperatorCoordinator [] Commit instant [20220124103157] success!
	// LOG: AbstractHoodieWriteClient [] Committing 20220124103157 action deltacommit
	// LOG: StreamWriteOperatorCoordinator [] Commit instant [20220124103157] success!

	OperatorCoordinatorHolder.notifyCheckpointComplete(){
		mainThreadExecutor.execute(() -> coordinator.notifyCheckpointComplete(checkpointId));{//StreamWriteOperatorCoordinator.notifyCheckpointComplete()
			executor.execute(()->{
				final boolean committed = commitInstant(this.instant);{
					if (Arrays.stream(eventBuffer).allMatch(Objects::isNull)) {
						return false;
					}
					List<WriteStatus> writeResults = Arrays.stream(eventBuffer)
						.filter(Objects::nonNull).map(WriteMetadataEvent::getWriteStatuses)
						.flatMap(Collection::stream).collect(Collectors.toList());
						
					doCommit(instant, writeResults);{// StreamWriteOperatorCoordinator.doCommit()
						long totalErrorRecords = writeResults.stream().map(WriteStatus::getTotalErrorRecords).reduce(Long::sum).orElse(0L);
						long totalRecords = writeResults.stream().map(WriteStatus::getTotalRecords).reduce(Long::sum).orElse(0L);
						if (!hasErrors || this.conf.getBoolean(FlinkOptions.IGNORE_FAILED)) {
							boolean success = writeClient.commit(instant, writeResults);// Դ����� HoodieFlinkWriteClient.commit()
							if (success) {
								reset();
								// LOG: StreamWriteOperatorCoordinator [] Commit instant [20220124103157] success!
								LOG.info("Commit instant [{}] success!", instant);
							}else{
								throw new HoodieException(String.format("Commit instant [%s] failed!", instant));
							}
						}else {
							throw new HoodieException(String.format("Commit instant [%s] failed and rolled back !", instant));
						}
					}
					return true;
				}
				if (committed) {
					if (tableState.scheduleCompaction) {
						writeClient.scheduleCompaction(Option.empty());{//AbstractHoodieWriteClient.scheduleCompaction(extraMetadata)
							String instantTime = HoodieActiveTimeline.createNewInstantTime();
							return scheduleCompactionAtInstant(instantTime, extraMetadata) ? Option.of(instantTime) : Option.empty();{
								return scheduleTableService(instantTime, extraMetadata, TableServiceType.COMPACT).isPresent();{//AbstractHoodieWriteClient.
									try{
										this.txnManager.beginTransaction();
										LOG.info("Scheduling table service " + tableServiceType);
										return scheduleTableServiceInternal(instantTime, extraMetadata, tableServiceType);{//AbstractHoodieWriteClient.
											switch (tableServiceType) {
												case CLUSTER:
													Option<HoodieClusteringPlan> clusteringPlan = createTable(config, hadoopConf).scheduleClustering(context, instantTime, extraMetadata);
													return clusteringPlan.isPresent() ? Option.of(instantTime) : Option.empty();
												case COMPACT:
													LOG.info("Scheduling compaction at instant time :" + instantTime);
													// 1. ����, HoodieFlinkWriteClient.createTable() ʵ��;
													HoodieTable table = createTable(config, hadoopConf);{//HoodieFlinkWriteClient.createTable()
														return HoodieFlinkTable.create(config, (HoodieFlinkEngineContext) context);{
															HoodieTableMetaClient metaClient = HoodieTableMetaClient.builder().setConf(context.getHadoopConf().get()).setBasePath(config.getBasePath())
																	.setLoadActiveTimelineOnLoad(true).setConsistencyGuardConfig(config.getConsistencyGuardConfig())
																	.setLayoutVersion(Option.of(new TimelineLayoutVersion(config.getTimelineLayoutVersion())))
																	.build();{
																		new HoodieTableMetaClient(conf, basePath,,);{
																			LOG.info("Loading HoodieTableMetaClient from " + basePath);
																			this.metaPath = new Path(basePath, METAFOLDER_NAME).toString();
																			this.timelineLayoutVersion = layoutVersion.orElseGet(() -> tableConfig.getTimelineLayoutVersion().get());
																			// LOG: HoodieTableMetaClient [] Finished Loading Table of type MERGE_ON_READ(version=1, baseFileFormat=PARQUET) from hdfs://bdnode102:9000/hudi/lakehouse2_dwd_order_hudi
																			LOG.info("Finished Loading Table of type " + tableType + "(version=" + timelineLayoutVersion + ", baseFileFormat=" + ") from " + basePath);
																			if (loadActiveTimelineOnLoad) {
																				getActiveTimeline();
																			}
																		}
																	}
															return HoodieFlinkTable.create(config, context, metaClient);
														}
													}
													// 2. (���������߳�)ִ�� compact
													Option<HoodieCompactionPlan> compactionPlan = table.scheduleCompaction(context, instantTime, extraMetadata);{//HoodieFlinkMergeOnReadTable.scheduleCompaction
														scheduleCompactionExecutor = new FlinkScheduleCompactionActionExecutor();
														return scheduleCompactionExecutor.execute();{//BaseScheduleCompactionActionExecutor.execute
															HoodieCompactionPlan plan = scheduleCompaction();{//FlinkScheduleCompactionActionExecutor.scheduleCompaction
																boolean compactable = needCompact(config.getInlineCompactTriggerStrategy());
																if (compactable) {
																	// LOG: FlinkScheduleCompactionActionExecutor [] Generating compaction plan for merge on read table hdfs://bdnode102:9000/hudi/lakehouse2_dwd_order_hudi
																	LOG.info("Generating compaction plan for merge on read table " + config.getBasePath());
																	HoodieFlinkMergeOnReadTableCompactor compactor = new HoodieFlinkMergeOnReadTableCompactor();
																	SyncableFileSystemView fileSystemView = (SyncableFileSystemView) table.getSliceView();{//HoodieTable.getSliceView()
																		return getViewManager().getFileSystemView(metaClient);{// FileSystemViewManager.getFileSystemView()
																			return globalViewMap.computeIfAbsent(metaClient.getBasePath(),(path) -> viewCreator.apply(metaClient, viewStorageConfig));{
																				FileSystemViewManager.createViewManager(context,metadataConfig,config,){
																					final SerializableConfiguration conf = context.getHadoopConf();
																					switch (config.getStorageType()) {
																						case EMBEDDED_KV_STORE:
																							 return new FileSystemViewManager(context, config, (metaClient, viewConf) -> createRocksDBBasedFileSystemView(conf, viewConf, metaClient));
																						case SPILLABLE_DISK:
																						case MEMORY: 
																							LOG.info("Creating in-memory based Table View");
																							return new FileSystemViewManager(context, config,()-> createInMemoryFileSystemView());
																						case REMOTE_ONLY:
																						case REMOTE_FIRST:
																						default: throw new IllegalArgumentException();
																					}
																				}
																			}
																		}
																	}
																	
																	return compactor.generateCompactionPlan(context, table, config, instantTime, fgInPendingCompactionAndClustering);
																}
																return new HoodieCompactionPlan();
															}
															
															if (plan != null && (plan.getOperations() != null) && (!plan.getOperations().isEmpty())) {
																extraMetadata.ifPresent(plan::setExtraMetadata);
																HoodieInstant compactionInstant = new HoodieInstant(HoodieInstant.State.REQUESTED, HoodieTimeline.COMPACTION_ACTION, instantTime);
																table.getActiveTimeline().saveToCompactionRequested(compactionInstant,TimelineMetadataUtils.serializeCompactionPlan(plan));
															}
															return Option.empty();
														}
													}
													
													return compactionPlan.isPresent() ? Option.of(instantTime) : Option.empty();
												case CLEAN: 
													createTable(config, hadoopConf).scheduleCleaning(context, instantTime, extraMetadata);
													return cleanerPlan.isPresent() ? Option.of(instantTime) : Option.empty();
												default: throw new IllegalArgumentException("Invalid TableService " + tableServiceType);
											}
										}
									}finally {
										this.txnManager.endTransaction();
									}
								}
							}
						}
					}
					// start new instant.
					startInstant();
					// sync Hive if is enabled
					syncHiveIfEnabled();{// StreamWriteOperatorCoordinator.syncHiveIfEnabled()
						if (tableState.syncHive) {
							this.hiveSyncExecutor.execute(this::syncHive, "sync hive metadata for instant %s", this.instant);{
								// �����߳� �첽��� syncHive Hiveͬ��
								StreamWriteOperatorCoordinator.syncHive(); {
									HiveSyncTool syncTool = hiveSyncContext.hiveSyncTool();{
										new HiveSyncTool(this.syncConfig, this.hiveConf, this.fs);
									}
									syncTool.syncHoodieTable();{//HiveSyncTool.syncHoodieTable()
										if (hoodieHiveClient != null){
											doSync();{//HiveSyncTool.doSync
												switch (hoodieHiveClient.getTableType()) {
													case COPY_ON_WRITE:
														syncHoodieTable(snapshotTableName, false, false);
														break;
													case MERGE_ON_READ:
														// sync a RO table for MOR
														syncHoodieTable(roTableName.get(), false, true);
														// sync a RT table for MOR
														syncHoodieTable(snapshotTableName, true, false);
													break;
													default: throw new InvalidTableException(hoodieHiveClient.getBasePath());
												}
											}
										}
									}
								}
							}
						}
					}
					// sync metadata if is enabled
					syncMetadataIfEnabled();
				}
			});
		}
	}

	// ckp7.2.1 ���notifyCheckpointComplete �� JobMaster ��һ�������ύ: hudi�����ύ
	HoodieFlinkWriteClient.commit(){//HoodieFlinkWriteClient.commit()
		List<HoodieWriteStat> writeStats = writeStatuses.parallelStream().map(WriteStatus::getStat).collect(Collectors.toList());
		return commitStats(instantTime, writeStats, extraMetadata, commitActionType, partitionToReplacedFileIds);{//AbstractHoodieWriteClient.
			// LOG: AbstractHoodieWriteClient [] Committing 20220124103157 action deltacommit
			LOG.info("Committing " + instantTime + " action " + commitActionType);
			HoodieTable table = createTable(config, hadoopConf);{//HoodieFlinkWriteClient.createTable()
				return HoodieFlinkTable.create(config, (HoodieFlinkEngineContext) context);{
					HoodieTableMetaClient metaClient = HoodieTableMetaClient.builder().setConf(context.getHadoopConf().get()).setBasePath(config.getBasePath())
							.setLoadActiveTimelineOnLoad(true).setConsistencyGuardConfig(config.getConsistencyGuardConfig())
							.setLayoutVersion(Option.of(new TimelineLayoutVersion(config.getTimelineLayoutVersion())))
							.build();{
								new HoodieTableMetaClient(conf, basePath,,);{
									LOG.info("Loading HoodieTableMetaClient from " + basePath);
									this.metaPath = new Path(basePath, METAFOLDER_NAME).toString();
									this.timelineLayoutVersion = layoutVersion.orElseGet(() -> tableConfig.getTimelineLayoutVersion().get());
									// LOG: HoodieTableMetaClient [] Finished Loading Table of type MERGE_ON_READ(version=1, baseFileFormat=PARQUET) from hdfs://bdnode102:9000/hudi/lakehouse2_dwd_order_hudi
									LOG.info("Finished Loading Table of type " + tableType + "(version=" + timelineLayoutVersion + ", baseFileFormat=" + ") from " + basePath);
									if (loadActiveTimelineOnLoad) {
										getActiveTimeline();
									}
								}
							}
					return HoodieFlinkTable.create(config, context, metaClient);
				}
			}
			
			HoodieCommitMetadata metadata = CommitUtils.buildMetadata(stats, extraMetadata, operationType,commitActionType);
			HeartbeatUtils.abortIfHeartbeatExpired(instantTime, table, heartbeatClient, config);
			this.txnManager.beginTransaction();
			try {
				preCommit(instantTime, metadata);
				commit(table, commitActionType, instantTime, metadata, stats);{// AbstractHoodieWriteClient.commit
					HoodieActiveTimeline activeTimeline = table.getActiveTimeline();
					finalizeWrite(table, instantTime, stats);{
						Timer.Context finalizeCtx = metrics.getFinalizeCtx();
						table.finalizeWrite(context, instantTime, stats);{//HoodieTable.finalizeWrite
							reconcileAgainstMarkers();{//HoodieTable.reconcileAgainstMarkers()
								String basePath = getMetaClient().getBasePath();
								Set<String> invalidDataPaths = getInvalidDataPaths(markers);
								invalidDataPaths.removeAll(validDataPaths);
								deleteInvalidFilesByPartitions(context, invalidPathsByPartition);
							}
						}
					}
					activeTimeline.saveAsComplete(new HoodieInstant());
				}
				postCommit(table, metadata, instantTime, extraMetadata);
				// LOG: AbstractHoodieWriteClient [] Committed 20220124103157
				LOG.info("Committed " + instantTime);
				releaseResources();
			} finally {
				this.txnManager.endTransaction();
			}
			
			runTableServicesInline(table, metadata, extraMetadata);
			emitCommitMetrics(instantTime, metadata, commitActionType);
			return true;
		}
	}
	
	// ckp7.2.2 �첽ͬ�� hive ��;
	// OperatorCoordinatorHolder.notifyCheckpointComplete() -> syncHiveIfEnabled() �����߳�ִ��: StreamWriteOperatorCoordinator.syncHive() -> HiveSyncTool.syncHoodieTable()
	HiveSyncTool.syncHoodieTable(tableName,useRealtimeInputFormat,readAsOptimized){
		
		//LOG: HiveSyncTool [] Trying to sync hoodie table unknown_ro with base path hdfs://bdnode102:9000/hudi/lakehouse2_dwd_order_hudi of type MERGE_ON_READ
		LOG.info("Trying to sync hoodie table " + tableName + " with base path " + hoodieHiveClient.getBasePath()+ " of type " + hoodieHiveClient.getTableType());
		
	}














