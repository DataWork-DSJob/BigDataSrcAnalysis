
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



StreamTask.triggerCheckpoint() -> performCheckpoint() 
    operatorChain.prepareSnapshotPreBarrier(checkpointId);
    operatorChain.broadcastCheckpointBarrier();
    checkpointState(checkpointMetaData);
        // ������ͬ��ִ��Checkpoint;
        * for (StreamOperator<?> op : allOperators) 
            op[AbstractStreamOperator].snapshotState();
        // �첽ִ��Checkpoint
        owner.asyncOperationsThreadPool.execute(new AsyncCheckpointRunnable());
 

















