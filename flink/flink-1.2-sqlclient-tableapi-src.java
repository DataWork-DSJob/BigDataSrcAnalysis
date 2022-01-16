


/** 3	FlinkSqlCli 
*
*/



// SqlClient������ TableEnvInit��ʼ���� CatalogManager����;
// client.start().openSession().build(): ExecutionContext.initializeTableEnvironment()��ʼ��Table������Դ, initializeCatalogs()������������Catalogs��curdb;
// A. client.start().open().parseCommand(line).sqlParser.parse(stmt): PlannerContext.createCatalogReader() ��CatalogManager��curCatalog/DB��ΪdefaultSchemas ��װ��FlinkCatalogReader;
// B. client.start().open().callCommand().callSelect(cmdCall):executor.executeQuery():tableEnv.sqlQuery(selectQuery) �ύTable��ѯ����: TableEnvironmentImpl.sqlQuery()

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
				return new ExecutionContext<>(this.sessionContext,this.sessionState,this.dependencies,,,); // Դ��������� ExecutionContext.Builder.build();
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
					// 1. �����û���ѯ������� Calcite����,������Ĭ�� curCatalog,curDB���� FlinkCatalogReader;
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
					// 2. �ύִ��sql Calcite����; 
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

	// ���廷�����ú� ִ�б�����Դ��
	ExecutionContext$Builder.build(){
		Environment curEvn = this.currentEnv == null? Environment.merge(defaultEnv, sessionContext.getSessionEnv()): this.currentEnv
		return new ExecutionContext<>(curEvn,this.sessionContext,this.sessionState,this.dependencies,,,);{//ExecutionContext()���캯��, ����һ�ѵ�ִ�л���;
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
										tableEnv.registerCatalog(name, catalog);{//TableEnvironmentImpl.registerCatalog
											catalogManager.registerCatalog(catalogName, catalog);{//CatalogManager.registerCatalog
												catalog.open();{// ��ͬ��catalog,��ͬ��ʵ��;
													HiveCatalog.open();
												}
												catalogs.put(catalogName, catalog);
											}
										}
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
			
			final CommandLine commandLine = createCommandLine(environment.getDeployment(), commandLineOptions);
			
			flinkConfig.addAll(createExecutionConfig(commandLine, commandLineOptions, availableCommandLines, dependencies));{//createExecutionConfig()
				final CustomCommandLine activeCommandLine =findActiveCommandLine(availableCommandLines, commandLine);
				Configuration executionConfig = activeCommandLine.toConfiguration(commandLine);{
					// ��ͬ����, Cli��ʵ�ֲ�һ��
					FlinkYarnSessionCli.toConfiguration(commandLine){
						final Configuration effectiveConfiguration = new Configuration();
						if (applicationId != null) {
							effectiveConfiguration.setString(HA_CLUSTER_ID, zooKeeperNamespace);
							effectiveConfiguration.setString(YarnConfigOptions.APPLICATION_ID, ConverterUtils.toString(applicationId));
							effectiveConfiguration.setString(DeploymentOptions.TARGET, YarnSessionClusterExecutor.NAME);
						}
						if (commandLine.hasOption(slots.getOpt())) {// -s or --slots
							effectiveConfiguration.setInteger(TaskManagerOptions.NUM_TASK_SLOTS,Integer.parseInt(commandLine.getOptionValue(slots.getOpt())));
						}
						if (isYarnPropertiesFileMode(commandLine)) {
							return applyYarnProperties(effectiveConfiguration);
						} else {
							return effectiveConfiguration;
						}
					}
					
				}
				LOG.info("Executor config: {}", executionConfig); // ��ӡ����־�������;
				return executionConfig;
			}
			
			clusterClientFactory = serviceLoader.getClusterClientFactory(flinkConfig);
			clusterSpec = clusterClientFactory.getClusterSpecification(flinkConfig);
			
		}
	}



// 3.2: CliClient.parseCommand() ����lineStr�� SqlNode->Operation,��validate()У��; 

