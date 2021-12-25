
/** 1 flinkrun: CliFrontend ����
*/

// 1.1  flink run ʶ�� execution.target ��������в���, ��������Ӧ�� Execution����; 
CliFrontend.main() -> run()
	- validateAndGetActiveCommandLine(commandLine); 	�����Ƿ��� /tmp/.yarn-properties-$user �ļ�����Commaon Cli����; 
	- effectiveConfiguration =getEffectiveConfiguration(commandLine) �����ʹ��� ִ�л���Configuration;
	- executeProgram() -> mainMethod.invoke() ִ�� WordCount��Ӧ��App�� main()����
	- env.execute() ��ʼ�ύJobִ��; 

cli.CliFrontend.main(){
	// ���ض�� ������, Ĭ��3��: GenericCLI, FlinkYarnSessionCli, DefaultCLI;  
	final List<CustomCommandLine> customCommandLines=loadCustomCommandLines(configuration, configurationDirectory);{
		customCommandLines.add(new GenericCLI(configuration, configurationDirectory));
		customCommandLines.add(loadCustomCommandLine(flinkYarnSessionCLI,configuration,configurationDirectory,"y","yarn"));// "org.apache.flink.yarn.cli.FlinkYarnSessionCli"
		customCommandLines.add(new DefaultCLI());
	}
	final CliFrontend cli = new CliFrontend(configuration, customCommandLines);
	int retCode =SecurityUtils.getInstalledContext().runSecured(() -> cli.parseAndRun(args));{//CliFrontend.parseAndRun()
		String action = args[0];// run/applicaton-run 
		switch (action) {
			case ACTION_RUN: run(params);{//CliFrontend.run()
					final CommandLine commandLine = getCommandLine(commandOptions, args, true);
					activeCommandLine =validateAndGetActiveCommandLine(checkNotNull(commandLine));{
						for (CustomCommandLine cli : customCommandLines) {
							cli.isActive(commandLine){
								GenericCLI.isActive(){return configuration.getOptional(DeploymentOptions.TARGET).isPresent()
									|| commandLine.hasOption(executorOption.getOpt())
									|| commandLine.hasOption(targetOption.getOpt());}
							
								FlinkYarnSessionCli.isActive(){
									if (!super.isActive(commandLine)) {
										boolean isYarnMode = isYarnPropertiesFileMode(commandLine);{
											// ���,ֻҪ args=>commandLine �в����� "m" ����,���� canApplyYarn��==ture ? Ĭ�϶����� yarn?
											boolean canApplyYarnProperties = !commandLine.hasOption(addressOption.getOpt()); // commandLine.hasOption("m")
											if (canApplyYarnProperties) {
												for (Option option : commandLine.getOptions()) {
													if (!isDetachedOption(option)) {
														canApplyYarnProperties = false;
														break;
													}
												}
											}
											return canApplyYarnProperties;
										}
										// ����/tmp/.yarn-properties-bigdata. ($java.io.tmpdir/.yarn-properties-$user/ Ŀ¼�²鿴 ��� ApplicationID ��Ӧ��session; 
										File yarnPropertiesLocation = getYarnPropertiesLocation(yarnPropertiesFileLocation);{
											if (yarnPropertiesFileLocation != null) {
												propertiesFileLocation = yarnPropertiesFileLocation;
											}else {
												propertiesFileLocation = System.getProperty("java.io.tmpdir");
											}
											return new File(propertiesFileLocation, YARN_PROPERTIES_FILE + currentUser);
										}
										yarnPropertiesFile.load(new FileInputStream(yarnPropertiesLocation));
										
										final String yarnApplicationIdString =yarnPropertiesFile.getProperty(YARN_APPLICATION_ID_KEY);// ��ȡapplicationID
										yarnApplicationIdFromYarnProperties =ConverterUtils.toApplicationId(yarnApplicationIdString);
										return ( isYarnMode && yarnApplicationIdFromYarnProperties != null);
									}
									return true;
								};
							}
							if (cli.isActive(commandLine)) {
								return cli;
							}
						}
					}
					
					final List<URL> jobJars = getJobJarAndDependencies(programOptions);
					// ������Ч�ĺ�������,���� execution.target, 
					Configuration effectiveConfiguration =getEffectiveConfiguration(activeCommandLine, commandLine, programOptions, jobJars);{
						commandLineConfiguration =activeCustomCommandLine.toConfiguration(commandLine);{//CustomCommandLine.toConfiguration()
							FlinkYarnSessionCli.toConfiguration(){}
							
							DefaultCLI.toConfiguration()
							
							KubernetesSessionCli.toConfiguration(){}
							
						}
						return new Configuration(commandLineConfiguration);
					}
					
					executeProgram(effectiveConfiguration, program);{
						ClientUtils.executeProgram(new DefaultExecutorServiceLoader(), configuration, program, false, false);{
							// �ѻ��������͸�ִ�������� ��װ�� StreamContextEnvironment
							ContextEnvironment.setAsContext();
							StreamContextEnvironment.setAsContext();
							
							program.invokeInteractiveModeForExecution();{
								mainMethod = entryClass.getMethod("main", String[].class);
								// ִ�� App��main()����,�� WordCount.main()
								mainMethod.invoke(null, (Object) args);{
									// app�ص� env.execute()����������ҵ��ִ��; Դ��� flink-core: ExecutionEnvironment.execute()
									ExecutionEnvironment.execute();
									StreamExecutionEnvironment.execute();
								}
							}
						}
					}
				}
			case ACTION_RUN_APPLICATION: 
				runApplication(params); 
			case ACTION_STOP:	
				stop(params);
				
		}
	}
}



// 1.2 Yarn Cli 
// 1. FlinkYarnSessionCli ����: "main"�߳� ��������yarnRM ���� amContainer 

// Cli��main�߳���Ҫ�Ǵ���YarnClient, �����չ���1�� appContext: ApplicationSubmissionContext ,����Java��������jar/env������; 
// YarnClusterDescriptor.startAppMaster() �д��� appContext,���� yarnClient.submitApplication(appContext) ����Yarn;
// %java% %jvmmem% %jvmopts% %logging% %class% %args% %redirects%
FlinkYarnSessionCli.run()
	- yarnClusterClientFactory.createClusterDescriptor(effectiveConfiguration); ����Yarn RM,������RMClient;
	- yarnClusterDescriptor.deploySessionCluster() 
	- yarnApplication = yarnClient.createApplication(); ����Application
	- startAppMaster(); 
		* appContext = yarnApplication.getApplicationSubmissionContext(); ����Ӧ��ִ�������� appCtx;
		* amContainer =setupApplicationMasterContainer(); �д��� %java% %jvmmem% %jvmopts% %logging% %class% %args% %redirects% ��ʽ������;
		* yarnClient.submitApplication(appContext); �� appCtx����RM/NM ����Զ��Container/Java��������; 
	

FlinkYarnSessionCli.main(){
	final String configurationDirectory = CliFrontend.getConfigurationDirectoryFromEnv();
	final FlinkYarnSessionCli cli = new FlinkYarnSessionCli(flinkConfiguration,configurationDirectory,"");
	retCode = SecurityUtils.getInstalledContext().runSecured(() -> cli.run(args));{//FlinkYarnSessionCli.run(){
		final CommandLine cmd = parseCommandLineOptions(args, true);
		// ��Ҫ��ʱ1:  ����Yarn ResurceManager
		final YarnClusterDescriptor yarnClusterDescriptor =yarnClusterClientFactory.createClusterDescriptor(effectiveConfiguration);{
			return getClusterDescriptor(configuration);{
				final YarnClient yarnClient = YarnClient.createYarnClient();
				yarnClient.init(yarnConfiguration);{
					super.serviceStart();
				}
				yarnClient.start();{//AbstractService.start()
					serviceStart();{//YarnClientImpl.serviceStart()
						rmClient = ClientRMProxy.createRMProxy(getConfig(),ApplicationClientProtocol.class);{
							return createRMProxy(configuration, protocol, INSTANCE);{//RMProxy.createRMProxy()
								RetryPolicy retryPolicy = createRetryPolicy(conf);
								if (HAUtil.isHAEnabled(conf)) {
									RMFailoverProxyProvider<T> provider =instance.createRMFailoverProxyProvider(conf, protocol);
									return (T) RetryProxy.create(protocol, provider, retryPolicy);
								}else{// ��HA, ����; 
									InetSocketAddress rmAddress = instance.getRMAddress(conf, protocol);
									LOG.info("Connecting to ResourceManager at " + rmAddress);
									T proxy = RMProxy.<T>getProxy(conf, protocol, rmAddress);
									return (T) RetryProxy.create(protocol, proxy, retryPolicy);
								}
							}
						}
						if (historyServiceEnabled) {
							historyClient.start();
						}
					}
				}
				return new YarnClusterDescriptor(configuration,yarnConfiguration,yarnClient);
			}
		}
		if (cmd.hasOption(applicationId.getOpt())) {
			clusterClientProvider = yarnClusterDescriptor.retrieve(yarnApplicationId);
		}else{
			final ClusterSpecification clusterSpecification = yarnClusterClientFactory.getClusterSpecification(effectiveConfiguration);
			// ��Ҫ��ʱ2: ����Ӧ��; 
			clusterClientProvider = yarnClusterDescriptor.deploySessionCluster(clusterSpecification);{//YarnClusterDescriptor.deploySessionCluster()
				return deployInternal(clusterSpecification, getYarnSessionClusterEntrypoint());// Դ����� yarn: resourcenanger-src 
			}
		}
	}
}




