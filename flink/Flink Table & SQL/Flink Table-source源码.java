

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





































