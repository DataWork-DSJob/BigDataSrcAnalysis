// xxģ��: xx ����: xx �߳�: 
// jar/coreClass: 
// �����߼�: 
// nextTh: 




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





// FlinkTable: xx ����: xx �߳�: 
// jar/coreClass: 
// �����߼�: 
// nextTh: 


Caused by: org.apache.calcite.sql.validate.SqlValidatorException: Object 'tb_user' not found
	at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method) ~[?:1.8.0_261]
	at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62) ~[?:1.8.0_261]
	at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45) ~[?:1.8.0_261]
	at java.lang.reflect.Constructor.newInstance(Constructor.java:423) ~[?:1.8.0_261]
	at org.apache.calcite.runtime.Resources$ExInstWithCause.ex(Resources.java:467) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.runtime.Resources$ExInst.ex(Resources.java:560) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.SqlUtil.newContextException(SqlUtil.java:883) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.SqlUtil.newContextException(SqlUtil.java:868) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.SqlValidatorImpl.newValidationError(SqlValidatorImpl.java:5043) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.IdentifierNamespace.resolveImpl(IdentifierNamespace.java:179) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.IdentifierNamespace.validateImpl(IdentifierNamespace.java:184) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.AbstractNamespace.validate(AbstractNamespace.java:84) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.SqlValidatorImpl.validateNamespace(SqlValidatorImpl.java:1067) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.SqlValidatorImpl.validateQuery(SqlValidatorImpl.java:1041) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.SqlValidatorImpl.validateFrom(SqlValidatorImpl.java:3205) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.SqlValidatorImpl.validateFrom(SqlValidatorImpl.java:3187) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.SqlValidatorImpl.validateSelect(SqlValidatorImpl.java:3461) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.SelectNamespace.validateImpl(SelectNamespace.java:60) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.AbstractNamespace.validate(AbstractNamespace.java:84) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.SqlValidatorImpl.validateNamespace(SqlValidatorImpl.java:1067) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.SqlValidatorImpl.validateQuery(SqlValidatorImpl.java:1041) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.SqlSelect.validate(SqlSelect.java:232) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.SqlValidatorImpl.validateScopedExpression(SqlValidatorImpl.java:1016) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.calcite.sql.validate.SqlValidatorImpl.validate(SqlValidatorImpl.java:724) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.flink.table.planner.calcite.FlinkPlannerImpl.org$apache$flink$table$planner$calcite$FlinkPlannerImpl$$validate(FlinkPlannerImpl.scala:147) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.flink.table.planner.calcite.FlinkPlannerImpl.validate(FlinkPlannerImpl.scala:111) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.flink.table.planner.operations.SqlToOperationConverter.convert(SqlToOperationConverter.java:189) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.flink.table.planner.delegation.ParserImpl.parse(ParserImpl.java:77) ~[flink-table-planner-blink_2.11-1.12.2.jar:1.12.2]
	at org.apache.flink.table.api.internal.TableEnvironmentImpl.sqlQuery(TableEnvironmentImpl.java:640) ~[flink-table-api-java-1.12.2.jar:1.12.2]
	at com.webank.wedatasphere.linkis.engineconnplugin.flink.client.sql.operation.impl.SelectOperation.lambda$createTable$4(SelectOperation.java:198) ~[linkis-engineconn-plugin-flink-1.0.2.jar:?]
	at com.webank.wedatasphere.linkis.engineconnplugin.flink.client.context.ExecutionContext.wrapClassLoader(ExecutionContext.java:175) ~[linkis-engineconn-plugin-flink-1.0.2.jar:?]
	at com.webank.wedatasphere.linkis.engineconnplugin.flink.client.sql.operation.impl.SelectOperation.createTable(SelectOperation.java:198) ~[linkis-engineconn-plugin-flink-1.0.2.jar:?]
	... 35 more





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
							if (e.depth == 1) { // ���������׳� "SqlValidatorException: Object 'tb_user' not found"
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



