/**	1.2.1 flinkCli-yarn-SubmitAM: deploySessionCluster()-> startAppMaster()-> yarnClient.submitApplication(appContext) 

YarnClusterDescriptor.startAppMaster(): ���������� am: ApplicationMaster
	//1. ����AM����: YarnClusterDescriptor.setupApplicationMasterContainer(): ����%java% %jvmmem% %jvmopts% %logging% %class% %args% %redirects% ����AM����;
	//2. ƴ��CLASSPATH: YarnClusterDescriptor.startAppMaster()ƴ��$CLASSPATH,���β���: $FLINK_CLASSPATH() + yarn.application.classpath ,�乹������
			userClassPath(jobGraph.getUserJars(), pipeline.jars, usrlib) 
			* 	systemClassPaths = yarn.ship-files���� + $FLINK_LIB_DIR������jars + logConfigFile;
			*	localResourceDescFlinkJar.getResourceKey() + jobGraphFilename + "flink-conf.yaml"
			yarn.application.classpath Ĭ�ϲ���: $HADOOP_CONF_DIR�� share�µ�common,hdfs,yar3��ģ���Ŀ¼;
	//3. ����yarn api: YarnClientImpl.submitApplication() ��Yarn RMͨ�Ų��ύ���� ApplicationMaster: YarnSessionClusterEntrypoint;
*/

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
			ApplicationSubmissionContext appContext = yarnApplication.getApplicationSubmissionContext();
			
			// ƴ�� %java% %jvmmem% %jvmopts% %logging% %class% %args% %redirects% ����;
			JobManagerProcessSpec processSpec =JobManagerProcessUtils.processSpecFromConfigWithNewOptionToInterpretLegacyHeap(flinkConfiguration, JobManagerOptions.TOTAL_PROCESS_MEMORY);
			final ContainerLaunchContext amContainer =setupApplicationMasterContainer(yarnClusterEntrypoint, hasKrb5, processSpec);{//YarnClusterDescriptor.
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
				//ʹ��yarn.container-start-command-template,���߲���Ĭ�� %java% %jvmmem% %jvmopts% %logging% %class% %args% %redirects%
				 String commandTemplate =flinkConfiguration.getString(ConfigConstants.YARN_CONTAINER_START_COMMAND_TEMPLATE,ConfigConstants.DEFAULT_YARN_CONTAINER_START_COMMAND_TEMPLATE);
				String amCommand =BootstrapTools.getStartCommand(commandTemplate, startCommandValues);
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

// FlinkYarn AM��CLASSPATH����Դ��
/*
$CLASSPATH
	$FLINK_CLASSPATH
		userClassPath = jobGraph.getUserJars() + pipeline.jars����ֵ +  usrlib Ŀ¼
			- jobGraph.getUserJars()
			- pipeline.jars
			- usrlib
		systemClassPaths = yarn.ship-files���� + $FLINK_LIB_DIR������jars + logConfigFile
			- yarn.ship-files
			- $FLINK_LIB_DIR
			- logConfigFile
			
			systemShipFiles: 
				- logConfigFilePath, ����$internal.yarn.log-config-file, ��Ӧ����: /opt/flink/conf/log4j.properties
				- providedLibDirs: 
			
		localResourceDescFlinkJar.getResourceKey()
		jobGraphFilename
		"flink-conf.yaml"
		
	yarn.application.classpath
		$HADOOP_CONF_DIR
		common: $HADOOP_COMMON_HOME/share/*/common/*
		hdfs: $HADOOP_HDFS_HOME/share/*/hdfs/*
		yarn: $HADOOP_YARN_HOME/share/*/yarn/*

// 
yarn.per-job-cluster.include-user-jar
yarn.provided.lib.dirs
$internal.yarn.log-config-file


*/

// flink yarn CLASSPATH ����
{
	/* 1. ���� dist,ship,archives��Դ·��: 
		flinkJarPath:	�� yarn.flink-dist-jar����,���߽� this.codesource������Ϊ dist��·��; 	δ����Ĭ��: /opt/flink/flink-1.12.2/lib/flink-dist_2.11-1.12.2.jar
		shipFiles:		�� yarn.ship-files��ȡ			δ��Ĭ��Ϊ��;
		shipArchives:	�� yarn.ship-archives ��ȡ 		δ��Ĭ��Ϊ��;
	*/
	AbstractJobClusterExecutor.execute().createClusterDescriptor().getClusterDescriptor(){
		final YarnClient yarnClient = YarnClient.createYarnClient();
		yarnClient.init(yarnConfiguration); yarnClient.start();
		
		new YarnClusterDescriptor();{
			this.userJarInclusion = getUserJarInclusionMode(flinkConfiguration);
			//1 �� yarn.flink-dist-jar����,���߽� this.codesource������Ϊ dist��·��,����ֵ YarnClusterDescriptor.flinkJarPath ����;
			getLocalFlinkDistPath(flinkConfiguration).ifPresent(this::setLocalJarPath);{
				String localJarPath = configuration.getString(YarnConfigOptions.FLINK_DIST_JAR); // yarn.flink-dist-jar
				if (localJarPath != null) {
					return Optional.of(new Path(localJarPath));
				}
				final String decodedPath = getDecodedJarPath();{//�� Class.pd.codesource.location.path //this������� flink-dist.jar������;
					final String encodedJarPath =getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
					return URLDecoder.decode(encodedJarPath, Charset.defaultCharset().name());
				}
				return decodedPath.endsWith(".jar")? Optional.of(new Path(new File(decodedPath).toURI())): Optional.empty();
			}
			//2 �� yarn.ship-files��ȡ ��Դ�ļ�·��,����ֵ YarnClusterDescriptor.shipFiles ����;
			decodeFilesToShipToCluster(flinkConfiguration, YarnConfigOptions.SHIP_FILES){// YarnClusterDescriptor.decodeFilesToShipToCluster
				final List<File> files =ConfigUtils.decodeListFromConfig(configuration, configOption, File::new);// yarn.ship-files ���� ship:jar����?
				return files.isEmpty() ? Optional.empty() : Optional.of(files);
			}.ifPresent(this::addShipFiles);
			//3 �� yarn.ship-archives ��ȡ ��Դ�ļ�·��,����ֵ YarnClusterDescriptor.shipArchives ����;
			decodeFilesToShipToCluster(flinkConfiguration, YarnConfigOptions.SHIP_ARCHIVES).ifPresent(this::addShipArchives);
			this.yarnQueue = flinkConfiguration.getString(YarnConfigOptions.APPLICATION_QUEUE);
		}
	}

	/* 2. ����$FLINK_CLASSPATH, CLASSPATH
		List<Path> providedLibDirs:		�� yarn.provided.lib.dirs �ж�ȡ����	δ������δ��;
		
	$FLINK_CLASSPATH

			
		systemClassPaths = yarn.ship-files���� + $FLINK_LIB_DIR������jars + logConfigFile
			- fileUploader.providedSharedLibs �е� ��dist��plugin ����;
				* List<Path> providedLibDirs: yarn.provided.lib.dirs��������Чdir����
				
			- uploadedDependencies: �����systemShipFiles�� ��PUBLIc&& ��dist �Ĳ���;
				- systemShipFiles
					- logConfigFilePath
					- $FLINK_LIB_DIR, �� providedLibDirs(yarn.provided.lib.dirs) Ϊ��ʱ, ����� $FLINK_LIB_DIR
			
			- userClassPaths:  ����yarn.per-job-cluster.include-user-jar=orderʱ, ��� userJarFiles
				- userJarFiles:	
		
		userClassPath: 		ȡ[��PUBLIc &&��dist]��userJarFiles;
			- userJarFiles:	
				- jobGraph.getUserJars()
				- pipeline.jars
				- usrlib
		
		flinkJarPath: (yarn.flink-dist-jar �� this.codesource.localpath
			yarn.flink-dist-jar
				��������,��ʹ�� this.codesource.localpath(��flink-dist����)
		
		localResourceDescFlinkJar.getResourceKey()
		jobGraphFilename
		"flink-conf.yaml"
		
	yarn.application.classpath
		$HADOOP_CONF_DIR
		common: $HADOOP_COMMON_HOME/share/*/common/*
		hdfs: $HADOOP_HDFS_HOME/share/*/hdfs/*
		yarn: $HADOOP_YARN_HOME/share/*/yarn/*
		
	*/

	YarnClusterDescriptor.startAppMaster(){//YarnClusterDescriptor.startAppMaster()
		final FileSystem fs = FileSystem.get(yarnConfiguration);
		ApplicationSubmissionContext appContext = yarnApplication.getApplicationSubmissionContext();
		// �� yarn.provided.lib.dirs �ж�ȡ����;��������ӵ� systemClassPaths->CLASSPATH; ��������,������ü���$FLINK_LIB_DIR��systemClassPaths(->CP);
		final List<Path> providedLibDirs =Utils.getQualifiedRemoteSharedPaths(configuration, yarnConfiguration);{
			return getRemoteSharedPaths(){//Utils.
				// yarn.provided.lib.dirs
				final List<Path> providedLibDirs =ConfigUtils.decodeListFromConfig(configuration, YarnConfigOptions.PROVIDED_LIB_DIRS, strToPathMapper);
				return providedLibDirs;
			}
		}
		//�ص��ǹ��� providedLibDirs(yarn.provided.lib.dirs) �� ΪdirĿ¼��,����ֵ�� fileUploader.providedSharedLibs
		final YarnApplicationFileUploader fileUploader =YarnApplicationFileUploader.from(fs,providedLibDirs);{new YarnApplicationFileUploader(){
			this.applicationDir = getApplicationDir(applicationId);
			this.providedSharedLibs = getAllFilesInProvidedLibDirs(providedLibDirs);{
				Map<String, FileStatus> allFiles = new HashMap<>();
				providedLibDirs.forEach(path -> {
					if (!fileSystem.exists(path) || !fileSystem.isDirectory(path)) {
						LOG.warn("Provided lib dir {} does not exist or is not a directory. Ignoring.",path);
					}else{
						final RemoteIterator<LocatedFileStatus> iterable =fileSystem.listFiles(path, true).forEach(()-> allFiles.put(name, locatedFileStatus););
					}
				});
				return Collections.unmodifiableMap(allFiles);
			}
		}}
		
		// ��shipFiles��(yarn.ship-files ���), ��ӵ� systemShipFiles��,������ -> uploadedDependencies -> systemClassPaths -> $FLINK_CLASSPATH -> CLASSPATH
		Set<File> systemShipFiles = new HashSet<>(shipFiles.size());
		for (File file : shipFiles) {
			 systemShipFiles.add(file.getAbsoluteFile());
		}
		// $internal.yarn.log-config-file ,���������ӵ� systemShipFiles��; һ��������: /opt/flink/conf/log4j.properties; ���� -> uploadedDependencies -> systemClassPaths -> $FLINK_CLASSPATH -> CLASSPATH
		final String logConfigFilePath =configuration.getString(YarnConfigOptionsInternal.APPLICATION_LOG_CONFIG_FILE);
		if (null !=logConfigFilePath) {
			systemShipFiles.add(new File(logConfigFilePath));
		}
		// �� yarn.provided.lib.dirs������, ����� FLINK_LIB_DIR �� systemShipFiles -> systemClassPaths -> CLASSPATH;
		if (providedLibDirs == null || providedLibDirs.isEmpty()) {
			addLibFoldersToShipFiles(systemShipFiles);{//YarnClusterDescriptor.addLibFoldersToShipFiles()
				String libDir = System.getenv().get(ENV_FLINK_LIB_DIR);// �� $FLINK_LIB_DIR ��������;
				if (libDir != null) {
					File directoryFile = new File(libDir);
					if (directoryFile.isDirectory()) {
						effectiveShipFiles.add(directoryFile);//effectiveShipFiles ������� systemShipFiles;
					}
				};
			}
		}
		
		final Set<Path> userJarFiles = new HashSet<>();
		// ��JobGraph.userJars��ӵ� userJarFiles��; 	Ӧ�þ��� -jarָ����App��,��examples/batch/WordCount.jar;  ������ userJarFiles-> userClassPaths -> $FLINK_CLASSPATH -> CLASSPATH
		if (jobGraph != null) {
			List<Path> jobUserJars = jobGraph.getUserJars().stream().map(f -> f.toUri()).map(Path::new).collect(Collectors.toSet());
			userJarFiles.addAll(jobUserJars);
		}
		//�� pipeline.jars�ж�ȡֵ����ֵ�� jarUrls;  Ĭ�Ͼ���-jar ·��: examples/batch/WordCount.jar
		final List<URI> jarUrls =ConfigUtils.decodeListFromConfig(configuration, PipelineOptions.JARS, URI::create);// ��pipeline.jars��ȡֵ;
		//ֻ�е� YarnApplication ģʽʱ,�Ż�ӵ� userClassPaths ->$FLINK_CLASSPATH��;  һ�� yarnClusterEntrypoint�� YarnJob or YarnSession, ���Բ����� CP;
		if (jarUrls != null && YarnApplicationClusterEntryPoint.class.getName().equals(yarnClusterEntrypoint)) {
			userJarFiles.addAll(jarUrls.stream().map(Path::new).collect(Collectors.toSet()));
		}
		
		// Register all files in provided lib dirs as local resources with public visibility and upload the remaining dependencies as local resources with APPLICATION visibility.
		// ��fileUploader.providedSharedLibs( yarn.provided.lib.dirs��������Чdir����) �е� ��dist��plugin��, 
		final List<String> systemClassPaths = fileUploader.registerProvidedLocalResources();{// YarnApplicationFileUploader.registerProvidedLocalResources()
			final ArrayList<String> classPaths = new ArrayList<>();
			providedSharedLibs.forEach((fileName, fileStatus)->{
				final Path filePath = fileStatus.getPath();
				if (!isFlinkDistJar(filePath.getName()) && !isPlugin(filePath)) {// �ѷ�dist��plugin�������ļ�,��ӵ� classPaths��;
					classPaths.add(fileName);
				}else if (isFlinkDistJar(filePath.getName())) { // �����flink-dist�ļ�,ֱ�Ӹ�ֵ�� flinkDist;
					flinkDist = descriptor;
				}
			});
		}
		// ��systemShipFiles��(logConfigFile + $FLINK_LIB_DIR(������yarn.provided.lib.dirsʱ) )���ݸ��� shipFiles;
		Collection<Path> shipFiles = systemShipFiles.stream().map(e -> new Path(e.toURI())).collect(Collectors.toSet());
		// ��shipFiles��(1��2��)����(�ݹ����)����,���˳� [PUBLIC] && ��dist] ������ archives & resources, һ�𷵻ظ���uploadedDependencies;
		final List<String> uploadedDependencies =fileUploader.registerMultipleLocalResources(shipFiles,Path.CUR_DIR,LocalResourceType.FILE);{
			final List<Path> localPaths = new ArrayList<>();
			for (Path shipFile : shipFiles) {
				if (Utils.isRemotePath(shipFile.toString())) {
					
				}else{
					final File file = new File(shipFile.toUri().getPath());
					if (file.isDirectory()) {// 
						Files.walkFileTree();//��Ŀǰ���������ö�����?
					}
				}
				localPaths.add(shipFile);
				relativePaths.add(new Path(localResourcesDirectory, shipFile.getName()));
			}
			for (int i = 0; i < localPaths.size(); i++) {
				if (!isFlinkDistJar(relativePath.getName())) {
					if (!resourceDescriptor.alreadyRegisteredAsLocalResource(){// ֻ��PUBLIC�����������Դ �����; log4j.properties��Ϊ��APP���𱻹��˵�;
						return this.visibility.equals(LocalResourceVisibility.PUBLIC)
					}) {
						if (key.endsWith("jar")) { //��jar���㵽 archives,
							archives.add(relativePath.toString());
						}else{ //���з�jar��file ���㵽 resource��; 
							resources.add(relativePath.getParent().toString());
						}
					}
				}
			}
			
			final ArrayList<String> classPaths = new ArrayList<>();
			resources.stream().sorted().forEach(classPaths::add);
			archives.stream().sorted().forEach(classPaths::add);
			return classPaths;
		}
		systemClassPaths.addAll(uploadedDependencies);
		
		if (providedLibDirs == null || providedLibDirs.isEmpty()) {
			addPluginsFoldersToShipFiles(shipOnlyFiles);
			fileUploader.registerMultipleLocalResources();
		}
		if (!shipArchives.isEmpty()) {//��yarn.ship-archives��Ϊ��,
			shipArchivesFile = shipArchives.stream().map(e -> new Path(e.toURI())).collect(Collectors.toSet());
			fileUploader.registerMultipleLocalResources(shipArchivesFile);
		}
		
		// ����env: _FLINK_CLASSPATH ��������
		userJarFiles.addAll(jobGraph.getUserJars().stream().map(f -> f.toUri())); //��� jobGraph.getUserJars() �е�jars
		userJarFiles.addAll(jarUrls.stream().map(Path::new).collect(Collectors.toSet())); // ��� pipeline.jars�е�jars;
		// localResourcesDir= "."
		String localResourcesDir= userJarInclusion == YarnConfigOptions.UserJarInclusion.DISABLED ? ConfigConstants.DEFAULT_FLINK_USR_LIB_DIR : Path.CUR_DIR, LocalResourceType.FILE;
		final List<String> userClassPaths =fileUploader.registerMultipleLocalResources(userJarFiles, localResourcesDir);{// ��������[��PUBLIC] && ��dist] 
			for (int i = 0; i < localPaths.size(); i++) {
				final Path relativePath = localPaths.get(i).get(i);
				if (!isFlinkDistJar(relativePath.getName())) {
					// ֻҪ����PUBLIC �����, �����; ����� userJar(��:examples/batch/WordCount.jar) ���ɹ����;
					if (!resourceDescriptor.alreadyRegisteredAsLocalResource(){// ֻҪ��PUBLIC���������, �����; 
						return this.visibility.equals(LocalResourceVisibility.PUBLIC)
					}) {
						if (key.endsWith("jar")) { //��jar���㵽 archives,
							archives.add(relativePath.toString());
						}else{ //���з�jar��file ���㵽 resource��; 
							resources.add(relativePath.getParent().toString());
						}
					}
				}
			}
		}
		// ��yarn.per-job-cluster.include-user-jar=orderʱ, ���userClassPaths�� systemClassPath
		if (userJarInclusion == YarnConfigOptions.UserJarInclusion.ORDER) {//yarn.per-job-cluster.include-user-jar=orderʱ 
			systemClassPaths.addAll(userClassPaths);
		}
		
		//FLINK_CLASSPATH 1: include-user-jar=firstʱ,�� jobGraph.getUserJars() &pipeline.jars &usrlib Ŀ¼��jars �ӵ�ǰ��;
		if (userJarInclusion == YarnConfigOptions.UserJarInclusion.FIRST){////yarn.per-job-cluster.include-user-jar=firstʱ, userClassPath��ǰ��;
			classPathBuilder.append(userClassPath).append(File.pathSeparator);
		} 
		Collections.sort(systemClassPaths);
		Collections.sort(userClassPaths);
		StringBuilder classPathBuilder = new StringBuilder();
		
		for (String classPath : systemClassPaths) {// ���system�����CP
            classPathBuilder.append(classPath).append(File.pathSeparator);
        }
		// ��װ flinkJarPath(yarn.flink-dist-jar �� this.codesource.localpath����,��flink-dist��); ����ӵ� classPath��;
		final YarnLocalResourceDescriptor localResourceDescFlinkJar =fileUploader.uploadFlinkDist(flinkJarPath);
		classPathBuilder.append(localResourceDescFlinkJar.getResourceKey()).append(File.pathSeparator);
		
		// ��jobGraph���кų��ļ�,���� "job.graph" ��ӵ�classpath;
		if (jobGraph != null) {
			File tmpJobGraphFile = File.createTempFile(appId.toString(), null);
			// ��jobGraph����д������ʱ�ļ�: /tmp/application_1639998011452_00604014191052203287620.tmp
			try (FileOutputStream output = new FileOutputStream(tmpJobGraphFile);
				 ObjectOutputStream obOutput = new ObjectOutputStream(output)) {
                    obOutput.writeObject(jobGraph);
            }
			final String jobGraphFilename = "job.graph";
			configuration.setString(JOB_GRAPH_FILE_PATH, jobGraphFilename);
			fileUploader.registerSingleLocalResource();
			classPathBuilder.append(jobGraphFilename).append(File.pathSeparator);
		}
		
		
		
		
		
		
		
		
		
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
		
	}



	
}





/** 1.2.2 yarnCli: submitApplication ��Yarn RMͨ��,�ύ����AM; 
	//1. ����Rpc����: ProtobufRpcEngine.invoke()
	//2. ͨ�ŵȴ��� waitingStates�ͽ�������,���� applicationId
*/

YarnClientImpl.submitApplication(ApplicationSubmissionContext appContext){//YarnClientImpl.submitApplication()
	SubmitApplicationRequest request =Records.newRecord(SubmitApplicationRequest.class);
	request.setApplicationSubmissionContext(appContext);
	
	rmClient.submitApplication(request);{// ApplicationClientProtocolPBClientImpl.submitApplication()
		// yarn ��resourceManager�� resourcemanager.ClientRMService ���д���
		SubmitApplicationRequestProto requestProto= ((SubmitApplicationRequestPBImpl) request).getProto();
		SubmitApplicationResponseProto proto= proxy.submitApplication(null,requestProto){
			// ʵ��ִ��: 
			ProtobufRpcEngine.invoke(Object proxy, Method method, Object[] args){}{
				// method= ApplicationClientProtocol.BlokingInterface.submitApplication()
				RequestHeaderProto rpcRequestHeader = constructRpcRequestHeader(method);
				RpcRequestWrapper rpcRequest= new RpcRequestWrapper(rpcRequestHeader, theRequest), remoteId,fallbackToSimpleAuth);
				RpcResponseWrapper val=(RpcResponseWrapper) client.call(RPC.RpcKind.RPC_PROTOCOL_BUFFER,rpcRequest);
				Message returnMessage = prototype.newBuilderForType().mergeFrom(val.theResponseRead).build();
				return returnMessage;
			}
		}
		return new SubmitApplicationResponsePBImpl(proxy.submitApplication(null,requestProto));
	}
	while (true) {// ��waitingStates ���������� applicationId
		if (!waitingStates.contains(state)) {
			LOG.info("Submitted application " + applicationId);
			break;
		}
	}
	return applicationId;
}



