

# ZeppelinServer Web����Ľ������




main.(args){
    ZeppelinServer.conf = ZeppelinConfiguration.create();
    public static Server jettyWebServer = setupJettyServer(conf);
    
    jettyWebServer.setHandler(contexts);
    
    // Cluster Manager Server: ����Ǹ����?
    setupClusterManagerServer(sharedServiceLocator);
    
    try {// ������������Server; ����?
        jettyWebServer.start(); // Instantiates ZeppelinServer
        
        List<ErrorData> errorData = handler.waitForAtLeastOneConstructionError(5 * 1000);
        if(errorData.size() > 0 && errorData.get(0).getThrowable() != null) {
            throw new Exception(errorData.get(0).getThrowable());
        }
    } catch (Exception e) {
        LOG.error("Error while running jettyServer", e);
        System.exit(-1);
    }
    LOG.info("Done, zeppelin server started");
    
    Runtime.getRuntime().addShutdownHook(shutdown(conf)); //�Ӹ��˳��Ĺ��Ӻ���;
    
    //��ȡNotebook; ?
    Notebook notebook = sharedServiceLocator.getService(Notebook.class);
    notebook.recoveryIfNecessary(); //�ָ�ʲô?
    
    jettyWebServer.join();// �����ȴ�����;
    
    
}



