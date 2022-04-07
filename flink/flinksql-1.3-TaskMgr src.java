
/** fstx: Flink SQL TaskExecutor���� version-1.12.2 ��version-1.14.3 */

// fstx-1.0 Task������ run��ͣ; 

// flink-1.14.3 �汾��Դ�� 



StreamTask.restoreInternal(){
	
	CompletableFuture<Void> allGatesRecoveredFuture = actionExecutor.call(this::restoreGates);{
		StreamTask.restoreGates(){
			reader.readOutputData(getEnvironment().getAllWriters(), !configuration.isGraphContainingLoops());
			operatorChain.initializeStateAndOpenOperators(createStreamTaskStateInitializer());{//RegularOperatorChain.initializeStateAndOpenOperators
				StreamOperatorWrapper.ReadIterator<StreamOperatorWrapper> operators = getAllOperators(true);{ // reverse��ת,��β��ʼ��; 
					return reverse
						? new StreamOperatorWrapper.ReadIterator(tailOperatorWrapper, true)
						: new StreamOperatorWrapper.ReadIterator(mainOperatorWrapper, false);
				}
				// ����� reverse=true,��tail������ǰ(current.previous ) ����;
				// Դ��: for (StreamOperatorWrapper<?, ?> operatorWrapper : operators) {
				while(operators.hasNext()){ // this.current != null;
					StreamOperatorWrapper<?, ?> operatorWrapper = operators.next(){//StreamOperatorWrapper.ReadIterator.next()
						if (hasNext()) {
							StreamOperatorWrapper<?, ?> next = current;
							current = reverse ? current.previous : current.next;
							return next;
						}
					}
					
					// ����flinksql, ���α��� StreamSource, StreamFilter, StreamExecCalc,KeyedProcessOperator ; 
					StreamOperator<?> operator = operatorWrapper.getStreamOperator();
					operator.initializeState(streamTaskStateInitializer);
					operator.open();{
						
						StreamSource.open(){
							
						}
						
						StreamFilter.open(){
							
						}
						
						StreamExecCalc$21.open(){
							
						}
						
						KeyedProcessOperator.open(){
							
						}
						
					}
				}
			}
			
			channelIOExecutor.execute(() -> {reader.readInputData(inputGates);});
			
		}
	}
	
	mailboxProcessor.runMailboxLoop();
}
	
	// ����"Source: TableSourceScana" �߳���˵, ֻ��1��Operator: StreamSource ����;
	RegularOperatorChain.initializeStateAndOpenOperators(){
		StreamOperatorWrapper.ReadIterator<StreamOperatorWrapper> operators = getAllOperators(true);
		// ����flinksql, ���α��� StreamSource, StreamFilter, StreamExecCalc,KeyedProcessOperator ; 
		while(operators.hasNext()){ // this.current != null;
			StreamOperatorWrapper<?, ?> operatorWrapper = operators.next();
			StreamOperator<?> operator = operatorWrapper.getStreamOperator();
			operator.initializeState(streamTaskStateInitializer);
			operator.open();{
				StreamSource.open(){
					
				}
			}
		}
	}
	
	// �߳�"GroupAggregate(groupBy=xx)" �еĳ�ʼ�׶�: StreamFilter, StreamExecCalc,KeyedProcessOperator ; 
	RegularOperatorChain.initializeStateAndOpenOperators(){
		StreamOperatorWrapper.ReadIterator<StreamOperatorWrapper> operators = getAllOperators(true);
		// ����flinksql, ���α��� StreamSource, StreamFilter, StreamExecCalc,KeyedProcessOperator ; 
		while(operators.hasNext()){ // this.current != null;
			StreamOperatorWrapper<?, ?> operatorWrapper = operators.next();
			StreamOperator<?> operator = operatorWrapper.getStreamOperator();
			operator.initializeState(streamTaskStateInitializer);
			operator.open();{
				
				StreamFilter.open(){
					
				}
				
				StreamExecCalc$21.open(){
					
				}
				
				KeyedProcessOperator.open(){
					
				}
				
			}
		}
	}
	


	//���� SinkMeterializer -> Sinkxx �߳���˵, ��Ҫִ�� SinkUpertMeterializer, SinkOperator����;
	RegularOperatorChain.initializeStateAndOpenOperators(){
		StreamOperatorWrapper.ReadIterator<StreamOperatorWrapper> operators = getAllOperators(true);
		// ����flinksql, ���α��� SinkUpertMeterializer, SinkOperator����;
		while(operators.hasNext()){ // this.current != null;
			StreamOperatorWrapper<?, ?> operatorWrapper = operators.next();
			StreamOperator<?> operator = operatorWrapper.getStreamOperator();
			operator.initializeState(streamTaskStateInitializer);
			operator.open();{
				
				SinkUpertMeterializer.open(){
					
				}
				
				SinkOperator.open(){
					super.open();{//AbstractUdfStreamOperator.open()
						super.open();
						FunctionUtils.openFunction(userFunction, new Configuration());{
							if (function instanceof RichFunction) {
								richFunction.open(parameters);{
									// ����Jdbc����, ͨ�õ�Open ʵ��; Դ����� 3.2.1 
									GenericJdbcSinkFunction.open(){
										super.open(parameters);
										RuntimeContext ctx = getRuntimeContext();
										outputFormat.setRuntimeContext(ctx);
										
										outputFormat.open(ctx.getIndexOfThisSubtask(), ctx.getNumberOfParallelSubtasks());{//JdbcOutputFormat.open()
											connectionProvider.getOrEstablishConnection();
											jdbcStatementExecutor = createAndOpenStatementExecutor(statementExecutorFactory);
											this.scheduledFuture =this.scheduler.scheduleWithFixedDelay(()->flush(),);
										}
									}
									
								}
							}
						}
					}
					this.sinkContext = new SimpleContext(getProcessingTimeService());
				}
				
			}
		}
	}
	

		// "SinkMeterializer -> Sinkxx �߳�" ��Sink Operator��� Open 
		GenericJdbcSinkFunction.open(){
			super.open(parameters);
			RuntimeContext ctx = getRuntimeContext();
			outputFormat.setRuntimeContext(ctx);
			outputFormat.open(ctx.getIndexOfThisSubtask(), ctx.getNumberOfParallelSubtasks());{//JdbcOutputFormat.open
				connectionProvider.getOrEstablishConnection();
				
				jdbcStatementExecutor = createAndOpenStatementExecutor(statementExecutorFactory);{//JdbcOutputFormat.
					JdbcExec exec = statementExecutorFactory.apply(getRuntimeContext());{ //StatementExecutorFactory 
						
						//case 1: append sink: select * from tb_test;
						// �� JdbcOutputFormatBuilder.build() �ж���� StatementExecutorFactory �����ڲ���
						ctx -> createSimpleBufferedExecutor();{//JdbcOutputFormatBuilder.createSimpleBufferedExecutor()
							TypeSerializer<RowData> typeSerializer =rowDataTypeInfo.createSerializer(ctx.getExecutionConfig());
							 JdbcBatchStatementExecutor<RowData> statementExecutor =createSimpleRowExecutor(dialect, fieldNames, fieldTypes, sql);{//JdbcOutputFormatBuilder.createSimpleRowExecutor
								final JdbcRowConverter rowConverter = dialect.getRowConverter(RowType.of(fieldTypes));{
									MySQLDialect.getRowConverter(){
										return new MySQLRowConverter(rowType);{
											super(rowType);{//new AbstractJdbcRowConverter(), ��Ҫ�Ǹ���ÿ��field��type������Ӧ�� converter; 
												this.fieldTypes =rowType.getFields().stream().map(RowType.RowField::getType).toArray(LogicalType[]::new);
												this.toInternalConverters = new JdbcDeserializationConverter[rowType.getFieldCount()];
												this.toExternalConverters = new JdbcSerializationConverter[rowType.getFieldCount()];
												for (int i = 0; i < rowType.getFieldCount(); i++) {
													toInternalConverters[i] = createNullableInternalConverter(rowType.getTypeAt(i));
													// ��ÿ���ֶδ��� converter ת���� 
													toExternalConverters[i] = createNullableExternalConverter(fieldTypes[i]);{// AbstractJdbcRowConverter.createNullableExternalConverter
														JdbcSerializationConverter jdbcConverter= createExternalConverter(type);{//AbstractJdbcRowConverter.createExternalConverter()
															switch (type.getTypeRoot()) {
																case BOOLEAN:
																	return (val, index, statement) ->
																			statement.setBoolean(index, val.getBoolean(index));
																case TINYINT:
																	return (val, index, statement) -> statement.setByte(index, val.getByte(index));
																case SMALLINT:
																	return (val, index, statement) -> statement.setShort(index, val.getShort(index));
																case DOUBLE:
																	return (val, index, statement) -> statement.setDouble(index, val.getDouble(index));
																case CHAR:
																case VARCHAR:
																	// value is BinaryString
																	return (val, index, statement) -> statement.setString(index, val.getString(index).toString());
																case BINARY:
																case VARBINARY:
																	return (val, index, statement) -> statement.setBytes(index, val.getBinary(index));
																case DATE:
																	return (val, index, statement) ->statement.setDate(index, Date.valueOf(LocalDate.ofEpochDay(val.getInt(index))));
																case DECIMAL:
																	final int decimalPrecision = ((DecimalType) type).getPrecision();
																	final int decimalScale = ((DecimalType) type).getScale();
																	return (val, index, statement) ->statement.setBigDecimal(index,val.getDecimal(index, decimalPrecision, decimalScale).toBigDecimal());
																case ARRAY:case MAP:case MULTISET:case ROW:case RAW:
																default: throw new UnsupportedOperationException("Unsupported type:" + type);
															}
														}
														return wrapIntoNullableExternalConverter(jdbcConverter, type);
													}
												}
											}
										}
									}
									
									IotDBDialect.getRowConverter(RowType rowType){
										
									}
								}
								
								StatementFactory stmtFactory = (connection) -> FieldNamedPreparedStatement.prepareStatement(connection, sql, fieldNames);
								return new TableSimpleStatementExecutor(stmtFactory,rowConverter)
							 }
							 
							return new TableBufferedStatementExecutor(statementExecutor,valueTransform);
						}
						
						//flink-1.14.3 ��upsert jdbc sink; 
						// case 2: upsert sink: select id,count(*) from tb_test group by id; 
						createBufferReduceExecutor();{
							int[] pkFields = Arrays.stream(pkNames).mapToInt(Arrays.asList(opt.getFieldNames())::indexOf).toArray();
							TypeSerializer<RowData> typeSerializer =rowDataTypeInfo.createSerializer(ctx.getExecutionConfig());
							valueTransform =ctx.getExecutionConfig().isObjectReuseEnabled()? typeSerializer::copy: Function.identity();
							
							JdbcBatchStatementExecutor upsertExecutor = createUpsertRowExecutor();{//JdbcOutputFormatBuilder.createUpsertRowExecutor
								return dialect.getUpsertStatement(tableName, fieldNames, pkNames){
									MySQLDialect.getUpsertStatement(){
										//updateClause == `timestamp`=VALUES(`timestamp`), `status`=VALUES(`status`), `speed`=VALUES(`speed`), `temperature`=VALUES(`temperature`)
										String updateClause = Arrays.stream(fieldNames)
											.map(f -> quoteIdentifier(f) + "=VALUES(" + quoteIdentifier(f) + ")")
											.collect(Collectors.joining(", "));
										String statement = getInsertIntoStatement(tableName, fieldNames) + " ON DUPLICATE KEY UPDATE " + updateClause;
										// Insert��� + ON DUPLICATE KEY UPDATE + updateClause 
										//INSERT INTO `tb_device_upsert`(`timestamp`, `status`, `speed`, `temperature`) VALUES (:timestamp, :status, :speed, :temperature) ON DUPLICATE KEY UPDATE `timestamp`=VALUES(`timestamp`), `status`=VALUES(`status`), `speed`=VALUES(`speed`), `temperature`=VALUES(`temperature`)
										return Optional.of(statement);
									}
									
								}
									.map(sql -> createSimpleRowExecutor(dialect, fieldNames, fieldTypes, sql))
									.orElseGet(() -> createInsertOrUpdateExecutor());
									
							}
							JdbcBatchStatementExecutor deleteExecutor= createDeleteExecutor(dialect, tableName, pkNames, pkTypes);
							keyExtractor = createRowKeyExtractor(fieldTypes, pkFields);
							new TableBufferReducedStatementExecutor(stExecutor,deleteExecutor,keyExtractor);
						}
						
					}
					exec.prepareStatements(connectionProvider.getConnection());
					return exec;
				}
				
				if (executionOptions.getBatchIntervalMs() != 0 && executionOptions.getBatchSize() != 1) {
					this.scheduler =Executors.newScheduledThreadPool();
					this.scheduledFuture =this.scheduler.scheduleWithFixedDelay(()->flush(),
								executionOptions.getBatchIntervalMs(),
								executionOptions.getBatchIntervalMs(),
								TimeUnit.MILLISECONDS);
				}
			}
			
		}
		



	// TaskExecutor�� select, insert, upsert ���Դ��
	// 1.14.3 ������ TableBufferedStatementExecutor.buffer:List ��Record�Ļ��� addBatch(),������ executeBatch();


	// jdbc_TM_1.1 open ��ʼ���׶�: ���� dialectʾ������ �Ը�ÿ��field��type������Ӧ�� converter; 
	// ���ķ��� AbstractJdbcRowConverter.createExternalConverter(type)



	SourceStreamTask.LegacySourceFunctionThread.run(){
		if (!operatorChain.isTaskDeployedAsFinished()) {
			mainOperator.run(lock, operatorChain);{//StreamSource.run()
				run(lockingObject, output, operatorChain);{//StreamSource.run()
					 configuration =this.getContainingTask().getEnvironment().getTaskManagerInfo().getConfiguration();
					long latencyTrackingInterval =getExecutionConfig().isLatencyTrackingConfigured()
							? getExecutionConfig().getLatencyTrackingInterval()
							: configuration.getLong(MetricOptions.LATENCY_INTERVAL);
					
					long watermarkInterval =getRuntimeContext().getExecutionConfig().getAutoWatermarkInterval();
					
					this.ctx = StreamSourceContexts.getSourceContext(timeCharacteristic, watermarkInterval);
					userFunction.run(ctx);{// SourceFunction.run()
					   // flink kafka source 
					   FlinkKafkaConsumerBase.run()
					   
						// 2. jdbc ��ص� ����
						InputFormatSourceFunction.run(){
							if (isRunning && format instanceof RichInputFormat) {
								((RichInputFormat) format).openInputFormat();{// 
									JdbcRowDataInputFormat.openInputFormat(){
										Connection dbConn = connectionProvider.getOrEstablishConnection();
										if (autoCommit != null) {
											dbConn.setAutoCommit(autoCommit);
										}
										statement = dbConn.prepareStatement(queryTemplate, resultSetType, resultSetConcurrency);
										
									}
									
								}
							}
							OUT nextElement = serializer.createInstance();
							while (isRunning) {
								
								format.open(splitIterator.next());{
									//jdbc ���input ʵ��
									JdbcRowDataInputFormat.open(){
										if (inputSplit != null && parameterValues != null) {
											for (int i = 0; i < parameterValues[inputSplit.getSplitNumber()].length; i++) {
												Object param = parameterValues[inputSplit.getSplitNumber()][i];
												
											}
										}
										
										resultSet = statement.executeQuery();
										hasNext = resultSet.next();
									}
								}
								
								while (isRunning && !format.reachedEnd()) {
									
									nextElement = format.nextRecord(nextElement);{
										//jdbc ���input ʵ��
										JdbcRowDataInputFormat.nextRecord(reuse){
											if (!hasNext) {
												return null;
											}
											RowData row = rowConverter.toInternal(resultSet);
											hasNext = resultSet.next();
											return row;
										}
										// hdfs /hiveʵ��?
										
										
									}
									if (nextElement != null) {
										ctx.collect(nextElement);
									} else {
										break;
									}
								}
								format.close();
								completedSplitsCounter.inc();
								//��Ƭ���� �����Ƿ���next; 
								if (isRunning) {
									isRunning = splitIterator.hasNext();
								}
							}
							
						}
					
					}
				}
			}
		}
		completeProcessing();
		completionFuture.complete(null);
		headOperator.run(getCheckpointLock(), getStreamStatusMaintainer(), operatorChain);{//StreamSource
			run(lockingObject, streamStatusMaintainer, output, operatorChain);{

			}
		}
		completionFuture.complete(null);
	}


	// jdbc_TM_1.1.2  select from table ��jdbc source Դ������ 
	SourceStreamTask.LegacySourceFunctionThread
	