// 1.3 K8s Cli
// KubernetesSessionCli.main() ��Java�ύ����
org.apache.flink.kubernetes.cli.KubernetesSessionCli.main(){
	final Configuration configuration = getEffectiveConfiguration(args);{
		final CommandLine commandLine = cli.parseCommandLineOptions(args, true);
		final Configuration effectiveConfiguration = new Configuration(baseConfiguration);
        effectiveConfiguration.addAll(cli.toConfiguration(commandLine));
        effectiveConfiguration.set(DeploymentOptions.TARGET, KubernetesSessionClusterExecutor.NAME);
        return effectiveConfiguration;
	}
	final ClusterClientFactory<String> kubernetesClusterClientFactory = clusterClientServiceLoader.getClusterClientFactory(configuration);
	final KubernetesSessionCli cli = new KubernetesSessionCli(configuration, configDir);
	int retCode;
	try {
		final KubernetesSessionCli cli = new KubernetesSessionCli(configuration, configDir);
		retCode = SecurityUtils.getInstalledContext().runSecured(() -> cli.run(args));{//KubernetesSessionCli.run()
			final Configuration configuration = getEffectiveConfiguration(args);
			final ClusterClientFactory<String> kubernetesClusterClientFactory = clusterClientServiceLoader.getClusterClientFactory(configuration);
			String clusterId = kubernetesClusterClientFactory.getClusterId(configuration);
			final FlinkKubeClient kubeClient = FlinkKubeClientFactory.getInstance().fromConfiguration(configuration, "client");
			
			if (clusterId != null && kubeClient.getRestService(clusterId).isPresent()) {
				clusterClient = kubernetesClusterDescriptor.retrieve(clusterId).getClusterClient();
			}else{// ��һ��,��������; 
				clusterClient =kubernetesClusterDescriptor
										.deploySessionCluster(kubernetesClusterClientFactory.getClusterSpecification(configuration)){//KubernetesClusterDescriptor.deploySessionCluster
											KubernetesClusterDescriptor.deploySessionCluster(){
												final ClusterClientProvider<String> clusterClientProvider = deployClusterInternal(KubernetesSessionClusterEntrypoint.class.getName(),clusterSpecification, false);
												try (ClusterClient<String> clusterClient = clusterClientProvider.getClusterClient()) {
													LOG.info("Create flink session cluster {} successfully, JobManager Web Interface: {}", clusterId, clusterClient.getWebInterfaceURL());
												}
												return clusterClientProvider;
											}
										}
										.getClusterClient();
				clusterId = clusterClient.getClusterId();
			}
			
			clusterClient.close();
			kubeClient.close();
		}
	} catch (CliArgsException e) {
		retCode = AbstractCustomCommandLine.handleCliArgsException(e, LOG);
	} catch (Exception e) {
		retCode = AbstractCustomCommandLine.handleError(e, LOG);
	}
	System.exit(retCode);
	
	
}






