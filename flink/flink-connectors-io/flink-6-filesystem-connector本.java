
// �̺߳�ģ�鹦��
TM: ϵͳ��ʱ����?: "Source: Custom Source -> Map -> Sink":  Ƶ�ʺܸ� ��ÿ��element���Ƕ�ʱ����?

StreamingFileSink.initializeState(){
	Buckets<IN, ?> buckets = bucketsBuilder.createBuckets(getRuntimeContext().getIndexOfThisSubtask());
	ProcessingTimeService procTimeService = ((StreamingRuntimeContext) getRuntimeContext()).getProcessingTimeService();
	this.helper =new StreamingFileSinkHelper<>(buckets,context.isRestored(),
				context.getOperatorStateStore(), procTimeService, bucketCheckInterval);{
		this.procTimeService = procTimeService;
        if (isRestored) {
            buckets.initializeState(bucketStates, maxPartCountersState);
        }
        long currentProcessingTime = procTimeService.getCurrentProcessingTime();
		// ����ͽ� this: StreamingFileSinkHelper ��Ϊtarget���� �趨Ϊ Time��ʱ����; ִ��; 
        procTimeService.registerTimer(currentProcessingTime + bucketCheckInterval, this);{
			ProcessingTimeCallback targetCallback = addQuiesceProcessingToCallback(processingTimeCallbackWrapper.apply(target));
			return timerService.registerTimer(timestamp,targetCallback);{// SystemProcessingTimeService.registerTimer
				long delay =ProcessingTimeServiceUtil.getProcessingTimeDelay( timestamp, getCurrentProcessingTime());
				// delay = 1ms, 
				ScheduledTask task = wrapOnTimerCallback(callback, timestamp);
				return timerService.schedule(task, delay, TimeUnit.MILLISECONDS);{
					SystemProcessingTimeService.ScheduledTask.run(){
						callback.onProcessingTime(nextTimestamp);{
							ProcessingTimeServiceImpl.addQuiesceProcessingToCallback(ProcessingTimeCallback callback);{
								if (!isQuiesced()) {
									// callback: ProcessingTimeCallback = StreamTask.deferCallbackToMailbox() �ж����
									// callback= timestamp -> { mailboxExecutor.execute(() -> invokeProcessingTimeCallback(callback, timestamp),"Timer callback for %s @ %d",callback,timestamp);};
									callback.onProcessingTime(timestamp);{
										// callback: StreamingFileSinkHelper, 
										ThrowingRunnable<? extends Exception> command = () -> invokeProcessingTimeCallback(callback, timestamp);{
											callback.onProcessingTime(timestamp);
										}
										// ����execute���ǰ� command:ThrowingRunnable ��ӵ�TaskMailboxImpl.queue: Deque<Mail>���������
										MailboxExecutorImpl.execute(command,"Timer callback for %s @ %d",callback,timestamp);{
											mailbox.put(new Mail(command, priority, actionExecutor, descriptionFormat, descriptionArgs));{
												queue.addLast(mail);// queue: Deque<Mail> �����������; 
											}
										}
										
									}
								}
							}
						}
						nextTimestamp += period;
					}
				}
			}
		}
	}
}



TM: ϵͳ��ʱ����?: "Source: Custom Source -> Map -> Sink":  ��Ƶ����: ���ﵽ����ʱ����(60s)���inProgressPart��Ϊnull,���������ļ�;

StreamTask.invokeProcessingTimeCallback(ProcessingTimeCallback callback, timestamp){
	// ���callback���� StreamingFileSinkHelper
	callback.onProcessingTime(timestamp);{// StreamingFileSinkHelper.onProcessingTime
		buckets.onProcessingTime(currentTime);{//Buckets.
			// activeBuckets: Map<BucketID, Bucket<IN, BucketID>> , key=2022-04-07--09, ����һ������Ŀ¼һ��Bucket; 
			for (Bucket<IN, BucketID> bucket : activeBuckets.values()) {
				bucket.onProcessingTime(timestamp);{//Bucket.onProcessingTime() �Ե���Ŀ¼���� onProcessTime()
					boolean shouldRoll = rollingPolicy.shouldRollOnProcessingTime(inProgressPart, timestamp);{
						boolean canRollByTime = currentTime - partFileState.getCreationTime() >= rolloverInterval;	//Ĭ��60s 1���ӹ���;
						boolean canRollByInactivityInterval = currentTime - partFileState.getLastUpdateTime() >= inactivityInterval;	//Ĭ��60s,1���ӹ���;
						return canRollByTime || canRollByInactivityInterval;
					}
					if (inProgressPart != null && shouldRoll ) {
						closePartFile();{//Bucket.closePartFile() �� Bucket.inProgressPart:InProgressFileWriter��Ϊnull �Թ��������µ�;
							if (inProgressPart != null) {
								pendingFileRecoverable = inProgressPart.closeForCommit();
								pendingFileRecoverablesForCurrentCheckpoint.add(pendingFileRecoverable);
								inProgressPart = null;
							}
						}
					}
				}
			}
		}
		procTimeService.registerTimer(currentTime + bucketCheckInterval, this);
	}
}





TM: ���ݴ����߳�: "Legacy Source Thread: Custom Source -> Map -> Sink" : FileSink���Ӵ���ÿһ��Ҫд���� element;
	1. ����ÿһ��element������bucketId������Bucket��Ͱ, ���bucket.inProgressPart(�����ļ�)��׷������; 
	2. ��һ ProcessingTime�̻߳ᰴʱ������������ inProgressPart=null,�� inProgressPart.size > partSize(128M)ʱ, ���ۼ�partCounter ��������part�ļ�;

