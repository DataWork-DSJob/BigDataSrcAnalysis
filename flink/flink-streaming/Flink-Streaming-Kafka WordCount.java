

val stream: DataStream[String] = env.addSource(kafkaSource)
    stream.flatMap(new MySplitFlatMap)  // value.split(" ").foreach out.collect()
      .filter(new MyFilter)             // !value.isEmpty
      .map(new MyMapFunc)               // (value.trim.toLowerCase(),1)
      .keyBy(new MyKeySelector)         // value._1
      .reduce(new MyReduceFunc).setParallelism(2)   // (value1._1,value1._2+ value2._2)
      .print()

// ������Task;

FlinkKafkaConsumerBase.run(){
    while (running) {
        final ConsumerRecords<byte[], byte[]> records = handover.pollNext();
        for (ConsumerRecord<byte[], byte[]> record : partitionRecords) {
            emitRecord(value, partition, record.offset(), record);{//KafkaFetcher
                // StreamSourceContexts.NonTimestampContext.collect() -> Operator.CopyingChainingOutput.collect()
                pushToOperator(record);{// CopyingChainingOutput.
                    StreamRecord<T> copy = castRecord.copy(serializer.copy(castRecord.getValue()));
                    operator.processElement(copy);{ // StreamFlatMap.processElement() -> ���� MyMapFunc
                        
                        /** ��һ������: flatMap  */
                        MySplitFlatMap.flatMap(value: String, out: Collector[String]){
                            for(ele <- value.split(" ")){
                                out.collect(ele);{// TimestampedCollector.collect() 
                                    // StreamSourceContexts.NonTimestampContext.collect() -> Operator.CopyingChainingOutput.collect()
                                        pushToOperator(record);{// CopyingChainingOutput.pushToOperator()
                                            StreamRecord<T> copy = castRecord.copy(serializer.copy(castRecord.getValue()));
                                            operator.processElement(copy);{//StreamFilter.processElement()
                                                
                                                /** ����ڶ���tranformer ����: filter() */
                                                if(userFunction.filter(element.getValue()){ //userFunction.filter ʵ������: 
                                                    MyFilter.filter(value: String): Boolean = {
                                                        !value.isEmpty
                                                    }
                                                }){
                                                    output.collect(element);{// CountingOutput.collect()
                                                        // CopyingChainingOutput.collect() 
                                                            pushToOperator(record);{// CopyingChainingOutput.pushToOperator()
                                                                StreamRecord<T> copy = castRecord.copy(serializer.copy(castRecord.getValue()));
                                                                operator.processElement(copy);{//StreamMap.processElement()
                                                                
                                                                    /** ����������: map() */
                                                                    StreamRecord<X> newRecord = element.replace(userFunction.map(element.getValue()){
                                                                        MyMapFunc.map(value: String): (String,Int) ={
                                                                            (value.trim.toLowerCase(),1)
                                                                        }
                                                                    });
                                                                    output.collect(newRecord);{// CountingOutput.collect()
                                                                        // ��Ϊ����ӵ���keyBy()����, ��Ҫshuffle, �����Writer ����;
                                                                        output.collect(record);{// RecordWriterOutput.collect()
                                                                            pushToRecordWriter(record);{//RecordWriterOutput.
                                                                                serializationDelegate.setInstance(record); // ���л�
                                                                                recordWriter.emit(serializationDelegate);{
                                                                                    int nextChannelToSendTo = channelSelector.selectChannel(record); //�������?
                                                                                    emit(record, nextChannelToSendTo) -> copyFromSerializerToTargetChannel(targetChannel);
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
                }
            }
        }
    }
}




//Flink Shuffle�� ����: RecordWriterOutput.collect()

output.collect(record);{//RecordWriterOutput.collect()
    if (this.outputTag != null) return;
    pushToRecordWriter(record);{//RecordWriterOutput.
        serializationDelegate.setInstance(record);
        recordWriter.emit(serializationDelegate);{
            int nextChannelToSendTo = channelSelector.selectChannel(record);{// RebalancePartitioner.selectChannel() ��������?
                // �� keyBy()����ʱ: 
                KeyGroupStreamPartitioner.selectChannel(StreamRecord<T>> record){
                    key = keySelector.getKey(record.getInstance().getValue()); 
                    return KeyGroupRangeAssignment.assignKeyToParallelOperator(key, maxParallelism, numberOfChannels);{
                        // return computeOperatorIndexForKeyGroup(maxParallelism, parallelism, assignToKeyGroup(key, maxParallelism));
                            return MathUtils.murmurHash(keyHash) % maxParallelism;
                    }
                }
                
                RebalancePartitioner.selectChannel(){
                    nextChannelToSendTo = (nextChannelToSendTo + 1) % numberOfChannels;
                    return nextChannelToSendTo;
                }
                
            }
            emit(record, nextChannelToSendTo);{//ChannelSelectorRecordWriter.emit()
                serializer.serializeRecord(record);
                
                // �����л�����,��record��Ӧbytes copy��Ŀ�� Channel��;
                boolean pruneTriggered = copyFromSerializerToTargetChannel(targetChannel);{// RecordWriter.copyFromSerializerToTargetChannel()
                    
                    // һ��channel����һ��BufferBuilder; 
                    BufferBuilder bufferBuilder = getBufferBuilder(targetChannel);{//ChannelSelectorRecordWriter.getBufferBuilder()
                        if (bufferBuilders[targetChannel] != null) {
                            return bufferBuilders[targetChannel];// ��ʼ��ʱ�������鳤��Ϊ1; 
                        }else{ //���й����з��� channel ���˾��½�channel;
                            return requestNewBufferBuilder(targetChannel);
                        }
                    }
                    
                    SerializationResult result = serializer.copyToBufferBuilder(bufferBuilder);{// SpanningRecordSerializer.
                        targetBuffer.append(dataBuffer);
                        targetBuffer.commit();
                        return getSerializationResult(targetBuffer);
                    }
                    
                    
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
 


"Legacy Source Thread - Source: Custom File Source "�߳�

"Source:Custom File Source"�߳� ���ж�ȡ�ʹ���;

"OutputFlusher for Source: Custom Source -> FlatMap -> Filter ->Map " �߳�;

"Keyed Reduce -> Sink: Print to Std.Out" �߳�


// RecordWriter.OutputFlusher ��ѭ��flush()�߳�: "OutputFlusher for Source: Custom Source -> FlatMap -> Filter " �߳�


"OutputFlusher for Source: Custom Source -> FlatMap -> Filter ->Map " �߳�: 
RecordWriter.OutputFlusher.run(){
    while(running){
        Thread.sleep(timeout); //Ĭ��100ms, �� bufferTimeout
        flushAll();{ targetPartition.flushAll();{
            
            ReleaseOnConsumptionResultPartition.flushAll();{
                for (ResultSubpartition subpartition : subpartitions) {
                    subpartition.flush();{//PipelineSubpartition.flush()
                        // ��buffers ����Ϊ��ʱ, ��������, ����100ms����ѭ��; 
                        if (buffers.isEmpty()) { return;} 
                        
                        notifyDataAvailable = !flushRequested && buffers.size() == 1 && buffers.peek().isDataAvailable();
                        flushRequested = flushRequested || buffers.size() > 1 || notifyDataAvailable;
                        if (notifyDataAvailable) {
                            notifyDataAvailable();{//PipelinedSubpartition. ����֪ͨ���ݵ���, ���ݸ������߳�; 
                                readView.notifyDataAvailable();{availabilityListener.notifyDataAvailable();{
                                    notifyChannelNonEmpty();-> inputGate.notifyChannelNonEmpty(this); queueChannel(checkNotNull(channel));{
                                        CompletableFuture<?> toNotify = availabilityHelper.getUnavailableToResetAvailable();
                                        toNotify.complete(null);{
                                            postComplete();-> h.tryFire(NESTED));{
                                                d.uniRun(a = src, fn, mode > 0 ? null : this))
                                                // -> ҵ�����; jointFuture.thenRun(suspendedDefaultAction::resume);{
                                                    resume(){ //������ִ��; 
                                                        
                                                    }
                                                }
                                                
                                            }
                                        }
                                    }
                                }}
                            } 
                        }    
                    }
                }
            }
        }}
    }
}



// ͨ��Task ���п��;
Task.run(){
    doRun();{
        TaskKvStateRegistry kvStateRegistry = kvStateService.createKvStateTaskRegistry(jobId, getJobVertexId());
        Environment env = new RuntimeEnvironment();
        invokable = loadAndInstantiateInvokable(userCodeClassLoader, nameOfInvokableClass, env);
        
        invokable.invoke();{// OneInputStreamTask.invoke() -> ���ø���StreamTask.invoke()����;
            StreamTask.invoke(){
                beforeInvoke(); // ������������ǰ,�Ƚ��г�ʼ��
                // ����������ѭ��������Ϣ,������;
                runMailboxLoop();{//StreamTask.runMailboxLoop()
                    mailboxProcessor.runMailboxLoop();{// MailboxProcessor.runMailboxLoop()
                        boolean hasAction = processMail(localMailbox);{//Դ����while(processMail(localMailbox)); û��Mailʱ��һֱ�����ڴ�,����Ϣ��
                           return isMailboxLoopRunning();// return mailboxLoopRunning;
                        }
                        
                        while (hasAction = processMail(localMailbox)) {//�����������жϵķ�����, �жϻ�����Running״̬ʱ,���������� runDefaultAction()
                            mailboxDefaultAction.runDefaultAction(defaultActionContext); {
                                this.processInput();{
                                    StreamTask.processInput();{
                                        // ���ﲻͬ�� inputProcessor:StreamInputProcessor ʵ����,���в�ͬ����;
                                        InputStatus status = inputProcessor.processInput();{ //StreamoneInputProcessor.processInput()
                                            InputStatus status = input.emitNext(output);{ // StreamTaskNetworkInput.emitNext()
                                                
                                                
                                            }
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
 
