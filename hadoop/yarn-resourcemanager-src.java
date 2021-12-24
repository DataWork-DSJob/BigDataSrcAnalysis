

// 1. yarn client �ͻ������� Yarn��ҵ: 
// a).	YarnClientImpl.createApplication();
// b). YarnClusterDescriptor.startAppMaster() -> YarnClientImpl.submitApplication() ����AM����������

YarnClusterDescriptor.deploySessionCluster(ClusterSpecification clusterSpecification);{//YarnClusterDescriptor.deploySessionCluster()
	return deployInternal(clusterSpecification, getYarnSessionClusterEntrypoint());{
		isReadyForDeployment(clusterSpecification);
		checkYarnQueues(yarnClient);
		final YarnClientApplication yarnApplication = yarnClient.createApplication();
		final GetNewApplicationResponse appResponse = yarnApplication.getNewApplicationResponse();
		freeClusterMem = getCurrentFreeClusterResources(yarnClient);
		final int yarnMinAllocationMB = yarnConfiguration.getInt(YarnConfiguration.RM_SCHEDULER_MINIMUM_ALLOCATION_MB,.DEFAULT_RM_SCHEDULER_MINIMUM_ALLOCATION_MB);
		ApplicationReport report =startAppMaster();{//YarnClusterDescriptor.startAppMaster()
			final FileSystem fs = FileSystem.get(yarnConfiguration);
			ApplicationSubmissionContext appContext = yarnApplication.getApplicationSubmissionContext();
			final List<Path> providedLibDirs =Utils.getQualifiedRemoteSharedPaths(configuration, yarnConfiguration);
			final YarnApplicationFileUploader fileUploader =YarnApplicationFileUploader.from();
			
			userJarFiles.addAll(jobGraph.getUserJars().stream().map(f -> f.toUri()).map(Path::new).collect(Collectors.toSet()));
			userJarFiles.addAll(jarUrls.stream().map(Path::new).collect(Collectors.toSet()));
			// ����AM(ApplicationMaster)����Դ: amContainer ��Ҫ���� env,javaCammand, localResource����jar����Դ;
			processSpec =JobManagerProcessUtils.processSpecFromConfigWithNewOptionToInterpretLegacyHeap();{
				CommonProcessMemorySpec processMemory = PROCESS_MEMORY_UTILS.memoryProcessSpecFromConfig(config);{
					if (options.getRequiredFineGrainedOptions().stream().allMatch(config::contains)) {
						
					}else if (config.contains(options.getTotalFlinkMemoryOption())) {//jobmanager.memory.flink.size
						return deriveProcessSpecWithTotalFlinkMemory(config);
					}else if (config.contains(options.getTotalProcessMemoryOption())) {// jobmanager.memory.process.size
						return deriveProcessSpecWithTotalProcessMemory(config);{
							MemorySize totalProcessMemorySize =getMemorySizeFromConfig(config, options.getTotalProcessMemoryOption());
							// MetaspaceĬ�� 256Mb, jobmanager.memory.jvm-metaspace.size
							JvmMetaspaceAndOverhead jvmMetaspaceAndOverhead =deriveJvmMetaspaceAndOverheadWithTotalProcessMemory(config, totalProcessMemorySize);
							// Լ���� total - metaspace - overhead = 1024 - 256 -196 = 576Mb
							MemorySize totalFlinkMemorySize = totalProcessMemorySize.subtract(jvmMetaspaceAndOverhead.getTotalJvmMetaspaceAndOverheadSize());
							// �ְ� 576 ��һ���ֲ�heap /offHeap, ���� 448, ���� 128Mb; 
							FM flinkInternalMemory =flinkMemoryUtils.deriveFromTotalFlinkMemory(config, totalFlinkMemorySize);
							return new CommonProcessMemorySpec<>(flinkInternalMemory, jvmMetaspaceAndOverhead);
						}
					}
					return failBecauseRequiredOptionsNotConfigured();
				}
				return new JobManagerProcessSpec(processMemory.getFlinkMemory(), processMemory.getJvmMetaspaceAndOverhead());
			}
			flinkConfiguration, JobManagerOptions.TOTAL_PROCESS_MEMORY);
			final ContainerLaunchContext amContainer =setupApplicationMasterContainer(yarnClusterEntrypoint, hasKrb5, processSpec);{
				String javaOpts = flinkConfiguration.getString(CoreOptions.FLINK_JVM_OPTIONS);
				javaOpts += " " + flinkConfiguration.getString(CoreOptions.FLINK_JM_JVM_OPTIONS);
				startCommandValues.put("java", "$JAVA_HOME/bin/java");
				startCommandValues.put("jvmmem", jvmHeapMem);{
					jvmArgStr.append("-Xmx").append(processSpec.getJvmHeapMemorySize().getBytes());
					jvmArgStr.append(" -Xms").append(processSpec.getJvmHeapMemorySize().getBytes());
					if (enableDirectMemoryLimit) {//jobmanager.memory.enable-jvm-direct-memory-limit
						jvmArgStr.append(" -XX:MaxDirectMemorySize=").append(processSpec.getJvmDirectMemorySize().getBytes());
					}
					jvmArgStr.append(" -XX:MaxMetaspaceSize=").append(processSpec.getJvmMetaspaceSize().getBytes());
				}
				startCommandValues.put("jvmopts", javaOpts);
				startCommandValues.put("class", yarnClusterEntrypoint);
				startCommandValues.put("args", dynamicParameterListStr);
				
				//%java% %jvmmem% %jvmopts% %logging% %class% %args% %redirects%
				
				
			}
			amContainer.setLocalResources(fileUploader.getRegisteredLocalResources());
			// ����env: _FLINK_CLASSPATH ��������
			userJarFiles.addAll(jobGraph.getUserJars().stream().map(f -> f.toUri())); //��� jobGraph.getUserJars() �е�jars
			userJarFiles.addAll(jarUrls.stream().map(Path::new).collect(Collectors.toSet())); // ��� pipeline.jars�е�jars;
			final List<String> userClassPaths =fileUploader.registerMultipleLocalResources(
				userJarFiles, // =  jobGraph.getUserJars() + pipeline.jars 
				userJarInclusion == YarnConfigOptions.UserJarInclusion.DISABLED ? ConfigConstants.DEFAULT_FLINK_USR_LIB_DIR : Path.CUR_DIR, LocalResourceType.FILE); // ���usrlib/Ŀ¼��
			
			//FLINK_CLASSPATH 1: include-user-jar=firstʱ,�� jobGraph.getUserJars() &pipeline.jars &usrlib Ŀ¼��jars �ӵ�ǰ��;
			if (userJarInclusion == YarnConfigOptions.UserJarInclusion.FIRST) classPathBuilder.append(userClassPath).append(File.pathSeparator);//yarn.per-job-cluster.include-user-jar
			// FLINK_CLASSPATH 2: systemClassPaths= shipFiles(yarn.ship-files����) + logConfigFile +systemShipFiles(Sys.FLINK_LIB_DIR����) , ���� localResources���ϴ���13��flink��lib��jar��;
			addLibFoldersToShipFiles(systemShipFiles);{
				String libDir = System.getenv().get(ENV_FLINK_LIB_DIR);//��ϵͳ������ȡFLINK_LIB_DIR ��ֵ;
				effectiveShipFiles.add(new File(libDir));
			}
			for (String classPath : systemClassPaths) classPathBuilder.append(classPath).append(File.pathSeparator);
			// FLINK_CLASSPATH 3: 
			classPathBuilder.append(localResourceDescFlinkJar.getResourceKey()).append(File.pathSeparator);
			classPathBuilder.append(jobGraphFilename).append(File.pathSeparator);
			classPathBuilder.append("flink-conf.yaml").append(File.pathSeparator);
			//FLINK_CLASSPATH 6: include-user-jar=lastʱ, ��userClassPath ��jars�ӵ�CP����; 
			if (userJarInclusion == YarnConfigOptions.UserJarInclusion.LAST) classPathBuilder.append(userClassPath).append(File.pathSeparator);
			
			appMasterEnv.put(YarnConfigKeys.ENV_FLINK_CLASSPATH, classPathBuilder.toString());
			appMasterEnv.put(YarnConfigKeys.FLINK_YARN_FILES,fileUploader.getApplicationDir().toUri().toString());
			// ���� CLASSPATH�Ĳ���
			Utils.setupYarnClassPath(yarnConfiguration, appMasterEnv);{
				// 1. �Ȱ� _FLINK_CLASSPATH�� lib��13��flink���jar���ӵ�CP
				addToEnvironment(appMasterEnv, Environment.CLASSPATH.name(), appMasterEnv.get(ENV_FLINK_CLASSPATH));
				// 2. yarn.application.classpath + 
				String[] applicationClassPathEntries =conf.getStrings(YarnConfiguration.YARN_APPLICATION_CLASSPATH,YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH);{
					String valueString = get(name);// ��ȡyarn.application.classpath ����
					if (valueString == null) {// ����YarnĬ��CP: YarnConfiguration.DEFAULT_YARN_APPLICATION_CLASSPATH, ����7��;
						return defaultValue;// Ĭ��YarnCP����4��: CONF_DIR�� share�µ�common,hdfs,yar3��ģ���Ŀ¼;
					} else {
						return StringUtils.getStrings(valueString);
					}
				}
				for (String c : applicationClassPathEntries) {
					addToEnvironment(appMasterEnv, Environment.CLASSPATH.name(), c.trim());
				}
			}
			amContainer.setEnvironment(appMasterEnv);
			appContext.setAMContainerSpec(amContainer);
			
			// ����CPU/Memory��Դ��С; 
			capability.setMemory(clusterSpecification.getMasterMemoryMB());
			capability.setVirtualCores(flinkConfiguration.getInteger(YarnConfigOptions.APP_MASTER_VCORES));
			appContext.setResource(capability);
			
			setApplicationTags(appContext);
			yarnClient.submitApplication(appContext);{//YarnClientImpl.submitApplication()
				SubmitApplicationRequest request =Records.newRecord(SubmitApplicationRequest.class);
				request.setApplicationSubmissionContext(appContext);
				rmClient.submitApplication(request);{
					// yarn ��resourceManager�� resourcemanager.ClientRMService ���д���
					
				}
				while (true) {// ��waitingStates ���������� applicationId
					if (!waitingStates.contains(state)) {
						LOG.info("Submitted application " + applicationId);
						break;
					}
				}
				return applicationId;
			}
			
			LOG.info("Waiting for the cluster to be allocated");
			while (true) {
				report = yarnClient.getApplicationReport(appId);
				YarnApplicationState appState = report.getYarnApplicationState();
				switch (appState) {
					case FAILED: case KILLED:
						throw new YarnDeploymentException();
					case RUNNING:case FINISHED:
						break loop;
					default:
				}
				Thread.sleep(250);
			}
			
		}
		return () -> {return new RestClusterClient<>(flinkConfiguration, report.getApplicationId());};
		
	}
}