StreamingFileSink.invoke(IN value, SinkFunction.Context context){
	this.helper.onElement(value,context.timestamp(),context.currentWatermark());{//StreamingFileSinkHelper.
		buckets.onElement(value, currentProcessingTime, elementTimestamp, currentWatermark);{//Buckets.onElement()
			// �ȸ���element���� bucketId: BucketID; ��activeBuckets:Map<BucketID, Bucket<IN, BucketID>> �в��Ҹ�Bucket; 
			final BucketID bucketId = bucketAssigner.getBucketId(value, bucketerContext);
			final Bucket<IN, BucketID> bucket = getOrCreateBucketForBucketId(bucketId);{
				Bucket<IN, BucketID> bucket = activeBuckets.get(bucketId);
				if (bucket == null) {// ����bucketͰ������,���½�
					bucket =bucketFactory.getNewBucket();
					activeBuckets.put(bucketId, bucket);
				}
				return bucket;
			}
			//����ӦbucketId�� BucketͰд������; 
			bucket.write(value, currentProcessingTime);{
				// 1. ��processTime�߳��и��̶�ʱ������,����closePartFile()�а� inProgressPart=null�ÿ�;
				// 2. ���ߵ� inProgressPart.size > rollingPolicy.partSize (Ĭ��128M)ʱ,�� roll����������part�ļ�; 
				boolean shouldRollFile = rollingPolicy.shouldRollOnEvent(inProgressPart, element);{
					// �Ƚϵ�ǰ���ļ���С(partFile.size) ���� rollingPolicy�����õ� �ļ���Сʱ, 
					return partFileState.getSize() > partSize;
				}
				if (inProgressPart == null || shouldRollFile) {
					inProgressPart = rollPartFile(currentTime);{// Bucket.rollPartFile()
						closePartFile();
						final Path partFilePath = assembleNewPartPath();{ // ���� Bucket.partCounter �����������ۼ� ���ļ����; 
							long currentPartCounter =  partCounter++;
							String newFilePath = outputFileConfig.getPartPrefix() + '-'+ subtaskIndex+ '-'+ currentPartCounter+ outputFileConfig.getPartSuffix();
							return new Path(bucketPath,newFilePath);
						}
						return bucketWriter.openNewInProgressFile(bucketId, partFilePath, currentTime);
					}
				}
				inProgressPart.write(element, currentTime);{//RowWisePartWriter.write(IN element, long currentTime)
					encoder.encode(element, currentPartStream);{//SimpleStringEncoder.encode()
						stream.write(element.toString().getBytes(charset));{//HadoopRecoverableFsDataOutputStream.write
							out.write(b, off, len);{// FSDataOutputStream.write()
								out.write(b, off, len);
								position += len; 
								if (statistics != null) {
									statistics.incrementBytesWritten(len);
								}
							}
						}
						stream.write('\n');
					}
					markWrite(currentTime);
				}
			}
		}
	}
}





JM/TM: checkpoint�ɹ���commit: "Source: Custom Source -> Map -> Sink": 

StreamTask.notifyCheckpointComplete(checkpointId){
	subtaskCheckpointCoordinator.notifyCheckpointComplete(checkpointId, operatorChain, this::isRunning);{
		super.notifyCheckpointComplete(checkpointId);
		if (userFunction instanceof CheckpointListener) {
			((CheckpointListener) userFunction).notifyCheckpointComplete(checkpointId);{
				
				StreamingFileSink.notifyCheckpointComplete(checkpointId){
					this.helper.commitUpToCheckpoint(checkpointId);{
						buckets.commitUpToCheckpoint(checkpointId);{//Buckets.commitUpToCheckpoint()
							Iterator<Map.Entry<BucketID, Bucket<IN, BucketID>>> activeBucketIt = activeBuckets.entrySet().iterator();
							while (activeBucketIt.hasNext()) {
								Bucket<IN, BucketID> bucket = activeBucketIt.next().getValue();
								// ����: �������ﴥ���ύ: 
								bucket.onSuccessfulCompletionOfCheckpoint(checkpointId);{//Bucket.
									// it: Map<checkpointId,List<PendingFileRecoverable>>  ����Bucket��Ͱ�µ�ÿһ���ļ�, 
									Iterator<Map.Entry<Long, List<InProgressFileWriter.PendingFileRecoverable>>> it  =pendingFileRecoverablesPerCheckpoint
										.headMap(checkpointId, true).entrySet().iterator();
									while (it.hasNext()) {//
										Map.Entry<Long, List<InProgressFileWriter.PendingFileRecoverable>> entry = it.next();
										for (InProgressFileWriter.PendingFileRecoverable pendingFileRecoverable :entry.getValue()) {
											bucketWriter
												.recoverPendingFile(pendingFileRecoverable)
												.commit();{//OutputStreamBasedPartFileWriter$OutputStreamBasedPendingFile.commit()
													committer.commit();{//HadoopRecoverableFsDataOutputStream$HadoopFsCommitter.commit()
														
														// ������� hdfs�� rename: 
														HadoopFsCommitter.commit(){
															Path src = recoverable.tempFile(); // .prefix-0-14.txt.inprogress.dfe9798d-78c4-4bd3-9843-c893997ca336
															Path dest = recoverable.targetFile();// prefix-0-14.txt
															mlong expectedLength = recoverable.offset();
															FileStatus srcStatus = fs.getFileStatus(src);
															fs.rename(src, dest);
														}
													}
												}
										}
										it.remove();
									}
								}
								if (!bucket.isActive()) {
									activeBucketIt.remove();
									notifyBucketInactive(bucket);
								}
							}
						}
					}
				}
				
			}
		}
	}
}