/** 2	env.execute() �ύִ��
*
*/





// 2.1 env.execute() ������ҵִ��:  env.execute() : ������ӦFactory��Executor,����������, submittJob()�ύִ��; 
env.execute()
	- executorServiceLoader.getExecutorFactory() ͨ�� ���غͱȽ����е� PipelineExecutorFactory.name()�Ƿ�==  execution.target
	- PipelineExecutorFactory.getExecutor() ������Ӧ PipelineExecutorʵ����: YarnSession,YarnPerJob, KubernetesExecutor,LocalExecutor ��; 
	- PipelineExecutor.execute() �ύִ����Ӧ��job��ҵ; 


ExecutionEnvironment.execute(){
	// Streaming ��ִ��
	StreamExecutionEnvironment.execute(){
		return execute(getStreamGraph(jobName));{
			final JobClient jobClient = executeAsync(streamGraph);
			jobListeners.forEach(jobListener -> jobListener.onJobExecuted(jobExecutionResult, null));
			return jobExecutionResult;
		}
	}
	// �ֱ���ִ�л����� Զ��ִ�л���
	LocalStreamEnvironment.execute(){
		return super.execute(streamGraph);
	}
	
	RemoteStreamEnvironment.execute(){
		
	}
	StreamContextEnvironment.execute(){};
	StreamPlanEnvironment.execute();{}// ? strema sql ?
	
}