// 2. Yarn ResourceManager����: �����AM��������Դ�� Э��NodeMgr���� Application����;
// from : ���� YarnClusterDescriptor.deploySessionCluster(): startAppMaster() -> YarnClientImpl.submitApplication() 
// ����yarn-session��������FlinkYarnSessionCli����, linkis-cli�ύ��flink��ҵ�� EngineConnServer/FlinkClient����

ClientRMService.submitApplication(SubmitApplicationRequest request):SubmitApplicationResponse {
	// �������л�ȡ submissionContext;
	ApplicationSubmissionContext submissionContext = request.getApplicationSubmissionContext();{
		this.applicationSubmissionContext = convertFromProtoFormat(p.getApplicationSubmissionContext());{
			return new ApplicationSubmissionContextPBImpl(p);
		}
		return this.applicationSubmissionContext;
	}
	ApplicationId applicationId = submissionContext.getApplicationId();
	rmAppManager.submitApplication(submissionContext,System.currentTimeMillis(), user);{//RMAppManager.submitApplication()
		ApplicationId applicationId = submissionContext.getApplicationId();
		RMAppImpl application =createAndPopulateNewRMApp(submissionContext, submitTime, user, false);
		ApplicationId appId = submissionContext.getApplicationId();
		this.rmContext.getDispatcher().getEventHandler().handle(new RMAppEvent(applicationId, RMAppEventType.START));
	}
}