/** "qtp*"�߳�:  NotebookSocket ���һ�����������ִ��;
*   - fromThread: 
    NotebookSocket.onWebSocketText() -> NotebookServer.onMessage(){ //������ݲ�ͬ�Ĳ�������,���÷���;
        case RUN_PARAGRAPH: runParagraph() -> Note.run() -> Paragraph.execute():
            -> InterpreterFactory.getInterpreter() -> ManagedInterpreterGroup.getOrCreate() -> createInterpreterGroup(groupId);
            -> interpreter.getScheduler()[AbstractScheduler].submit(this) ->  queue.put(job),jobs.put(job.getId(), job); ��Job���ڶ���,�ȴ�"SchedulerFactory2" �߳���ȡ���и�Job;
            
        case RUN_ALL_PARAGRAPHS:
    }
*   - nextThread: 
*/
NotebookSocket.onWebSocketText(){
    listener.onMessage(this, message);{//NotebookServer:
        Message messagereceived = deserializeMessage(msg);
        switch (messagereceived.op) {
            case RUN_PARAGRAPH:
                runParagraph(conn, messagereceived);{//NotebookServer:
                    String paragraphId = (String) fromMessage.get("id");
                    String noteId = getConnectionManager().getAssociatedNoteId(conn);
                    String text = (String) fromMessage.get("paragraph");
                    getNotebookService().runParagraph(noteId, paragraphId, title, text, params, config,
                        false, false, getServiceContext(fromMessage),
                        new WebSocketServiceCallback<Paragraph>(conn) {
                          @Override public void onSuccess(Paragraph p, ServiceContext context) throws IOException {
                              //todo
                          }
                        });{
                            // ��ȡ��Note��Paragraph, �����и� Paragraph;
                            Note note = notebook.getNote(noteId);
                            notebook.saveNote(note, context.getAutheInfo());
                            
                            note.run(p.getId(), blocking, context.getAutheInfo().getUser());{//Note.run()
                                Paragraph p = getParagraph(paragraphId);
                                // �����߼�: ����, ��ɻ�ȡ��Ӧ������,��ִ�м���, �޸����״̬;
                                return p.execute(blocking);{//Paragraph.execute(boolean blocking): 
                                    //��ʼ��ʱ,û�а�Ҫ������ӦInterpreter(Spark/Flink)��ʵ��?
                                    this.interpreter = getBindedInterpreter();{
                                        // ������, ������Ӧ�Ľ�����: Flink��Spark;
                                        return this.note.getInterpreterFactory().getInterpreter(intpText, executionContext);{//InterpreterFactory.getInterpreter():
                                            InterpreterSetting setting =interpreterSettingManager.getByName(executionContext.getDefaultInterpreterGroup());
                                            Interpreter interpreter = setting.getInterpreter(executionContext, replName);{//InterpreterSetting: ������������ Interpreter��ʵ��
                                                List<Interpreter> interpreters = getOrCreateSession(executionContext);{
                                                    // ��INTP������interpreterGroups:Map<String, ManagedInterpreterGroup> �л�ȡ���½� ������;
                                                    ManagedInterpreterGroup interpreterGroup = getOrCreateInterpreterGroup(executionContext);{
                                                        //���ݽ��������� ����/���� ��������ʵ��; �����½�����, ���½�����ڸ� interpreterGroups INTP����Ļ�����;
                                                        String groupId = getInterpreterGroupId(executionContext);
                                                        if (!interpreterGroups.containsKey(groupId)) { 
                                                            ManagedInterpreterGroup intpGroup = createInterpreterGroup(groupId);
                                                            interpreterGroups.put(groupId, intpGroup);
                                                        }
                                                        return interpreterGroups.get(groupId);
                                                    }
                                                    
                                                    String sessionId = getInterpreterSessionId(executionContext);
                                                    //�ӻ�����,���ݻػ�ID��ȡ���е� ������;
                                                    return interpreterGroup.getOrCreateSession(executionContext.getUser(), sessionId);{
                                                        if (sessions.containsKey(sessionId)) {
                                                            return sessions.get(sessionId);
                                                        } else {
                                                            List<Interpreter> interpreters = interpreterSetting.createInterpreters(user, id, sessionId);
                                                            for (Interpreter interpreter : interpreters) {
                                                                interpreter.setInterpreterGroup(this);
                                                            }
                                                            sessions.put(sessionId, interpreters);
                                                            return interpreters;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    
                                    setStatus(Status.READY);{//Job.setStatus(): �и���Jobʵ��;
                                        if (this.status == status) return;
                                        if (listener != null && before != null && before != after) {
                                            listener.onStatusChange(this, before, after);{//NotebookServer.onStatusChange
                                                broadcastParagraph(p.getNote(), p);{//NotebookServer:
                                                    inlineBroadcastParagraph(note, p);
                                                    broadcastClusterEvent(ClusterEvent.BROADCAST_PARAGRAPH, note, p);
                                                }
                                                broadcastUpdateNoteJobInfo(p.getNote(), System.currentTimeMillis() - 5000);
                                            }
                                        }
                                    }
                                    
                                    if (getConfig().get("enabled") == null || (Boolean) getConfig().get("enabled")) {
                                        interpreter.getScheduler().submit(this);{//AbstractScheduler.submit()
                                            job.setStatus(Job.Status.PENDING);
                                            queue.put(job);
                                            jobs.put(job.getId(), job);
                                        }
                                    }
                                    
                                    if (blocking) {// ��������ѭ���ȴ�, ���������ѭ��; 
                                        
                                    }
                                    return true;
                                }
                            }
                            
                            callback.onSuccess(p, context);// ������WebSocketServiceCallback����������;
                        }
                }
                break;
            case RUN_ALL_PARAGRAPHS:
                runAllParagraphs(conn, messagereceived);
                break;
        }
    }
}


/** "SchedulerFactory2"�߳�: 
*   - fromThread: ?
        while(){
            Job runningJob = queue.take();
            runJobInScheduler(runningJob); => executor.execute(new JobRunner()): ����ڶ����߳�"SchedulerFactory-n" ���Job���ύ
                "SchedulerFactory-n"�߳�:JobRunner.run()
                    => AbstractScheduler.runJob() -> Job.run() -> Paragraph.jobRun() -> RemoteInterprete.interpret() => 
                        - getOrCreateInterpreterProcess(); ��ȡ���½�Զ�̽�����;
                        - client.interpret()    :   ִ�нű�;
        }
*   - nextThread: "SchedulerFactory3"�߳�
*/
  AbstractScheduler.run() {
    schedulerThread = Thread.currentThread();
    while (!terminate && !schedulerThread.isInterrupted()) {
        Job runningJob = null;
        try {
            runningJob = queue.take();//��������,û����ʱ,��һֱ�����ȴ���;
        } catch (InterruptedException e) {
            LOGGER.warn("{} is interrupted", getClass().getSimpleName());
            break;
        }
        // ��������ɼ���;
        runJobInScheduler(runningJob);{//RemoteScheduler.runJobInScheduler(): ����"SchedulerFactory-n"�߳�,���Job�ύ;
            JobRunner jobRunner = new JobRunner(this, job);
            //����һ�߳�,ִ�и�Job: "SchedulerFactory-n" �̵߳�ִ���߼�;
            executor.execute(jobRunner);{
                JobRunner.run(){
                    JobStatusPoller jobStatusPoller = new JobStatusPoller(job, this, 100);
                    jobStatusPoller.start();
                    scheduler.runJob(job);{//AbstractScheduler.runJob()
                        runningJob.run();{//Job.run()
                            onJobStarted();
                            completeWithSuccess(jobRun());{//Paragraph.jobRun()
                                this.interpreter = getBindedInterpreter();
                                String script = this.scriptText;
                                // ��ʼִ�нű�
                                InterpreterContext context = getInterpreterContext();
                                /* ���ķ���: ������Խű�,ִ�н�����;
                                *   RemoteInterprete: ��Զ�̽������Ĵ���,Ӧ����������Զ�̵�(Flink/Spark) ִ����ʵ��,�ύ��ҵ��;
                                */
                                InterpreterResult ret = interpreter.interpret(script, context);{//RemoteInterprete.interpret()
                                    RemoteInterpreterProcess interpreterProcess = getOrCreateInterpreterProcess();{
                                        ManagedInterpreterGroup intpGroup = getInterpreterGroup();
                                        this.interpreterProcess = intpGroup.getOrCreateInterpreterProcess(getUserName(), properties);{//ManagedInterpreterGroup.getOrCreateInterpreterProcess()
                                            if (remoteInterpreterProcess == null) {
                                                // ������,��ʽ���� һ��Զ�̽������Ľ���: 
                                                remoteInterpreterProcess = interpreterSetting.createInterpreterProcess(id, userName, properties);{
                                                    InterpreterLauncher launcher = createLauncher(properties);
                                                    InterpreterLaunchContext launchContext = new InterpreterLaunchContext(properties, option, interpreterRunner, userName, interpreterGroupId, id, group, name, interpreterEventServer.getPort(), interpreterEventServer.getHost());
                                                    RemoteInterpreterProcess process = (RemoteInterpreterProcess) launcher.launch(launchContext);{
                                                        
                                                    }
                                                    
                                                    recoveryStorage.onInterpreterClientStart(process);
                                                    return process;
                                                }
                                                interpreterSetting.getLifecycleManager().onInterpreterProcessStarted(this);
                                                getInterpreterSetting().getRecoveryStorage().onInterpreterClientStart(remoteInterpreterProcess);
                                                
                                            }
                                        }
                                        return interpreterProcess;
                                    }
                                    
                                    if (!interpreterProcess.isRunning()) { //���Զ��ִ������������,���쳣;
                                        return new InterpreterResult(InterpreterResult.Code.ERROR,"Interpreter process is not running\n" + interpreterProcess.getErrorMessage());
                                    }
                                    interpreterProcess.callRemoteFunction(client -> {
                                        RemoteInterpreterResult remoteResult = client.interpret(sessionId, className, st, convert(context));{//RemoteInterpreteService.interpret()
                                            send_interpret(sessionId, className, st, interpreterContext);
                                            //���淢������; ����ȴ����;
                                            return recv_interpret();{//RemoteInterpreteService.recv_interpret()
                                                receiveBase(result, "interpret");
                                            }
                                        }
                                        return convert(remoteResult);
                                    });
                                        
                                    
                                }
                                
                                return ret;
                            }
                        }
                        
                        Object jobResult = runningJob.getReturn();
                    }
                }
            }
            
            if (executionMode.equals("paragraph")) {
                while (!jobRunner.isJobSubmittedInRemote()) {
                    Thread.sleep(100);
                }
            }
        }
    }
  }



            


















