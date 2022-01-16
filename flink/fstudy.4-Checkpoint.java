
Checkpoint�ĺ�������CheckpointCoordinator: ����Э����
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



// �⻹���� JobMaster������; 

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


// 2.1 TaskExecutor ���յ� ckp�����ź�, ִ������ ���ӵ�ckp 





// ��Task Executor ��������
// ����һ������,��miniCluster�е�TM�߳���:-> TaskExecutor.triggerCheckpoint()
TaskExecutor.triggerCheckpoint(){
    task.triggerCheckpointBarrier(checkpointId, checkpointTimestamp, checkpointOptions, advanceToEndOfEventTime);{//Task.
        invokable.triggerCheckpointAsync(checkpointMetaData, checkpointOptions, advanceToEndOfEventTime);{
            SourceStreamTask.triggerCheckpointAsync(){
                return super.triggerCheckpointAsync(checkpointMetaData, checkpointOptions, advanceToEndOfEventTime);{
                    return mailboxProcessor.getMainMailboxExecutor().submit(() -> triggerCheckpoint(checkpointMetaData, checkpointOptions,checkpointMetaData,checkpointOptions);{
                        MailboxExecutor.submit() -> StreamTask.triggerCheckpoint(){
                            boolean success = performCheckpoint(checkpointMetaData, checkpointOptions, checkpointMetrics, advanceToEndOfEventTime);{//StreamTask.
                                final long checkpointId = checkpointMetaData.getCheckpointId();
                                actionExecutor.runThrowing(() -> {
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


	StreamTask.triggerCheckpoint() -> performCheckpoint() 
		operatorChain.prepareSnapshotPreBarrier(checkpointId);
		operatorChain.broadcastCheckpointBarrier();
		checkpointState(checkpointMetaData);
			// ������ͬ��ִ��Checkpoint;
			* for (StreamOperator<?> op : allOperators) 
				op[AbstractStreamOperator].snapshotState();
			// �첽ִ��Checkpoint
			owner.asyncOperationsThreadPool.execute(new AsyncCheckpointRunnable());
	 

	// Task����� checkpoint, ���ľ���: ����operatorChain ִ��ÿ��Operator.snapshotState()
	SubtaskCheckpointCoordinatorImpl.checkpointState(metadata,operatorChain){
		if (lastCheckpointId >= metadata.getCheckpointId()) {
			channelStateWriter.abort(metadata.getCheckpointId(), new CancellationException(), true);
			return;
		}
		// Step (1): Prepare the checkpoint, allow operators to do some pre-barrier work.
		operatorChain.prepareSnapshotPreBarrier(metadata.getCheckpointId());
		// Step (2): Send the checkpoint barrier downstream
		operatorChain.broadcastEvent(new CheckpointBarrier());
		// Step (3): Prepare to spill the in-flight buffers for input and output
		if (options.isUnalignedCheckpoint()) {
			channelStateWriter.finishOutput(metadata.getCheckpointId());
		}
		// Step (4): Take the state snapshot. This should be largely asynchronous, to not impact
		boolean snapshotSyncOk = takeSnapshotSync(snapshotFutures, metadata, metrics, options, operatorChain, isRunning);{//SubtaskCheckpointCoordinatorImpl.takeSnapshotSync
			CheckpointStreamFactory storage =checkpointStorage.resolveCheckpointStorageLocation(checkpointId, checkpointOptions.getTargetLocation());
			for (StreamOperatorWrapper<?, ?> operatorWrapper : operatorChain.getAllOperators(true)) {
				if (!operatorWrapper.isClosed()) {
					OperatorSnapshotFutures snapshotFuture = buildOperatorSnapshotFutures();{// SubtaskCheckpointCoordinatorImpl.buildOperatorSnapshotFutures()
						OperatorSnapshotFutures snapshotInProgress =checkpointStreamOperator();{
							return op.snapshotState(checkpointId,timestamp,checkpointOptions,factory);{// StreamOperator.snapshotState() ���� AbstractStreamOperator.snapshotState
								return stateHandler.snapshotState();{//StreamOperatorStateHandler.snapshotState()
									KeyGroupRange keyGroupRange = null != keyedStateBackend? keyedStateBackend.getKeyGroupRange(): KeyGroupRange.EMPTY_KEY_GROUP_RANGE;
									StateSnapshotContextSynchronousImpl snapshotContext = new StateSnapshotContextSynchronousImpl();
									snapshotState();{// StreamOperatorStateHandler.snapshotState()
										try {
											streamOperator.snapshotState(snapshotContext);
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
						return snapshotInProgress;
					}
					operatorSnapshotsInProgress.put(operatorID,snapshotFuture);
				}
			}
		}
		if (snapshotSyncOk) {
			finishAndReportAsync(snapshotFutures, metadata, metrics, isRunning);
		}else{
			cleanup(snapshotFutures, metadata, metrics, new Exception("Checkpoint declined"));
		}
	}



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


// 2.2 ��ͬ���ӵ� snapshotState ״̬���� ʵ�ַ��� 
ListCheckpointed.snapshotState();
CheckpointedFunction.snapshotState(FunctionSnapshotContext context){
	 //�û���ʵ��Operator�� ������սӿڷ���;
    ExampleIntegerSource.snapshotState();
	
	
	AbstractUdfStreamOperator.snapshotState();{
		
	}
	
	
}



	
Caused by: org.apache.flink.util.SerializedThrowable:
 Task java.util.concurrent.FutureTask@7042bcc9 rejected from java.util.concurrent.ThreadPoolExecutor@6acee30f
 [Terminated, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 0]
	at java.util.concurrent.ThreadPoolExecutor$AbortPolicy.rejectedExecution(ThreadPoolExecutor.java:2063) ~[?:1.8.0_261]
	at java.util.concurrent.ThreadPoolExecutor.reject(ThreadPoolExecutor.java:830) ~[?:1.8.0_261]
	at java.util.concurrent.ThreadPoolExecutor.execute(ThreadPoolExecutor.java:1379) ~[?:1.8.0_261]
	at java.util.concurrent.AbstractExecutorService.submit(AbstractExecutorService.java:112) ~[?:1.8.0_261]
	at java.util.concurrent.Executors$DelegatedExecutorService.submit(Executors.java:678) ~[?:1.8.0_261]
	at org.apache.hudi.async.HoodieAsyncService.monitorThreads(HoodieAsyncService.java:154) ~[hudi-flink-bundle_2.11-0.9.0.jar:0.9.0]
	at org.apache.hudi.async.HoodieAsyncService.start(HoodieAsyncService.java:133) ~[hudi-flink-bundle_2.11-0.9.0.jar:0.9.0]
	at org.apache.hudi.client.AsyncCleanerService.startAsyncCleaningIfEnabled(AsyncCleanerService.java:62) ~[hudi-flink-bundle_2.11-0.9.0.jar:0.9.0]
	at org.apache.hudi.client.HoodieFlinkWriteClient.startAsyncCleaning(HoodieFlinkWriteClient.java:272) ~[hudi-flink-bundle_2.11-0.9.0.jar:0.9.0]
	at org.apache.hudi.sink.CleanFunction.snapshotState(CleanFunction.java:84) ~[hudi-flink-bundle_2.11-0.9.0.jar:0.9.0]
	at org.apache.flink.streaming.util.functions.StreamingFunctionUtils.trySnapshotFunctionState(StreamingFunctionUtils.java:118) ~[flink-dist_2.11-1.12.2.jar:1.12.2]
	at org.apache.flink.streaming.util.functions.StreamingFunctionUtils.snapshotFunctionState(StreamingFunctionUtils.java:99) ~[flink-dist_2.11-1.12.2.jar:1.12.2]
	at org.apache.flink.streaming.api.operators.AbstractUdfStreamOperator.snapshotState(AbstractUdfStreamOperator.java:89) ~[flink-dist_2.11-1.12.2.jar:1.12.2]
	at org.apache.flink.streaming.api.operators.StreamOperatorStateHandler.snapshotState(StreamOperatorStateHandler.java:205) ~[flink-dist_2.11-1.12.2.jar:1.12.2]
	... 23 more
2022-01-13 17:02:51,753 INFO  org.apache.hudi.common.table.HoodieTableMetaClient           [] - Finished Loading Table of type MERGE_ON_READ(version=1, baseFileFormat=PARQUET) from hdfs://bdnode102:9000/hudi/lakehouse2_dwd_order_hudi
