// fstx-2.0		Operator��open() ��ʼ���׶�: Source�ĳ�ʼ��, Operator�ĳ�ʼ��, Sink�ĳ�ʼ��; 


// fstx-3.0		Operator������ invoke: source,operator,sink�� ��ʼ��; 

	// 3.1 source�����ӵ� invoke; 
	

	// 3.2  userFunction.invoke(record) : ����ÿһ����Ϣ��
	// ���ķ���  
	SinkOperator.processElement(StreamRecord<RowData> element){
		userFunction.invoke(element.getValue(), sinkContext);{
			//jdbc ��ʵ����: GenericJdbcSinkFunction.invoke(value,context)
			GenericJdbcSinkFunction.invoke(value,context){
				outputFormat.writeRecord(value);{//JdbcOutputFormat.writeRecord()
					checkFlushException();
					In recordCopy = copyIfNecessary(record);
					//1. �Ȱ�һ����record�ŵ�����; addBatch
					JdbcIn jdbcRecord = jdbcRecordExtractor.apply(recordCopy);
					addToBatch(record, jdbcRecord);{//JdbcOutputFormat.addToBatch(original,extracted)
						jdbcStatementExecutor.addToBatch(extracted);{
							TableBufferedStatementExecutor.addToBatch(record){
								RowData value = valueTransform.apply(record); // copy or not
								buffer.add(value);// buffer: List<RowData>, ������ flush() -> executeBatch()�б��� buffer
							}
						}
					}
					
					batchCount++;
					// 2. ����ˢ���� �� batchCount > batchSize(Ĭ�� 500/20000?)ʱ,ˢ����db: executeBatch()
					if (executionOptions.getBatchSize() > 0&& batchCount >= executionOptions.getBatchSize()) {
						flush();{//JdbcOutputFormat.flush()
							for (int i = 0; i <= executionOptions.getMaxRetries(); i++) {
								attemptFlush();{//JdbcOutputFormat.attemptFlush()
									jdbcStatementExecutor.executeBatch();{
										
										// table/sql api��Ĭ�ϵ� buffִ���� ; 
										// PreparedStatement#executeBatch()may fail and clear buffered records, so we replay the records when retrying 
										TableBufferedStatementExecutor.executeBatch();{
											//�� buffer������Ԫ�ض� ps.addBatch()
											for (RowData value : buffer) {
												statementExecutor.addToBatch(value);{// TableSimpleStatementExecutor.addToBatch(record)
													converter.toExternal(record, st);{// AbstractJdbcRowConverter.toExternal(rowData,statement)
														for (int index = 0; index < rowData.getArity(); index++) {
															// ÿ���ֶε�ת����ִ��ת���� JdbcSerializationConverter.serialize()
															// ��open�׶ε� AbstractJdbcRowConverter.createExternalConverter(type)�����ж�����ÿ���ֶε� converter; 
															toExternalConverters[index].serialize(rowData, index, statement);{
																switch (type.getTypeRoot()) {
																	case TINYINT:
																		return (val, index, statement) -> statement.setByte(index, val.getByte(index));
																	case BIGINT: case INTERVAL_DAY_TIME:
																		return (val, index, statement) -> statement.setLong(index, val.getLong(index));
																	case CHAR:
																	case VARCHAR: // value is BinaryString
																		return (val, index, statement) -> statement.setString(index, val.getString(index).toString());
																}
															}
														}
														return statement;
													}
													st.addBatch();{//FieldNamedPreparedStatementImpl.addBatch()
														// java.sql.PreparedStatement �ӿ�ִ�� addBatch()
														statement.addBatch();{
															//mysql�� PSʵ����
															JDBC42PreparedStatement.addBatch()
															
															IoTDBPreparedStatement.addBatch();
															
														}
													}
												}
											}
											
											// ����ˢ����db;
											statementExecutor.executeBatch();{//TableSimpleStatementExecutor.executeBatch()
												st.executeBatch();{//FieldNamedPreparedStatementImpl.executeBatch()
													return statement.executeBatch();{//PreparedStatement.executeBatch() �ӿ�; 
														//mysql jdbc: 
														com.mysql.jdbc.StatementImpl.executeBatch()
														
													}
												}
											}
											buffer.clear();
										}
										
										// Insert or Upsert ��ص� 
										TableInsertOrUpdateStatementExecutor.executeBatch();
										
										TableBufferReducedStatementExecutor.executeBatch();
										
									}
								}
								batchCount = 0;
								break;
							}
						}
					}
				}
			}
		}
	}





// fstx-4.0		�����̺߳������߼�; 


// jdbc_TM_3.1  ��ʱ�߳�ˢ��,��ʱ���� buffer���ӵ� PreparedStatement�У������� ps.executeBatch() ��db;
	scheduler.scheduleWithFixedDelay(()->flush(),executionOptions.getBatchIntervalMs(),executionOptions.getBatchIntervalMs(),TimeUnit.MILLISECONDS);{
		JdbcOutputFormat.flush(){
			JdbcOutputFormat.attemptFlush();
				TableBufferedStatementExecutor.executeBatch();{
					for (RowData value : buffer) {
						statementExecutor.addToBatch(value);
					}
					statementExecutor.executeBatch();
					buffer.clear();
				}
		}
	}






