// AsyncDispatcher event handle �߳�, ���� Accceped Event�¼�, ���� RMAppAttempt����,���� submissionContext ���ݴ���; 
AsyncDispatcher.dispatch(Event event){
	EventHandler handler = eventDispatchers.get(type);
	handler.handle(event);{// ResourceManager.ApplicationEventDispatcher.handle()
		ApplicationId appID = event.getApplicationId();
		// rmApp: RMAppImpl, ������Ҫ��װ�� appId,submissionContext �ȱ�������Java���̵�����; 
		RMApp rmApp = this.rmContext.getRMApps().get(appID);
		rmApp.handle(event);{//RMAppImpl.handle
			ApplicationId appID = event.getApplicationId();
			this.stateMachine.doTransition(event.getType(), event);{
				currentState = StateMachineFactory.this.doTransition(operand, currentState, eventType, event);{
					return transition.doTransition(operand, oldState, event, eventType);//StateMachineFactory$SingleInternalArc
					-> hook.transition(operand, event);//RMAppImpl$StartAppAttemptTransition
					-> app.createAndStartNewAttempt(false);{//RMAppImpl.createAndStartNewAttempt
						createNewAttempt();{
							ApplicationAttemptId appAttemptId =ApplicationAttemptId.newInstance(applicationId, attempts.size() + 1);
							// ����������, ��submissionContext: ApplicationSubmissionContextPBImpl �������˲���; 
							RMAppAttempt attempt =new RMAppAttemptImpl(appAttemptId, rmContext, scheduler, masterService,submissionContext, conf,);
							attempts.put(appAttemptId, attempt);
						}
						handler.handle(new RMAppStartAttemptEvent(currentAttempt.getAppAttemptId(),transferStateFromPreviousAttempt));
					}
				}
			}
		}
	}
}