SqlCommandParser.parse(Parser sqlParser, String stmt){
	Optional<SqlCommandCall> callOpt = parseByRegexMatching(stmt);
	if (callOpt.isPresent()) {
		return callOpt.get();
	}else{ // û������, ��������; 
		return parseBySqlParser(sqlParser, stmt);{//SqlCommandParser.parseBySqlParser
			operations = sqlParser.parse(stmt);{//LocalExecutor.Parser������.parse()
				return context.wrapClassLoader(() -> parser.parse(statement));{// ParserImpl.parse()
					CalciteParser parser = calciteParserSupplier.get();
					FlinkPlannerImpl planner = validatorSupplier.get();
					// 1. ������ SqlNode: ����˭? ��Ҫ�� calcite���
					SqlNode parsed = parser.parse(statement);
					
					// 2. ����SqlNode����Flink Operator����? 
					Optional<Operation> operationOp = SqlToOperationConverter.convert(planner, catalogManager, parsed);{
						SqlNode validated = flinkPlanner.validate(sqlNode);{// FlinkPlannerImpl.validate()
							// �� CatalogManager (����sql-client.yaml����)�� curCatalog,curDatabase ��Ϊ validatorУ����; 
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
							// ִ�� sql��֤; 
							validate(sqlNode, validator);{}
						}
						
						SqlToOperationConverter converter =new SqlToOperationConverter(flinkPlanner, catalogManager);
						
						if (validated instanceof SqlCreateCatalog) {
							return Optional.of(converter.convertCreateCatalog((SqlCreateCatalog) validated));
						} else if (validated instanceof RichSqlInsert) {
							// Insert�� ת�����; 
							return Optional.of(converter.convertSqlInsert((RichSqlInsert) validated));{
								// ���� catalog.db.tableName �� Id;
								ObjectIdentifier identifier = catalogManager.qualifyIdentifier(unresolvedIdentifier);
								
								PlannerQueryOperation query = SqlToOperationConverter.convert(flinkPlanner, insert.getSource()).orElseThrow();{
									// �ֽ��� SqlKind.QUERY ��query��ѯ����; ����ͬ�� converter.convertSqlQuery
									converter.convertSqlQuery(validated); 
								}
								return new CatalogSinkModifyOperation();
							}
						}else if (validated.getKind().belongsTo(SqlKind.QUERY)) {
							// ��ѯ�� ת�����; 
							return Optional.of(converter.convertSqlQuery(validated));{//SqlToOperationConverter.convertSqlQuery
								return toQueryOperation(flinkPlanner, node);{
									RelRoot relational = planner.rel(validated);{// FlinkPlannerImpl.rel()
										val sqlToRelConverter: SqlToRelConverter = createSqlToRelConverter(sqlValidator);
										sqlToRelConverter.convertQuery(validatedSqlNode, false, true);{
											// calcite ��������һ��ת���� 
											calcite.sql2rel.SqlToRelConverter.convertQuery()��
											// һ�ѵ� calcite convert ת��
											
											CatalogSourceTable.toRel(){
												final CatalogTable catalogTable = createFinalCatalogTable(context, hintedOptions);
												// 1. create and prepare table source
												DynamicTableSource tableSource = createDynamicTableSource(context, catalogTable);{
													return FactoryUtil.createTableSource();// table-commonԴ�룬 ����ϸ�� ���£� 
												}
												
												// 2. push table scan
												pushTableScan(relBuilder, cluster, catalogTable, tableSource, typeFactory, hints);
												
												// 4. push watermark assigner
												if (schemaTable.isStreamingMode() && !schema.getWatermarkSpecs().isEmpty()) {
													pushWatermarkAssigner(context, relBuilder, schema);
												}
												return relBuilder.build();
											}
										}
									}
									return new PlannerQueryOperation(relational.project());
								}
							}
						}
					}
					
					Operation operation =operationOp.orElseThrow(() -> new TableException("Unsupported query: " + statement));
					return Collections.singletonList(operation);
				}
			}
			return new SqlCommandCall(cmd, operands);
		}
	}
}






// 3.3 calCommand() : ִ��sql����/�ύ Operator��ҵ

