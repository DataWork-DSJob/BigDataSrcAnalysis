

StreamTask.invoke(){
    
    
}



open:51, StatefulFunction$class (org.apache.flink.streaming.api.scala.function)
open:572, KeyedStream$$anon$2 (org.apache.flink.streaming.api.scala)
openFunction:36, FunctionUtils (org.apache.flink.api.common.functions.util)
open:102, AbstractUdfStreamOperator (org.apache.flink.streaming.api.operators)
initializeStateAndOpen:990, StreamTask (org.apache.flink.streaming.runtime.tasks)
lambda$beforeInvoke$0:453, StreamTask (org.apache.flink.streaming.runtime.tasks)
run:-1, 528981627 (org.apache.flink.streaming.runtime.tasks.StreamTask$$Lambda$425)
runThrowing:94, StreamTaskActionExecutor$SynchronizedStreamTaskActionExecutor (org.apache.flink.streaming.runtime.tasks)
beforeInvoke:448, StreamTask (org.apache.flink.streaming.runtime.tasks)
invoke:460, StreamTask (org.apache.flink.streaming.runtime.tasks)
doRun:708, Task (org.apache.flink.runtime.taskmanager)
run:533, Task (org.apache.flink.runtime.taskmanager)
run:748, Thread (java.lang)




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
                beforeInvoke();{ //StreamTask.beforeInvoke()
                    actionExecutor.runThrowing(() -> {
                        initializeStateAndOpen();{
                            StreamOperator<?>[] allOperators = operatorChain.getAllOperators();
                            for (StreamOperator<?> operator : allOperators) {
                                if (null != operator) {
                                    operator.initializeState();
                                    /**
                                    * ������,��ÿ��Operator��; 
                                    */
                                    operator.open();{ // StatefulFunction.open()
                                        
                                        val info = new ValueStateDescriptor[S]("state", stateSerializer)
                                        // ����һ�� HeapValueState ��Ϊ(��ͬ���μ��)״̬;
                                        
                                        state = getRuntimeContext().getState(info)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // ����������ѭ��������Ϣ,������;
                runMailboxLoop();
                
                // �������Ѻʹ����,��Դ�ͷ�;
                afterInvoke();
                
            }
        }
    }
}

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
                                            output.emitRecord(recordOrMark.asRecord());{
                                                
                                                OneInputStreamTask.StreamTaskNetworkOutput.emitRecord(){
                                                    operator.processElement(record);{
                                                        output.collect(element.replace(userFunction.map(element.getValue())));{
                                                            KeyedStream$$anon$2.map(){
                                                                applyWithState(in, cleanFun);{  // applyWithState(in: I, fun: (I, Option[S]) => (O, Option[S])): O
                                                                    val (o, s: Option[S]) = fun(in, Option(state.value())){
                                                                        // ��������û������ �����߼�: �ۼ����;
                                                                        
                                                                    }
                                                                    
                                                                    s match {
                                                                        case Some(v) => state.update(v)
                                                                        case None => state.update(null.asInstanceOf[S])
                                                                    }
                                                                    
                                                                }
                                                                
                                                            }
                                                        }
                                                    }
                                                }
                                                
                                            }
                                            
                                            
                                        } else if (recordOrMark.isWatermark()) { // return getClass() == Watermark.class;
                                            statusWatermarkValve.inputWatermark(recordOrMark.asWatermark(), lastChannel);{//StatusWatermarkValue.inputWatermark()
                                               
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