// 3. yarn.ResourceManager����: "ApplicationMasterLauncher" �߳�: ContainerLaunch �߳�

// ApplicationMasterLauncher �߳�; �����յ��� Launch�¼�, ���� submissionContext(launchContext) ���� StartContainerRequest ����nodeMgrȥ����; 
ApplicationMasterLauncher{
	
	final BlockingQueue<Runnable> masterEvents=new LinkedBlockingQueue<Runnable>();
	// �����߳�, �� ApplicationMasterLauncher.masterEvents ����,��amLunch�¼� ������/������ģʽ���д���; 
	ApplicationMasterLauncher.serviceStart(){
		launcherHandlingThread.start();{
			// ���� ApplicationMaster Launcher �߳�, Դ������; 
			// ApplicationMaster Launcher �߳�: ��BlockingQueue<Runnable>: masterEvents ȡ���¼�,��ִ��; 
			ApplicationMasterLauncher.LauncherThread.run(){
				while (!this.isInterrupted()) {
					toLaunch = masterEvents.take();
					launcherPool.execute(toLaunch);{
						AMLauncher.run();// ������Դ��
					}
				}
			}
		}
		super.serviceStart();
	}

	void launch(){
		Runnable launcher = createRunnableLauncher(application, AMLauncherEventType.LAUNCH);{
			Runnable launcher =new AMLauncher(context, application, event, getConfig());
			return launcher;
		}
		masterEvents.add(launcher);
	}
}


AMLauncher.run(){
	switch (eventType) {
		case LAUNCH:
			launch();{
				connect();
				ApplicationSubmissionContext applicationContext =application.getSubmissionContext();
				ContainerLaunchContext launchContext =createAMContainerLaunchContext(applicationContext, masterContainerID);
				StartContainerRequest scRequest =StartContainerRequest.newInstance(launchContext, masterContainer.getContainerToken());
				
				StartContainersResponse response =containerMgrProxy.startContainers(allRequests);{
					ContainerManagementProtocolPBClientImpl.startContainers(){
						StartContainersRequestProto requestProto =((StartContainersRequestPBImpl) requests).getProto();
						return new StartContainersResponsePBImpl(proxy.startContainers(null,requestProto));
					}
				}
				
			}
			handler.handle(new RMAppAttemptEvent(application.getAppAttemptId(), RMAppAttemptEventType.LAUNCHED));
			break;
		case CLEANUP:	
			cleanup();break;
	}
}