CliClient.callCommand(){
	switch (cmdCall.command) {
		case SET:
			callSet(cmdCall);
			break;
		case SELECT:
			callSelect(cmdCall);{//CliClient.callSelect()
				// executeQuery()/executeInsert()/ executeDelete() �������Ƶ�����: 
				// Executor��3������: LocalExecutor, TestingExeutor, MockExecutor; 
				executor.executeXXX(){// LocalExecutor.executeXXX()
					// 1. ��ȡ������ ִ��������: ExecutionContext 
					final ExecutionContext<?> context = getExecutionContext(sessionId);
					return executeQueryInternal(sessionId, context, query);{//LocalExecutor.
						// 2. ׼����ص�Table Api 
						{
							// query/ select �������: 
							table = createTable(context, context.getTableEnvironment(), query);
							
							// insert / upsert�������
							applyUpdate(context, statement);
							//todo... 
						}
						
						// 3. ����һ��Pipeline; 
						pipeline = context.createPipeline(jobName);
						// 4. ���� Deployer���ύ���� deployer.deploy(); 
						final ProgramDeployer deployer =new ProgramDeployer(configuration, jobName, pipeline, context.getClassLoader());
						deployer.deploy().get();
						return new ResultDescriptor();
					}
				}
			}
			break;
		case INSERT_INTO:
		case INSERT_OVERWRITE:
			callInsert(cmdCall);{CliClient.callInsert()
				// executeQuery()/executeInsert()/ executeDelete() �������Ƶ�����: 
				// Executor��3������: LocalExecutor, TestingExeutor, MockExecutor; 
				executor.executeXXX(){// LocalExecutor.executeXXX()
					// 1. ��ȡ������ ִ��������: ExecutionContext 
					final ExecutionContext<?> context = getExecutionContext(sessionId);
					return executeQueryInternal(sessionId, context, query);{//LocalExecutor.
						// 2. ׼����ص�Table Api 
						{
							// query/ select �������: 
							table = createTable(context, context.getTableEnvironment(), query);
							
							// insert / upsert�������
							applyUpdate(context, statement);
							//todo... 
						}
						
						// 3. ����һ��Pipeline; 
						pipeline = context.createPipeline(jobName);
						// 4. ���� Deployer���ύ���� deployer.deploy(); 
						final ProgramDeployer deployer =new ProgramDeployer(configuration, jobName, pipeline, context.getClassLoader());
						deployer.deploy().get();
						return new ResultDescriptor();
					}
				}
			}
			break;
		case CREATE_TABLE:
			callDdl(cmdCall.operands[0], CliStrings.MESSAGE_TABLE_CREATED);
			break;
	}
}

	/** LocalExecutor.executeQuery()/ executeInsert() ���������Ƶ�ִ������: 
	*	1. ��ȡִ��������:  getExecutionContext();
	*	2. ׼����ص�Table Api : createTable(), applyUpdate(), 
	*	3. ����һ��Pipeline	:  context.createPipeline(jobName);
	*	4. ִ��һ���ύ����:  deployer.deploy(); 
	*/

	// 3.3.1 ִ�� select ���Ͳ�ѯ
	CliClient.callSelect(cmdCall);{
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
	
	

	// 3.3.2 ִ�� insert ���͵�sql���; 
	CliClient.callInsert(){
		ProgramTargetDescriptor programTarget=executor.executeUpdate(sessionId, cmdCall.operands[0]);{//LocalExecutor.executeUpdate
			final ExecutionContext<?> context = getExecutionContext(sessionId);
			return executeUpdateInternal(sessionId, context, statement);{//LocalExecutor.executeUpdateInternal
				applyUpdate(context, statement);
				Pipeline pipeline = context.createPipeline(jobName);
				ProgramDeployer deployer =new ProgramDeployer(configuration, jobName, pipeline, context.getClassLoader());
				// �����ύ 
				context.wrapClassLoader(()->{JobClient jobClient = deployer.deploy().get();})
			}
		}
		terminal.writer().println(programTarget.toString());//��ӡsql���?
		terminal.flush();
	}


	// 3.3.3 ����һ��Pipeline	:  context.createPipeline(jobName);

	ExecutionContext.createPipeline(jobName);{
		if (streamExecEnv != null) {// ���ȿ��ǲ��� Streamִ��ģʽ; 
			StreamTableEnvironmentImpl streamTableEnv =(StreamTableEnvironmentImpl) tableEnv;
			return streamTableEnv.getPipeline(name);{//StreamTableEnvironmentImpl.getPipeline()
				List<Transformation<?>> transformations = translateAndClearBuffer();{//TableEnvironmentImpl.
					transformations = translate(bufferedModifyOperations);{
						return planner.translate(modifyOperations);{// �� Sql / Table ת���� runtime stream Operator����; 
							// Planner�ӿ���2��ʵ����: StreamPlanner, BatchPlanner, ���̳����м�� ����������: PlannerBase
							
							StreamPlanner.translate(){// PlannerBase.translate() �����๫�� ת������; 
								// ����: ����һ�� �߼�tree; 
								val relNodes = modifyOperations.map(translateToRel);{// PlannerBase.translateToRel
									overrideEnvParallelism();
									modifyOperation match {
										case s: UnregisteredSinkModifyOperation[_] =>{}
										
										case s: SelectSinkOperation =>{}
										
										// ��insert select  ����������?
										case catalogSink: CatalogSinkModifyOperation =>{
											val input = getRelBuilder.queryOperation(modifyOperation.getChild).build();
											val sinkOption: Option[(CatalogTable, Any)] = getTableSink(identifier, dynamicOptions);{
												val lookupResult = catalogManager.getTable(objectIdentifier);
												lookupResult.map(_.getTable) match {
													case Some(table: ConnectorCatalogTable[_, _]) =>{}
													// ʲô�����, �� CatalogTable? 
													case Some(table: CatalogTable) => {
														val catalog = catalogManager.getCatalog(objectIdentifier.getCatalogName)
														val tableToFind = table
														val isTemporary = lookupResult.get.isTemporary
														// Checks whether the [[CatalogTable]] uses legacy connector sink options
														val isGoodConnector = isLegacyConnectorOptions(objectIdentifier, table, isTemporary);{
															val properties = new DescriptorProperties(true);
															// ����ʱ���ָ�� connector.type����, ֱ�ӷ���true ��ʾ�Ϸ�/Legacy�� Connector; 
															properties.putProperties(catalogTable.getOptions);
															if (properties.containsKey(ConnectorDescriptorValidator.CONNECTOR_TYPE)) { // ���ñ� connector.type �����Ƿ����; 
																true
															}else{
																val catalog = catalogManager.getCatalog(objectIdentifier.getCatalogName);
																try{
																	TableFactoryUtil.findAndCreateTableSink();
																	true
																}catch {
																	false
																}
															}
														}
														if ( isGoodConnector) {
															val tableSink = TableFactoryUtil.findAndCreateTableSink()
														}else{// ����ʱ��(dwd_orders_cate) ���� connector-type,Ҳû�� TableFactory ʵ����,�����ڷǷ�, ��������;
															val tableSink = FactoryUtil.createTableSink();{
																
															}
														}
													}
												}
											}
											
										}
										
										case (table, sink: DynamicTableSink) =>{DynamicSinkUtils.toRel(getRelBuilder, input, catalogSink, sink, table);}
									}
								}
								// ����: ִ���Ż�;
								val optimizedRelNodes = optimize(relNodes)
								val execNodes = translateToExecNodePlan(optimizedRelNodes)
								// ����: ת���� ִ�мƻ�; 
								translateToPlan(execNodes);{// StreamPlanner.translateToPlan()
									planner.overrideEnvParallelism();
									execNodes.map {case node: StreamExecNode[_] => node.translateToPlan(planner){ //ExecNode.translateToPlan
										if (transformation == null) {// ��Ա����, ÿ��ExecNodeֻ��1�� Transformation
											transformation = translateToPlanInternal(planner);{
												
												StreamExecLegacySink.translateToPlanInternal(){
													val resultTransformation = sink match {
														// streaming ģʽ�� ����;
														case streamTableSink: StreamTableSink[T] =>{
															val transformation = streamTableSink match {
																case _: RetractStreamTableSink[T] => translateToTransformation(withChangeFlag = true, planner);
																
																case upsertSink: UpsertStreamTableSink[T] =>{
																	val isAppendOnlyTable = ChangelogPlanUtils.inputInsertOnly(this)
																	upsertSink.setIsAppendOnly(isAppendOnlyTable);
																	UpdatingPlanChecker.getUniqueKeyForUpsertSink(this, planner, upsertSink){
																		val sinkFieldNames = sink.getTableSchema.getFieldNames;
																		 val fmq: FlinkRelMetadataQuery = FlinkRelMetadataQuery.reuseOrCreate(planner.getRelBuilder.getCluster.getMetadataQuery);
																		 // ��ѯ����, û������; 
																		 val uniqueKeys = fmq.getUniqueKeys(sinkNode.getInput);
																		 if (uniqueKeys != null && uniqueKeys.size() > 0) {
																			 uniqueKeys
																				  .filter(_.nonEmpty).map(_.toArray.map(sinkFieldNames))
																				  .toSeq.sortBy(_.length).headOption
																		 }else {Nonde };
																	} match { 
																		case Some(keys) => upsertSink.setKeyFields(keys);
																		case None if isAppendOnlyTable => upsertSink.setKeyFields(null);
																		// ���쳣�������׳�, upsertSink����Ҫ������; 
																		case None if !isAppendOnlyTable => throw new TableException("UpsertStreamTableSink requires that Table has a full primary keys if it is updated.")
																	}
																	translateToTransformation(withChangeFlag = true, planner);
																}
																
																case _: AppendStreamTableSink[T] =>{
																	
																}
															}
															val dataStream = new DataStream(planner.getExecEnv, transformation)
															val dsSink = streamTableSink.consumeDataStream(dataStream)
															dsSink.getTransformation
														}
														// batch ģʽ������? 
														case dsTableSink: DataStreamTableSink[_] =>{}
													}
													
													resultTransformation.asInstanceOf[Transformation[Any]]
												}
												
											}
										}
										transformation
									}}
								}
							}
							
							BatchPlanner.translate()
							
							
						}
					}
					bufferedModifyOperations.clear();
				}
				return execEnv.createPipeline(transformations, tableConfig, jobName);{// Executor.createPipeline()
					// execution.type=batch ģʽsetBatchProperties(), ��� checkpoing�ÿ�;  
					BatchExecutor.createPipeline(){
						StreamExecutionEnvironment execEnv = getExecutionEnvironment();
						ExecutorUtils.setBatchProperties(execEnv, tableConfig);
						StreamGraph streamGraph = ExecutorUtils.generateStreamGraph(execEnv, transformations);
						
						ExecutorUtils.setBatchProperties(streamGraph, tableConfig);{
							streamGraph.getStreamNodes().forEach(sn -> sn.setResources(ResourceSpec.UNKNOWN, ResourceSpec.UNKNOWN));
							streamGraph.setScheduleMode(ScheduleMode.LAZY_FROM_SOURCES_WITH_BATCH_SLOT_REQUEST);
							streamGraph.setStateBackend(null);
							if (streamGraph.getCheckpointConfig().isCheckpointingEnabled(){
								return checkpointInterval > 0;// execution.checkpointing.interval �Ƿ�>0 �ж��Ƿ���ckp; 
							}) {
								throw new IllegalArgumentException("Checkpoint is not supported for batch jobs.");
							}
							streamGraph.setGlobalDataExchangeMode(getGlobalDataExchangeMode(tableConfig));
						}
					}
					
					StreamExecutor.createPipeline(){
						StreamGraph streamGraph =ExecutorUtils.generateStreamGraph(getExecutionEnvironment(), transformations);
						return streamGraph;
					}
				}
			}
		}else{
			BatchTableEnvironmentImpl batchTableEnv=(BatchTableEnvironmentImpl) tableEnv;
			return batchTableEnv.getPipeline(name);
		}
	}


	// 3.3.4  set�﷨
	CliClient.callSet(){
		if (cmdCall.operands.length == 0) {
			Map<String, String> properties = executor.getSessionProperties(sessionId);
			terminal.writer().println(CliStrings.messageInfo(CliStrings.MESSAGE_EMPTY).toAnsi());
		}else{
			executor.setSessionProperty(sessionId, cmdCall.operands[0], cmdCall.operands[1].trim());{
				Environment newEnv = Environment.enrich(env, Collections.singletonMap(key, value));
				ExecutionContext<?> newContext =createExecutionContextBuilder(context.getOriginalSessionContext())
                        .env(newEnv)
                        .sessionState(context.getSessionState())
                        .build();// Դ��μ������ ExecutionContext.Builder.build();
				//  LocalExecutor.contextMap: Map<String, ExecutionContext<?>> �洢����Ӧ�Ļ�������; 
				this.contextMap.put(sessionId, newContext);
			}
		}
	}
	
createExecutionConfig:363, ExecutionContext (org.apache.flink.table.client.gateway.local)
<init>:194, ExecutionContext (org.apache.flink.table.client.gateway.local)
<init>:138, ExecutionContext (org.apache.flink.table.client.gateway.local)
build:961, ExecutionContext$Builder (org.apache.flink.table.client.gateway.local)
setSessionProperty:306, LocalExecutor (org.apache.flink.table.client.gateway.local)
callSet:433, CliClient (org.apache.flink.table.client.cli)



	
	// ���� connector-type����Ӧ TableFactory�����ñ�������(Sink/Source)������? 
	TableFactoryUtil.findAndCreateTableSink(catalog,identifier,catalogTable,isStreamingMode,isTemporary);{
		TableSinkFactory.Context context =new TableSinkFactoryContextImpl();
		if (catalog == null) {
			return findAndCreateTableSink(context);
		} else {
			Optional<TableSink> sinkOp = createTableSinkForCatalogTable(catalog, context);{
				// GenericInMemoryCatalog
				TableFactory tableFactory = catalog.getTableFactory().orElse(null);{
					// default_catalogʵ���� GenericInMemoryCatalog, ���ṩ�� TableFactoryΪ��; 
					GenericInMemoryCatalog.getTableFactory(){
						return Optional.empty();
					}
					// Hive��Catalog
					HiveCatalog.getTableFactory();
					
					// JDBC
					JDBCCatalog.getTableFactory();
					
				}
				if (tableFactory instanceof TableSinkFactory) {
					return ((TableSinkFactory) tableFactory).createTableSink(context);{
						
					}
				}
				return Optional.empty();
			}
			return sinkOp.orElseGet(() -> findAndCreateTableSink(context));
		}
	}

	// ���� catalog��"connector"�ֶβ� TableFactory,������ Sink��; 
	TableUtil.createTableSink(){
		// ����Ĭ�ϵ�ʵ����: DefaultDynamicTableContext
		DefaultDynamicTableContext context = new DefaultDynamicTableContext();
		DynamicTableSinkFactory factory = getDynamicTableFactory(DynamicTableSinkFactory.class, catalog, context);{
			if (catalog != null) {
				Factory factory =catalog.getFactory()
								.filter(f -> factoryClass.isAssignableFrom(f.getClass()))
								.orElse(null);
				if (factory != null) { return (T) factory;}
			}
			// �� DynamicTableFactory.Context.CatalogTable.Properties ��, ��ȡ"connector" �ֶ�ֵ: �� hive, mysql-cdc,jdbc,kafka��; ; 
			final String connectorOption = context.getCatalogTable().getOptions().get(CONNECTOR.key());
			if (connectorOption == null) { 
				// ������������Ĵ���; 
				throw new ValidationException( "Table options do not contain an option key '%s' for discovering a connector.");
			}
			return discoverFactory(context.getClassLoader(), factoryClass, connectorOption);
		}
		return factory.createDynamicTableSink(context);
	}

	FactoryUtil��createTableSource(){
		DefaultDynamicTableContext context = new DefaultDynamicTableContext();
		DynamicTableSourceFactory factory = getDynamicTableFactory(DynamicTableSourceFactory.class, catalog, context);{
			final String connectorOption = context.getCatalogTable().getOptions().get(CONNECTOR.key());
			if (connectorOption == null) { 
				// ������������Ĵ���; 
				throw new ValidationException( "Table options do not contain an option key '%s' for discovering a connector.");
			}
			return discoverFactory(context.getClassLoader(), factoryClass, connectorOption);
		}
		return factory.createDynamicTableSource(context);
	}


// HiveCatalog.open()
HiveCatalog.open(){
	client = HiveMetastoreClientFactory.create(hiveConf, hiveVersion);{return new HiveMetastoreClientWrapper(hiveConf, hiveVersion);{
		this.hiveConf = Preconditions.checkNotNull(hiveConf, "HiveConf cannot be null");
		
		hiveShim = HiveShimLoader.loadHiveShim(hiveVersion);
		
		// ��Ϊhive.metastore.uris�ǿ�,���Խ������� newSynchronizedClient(createMetastoreClient());
		boolean isEmbeddedMeta = HiveCatalog.isEmbeddedMetastore(hiveConf);{
			// ��� hive.metastore.uris == null,empty,�հ�,����true,��ʾ ��ʹ���ⲿMetastore Service,ʹ�� Ƕ��ʽ Embedded Metastore;
			return isNullOrWhitespaceOnly(hiveConf.getVar(HiveConf.ConfVars.METASTOREURIS));{ // hive.metastore.uris
				if (str == null || str.length() == 0) {
					return true;
				}
				return true;
			}
		}
		client = isEmbeddedMeta? createMetastoreClient(): HiveMetaStoreClient.newSynchronizedClient(createMetastoreClient());{
			HiveMetastoreClientWrapper.createMetastoreClient(){
				return hiveShim.getHiveMetastoreClient(hiveConf);{
					// ����ִ�� RetryingMetaStoreClient.getProxy()
					return (IMetaStoreClient) method.invoke(null, (hiveConf));{// RetryingMetaStoreClient.getProxy()
						// org.apache.hadoop.hive.metastore.HiveMetaStoreClient
						Class<? extends IMetaStoreClient> baseClass = (Class<? extends IMetaStoreClient>) MetaStoreUtils.getClass(mscClassName);
						RetryingMetaStoreClient handler =new RetryingMetaStoreClient(hiveConf, constructorArgTypes, constructorArgs, metaCallTimeMap, baseClass);{
							String msUri = hiveConf.getVar(HiveConf.ConfVars.METASTOREURIS);//��ȡmetastore.urisֵ: thrift://bdnode102:9083
							this.base = (IMetaStoreClient) MetaStoreUtils.newInstance(msClientClass, constructorArgTypes, constructorArgs);{
								// ���乹�� HiveMetaStoreClient����
								new HiveMetaStoreClient(){
									localMetaStore = HiveConfUtil.isEmbeddedMetaStore(msUri);
									retries = HiveConf.getIntVar(conf, HiveConf.ConfVars.METASTORETHRIFTCONNECTIONRETRIES);
									metastoreUris = new URI[metastoreUrisString.length];
									for (String s : metastoreUrisString) {
										metastoreUris[i++] = tmpUri;
									}
									open();{//HiveMetaStoreClient.open
										for (URI store : metastoreUris) {
											transport = new TSocket(store.getHost(), store.getPort(), clientSocketTimeout);
											client = new ThriftHiveMetastore.Client(protocol);
											transport.open();
											UserGroupInformation ugi = Utils.getUGI();
											String[] groups = ugi.getGroupNames();{//Groups.getGroupNames() -> ShellBasedUnixGroupsMapping.getUnixGroups(user)
												String[] cmd= Shell.getGroupsForUserCommand(user);{//org.apache.hadoop.util.Shell getGroupsForUserCommand()
													//��Ϊwinû�� WINUTILS ����, ��������=null; 
													return (WINDOWS)? new String[] { WINUTILS, "groups", "-F", "\"" + user + "\""}: new String [] {"bash", "-c", "id -Gn " + user};
												}
												result = Shell.execCommand(cmd);
											}
											client.set_ugi(ugi.getUserName(), Arrays.asList(groups));
										}
									}
								}
								
							}
						}
						return (IMetaStoreClient) Proxy.newProxyInstance(RetryingMetaStoreClient.class.getClassLoader(), baseClass.getInterfaces(), handler);
					}
				}
			}
		}
		
	}}
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



apache.calcite.rel.metadata.RelMetadataQuery.getUniqueKeys(RelNode rel){
	return getUniqueKeys(rel, false);{
		try {
			return uniqueKeysHandler.getUniqueKeys(rel, this, ignoreNulls);{
				{ // ���캯�� ���� uniqueKeysHandler
					this.uniqueKeysHandler = initialHandler(BuiltInMetadata.UniqueKeys.Handler.class);{
						return handlerClass.cast(Proxy.newProxyInstance(RelMetadataQuery.class.getClassLoader(),
								new Class[] {handlerClass}, (proxy, method, args) -> {
								  // r��ʾ��: StreamExecCalc
								  final RelNode r = (RelNode) args[0];
								  throw new JaninoRelMetadataProvider.NoHandler(r.getClass());
								}));
					}
				}
			}
		} catch (JaninoRelMetadataProvider.NoHandler e) {
			uniqueKeysHandler =revise(e.relClass, BuiltInMetadata.UniqueKeys.DEF);
		}
	}
}

// ��ͬ sql������ʵ��
ExecNode.translateToPlanInternal(){
	// select * from ����;
	StreamExecTableSourceScan.translateToPlanInternal(){
		createSourceTransformation(planner.getExecEnv, getRelDetailedDescription);{//CommonPhysicalTableSourceScan
			val runtimeProvider = tableSource.getScanRuntimeProvider(ScanRuntimeProviderContext.INSTANCE)
			runtimeProvider match {
				case provider: SourceFunctionProvider =>{}
				
				case provider: InputFormatProvider => {}
				
				case provider: DataStreamScanProvider => {
					provider
						.produceDataStream(env){// provider: DataStreamScanProvider ���ʵ����
							// hive table��ʵ����: HiveTableSource
							HiveTableSource.produceDataStream(){
								return getDataStream(execEnv); // ������� HiveTableSource.produceDataStream() Դ��;
							}
						}
						.getTransformation(){};
				}
			}
		}
	}
	
}


	// Hive Source Table�� ������ʼ�ʹ���Դ�� 
	HiveTableSource.produceDataStream(){
		return getDataStream(execEnv);{// HiveTableSource.getDataStream()
			validateScanConfigurations();
			// ��ȡ���� hive �ķ���? ������92�� 
			List<HiveTablePartition> allHivePartitions =getAllPartitions(jobConf, hiveVersion, remainingPartitions);
			HiveSource.HiveSourceBuilder sourceBuilder =new HiveSource.HiveSourceBuilder(allHivePartitions,limit,hiveVersion,
				flinkConf.get(HiveOptions.TABLE_EXEC_HIVE_FALLBACK_MAPRED_READER),// table.exec.hive.fallback-mapred-reader
				(RowType) getProducedDataType().getLogicalType());
			DataStreamSource<RowData> source =
			execEnv.fromSource(hiveSource, WatermarkStrategy.noWatermarks(), "HiveSource-" + tablePath.getFullName());
			boolean isStreamSource = isStreamingSource(){ // streaming-source.enable=true ��ʾ����;
				return Boolean.parseBoolean(catalogTable.getOptions().getOrDefault(
								STREAMING_SOURCE_ENABLE.key(),// streaming-source.enable ����ʱָ��,Ĭ��false
								STREAMING_SOURCE_ENABLE.defaultValue().toString())); //false 
			}
			if (isStreamSource) {
				return source;
			}else{
				HiveParallelismInference hiveInfer = new HiveParallelismInference(tablePath, flinkConf){ // inferred ����,�Ƿ����� splits��Ƭ���ƶ�;
					this.infer = flinkConf.get(HiveOptions.TABLE_EXEC_HIVE_INFER_SOURCE_PARALLELISM);//table.exec.hive.infer-source-parallelism
					this.inferMaxParallelism = flinkConf.get(HiveOptions.TABLE_EXEC_HIVE_INFER_SOURCE_PARALLELISM_MAX);// table.exec.hive.infer-source-parallelism.max
					this.parallelism =flinkConf.get(ExecutionConfigOptions.TABLE_EXEC_RESOURCE_DEFAULT_PARALLELISM); //table.exec.resource.default-parallelism
				};
				hiveInfer.infer(
					() ->HiveSourceFileEnumerator.getNumFiles(allHivePartitions, jobConf),
					() -> HiveSourceFileEnumerator.createInputSplits(0, allHivePartitions, jobConf).size());{//HiveParallelismInference.infer()
						if (!infer) {//(Ĭ�ϲ���) ������� hive.infer-source-parallelism=falseʱ(Ĭ��true����); ��infer�ƶ�
							return this;
						}
						int lowerBound = logRunningTime("getNumFiles", numFiles);// numFiles���� hive�ķ�Ƭ/�ļ�����;
						if (lowerBound >= inferMaxParallelism) {
							parallelism = inferMaxParallelism;// ��������� ������ table.exec.hive.infer-source-parallelism.max(Ĭ��1000)
							return this;
						}
						int splitNum = logRunningTime("createInputSplits", numSplits); // 733?
						parallelism = Math.min(splitNum, inferMaxParallelism);
				}
				hiveInfer.limit(limit);{//HiveParallelismInference.limit()
					if (limit != null) {// select * limit xxx �е� limit ��������;
						parallelism = Math.min(parallelism, (int) (limit / 1000));
					}
					return Math.max(1, parallelism);// һ��������沢���� parallelism=> splitNum 
				}
				return source.setParallelism(parallelism);
			}
		}
	}