// 2.2 ����������ĵ��� Streamģʽ �첽ִ����ҵ
// PipelineExecutor.execute() clusterClient.submitJob(): RestClient.sendRequest() ��Զ��JobManager���̷��� JobSubmit ����
StreamExecutionEnvironment.executeAsync(StreamGraph streamGraph);{
	// ���ﶨ���� ִ��ģʽ��ִ������; ��Ҫͨ�� ���غͱȽ����е� PipelineExecutorFactory.name()�Ƿ�==  execution.target
	final PipelineExecutorFactory executorFactory = executorServiceLoader.getExecutorFactory(configuration);{//core.DefaultExecutorServiceLoader.
		final ServiceLoader<PipelineExecutorFactory> loader = ServiceLoader.load(PipelineExecutorFactory.class);
		while (loader.iterator().hasNext()) {
			// ���� execution.target ������� PipelineExecutorFactory.NAME ���бȽ�,���Ƿ����; 
			boolean isCompatible = factories.next().isCompatibleWith(configuration);{
				RemoteExecutorFactory.isCompatibleWith(){
					return RemoteExecutor.NAME.equalsIgnoreCase(configuration.get(DeploymentOptions.TARGET)); //execution.target==remote
				}
				LocalExecutorFactory.isCompatibleWith(){ // ��execution.target== local
					return LocalExecutor.NAME.equalsIgnoreCase(configuration.get(DeploymentOptions.TARGET));
				}
				KubernetesSessionClusterExecutorFactory.isCompatibleWith(){//��execution.target�Ƿ�== kubernetes-session
					return configuration.get(DeploymentOptions.TARGET).equalsIgnoreCase(KubernetesSessionClusterExecutor.NAME);
				}
				//Yarn�����ֲ���ģʽ:  yarn-per-job, yarn-session, yarn-application
				YarnSessionClusterExecutorFactory.isCompatibleWith(){ // ��execution.target== yarn-session  
					YarnSessionClusterExecutor.NAME.equalsIgnoreCase(configuration.get(DeploymentOptions.TARGET));
				}
			}
			if (factory != null && isCompatible) compatibleFactories.add(factories.next());
		}
		if (compatibleFactories.size() > 1) { 
			throw new IllegalStateException("Multiple compatible client factories found for:\n" + configStr + ".");
		}
		if (compatibleFactories.isEmpty()) {
			throw new IllegalStateException("No ExecutorFactory found to execute the application.");
		}
		return compatibleFactories.get(0); // ֻ�ܶ���1�� PipelineExecutorFactory, ���򱨴�; 
	}
	CompletableFuture<JobClient> jobClientFuture = executorFactory
		.getExecutor(configuration){//PipelineExecutorFactory.getExecutor()
			LocalExecutorFactory.getExecutor()
			RemoteExecutorFactory.getExecutor()
			EmbeddedExecutorFactory.getExecutor()
			WebSubmissionExecutorFactory.getExecutor()
			
			KubernetesSessionClusterExecutorFactory.getExecutor(){}
			YarnJobClusterExecutorFactory.getExecutor(){}
			YarnSessionClusterExecutorFactory.getExecutor(){
				return new YarnSessionClusterExecutor();
			}
			
		}
		.execute(streamGraph, configuration, userClassloader);{//PipelineExecutor.execute(pipeline,configuration,userCodeClassloader)
			LocalExecutor.execute(){//LocalExecutor.execute()
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
			
			AbstractJobClusterExecutor.execute();
			
			// YarnCluster, KubeClient �ȶ��� ���
			AbstractSessionClusterExecutor.execute(){
				final JobGraph jobGraph = PipelineExecutorUtils.getJobGraph(pipeline, configuration);
				// �жϺʹ�����Ҫ�� Cluster���Ӷ�
				ClusterDescriptor clusterDescriptor =clusterClientFactory.createClusterDescriptor(configuration);{//AbstractSessionClusterExecutor.
					final String configurationDirectory = configuration.get(DeploymentOptionsInternal.CONF_DIR);
					return getClusterDescriptor(configuration);{
						YarnClusterClientFactory.getClusterDescriptor(){
							YarnClient yarnClient = YarnClient.createYarnClient();
							yarnClient.init(yarnConfiguration);
							yarnClient.start();
							return new YarnClusterDescriptor(yarnConfiguration,yarnClient,YarnClientYarnClusterInformationRetriever.create(yarnClient));
						}
						
						kubernetesClusterClientFactory.getClusterDescriptor(){
							
						}
						
					}
				}
				
				ClusterClientProvider<ClusterID> clusterClientProvider =clusterDescriptor.retrieve(clusterID);
				return clusterClient
					.submitJob(jobGraph){// RestClusterClient.submitJob()
						
						Future<JobSubmitResponseBody> submissionFuture = requestFuture.thenCompose(sendRetriableRequest());
						// ����request: JobSubmitRequestBody
						Tuple2<JobSubmitRequestBody, Collection<FileUpload>> requestFuture= jobGraphFileFuture.thenApply(){
							final JobSubmitRequestBody requestBody =new JobSubmitRequestBody(jobGraphFile.getFileName().toString(),jarFileNames,artifactFileNames);
							return Tuple2.of(requestBody, Collections.unmodifiableCollection(filesToUpload));
						}
						// ����JobSumbit����: sendRequest(request)
						submissionFuture= sendRetriableRequest(request);{//RestClusterClient.
							getWebMonitorBaseUrl()
							return restClient.sendRequest(messageParameters,request, filesToUpload);{//RestClient.
								return sendRequest();{//RestClient.sendRequest()
									String targetUrl = MessageParameters.resolveUrl(versionedHandlerURL, messageParameters);// = /v1/jobs
									objectMapper.writeValue(new StringWriter(), request);
									Request httpRequest =createRequest(targetUrl,payload);
									// ������Ⱥ: bdnode102.hjq.com:36384(YarnSessionClusterEntrypoint) JobManager����JobSubmitRequest
									return submitRequest(targetAddress, targetPort, httpRequest, responseType);{//RestClient.submitRequest()
										connectFuture = bootstrap.connect(targetAddress, targetPort);
										httpRequest.writeTo(channel);
										future = handler.getJsonFuture();
										parseResponse(rawResponse, responseType);
									}
								}
							}
						}
						
						return submissionFuture.thenApply()
							.exceptionally();
					}
					.thenApplyAsync()
					.thenApplyAsync()
					.whenComplete((ignored1, ignored2) -> clusterClient.close());
			}
			RemoteExecutor[extends AbstractSessionClusterExecutor].execute();
			
			EmbeddedExecutor.execute();
		}
	
	try {
		JobClient jobClient = jobClientFuture.get();
		jobListeners.forEach(jobListener -> jobListener.onJobSubmitted(jobClient, null));
		return jobClient;
	} catch (ExecutionException executionException) {//ִ��ʧ��,�������׳��쳣; 
		throw new FlinkException(String.format("Failed to execute job '%s'.", streamGraph.getJobName()),strippedException);
	}
}



// 2.2.1 JobManagerģ��, ��ӦJobSubmit������߼�: submitJob(): RestClient.sendRequest() -> 











/** 3	FlinkSqlCli 
*
*/



// SqlClient������ TableEnvInit��ʼ���� CatalogManager����;
// client.start().openSession().build(): ExecutionContext.initializeTableEnvironment()��ʼ��Table������Դ, initializeCatalogs()������������Catalogs��curdb;
// client.start().open().parseCommand(line).sqlParser.parse(stmt): PlannerContext.createCatalogReader() ��CatalogManager��curCatalog/DB��ΪdefaultSchemas ��װ��FlinkCatalogReader;
// client.start().open().callCommand().callSelect(cmdCall):executor.executeQuery():tableEnv.sqlQuery(selectQuery) �ύTable��ѯ����: TableEnvironmentImpl.sqlQuery()

SqlClient.main(){
	final SqlClient client = new SqlClient(true, options);
	client.start();{
		final Executor executor = new LocalExecutor(options.getDefaults(), jars, libDirs);
        executor.start();
		final Environment sessionEnv = readSessionEnvironment(options.getEnvironment());
        appendPythonConfig(sessionEnv, options.getPythonConfiguration());
		context = new SessionContext(options.getSessionId(), sessionEnv);
		// ���� ModuleManager, CatalogManager, FunctionCatalog
		String sessionId = executor.openSession(context);{// LocalExecutor.
			String sessionId = sessionContext.getSessionId();// defaul;
			this.contextMap.put(sessionId, createExecutionContextBuilder(sessionContext).build());{//ExecutionContext$Builder.build()
				return new ExecutionContext<>(this.sessionContext,this.sessionState,this.dependencies,,,);{//ExecutionContext()���캯��, ����һ�ѵ�ִ�л���;
					classLoader = ClientUtils.buildUserCodeClassLoader();
					// ��Ҫ�Ļ������������� ���ж�������
					initializeTableEnvironment(sessionState);{//ExecutionContext.initializeTableEnvironment()
						EnvironmentSettings settings = environment.getExecution().getEnvironmentSettings();
						final TableConfig config = createTableConfig();
						if (sessionState == null) {
							// Step.1 Create environments
							final ModuleManager moduleManager = new ModuleManager();
							final CatalogManager catalogManager =CatalogManager.newBuilder()
										.classLoader(classLoader).config(config.getConfiguration())
										.defaultCatalog(settings.getBuiltInCatalogName(),
												new GenericInMemoryCatalog(settings.getBuiltInCatalogName(),settings.getBuiltInDatabaseName()))
										.build();{//CatalogManager.Builder.build()
											// default_catalog, default_database
											return new CatalogManager(defaultCatalogName,defaultCatalog,new DataTypeFactoryImpl(classLoader, config, executionConfig));
							}
							CommandLine commandLine =createCommandLine(environment.getDeployment(), commandLineOptions);
							clusterClientFactory = serviceLoader.getClusterClientFactory(flinkConfig);
							// Step 1.2 Initialize the FunctionCatalog if required.
							FunctionCatalog functionCatalog =new FunctionCatalog(config, catalogManager, moduleManager);
							// Step 1.3 Set up session state.
							this.sessionState = SessionState.of(catalogManager, moduleManager, functionCatalog);
							// Must initialize the table environment before actually the
							createTableEnvironment(settings, config, catalogManager, moduleManager, functionCatalog);
							// Step.2 Create modules and load them into the TableEnvironment.
							environment.getModules().forEach((name, entry) -> modules.put(name, createModule(entry.asMap(), classLoader)));
							// Step.3 create user-defined functions and temporal tables then register them.
							registerFunctions();
							// Step.4 Create catalogs and register them. ����config�����ļ�,�������Catalog�� curCatalog,curDatabase;
							initializeCatalogs();{// ExecutionContext.initializeCatalogs
								// Step.1 Create catalogs and register them.
								environment.getCatalogs().forEach((name, entry) -> {
												Catalog catalog=createCatalog(name, entry.asMap(), classLoader);
												tableEnv.registerCatalog(name, catalog);
											});
								// Step.2 create table sources & sinks, and register them.
								environment.getTables().forEach((name, entry) -> {
												if (entry instanceof SourceTableEntry|| entry instanceof SourceSinkTableEntry) {
													tableSources.put(name, createTableSource(name, entry.asMap()));
												}
												if (entry instanceof SinkTableEntry|| entry instanceof SourceSinkTableEntry) {
													tableSinks.put(name, createTableSink(name, entry.asMap()));
												}
											});
								tableSources.forEach(((TableEnvironmentInternal) tableEnv)::registerTableSourceInternal);
								tableSinks.forEach(((TableEnvironmentInternal) tableEnv)::registerTableSinkInternal);
								// Step.4 Register temporal tables.
								environment.getTables().forEach((name, entry) -> {registerTemporalTable(temporalTableEntry);});
								// Step.5 Set current catalog and database. �� 
								Optional<String> catalog = environment.getExecution().getCurrentCatalog();// "current-catalog" ����
								Optional<String> database = environment.getExecution().getCurrentDatabase();// current-database ����
								database.ifPresent(tableEnv::useDatabase);
							}
						}
					}
				}
			}
		}
		
		openCli(sessionId, executor);{//SqlClient.openCli
			CliClient cli = new CliClient(sessionId, executor, historyFilePath)
			cli.open();{//CliClient.
				terminal.writer().append(CliStrings.MESSAGE_WELCOME);
				while (isRunning) {
					terminal.writer().append("\n");
					// ��ȡһ������; 
					String line = lineReader.readLine(prompt, null, (MaskingCallback) null, null);
					// �����û���ѯ������� Calcite����,������Ĭ�� curCatalog,curDB���� FlinkCatalogReader;
					final Optional<SqlCommandCall> cmdCall = parseCommand(line);{//CliClient.
						parsedLine = SqlCommandParser.parse(executor.getSqlParser(sessionId), line);{
							Optional<SqlCommandCall> callOpt = parseByRegexMatching(stmt);
							if (callOpt.isPresent()) {//�����������; 
								return callOpt.get();
							}else{// û������, ��������; 
								return parseBySqlParser(sqlParser, stmt);{//SqlCommandParser.parseBySqlParser
									operations = sqlParser.parse(stmt);{//LocalExecutor.Parser������.parse()
										return context.wrapClassLoader(() -> parser.parse(statement));{// ParserImpl.parse()
											CalciteParser parser = calciteParserSupplier.get();
											FlinkPlannerImpl planner = validatorSupplier.get();
											SqlNode parsed = parser.parse(statement);
											Operation operation =SqlToOperationConverter.convert(planner, catalogManager, parsed)
											.orElseThrow(() -> new TableException("Unsupported query: " + statement));{// SqlToOperationConverter.convert()
												final SqlNode validated = flinkPlanner.validate(sqlNode);{// FlinkPlannerImpl.validate()
													val validator = getOrCreateSqlValidator();{
														val catalogReader = catalogReaderSupplier.apply(false);{
															PlannerContext.createCatalogReader(){
																SqlParser.Config sqlParserConfig = getSqlParserConfig();
																SqlParser.Config newSqlParserConfig =SqlParser.configBuilder(sqlParserConfig).setCaseSensitive(caseSensitive).build();
																SchemaPlus rootSchema = getRootSchema(this.rootSchema.plus());
																// ����� currentDatabase,currentDatabase ��Դ�� CatalogManager.����; Ӧ���Ǽ��� sql-client-defaults.yaml �����ɵ�; 
																// �� currentCatalog("myhive"), currentDatabase("default") ��ΪĬ�ϵ� SchemaPaths;
																List<List<String>> defaultSchemas = asList(asList(currentCatalog, currentDatabase), singletonList(currentCatalog));
																return new FlinkCalciteCatalogReader(CalciteSchema.from(rootSchema),defaultSchemas,typeFactory);
															}
														}
														validator = createSqlValidator(catalogReader)
													}
													validate(sqlNode, validator)
												}
												SqlToOperationConverter converter =new SqlToOperationConverter(flinkPlanner, catalogManager);
											}
											
										
										}
									}
									return new SqlCommandCall(cmd, operands);
								}
							}
						}
					}
					cmdCall.ifPresent(this::callCommand);{
						switch (cmdCall.command) {
							case SET:
								callSet(cmdCall);
								break;
							case SELECT:
								callSelect(cmdCall);{//CliClient.callSelect()
									resultDesc = executor.executeQuery(sessionId, cmdCall.operands[0]);{//LocalExecutor.executeQuery()
										final ExecutionContext<?> context = getExecutionContext(sessionId);
										return executeQueryInternal(sessionId, context, query);{//LocalExecutor.
											final Table table = createTable(context, context.getTableEnvironment(), query);{
												return context.wrapClassLoader(() -> tableEnv.sqlQuery(selectQuery));{
													//TableEnvironmentImpl.sqlQuery(selectQuery);
												}
											}
											final DynamicResult<C> result =resultStore.createResult();
											pipeline = context.createPipeline(jobName);
											final ProgramDeployer deployer =new ProgramDeployer(configuration, jobName, pipeline, context.getClassLoader());
											deployer.deploy().get();
											return new ResultDescriptor();
										}
									}
									if (resultDesc.isTableauMode()) {
										tableauResultView =new CliTableauResultView();
									}
								}
								break;
							case INSERT_INTO:
							case INSERT_OVERWRITE:
								callInsert(cmdCall);
								break;
							case CREATE_TABLE:
								callDdl(cmdCall.operands[0], CliStrings.MESSAGE_TABLE_CREATED);
								break;
						}
					}
				}
			}
		}
	}
}




TableEnvironmentImpl.sqlQuery(){
	ParserImpl.parse()
	SqlToOperationConverter.convert()
	FlinkPlannerImpl.validate(sqlNode: SqlNode, validator: FlinkCalciteSqlValidator){
		sqlNode.accept(new PreValidateReWriter(validator, typeFactory));
		sqlNode match { 
			case node: ExtendedSqlNode => node.validate()
			case _ =>
		}
		
		if (sqlNode.getKind.belongsTo(SqlKind.DDL) || sqlNode.getKind == SqlKind.INSERT ){
			return sqlNode
		}
		
		validator.validate(sqlNode);{//SqlValidatorImpl.validate()
			SqlValidatorImpl.validateScopedExpression()
			SqlSelect.validate()
			SqlValidatorImpl.validateQuery()
			SqlValidatorImpl.validateNamespace()
			AbstractNamespace.validate()
			IdentifierNamespace.validateImpl()
			IdentifierNamespace.resolveImpl()
			SqlValidatorImpl.newValidationError()
			SqlUtil.newContextException()
			
			
		}
		
	}
	
}


flink.table.api.internal.TableImpl.executeInsert(String tablePath, boolean overwrite){
	UnresolvedIdentifier unresolvedIdentifier =tableEnvironment.getParser().parseIdentifier(tablePath);
	ObjectIdentifier objectIdentifier =tableEnvironment.getCatalogManager().qualifyIdentifier(unresolvedIdentifier);
	ModifyOperation operation =new CatalogSinkModifyOperation();
	return tableEnvironment.executeInternal(Collections.singletonList(operation));{//TableEnvironmentImpl.executeInternal
		List<Transformation<?>> transformations = translate(operations);
		Pipeline pipeline = execEnv.createPipeline(transformations, tableConfig, jobName);
		JobClient jobClient = execEnv.executeAsync(pipeline);{//ExecutorBase.executeAsync()
			return executionEnvironment.executeAsync((StreamGraph) pipeline);{//StreamExecutionEnvironment.executeAsync()
				// ��ϸԴ��ο�����: 
				PipelineExecutorFactory executorFactory =executorServiceLoader.getExecutorFactory(configuration);
				jobClientFuture =executorFactory
					.getExecutor(configuration)
                    .execute(streamGraph, configuration, userClassloader);
				return jobClient;
			}
		}
	}
}


//flink-table-planner-blink_2.11-1.12.2.jar ������ calcite-core-1.26.0-jar 
// calcite-core-1.26.0 Դ��

SqlValidatorImpl.validateNamespace(){
	namespace.validate();{//AbstractNamespace[IdentifierNamespace].validate()
		switch (status) {
			case UNVALIDATED:
				status = SqlValidatorImpl.Status.IN_PROGRESS;
				RelDataType type = validateImpl();{//IdentifierNamespace.validateImpl()
					resolvedNamespace = Objects.requireNonNull(resolveImpl(id));{//IdentifierNamespace.resolveImpl()
						final SqlNameMatcher nameMatcher = validator.catalogReader.nameMatcher();
						ResolvedImpl resolved =new SqlValidatorScope.ResolvedImpl();
						try {
							parentScope.resolveTable(names, nameMatcher,SqlValidatorScope.Path.EMPTY, resolved);{// DelegatingScope.
								this.parent.resolveTable(names, nameMatcher, path, resolved);{// EmptyScope.resolveTable()
									final List<Resolve> resolves = ((ResolvedImpl) resolved).resolves;
									Iterator var7 = this.validator.catalogReader.getSchemaPaths().iterator();
									// �ؼ�������, ���й����� FlinkCalciteCatalogReader.schemaPaths ������ myhive.default�� ���õ����ݿ�; 
									List<List<String>> schemaPathList = validator.catalogReader.getSchemaPaths();{// 
										validator: FlinkCalciteSqlValidator ; 
										catalogReader: FlinkCalciteCatalogReader [extends CalciteCatalogReader]; {
											List<List<String>> schemaPaths;
											SqlNameMatcher nameMatcher;
										}
									}
									for (List<String> schemaPath : schemaPathList) {
										resolve_(validator.catalogReader.getRootSchema(), names, schemaPath,nameMatcher, path, resolved);{
											
										}
									}
								}
							}
						} catch (CyclicDefinitionException e) {
							if (e.depth == 1) { 
								throw validator.newValidationError(id,);
							}else{throw new CyclicDefinitionException(e.depth - 1, e.path);}
						}
					}
					if (resolved.count() == 1) {
						resolve = previousResolve = resolved.only();
						if (resolve.remainingNames.isEmpty()) {
							return resolve.namespace;
						}
					}
					// ��������, ���� �����resolved != 1, ������0,����>=2; 
					if (nameMatcher.isCaseSensitive()) {// FlinkSqlNameMatcher.isCaseSensitive()
						return this.baseMatcher.isCaseSensitive();{//FlinkSqlNameMatcher.BaseMatcher.isCaseSensitive()
							this.caseSensitive = caseSensitive;// caseSensitive=true;
						}
						SqlNameMatcher liberalMatcher = SqlNameMatchers.liberal();
						this.parentScope.resolveTable(names, liberalMatcher, Path.EMPTY, resolved);
						
					}
					
					// Failed to match.  If we're matching case-sensitively, try a more lenient match. If we find something we can offer a helpful hint.
					// ���������׳� Object 'tb_user' not found; 
					throw validator.newValidationError(id,RESOURCE.objectNotFound(id.getComponent(0).toString()));
				}
				setType(type);
				status = SqlValidatorImpl.Status.VALID;
				break;
			case IN_PROGRESS:
			  throw Util.newInternal("todo: Cycle detected during type-checking");
			case VALID:
			  break;
			default:
			  throw Util.unexpected(status);
		}
	}
	if (namespace.getNode() != null) {
		setValidatedNodeType(namespace.getNode(), namespace.getType());
    }
}



// select * from tb_user; �� Object 'tb_user' not found
/*
	SqlValidatorImpl.validate() -> SqlValidatorImpl.validateNamespace()
	IdentifierNamespace.resolveImpl() ��, �� parentScope.resolveTable() �޷�������id:'tb_user' ������ resolved��,���ջ�
	�����ߵ�����µ� throw validator.newValidationError(id,RESOURCE.objectNotFound(id.getComponent(0).toString()));
	- ԭ��Ӧ�þ���: ���е� resolveTable��Ҫ 'catlog.database.table'��ʽ, ����Ϊ�޷�����ǰ��� myhive.default,���±���; 
	
*/



/** Flink SQL & Table ��ʼ��
*
*/



//���� ��ѯ���TableFactory�Ĺ���:

StreamTableEnvironment.create(env)
    => StreamTableEnvironment.lookupExecutor()
        => TableFactoryService.findAll(factoryClass, propertyMap);


// 1. ��StreamTable������ʼ��ʱ, ��������е� TableFactory;
TableFactoryService.findAll(factoryClass, propertyMap);
    findAllInternal(factoryClass, propertyMap, Optional.empty());{
        List<TableFactory> tableFactories = discoverFactories(classLoader);
		return filter(tableFactories, factoryClass, properties);{
            List<T> contextFactories = filterByContext();
        }
    }

    TableFactoryService.findSingleInternal(){
        
    }
//# ����: ��ѯ�����˺���TableFactory�ĺ��Ĵ���:
    //ע��,findAllInternal() �� findSingleInternal() ���������´���;
find(){
    
    List<TableFactory> tableFactories = discoverFactories(classLoader);
    
	List<T> filtered = filter(tableFactories, factoryClass, properties);{//TableFactoryService.
        //  ���˳� TableFactory��ʵ����: �� HBase/CVS/ES/FS/Kafka�� Source/TableTableFactory;
        List<T> classFactories = filterByFactoryClass(factoryClass,properties,foundFactories);
        
        // ����contect-type? ���˳�����Ŀ������: CVS, Kafka ��;
        List<T> contextFactories = filterByContext(factoryClass,properties,classFactories);{//TableFactoryService.
            List<T> matchingFactories = new ArrayList<>();
            
            // �������� TableFactory����: �Ǵ������������?
            // ������KafkaTableSourceSinkFactory, Kafka010Table..; Kafka09Table.., CsvBatchTable, CsvAppendTableSinkFactory;
            
            for (T factory : classFactories) {
                Map<String, String> requestedContext = normalizeContext(factory);{
                    factory.requiredContext();// �ɲ�ͬfactoryʵ���� ���������� ����;
                    /* KafkaTable �������: connector.type, connector.version, connector.property-version;
                    *
                    */
                }
                
                // �Ƴ� xx.property-version ������;
                Map<String, String> plainContext = new HashMap<>(requestedContext);
                plainContext.remove(CONNECTOR_PROPERTY_VERSION);
                plainContext.remove(FORMAT_PROPERTY_VERSION);
                plainContext.remove(CATALOG_PROPERTY_VERSION);

                /* ����ÿ�� tableFactory�� ��������,�� with����������û�и� key(�� connector.type),��key��Ӧ��value����,����ӵ� miss & mismatch ����;
                *    ����: ���� KafkaTableFactory������connector.type-> kafka, ������sql with�ж����c.type= filesystem,��Ͳ�ƥ��,��ӵ� mismatch(�����);
                *   
                */
                // check if required context is met
                Map<String, Tuple2<String, String>> mismatchedProperties = new HashMap<>();
                Map<String, String> missingProperties = new HashMap<>();
                for (Map.Entry<String, String> e : plainContext.entrySet()) {
                    if (properties.containsKey(e.getKey())) {
                        String fromProperties = properties.get(e.getKey());
                        if (!Objects.equals(fromProperties, e.getValue())) {
                            mismatchedProperties.put(e.getKey(), new Tuple2<>(e.getValue(), fromProperties));
                        }
                    } else {
                        missingProperties.put(e.getKey(), e.getValue());
                    }
                }
                // matchedSize: ��factory����������, �۳�ȱʧ(��key��value����)��,���ڳɹ����ϵ���������; �����ƥ��4��,���withֻ��2��(key,value)��ȫƥ��;
                int matchedSize = plainContext.size() - mismatchedProperties.size() - missingProperties.size();
                if (matchedSize == plainContext.size()) {
                    matchingFactories.add(factory);
                } else {
                    if (bestMatched == null || matchedSize > bestMatched.matchedSize) {
                        bestMatched = new ContextBestMatched<>(
                                factory, matchedSize, mismatchedProperties, missingProperties);
                    }
                }
            }

            if (matchingFactories.isEmpty()) {
                String bestMatchedMessage = null;
                if (bestMatched != null && bestMatched.matchedSize > 0) {
                    StringBuilder builder = new StringBuilder();
                    builder.append(bestMatched.factory.getClass().getName());

                    if (bestMatched.missingProperties.size() > 0) {
                        builder.append("\nMissing properties:");
                        bestMatched.missingProperties.forEach((k, v) ->
                                builder.append("\n").append(k).append("=").append(v));
                    }

                    if (bestMatched.mismatchedProperties.size() > 0) {
                        builder.append("\nMismatched properties:");
                        bestMatched.mismatchedProperties
                            .entrySet()
                            .stream()
                            .filter(e -> e.getValue().f1 != null)
                            .forEach(e -> builder.append(
                                String.format(
                                    "\n'%s' expects '%s', but is '%s'",
                                    e.getKey(),
                                    e.getValue().f0,
                                    e.getValue().f1)));
                    }

                    bestMatchedMessage = builder.toString();
                }
                //noinspection unchecked
                throw new NoMatchingTableFactoryException(
                    "Required context properties mismatch.",
                    bestMatchedMessage,
                    factoryClass,
                    (List<TableFactory>) classFactories,
                    properties);
            }

            return matchingFactories;
        }
        
        // �жϸ�TableFactory���� �Ƿ�֧����ز���
        return filterBySupportedProperties();
    }
        
}

tableSource = TableFactoryUtil.findAndCreateTableSource(table);{
    return findAndCreateTableSource(table.toProperties());{
        return TableFactoryService
				.find(TableSourceFactory.class, properties){//TableFactoryService.find()
                    return findSingleInternal(factoryClass, propertyMap, Optional.empty());{
                        List<TableFactory> tableFactories = discoverFactories(classLoader);
                        
                        List<T> filtered = filter(tableFactories, factoryClass, properties);{
                            //  1. ���˳� TableFactory��ʵ����: �� HBase/CVS/ES/FS/Kafka�� Source/TableTableFactory;
                            List<T> classFactories = filterByFactoryClass(factoryClass,properties,foundFactories);{
                                
                            }
                            
                            // 2. ����contect-type? ���˳�����Ŀ������: CVS, Kafka ��;
                            List<T> contextFactories = filterByContext(factoryClass,properties,classFactories);{//TableFactoryService.
                                List<T> matchingFactories = new ArrayList<>();
                                // �������� TableFactory����: �Ǵ������������?������KafkaTableSourceSinkFactory, Kafka010Table..; Kafka09Table.., CsvBatchTable, CsvAppendTableSinkFactory;
                                for (T factory : classFactories) {
                                    // 1. factory�ı�������; ��TableFactory.requiredContext() ����ֵ.keySet();
                                    Map<String, String> requestedContext = normalizeContext(factory);
                                    // ��νplainContext���� ����������ȥ�� xx.property-version������; ������ֻ c.type,c.version��2������;
                                    Map<String, String> plainContext = new HashMap<>(requestedContext);
                                    plainContext.remove(CONNECTOR_PROPERTY_VERSION);//�Ƴ������е� connector.property-version
                                    
                                    //2. ����ÿ�� tableFactory�� ��������,�� with����������û�и� key(�� connector.type),��key��Ӧ��value����,����ӵ� miss & mismatch ����;
                                    Map<String, Tuple2<String, String>> mismatchedProperties = new HashMap<>();
                                    Map<String, String> missingProperties = new HashMap<>();
                                    for (Map.Entry<String, String> e : plainContext.entrySet()) {
                                        if (properties.containsKey(e.getKey())) {// factory.requestField ���� useDef.pros��,
                                            String fromProperties = properties.get(e.getKey());
                                            // 2.1 �Ƚ�factory��ƥ������Ե�ֵ(��type�Ƿ񶼵���kafka, version�Ƿ����0.10),�Ƿ����
                                            if (!Objects.equals(fromProperties, e.getValue())) {
                                                // �������ֶ��� ����������ƥ�䵫����ֵ����ȵļӵ� mismatched, ���ں��汨����ʾ?
                                                mismatchedProperties.put(e.getKey(), new Tuple2<>(e.getValue(), fromProperties));
                                            }
                                        } else {// ����factory��������,��useDef.props���޴����Ե�; �ӵ�missing��,��factory�϶����ϸ�;
                                            missingProperties.put(e.getKey(), e.getValue());
                                        }
                                    }
                                    // 3. plainContext:���������� key+value��ȫ��ȵ� ���: matchedSize; ֻҪ�б���������һȱʧ��value����,������matchingFactory;
                                    int matchedSize = plainContext.size() - mismatchedProperties.size() - missingProperties.size();
                                    if (matchedSize == plainContext.size()) {
                                        matchingFactories.add(factory); // ���������� key+value��ȫ��ȵ�factory, �żӵ� matchingFactories����;
                                    } else {
                                        if (bestMatched == null || matchedSize > bestMatched.matchedSize) {
                                            bestMatched = new ContextBestMatched<>(factory, matchedSize, mismatchedProperties, missingProperties);
                                        }
                                    }
                                }
                                if (matchingFactories.isEmpty()) { //һ��ƥ���ϵ� tableFactoryҲû��,���� NoMatchingTableFactoryException �쳣;
                                    String bestMatchedMessage = null;
                                    //noinspection unchecked
                                    throw new NoMatchingTableFactoryException("Required context properties mismatch.",
                                        bestMatchedMessage,factoryClass, (List<TableFactory>) classFactories, properties);
                                }
                                return matchingFactories;
                            }
                            
                            // 3. ��userDef.supportFields ��contextFacotry�����Support�ֶ�һһƥ��, ��У���û��������Ƿ�֧��; 
                            return filterBySupportedProperties(factoryClass, properties,classFactories,contextFactories);{//TableFactoryService.filterBySupportedProperties()
                                //3.1 ���û�Table.properties(schema+ �û���д)��schema.n.file�е������滻��#,������ plainGivenKeys: Set<key>
                                final List<String> plainGivenKeys = new LinkedList<>();
                                properties.keySet().forEach(k -> {
                                    String key = k.replaceAll(".\\d+", ".#");
                                });
                                // 3.2 ��(�û����õ�)���Զ���(��TableFactory.supported������)ƥ��� factory ,�ӵ� supportedFactories�����; 
                                List<T> supportedFactories = new LinkedList<>();
                                for (T factory: contextFactories) {
                                    // ��contextFactory�н��� required�����ֶ�; 
                                    Set<String> requiredContextKeys = normalizeContext(factory).keySet();
                                    // ��contextFactory�н��� supported ѡ���ֶ�; tuple2.f0Ϊ����ѡ���ֶ�; 
                                    Tuple2<List<String>, List<String>> tuple2 = normalizeSupportedProperties(factory);
                                    // givenFilteredKeys: ��ƽ���û�����(��)��(�Ǳ���) ѡ������; ���ڹ���table.supported�ֶ�?
                                    List<String> givenFilteredKeys = filterSupportedPropertiesFactorySpecific(factory, givenContextFreeKeys);
                                    boolean allTrue = true;
                                    List<String> unsupportedKeys = new ArrayList<>();
                                    for (String k : givenFilteredKeys) {
                                        // ��userDef.supportKeys�� contextFactory.supportFields ����ƥ��, �ҳ��κβ���ƥ��(����֧�ֵ�����)������name;
                                        if (!(tuple2.f0.contains(k) || tuple2.f1.stream().anyMatch(k::startsWith))) {
                                            allTrue = false; 
                                            unsupportedKeys.add(k);// ˵����userDef.prop Ϊ�Ƿ�����, ��ƥ��(���ڻ�ͨ��)��factory������supported�ֶ�
                                        }
                                    }
                                    if(allTrue){
                                        supportedFactories.add(factory);// ��factory�����û���������,���Ǳ�֧�ֵ�;
                                    }
                                }
                                return supportedFactories;
                            }
                        }
                    }
                }
				.createTableSource(properties);
    }
}

// 2. ����contect-type? ���˳�����Ŀ������: CVS, Kafka ��;
List<T> contextFactories = filterByContext(factoryClass,properties,classFactories);{//TableFactoryService.
    List<T> matchingFactories = new ArrayList<>();
    // �������� TableFactory����: �Ǵ������������?������KafkaTableSourceSinkFactory, Kafka010Table..; Kafka09Table.., CsvBatchTable, CsvAppendTableSinkFactory;
    for (T factory : classFactories) {
        // 1. factory�ı�������; ��TableFactory.requiredContext() ����ֵ.keySet();
        Map<String, String> requestedContext = normalizeContext(factory);
        // ��νplainContext���� ����������ȥ�� xx.property-version������; ������ֻ c.type,c.version��2������;
        Map<String, String> plainContext = new HashMap<>(requestedContext);
        plainContext.remove(CONNECTOR_PROPERTY_VERSION);//�Ƴ������е� connector.property-version
        
        //2. ����ÿ�� tableFactory�� ��������,�� with����������û�и� key(�� connector.type),��key��Ӧ��value����,����ӵ� miss & mismatch ����;
        Map<String, Tuple2<String, String>> mismatchedProperties = new HashMap<>();
        Map<String, String> missingProperties = new HashMap<>();
        for (Map.Entry<String, String> e : plainContext.entrySet()) {
            if (properties.containsKey(e.getKey())) {// factory.requestField ���� useDef.pros��,
                String fromProperties = properties.get(e.getKey());
                // 2.1 �Ƚ�factory��ƥ������Ե�ֵ(��type�Ƿ񶼵���kafka, version�Ƿ����0.10),�Ƿ����
                if (!Objects.equals(fromProperties, e.getValue())) {
                    // �������ֶ��� ����������ƥ�䵫����ֵ����ȵļӵ� mismatched, ���ں��汨����ʾ?
                    mismatchedProperties.put(e.getKey(), new Tuple2<>(e.getValue(), fromProperties));
                }
            } else {// ����factory��������,��useDef.props���޴����Ե�; �ӵ�missing��,��factory�϶����ϸ�;
                missingProperties.put(e.getKey(), e.getValue());
            }
        }
        // 3. plainContext:���������� key+value��ȫ��ȵ� ���: matchedSize; ֻҪ�б���������һȱʧ��value����,������matchingFactory;
        int matchedSize = plainContext.size() - mismatchedProperties.size() - missingProperties.size();
        if (matchedSize == plainContext.size()) {
            matchingFactories.add(factory); // ���������� key+value��ȫ��ȵ�factory, �żӵ� matchingFactories����;
        } else {
            if (bestMatched == null || matchedSize > bestMatched.matchedSize) {
                bestMatched = new ContextBestMatched<>(factory, matchedSize, mismatchedProperties, missingProperties);
            }
        }
    }
    if (matchingFactories.isEmpty()) { //һ��ƥ���ϵ� tableFactoryҲû��,���� NoMatchingTableFactoryException �쳣;
        String bestMatchedMessage = null;
        //noinspection unchecked
        throw new NoMatchingTableFactoryException("Required context properties mismatch.",
            bestMatchedMessage,factoryClass, (List<TableFactory>) classFactories, properties);
    }
    return matchingFactories;
}






