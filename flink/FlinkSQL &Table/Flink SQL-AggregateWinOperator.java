



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
 

accumulate:19, MultiArgSumAggFunc (com.bigdata.streaming.flink.mystudy.streamsql.sqlfunction.windowed.simpletest)
accumulate:-1, GroupingWindowAggsHandler$56
processElement:344, WindowOperator (org.apache.flink.table.runtime.operators.window)
emitRecord:173, OneInputStreamTask$StreamTaskNetworkOutput (org.apache.flink.streaming.runtime.tasks)


// GroupWindowAggregate(groupBy=...) �߳�
StreamTaskNetworkInput.emitNext(DataOutput<T> output){
    while (true) {
        if (result.isFullRecord()) {
            processElement(deserializationDelegate.getInstance(), output);{
                if (recordOrMark.isRecord()){ // ������,ֱ��������;
                    output.emitRecord(recordOrMark.asRecord());{//OneInputStreamTask$StreamTaskNetworkOutput.emitRecord()
                        operator.processElement(record);{
                            // ���ڴ�������: 
                            WindowOperator.processElement(record){
                                
                            }
                            
                        }
                    }
                    
                }else if (recordOrMark.isWatermark()) { // ��WM, ���ھۺϾ���Watermark;
                    
                    statusWatermarkValve.inputWatermark(recordOrMark.asWatermark(), lastChannel);{ //StatusWatermarkValve.inputWatermark()
                        
                        if (watermarkMillis > channelStatuses[channelIndex].watermark) {
                            channelStatuses[channelIndex].watermark = watermarkMillis;
                            
                            findAndOutputNewMinWatermarkAcrossAlignedChannels();{
                                output.emitWatermark(new Watermark(lastOutputWatermark));
                                    -> operator.processWatermark(watermark);
                                        -> timeServiceManager.advanceWatermark(mark); -> service.advanceWatermark(watermark.getTimestamp());{// InternalTimerServiceImpl.advanceWatermark()
                                            while ((timer = eventTimeTimersQueue.peek()) != null && timer.getTimestamp() <= time) {
                                                eventTimeTimersQueue.poll();
                                                triggerTarget.onEventTime(timer);{//WindowOperator.onEventTime()
                                                
                                                    WindowOperator.onEventTime(){
                                                        if (triggerContext.onEventTime(timer.getTimestamp())) {
                                                            // ���ڵĴ��ھۺ����� ���ɽ���߼�; ?
                                                            emitWindowResult(triggerContext.window);{//AggregateWindowOperator.emitWindowResult()
                                                                windowFunction.prepareAggregateAccumulatorForEmit(window);
                                                                BaseRow aggResult = aggWindowAggregator.getValue(window);
                                                                BaseRow previousAggResult = previousState.value();
                                                                if (previousAggResult != null) {
                                                                    collector.collect(reuseOutput);
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
                }else if (recordOrMark.isLatencyMarker()) {
                    
                }
                
            }
        }
    }
    
}




// ���ں����߼�1: ��ÿ���¼�(Record)���ۼ� : ʱ�䴰��groupBy����,���Ǵ� WindowOperator����;
WindowOperator.processElement(record){
    if (windowAssigner.isEventTime()) {
        timestamp = inputRow.getLong(rowtimeIndex);
    }
    // �������Ӱ��Ĵ���
    Collection<W> affectedWindows = windowFunction.assignStateNamespace(inputRow, timestamp);
    for (W window : affectedWindows) {
        windowState.setCurrentNamespace(window);
        windowAggregator.setAccumulators(window, acc);
        if (BaseRowUtil.isAccumulateMsg(inputRow)) {
            // ���ĵĴ����ۼ��߼�;
            windowAggregator.accumulate(inputRow);{ //GroupingWindowAggsHandler$56
                // ���� StreamExecGroupWindowAggregateBase.createAggsHandler() ���ɵ�, ��������ܰ���sum,count,avg,last_value,MulitArgSum�ȶ������;
                // �������ۺϺ���, Ӧ����ÿ������,��������� .accumulate();
                windowAggregator = {GroupingWindowAggsHandler$56@10703} 
                     agg0_sum = 1
                     agg0_sumIsNull = false
                     agg1_count1 = 1
                     agg1_count1IsNull = false
                     function_org$apache$flink$table$planner$functions$aggfunctions$LastValueAggFunction$IntLastValueAggF = {LastValueAggFunction$IntLastValueAggFunction@10752} "IntLastValueAggFunction"
                     function_com$bigdata$streaming$flink$mystudy$streamsql$sqlfunction$windowed$simpletest$MultiArgSumAg = {MultiArgSumAggFunc@10753} "MultiArgSumAggFunc"
                     converter = {DataFormatConverters$PojoConverter@10754} 
                     converter = {DataFormatConverters$PojoConverter@10755} 
                     agg2_acc_internal = {GenericRow@10756} "(+|1,-9223372036854775808)"
                     converter = {DataFormatConverters$PojoConverter@10759}                 
                
                MultiArgSumAg.accumulate(){
                    
                }
                
                IntLastValueAggFunction.accumulate(){
                    
                }
            }
        }else{
            windowAggregator.retract(inputRow);
        }
        acc = windowAggregator.getAccumulators();
        windowState.update(acc);
    }
    
}


// 2 �������Ӻ����߼�2: ���ڽ���,���;
WindowOperator.onEventTime(){
    if (triggerContext.onEventTime(timer.getTimestamp())) {
        // ���ڵĴ��ھۺ����� ���ɽ���߼�; ?
        emitWindowResult(triggerContext.window);{//AggregateWindowOperator.emitWindowResult()
            windowFunction.prepareAggregateAccumulatorForEmit(window);
            BaseRow aggResult = aggWindowAggregator.getValue(window);
            BaseRow previousAggResult = previousState.value();
            if (previousAggResult != null) {
                collector.collect(reuseOutput);
            }
        }
    }
}







