
// 1. �ύ�̣߳� ��main"�̣߳� ���ķ���: JobSubmitter.submitJobInternal()


UserDriver.main(){
    
    job.waitForCompletion(true){//Job: 
        if (state == JobState.DEFINE) {
            submit();{
                setUseNewAPI();//�����µ�Api
                connect();{//����Cluster����,���ڱ�ʾ��Ŀ��FileSystem����������;
                    if (cluster == null) {
                        
                        /*
                        *  ����Cluster����, ��Ҫ�Ƿ�װ��clientProtocol :ClientProtocol =  LocalJobRunner();
                        */
                         cluster =  ugi.doAs(new PrivilegedExceptionAction<Cluster>() { 
                            return new Cluster(getConfiguration());{
                                this(null, conf);{
                                    this.conf = conf;
                                    this.ugi = UserGroupInformation.getCurrentUser();
                                    initialize(jobTrackAddr, conf);{
                                        initProviderList();
                                        // providerList �������2��Provider��ʵ����: LocalClientProtocolProvider , YarnClientProtocolProvider
                                        for (ClientProtocolProvider provider : providerList) {
                                            if (jobTrackAddr == null) {
                                                clientProtocol = provider.create(conf);{
                                                    // ClientProtocolProvider��ʵ����: Localͨ�ŵ�Provider
                                                    LocalClientProtocolProvider.create(Configuration conf){
                                                        conf.setInt(JobContext.NUM_MAPS, 1);
                                                        return new LocalJobRunner(conf); 
                                                    }
                                                }
                                            }
                                            if (clientProtocol != null) {
                                                clientProtocolProvider = provider;
                                                client = clientProtocol;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                         });
                    }
                }
                // ����JobSubmitter , ��Ҫ�Ƿ�װ��: jtFs:Ŀ���ļ�ϵͳ(����/hdfs), submitClient(ͨ�ſͻ���), hostAndPort: ��ַ�˿�;
                final JobSubmitter submitter = getJobSubmitter(cluster.getFileSystem(), cluster.getClient());
                
                status = ugi.doAs(new PrivilegedExceptionAction<JobStatus>() {
                    run(){return submitter.submitJobInternal(Job.this, cluster);{//JobSubmitter.submitJobInternal()
                        // У��Jobs �������ʽ�ͺϷ���;
                        checkSpecs(job);
                        
                        //����(��ϣ)�㷨�������� submitJobDirĿ¼,�����ռ�/���/�ύ Job�������ú���Դ;
                        Path jobStagingArea = JobSubmissionFiles.getStagingDir(cluster, conf);{
                            client.getStagingAreaDir();{
                                Path stagingRootDir = new Path(conf.get(JTConfig.JT_STAGING_AREA_ROOT, "/tmp/hadoop/mapred/staging"));
                                user = ugi.getShortUserName() + rand.nextInt(Integer.MAX_VALUE);//username-randid;
                                return fs.makeQualified(new Path(stagingRootDir, user+"/.staging")).toString();
                            }
                        }
                        JobID jobId = submitClient.getNewJobID();{//LocalJobRunner.getNewJobID(): ��local+����rand.nextInt(MAX)���ɵ������+0/n, ��ΪjobId;
                            return new org.apache.hadoop.mapreduce.JobID("local" + randid, ++jobid);
                        }
                        Path submitJobDir = new Path(jobStagingArea, jobId.toString());
                        
                        try{
                            copyAndConfigureFiles(job, submitJobDir);//���������submitJobDirĿ¼;
                            
                            /* ���Ĵ���: ����file�ļ�����, �������MR����ķ�Ƭ/����: splits
                            *
                            */
                            int maps = writeSplits(job, submitJobDir);{//JobSubmitter.writeSplits()
                                if (jConf.getUseNewMapper()) {//mapred.mapper.new-api==true����ʱ,���µ�api
                                    maps = writeNewSplits(job, jobSubmitDir);{
                                        List<InputSplit> splits = input.getSplits(job);{//TextInputFormat ���ø���FileInputFormat.getSplits()
                                            List<FileStatus> files = listStatus(job);{
                                                Path[] dirs = getInputPaths(job);{//FileInputFormat.getInputPaths()
                                                    String dirs = context.getConfiguration().get(INPUT_DIR, "");// FileInputFormat.setInputPaths() => mapreduce.input.fileinputformat.inputdir (INPUT_DIR) ,������Input·��;
                                                    Path[] result = new Path[list.length];
                                                    return result;
                                                }
                                                
                                                // �����Ƿ�ݹ�,�ݹ��ȡ���ļ�
                                                boolean recursive = getInputDirRecursive(job);//INPUT_DIR_RECURSIVE����(mapreduce.input.fileinputformat.input.dir.recursive)�趨
                                            }
                                        }
                                        
                                        T[] array = (T[]) splits.toArray(new InputSplit[splits.size()]);
                                        Arrays.sort(array, new SplitComparator()); //��װInputSplit.length��������,size��Ŀ�ǰ����;
                                        
                                        // ������jobSubmitDirĿ¼д�� [job.split,job.splitmetainfo]�������ļ�;
                                        JobSplitWriter.createSplitFiles(jobSubmitDir, conf, jobSubmitDir.getFileSystem(conf), array);
                                        return array.length;
                                    }
                                }
                            }
                            conf.setInt(MRJobConfig.NUM_MAPS, maps);//��Splits��Ƭ����,д��mapreduce.job.maps������;
                            
                            // ��conf:Configuration ������Ϣд�뵽submitJobDir��ʱĿ¼�µ�job.xml �ļ���;
                            writeConf(conf, submitJobFile);{
                                // ��jobSubmitDir�����job.xml,
                                /tmp/hadoop-86177/mapred/staging/861771675066360/.staging/job_local1675066360_0001/job.xml
                                FSDataOutputStream out = FileSystem.create(jtFs, jobFile, new FsPermission(JobSubmissionFiles.JOB_FILE_PERMISSION));
                                conf.writeXml(out);{//Configuration.writeXml(OutputStream out)
                                    writeXml(String propertyName, Writer out){//Configuration.writeXml()
                                        Document doc = asXmlDocument(propertyName);
                                        DOMSource source = new DOMSource(doc);
                                        Transformer transformer = transFactory.newTransformer();
                                        transformer.transform(source, result);
                                    }
                                }
                            }
                            // �ύ����
                            status = submitClient.submitJob( jobId, submitJobDir.toString(), job.getCredentials());{//LocalJobRunner.submitJob()
                                Job job = new Job(JobID.downgrade(jobid), jobSubmitDir);{
                                    this.localJobFile = new Path(this.localJobDir, id + ".xml");
                                    file:/tmp/hadoop-86177/mapred/local/localRunner/86177/job_local1675066360_0001/job_local1675066360_0001.xml
                                    
                                    // 
                                    OutputStream out = localFs.create(localJobFile);
                                    conf.writeXml(out);{//Configuration.writeXml( OutputStream out)
                                        // ����ͬ��;
                                    }
                                    this.start();// ����Job�߳�,ִ��LocalJobRunner.Job.run(), ����"Thread-n"�߳�, ���MR����; 
                                    
                                }
                                job.job.setCredentials(credentials);
                                return job.status;
                            }
                        }finally{
                            jtFs.delete(submitJobDir, true); //��� submitJobDir��ʱ�ύĿ¼;
                        }
                    }}
                });
            }
        }
        if (verbose) {//��ӡ״̬;
            monitorAndPrintJob();
        }
    }
}

    // 1.1 ���Ĵ���: ��Input�ļ����з�Ƭ(����)���߼�
    FileInputFormat.getSplits(JobContext job):List<InputSplit> {
        
        long minSize = Math.max(getFormatMinSplitSize(), getMinSplitSize(job));{
            //getFormatMinSplitSize() return 1;
            getMinSplitSize(job) == return job.getConfiguration().getLong(SPLIT_MINSIZE, 1L);
            return Math.max(1, 1)
        }
        
        // ���Ƭ�����ֵ��
        long maxSize = getMaxSplitSize(job);{
            // Ĭ��Long.max, ����ȡmapreduce.input.fileinputformat.split.maxsize ����
            return context.getConfiguration().getLong(SPLIT_MAXSIZE,  Long.MAX_VALUE);
        }
        // ��Driver�� setInputPaths(inputs)�е��ַ��������շָ���(,;��?)�зֺ�,��ֳɶ��Path[]����;ÿ��Path -> FileStatus;
        List<FileStatus> files = listStatus(job);{
            Path[] dirs = getInputPaths(job);{//FileInputFormat.getInputPaths()
                String dirs = context.getConfiguration().get(INPUT_DIR, "");// FileInputFormat.setInputPaths() => mapreduce.input.fileinputformat.inputdir (INPUT_DIR) ,������Input·��;
                Path[] result = new Path[list.length];
                return result;
            }
            
            // �����Ƿ�ݹ�,�ݹ��ȡ���ļ�
            boolean recursive = getInputDirRecursive(job);//INPUT_DIR_RECURSIVE����(mapreduce.input.fileinputformat.input.dir.recursive)�趨
        }
        
        for (FileStatus file: files) {
            long length = file.getLen();
            if (length != 0) {
                BlockLocation[] blkLocations = file.getBlockLocations();//��ȡ�ļ��洢��Blockλ��
                
                boolean isSplitable = isSplitable(job, path);{//TextInputFormat.isSplitable()
                    final CompressionCodec codec = new CompressionCodecFactory(context.getConfiguration()).getCodec(file); //����û��ѹ��?
                    if (null == codec) {// û��ѹ���Ļ�, ���� codec ==null;
                      return true;
                    }
                    return codec instanceof SplittableCompressionCodec;
                }
                if (isSplitable) {
                    long blockSize = file.getBlockSize();{//FileStatus.getBlockSize()
                        return blockSize; //32M;
                        {//����listStatus(job)������ -> singleThreadedListStatus() -> globStatus() -> fs.getFileStatus(path)
                            fs.getFileStatus(path){
                                return fs.getFileStatus(f);{//RawLocalFileSystem.getFileStatus(path)
                                    return getFileLinkStatusInternal(f, true);{//RawLocalFileSystem.getFileLinkStatusInternal(final Path f,boolean dereference)
                                        if (!useDeprecatedFileStatus) {
                                        }else if (dereference) {//��������;
                                            return deprecatedGetFileStatus(f);{
                                                if (path.exists()) {
                                                    int defaultBlockSize = getDefaultBlockSize(f);{//RawLocalFileSystem ���ø��� FileSystem.getDefaultBlockSize()
                                                        return getDefaultBlockSize();{
                                                            // ����ָ�� Ĭ�ϵ� defaultBlockSize =32M, ����ͨ�� fs.local.block.size �����޸�;
                                                            return getConf().getLong("fs.local.block.size", 32 * 1024 * 1024);
                                                        }
                                                    }
                                                    return new DeprecatedRawLocalFileStatus(pathToFile(f), defaultBlockSize, this);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    long splitSize = computeSplitSize(blockSize, minSize, maxSize);{//��minSize(1)��blockSize(32M)֮��ȡ���ֵ��Ϊ ÿ�η�Ƭ�Ĵ�СsplitSize==32M;
                        return Math.max(minSize, Math.min(maxSize, blockSize));
                    }
                    long bytesRemaining = length;
                    //SPLIT_SLOP==1.1, ����һЩ���࣬�Է�block���жϣ�
                    while (((double) bytesRemaining)/splitSize > SPLIT_SLOP) {//�Ƚϸ��ļ���ʣ���С ���� ��Ƭ��С(32M),
                        int blkIndex = getBlockIndex(blkLocations, length-bytesRemaining);
                        splits.add(makeSplit(path, length-bytesRemaining, splitSize, blkLocations[blkIndex].getHosts(), blkLocations[blkIndex].getCachedHosts()));
                        bytesRemaining -= splitSize;
                    }

              
                }
                
            }
        }
        
    }





    
    
    
    
/** "Thread-"�߳�:ִ��Job.run(): ���һ��MapReducer������������й���; ��Ҫ���� mapRunnables,reduceRunnables����Ĵ���,�� MapTask,ReduceTask���ύ����;
*   - �����߳�(Localģʽ): "main"�̵߳�Job.submitJobInternal -> LocalJobRunner.submitJob() -> new Job() ���캯���� job.start()����Job���߳�
*   
*   - �����߳�: 
        * 1-���"LocalJobRunner Map Task Executor # 0/n" �߳�: ��MapTask����;
        * 1-��� "pool-3-thread-1"��ReduceTask�߳�; 
*/
// "Thread-3" �߳�
LocalJobRunner.Job.run(){
    JobID jobId = profile.getJobID();
    try{
        // ���������submitJobDir�µ�job.splitmetainfo�ļ��ж�ȡ����λ����Ϣ:InputSplit��job.split�е�λ����;
        TaskSplitMetaInfo[] taskSplitMetaInfos = SplitMetaInfoReader.readSplitMetaInfo(jobId, localFs, conf, systemJobDir);{
            FSDataInputStream in = fs.open(metaSplitFile);
            int numSplits = WritableUtils.readVInt(in); //�ڶ���Ӧ�þ��Ƿ�Ƭ����numSplits��ֵ;��ȡ;
            for (int i = 0; i < numSplits; i++) {
                splitMetaInfo.readFields(in);
                //��in���ж�ȡ�÷�Ƭ���ڵ� �ļ�����offset, splitIndex:TaskSplitIndex[splitLocaltion="**/job.split",startOffset=7];
                JobSplit.TaskSplitIndex splitIndex = new JobSplit.TaskSplitIndex(jobSplitFile, splitMetaInfo.getStartOffset());
                allSplitMetaInfo[i] = new JobSplit.TaskSplitMetaInfo(splitIndex, splitMetaInfo.getLocations(), splitMetaInfo.getInputDataLength());
            }
        }
        int numReduceTasks = job.getNumReduceTasks(); //��Ƭ���ݾ��� Reducer����;
        outputCommitter.setupJob(jContext);
        
        //����MapTask���߳�;
        List<RunnableWithThrowable> mapRunnables = getMapTaskRunnables(taskSplitMetaInfos, jobId, mapOutputFiles);{//Job.getMapTaskRunnables()
            for (TaskSplitMetaInfo task : taskInfo) {//taskInfo �� taskSplitMetaInfos
                list.add(new MapTaskRunnable(task, numTasks++, jobId,mapOutputFiles));//task -> splitIndex, ����Ƭ������װ��ÿ��MapRunnable����,����"LocalMapTask"�߳̾ݴ˷���job.split�͹���InputSplit����;
            }
        }
        
        runTasks(mapRunnables, mapService, "map");{//Job.runTasks()
            for (Runnable r : runnables) {
                service.submit(r);{//ExecutorService.submit(Runnable r)
                    r.run()// ����"LocalJobRunner Map Task Executor # 0/n" �߳�,ִ��һ��MapTask;
                }
            }
            service.shutdown();
            service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        
        if (numReduceTasks > 0) {
            //���������ReduceTask����;
            List<RunnableWithThrowable> reduceRunnables = getReduceTaskRunnables(jobId, mapOutputFiles);
            runTasks(reduceRunnables, reduceService, "reduce");{
                for (Runnable r : runnables) {
                    service.submit(r);{//ExecutorService.submit(Runnable r)
                        r.run()
                    }
                }
                service.shutdown();
                service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
        }
    }
}






// "LocalJobRunner Map Task Executor "�̣߳�ִ��Mapper �߳�: 
LocalJobRunner.JobRunner.MapTaskRunnable.run(){
    TaskAttemptID reduceId = new TaskAttemptID(new TaskID());
    MapTask map = new MapTask(systemJobFile.toString(), mapId, taskId, info.getSplitIndex(), 1);// �����ｫsplitIndex��Ƭ��Ϣ����MapTask.splitMetaInfo��Ա����;
    
    map.run(localConf, Job.this);{
        initialize(job, getJobID(), reporter, useNewApi);
        if (useNewApi) {
            runNewMapper(job, splitMetaInfo, umbilical, reporter);{//MapTask.runNewMapper(), 
                // 1. ��һ��,�ȴ�splitIndex������job.split�ļ��ж�ȡInputSplit���ݲ������л���InputSplit����;
                InputSplit split = getSplitDetails(new Path(splitIndex.getSplitLocation()), splitIndex.getStartOffset());{
                    FSDataInputStream inFile = fs.open(file);
                    inFile.seek(offset);
                    deserializer.open(inFile);//��ȡ�������ֽ���
                    T split = deserializer.deserialize(null);//�Ѵ�job.split��Ӧλ�õĶ�������,�����г� InputSplit����;
                }
                
                // 2. ���� RecordReader,�� RecordWriter
                RecordReader<INKEY,INVALUE> input = new NewTrackingRecordReader<INKEY,INVALUE> (split, inputFormat, reporter, taskContext);
                RecordWriter output = new NewOutputCollector(taskContext, job, umbilical, reporter);
                
                Context mapperContext = new WrappedMapper<INKEY, INVALUE, OUTKEY, OUTVALUE>().getMapContext(mapContext);
                //��������ɸ÷�Ƭ��Ӧ InputPath��������fileIn=fs.open();
                input.initialize(split, mapperContext);{//MapTask.NewTrackingRecordReader.initialize()
                    real.initialize(split, context);{//Ĭ�� LineRecordRecord.initialize()
                        start = split.getStart();
                        end = start + split.getLength();
                        final Path file = split.getPath();
                        fileIn = fs.open(file);
                        CompressionCodec codec = new CompressionCodecFactory(job).getCodec(file);
                        if (null!=codec) {
                            //todo
                        }else{
                            fileIn.seek(start); //��λ��startλ��;
                            in = new UncompressedSplitLineReader(fileIn, job, this.recordDelimiterBytes, split.getLength());
                        }
                    }
                }
                
                /** ���Ĵ���: ִ��Map��������,����ÿ��Map���KVд�뵽������,����ʱ����"SpillThread"�߳���д����;
                *       ������map(Key,Value)������:
                            - Map.map()����ɼ���;
                            - Map.map() -> context.write(k,v)��, �����д;
                */
                mapper.run(mapperContext);{
                    setup(context);
                    while (context.nextKeyValue(){//���Ƶ�������hasHext(), WrappedMapper.Context.nextKeyValue()
                        return mapContext.nextKeyValue();{//MapContextImpl.nextKeyValue() -> NewTrackingRecordReader.nextKeyValue()
                            boolean result = real.nextKeyValue();{//LineRecordReader.nextKeyValue()
                                if (key == null)key = new LongWritable();
                                if (value == null) value = new Text();
                                while (getFilePosition() <= end || in.needAdditionalRecordAfterSplit()) {
                                    newSize = in.readLine(value, maxLineLength, maxBytesToConsume(pos));{//UncompressedSplitLineReader.readLine()
                                        return bytesRead = super.readLine(str, maxLineLength, maxBytesToConsume);{//LineReader.readLine()
                                            if (this.recordDelimiterBytes != null) {
                                              return readCustomLine(str, maxLineLength, maxBytesToConsume);
                                            } else { //��������,��ȡĬ�ϵ�һ��;
                                                return readDefaultLine(str, maxLineLength, maxBytesToConsume);{
                                                    str.clear();//������ԭ����ַ��������;
                                                    do{
                                                        if (appendLength > 0) {
                                                            int appendLength = readLength - newlineLength;
                                                            
                                                            str.append(buffer, startPosn, appendLength);
                                                            txtLength += appendLength;
                                                        }
                                                    }while (newlineLength == 0 && bytesConsumed < maxBytesToConsume);
                                                }
                                            }
                                        }
                                    }
                                    if(newSize==0){
                                        return false;
                                    }else{
                                        return true;
                                    }
                                }
                            }
                            return result;
                        }
                    }) {
                        map(context.getCurrentKey(), context.getCurrentValue(), context);{//�����map()����
                            MyMapperImpl.map(KEYIN key, VALUEIN value, Context context){
                                // todo ҵ����
                                
                                context.write((KEYOUT) key, (VALUEOUT) value);{//WrappedMapper.write()
                                    mapContext.write(key, value);{//TaskInputOutputContextImpl.write()
                                        output.write(key, value);{//MapTask.NewOutputCollector.write()
                                            int = partitioner.getPartition(key, value, partitions);{//����
                                                
                                            }
                                            
                                            collector.collect(key, value, partiton);{//MapOutputBuffer.collect()
                                               //��������;
                                            }
                                        }
                                    }
                                }
                            }
                            
                        }
                    }
                }
            
                input.close();
                output.close(mapperContext);{//NewOutputCollector.close()
                    collector.flush();{
                        while (spillInProgress) { //����ֱ�� ��д״̬��Ϊtrue;
                            reporter.progress();
                            spillDone.await();
                        }
                        
                        if (kvindex != kvend) {
                            kvend = (kvindex + NMETA) % kvmeta.capacity();
                            sortAndSpill();{//MapOutputBuffer.sortAndSpill()
                                    //������������ sortAndSpill()����;
                            }
                        }
                        
                    }
                }
                
            }
        }
    }
}

    // ��һ��Map���(Key,Value) д�뻺��,����д����;
    output.write(key, value);{//MapTask.NewOutputCollector.write()
        int = partitioner.getPartition(key, value, partitions);{//����
            
        }
        
        // ����key,value,���㱾������(Key,Value) �������ĸ�����(��),��partition:int;
        //�ٽ�����,����key����,д�뵽������?
        collector.collect(key, value, partiton);{//MapOutputBuffer.collect()
            
            bufferRemaining -= METASIZE; //?
            
            if (bufferRemaining <= 0) {
                spillLock.lock();
                if (!spillInProgress) { //��û��spillʱ,�ſ�����д;
                    
                    final int bUsed = distanceTo(kvbidx, bufindex); {// ?
                        return i <= j ? j - i : mod - i + j;
                    }
                    final boolean bufsoftlimit = bUsed >= softLimit; //?
                    
                    if ((kvbend + METASIZE) % kvbuffer.length != equator - (equator % METASIZE)) {
                    
                        resetSpill();
                        bufferRemaining = Math.min( distanceTo(bufindex, kvbidx) - 2 * METASIZE, softLimit - bUsed) - METASIZE;
                    }else if (bufsoftlimit && kvindex != kvend) {
                        startSpill();{//MapOutputBuffer.startSpill()
                            kvend = (kvindex + NMETA) % kvmeta.capacity();
                            spillInProgress = true; //������ڽ�����дsplill;
                            spillReady.signal(); //��"SpillThread"�̷߳���׼����spill���ź���;
                            {//"SpillThread"�߳�,MapTask.MapOutputBuffer.SplitThread.run()������ִ��
                                while(true){
                                    spillDone.signal();
                                    while (!spillInProgress) { //����д״̬����: spillInProgress==falseʱ,һֱ�����ڴ˵ȴ����� spillReady.signal()�ź�;
                                        spillReady.await();
                                    }
                                    //��������,˵��spillInProgress==true,��Ҫ������д��;
                                    spillLock.unlock(); //Ϊʲô�ͷ���? ��֤������߳����ĸ������İ�ȫ? spillInProgress?
                                    /* ���Ĵ���: ��ʽ������д,
                                    *
                                    */
                                    sortAndSpill();{//MapOutputBuffer.sortAndSpill()
                                        // ����������� �� MapOutputBuffer.sortAndSpill()
                                    }
                                }
                            }
                        }
                        
                        final int distkvi = distanceTo(bufindex, kvbidx);
                        
                    }
                }
            }
            
            
            
        }
    }


    // 2.3 ������д������߼�: MapOutputBuffer.sortAndSpill 

    sortAndSpill();{//MapOutputBuffer.sortAndSpill()
        
        final Path filename = mapOutputFile.getSpillFileForWrite(numSpills, size);// 
        // /tmp/**/jobcache/job_local831756809_0001/attempt_local831756809_0001_m_000000_0/output/spill0.out
        FSDataOutputStream out = rfs.create(filename);//�����÷���д���ļ��������
        
        final int mend = 1 + // kvend is a valid record
              (kvstart >= kvend
              ? kvstart
              : kvmeta.capacity() + kvstart) / NMETA;
        //�Ƚ�������; �����㷨;
        sorter.sort(MapOutputBuffer.this, mstart, mend, reporter);{//QuickSort.sort()
            sortInternal(s, p, r, rep, getMaxDepth(r - p));
        }
        
        for (int i = 0; i < partitions; ++i) {
            partitionOut = CryptoUtils.wrapIfNecessary(job, out, false);// ����� partitionOut���� out,spill0�ļ��������;
            //����Writer,��֤�� OutputStream;
            IFile.Writer<K, V> writer = new Writer<K, V>(job, partitionOut, keyClass, valClass, codec,  spilledRecordsCounter);
            
            // ����ҪMap����Ԥ�ۺ�,������ǰ��conbiner��;
            if (combinerRunner == null) {//��û������Ԥ�ۺ�ʱ,�����������;
                DataInputBuffer key = new DataInputBuffer();
                while (spindex < mend && kvmeta.get(offsetFor(spindex % maxRec) + PARTITION) == i) {
                    final int kvoff = offsetFor(spindex % maxRec);
                    int keystart = kvmeta.get(kvoff + KEYSTART);
                    int valstart = kvmeta.get(kvoff + VALSTART);
                    key.reset(kvbuffer, keystart, valstart - keystart);
                    getVBytesForOffset(kvoff, value); //��ȡ����д�뵽value��;
                    writer.append(key, value);{//IFile.Writer.append(): value==��key��Ӧ��Map���? �����д�� key��Ӧ���ļ���?
                        int keyLength = key.getLength() - key.getPosition();
                        int valueLength = value.getLength() - value.getPosition();
                        WritableUtils.writeVInt(out, keyLength);
                        WritableUtils.writeVInt(out, valueLength);
                        out.write(key.getData(), key.getPosition(), keyLength); 
                        //�����bytesд���� output/spill0.out �ļ���;
                        out.write(value.getData(), value.getPosition(), valueLength); {//DataOutputStream.write()
                            out.write(b, off, len);{
                                sum.update(b, off,len);
                                out.write(b,off,len);{
                                    out.write(b, off, len);{//FSDataOutputStream.PositionCache.write()
                                        out.write(b, off, len);
                                        position += len;  
                                    }
                                }
                            }
                            incCount(len);
                        }
                          
                        ++numRecordsWritten;
                    } 
                    ++spindex;
                }
                
            }else{
                int spstart = spindex;
                while (spindex < mend && kvmeta.get(offsetFor(spindex % maxRec)+ PARTITION) == i) {
                    ++spindex;
                }
                if (spstart != spindex) {
                    combineCollector.setWriter(writer);
                    RawKeyValueIterator kvIter = new MRResultIterator(spstart, spindex);
                    // ��������Map��Ԥ����;
                    combinerRunner.combine(kvIter, combineCollector);{//Task.NewCombinerRunner.combine()
                        reducer =(Reducer<K,V,K,V>) ReflectionUtils.newInstance(reducerClass, job);
                        Context 
                       reducerContext = createReduceContext(reducer, job, taskId,iterator, null, inputCounter,
                                   new OutputConverter(collector), committer, reporter,
                                   comparator, keyClass, valueClass);
                        //����Map��Ԥ�ۺ� Reducer.run()
                        reducer.run(reducerContext);{//Reducer.run()
                            //ͬ��Reducer�߳��� Reducer.run()����;
                        }
                    }
                }
            }
        }
        
    }



// "pool-3-thread-1"�̣߳�ִ��Reducer�߳�
LocalJobRunner.JobRunner.ReduceTaskRunnable.run(){
    TaskAttemptID reduceId = new TaskAttemptID(new TaskID());
    ReduceTask reduce = new ReduceTask(systemJobFile.toString(),reduceId, taskId, mapIds.size(), 1);
    reduce.setLocalMapFiles(mapOutputFiles);//����mapOutput�ļ�·�����ڻ�ȡMap������;
    
    
    //����Reducer���߼�;
    reduce.run(localConf, Job.this);{//ReduceTask.run()
        initialize(job, getJobID(), reporter, useNewApi);
        CombineOutputCollector combineCollector = (null != combinerClass) ?  new CombineOutputCollector(reduceCombineOutputCounter, reporter, conf) : null;
        Class<? extends ShuffleConsumerPlugin> clazz = job.getClass(MRConfig.SHUFFLE_CONSUMER_PLUGIN, Shuffle.class, ShuffleConsumerPlugin.class);
		
        if (useNewApi) {
            runNewReducer(job, umbilical, reporter, rIter, comparator, keyClass, valueClass);{//ReducerTask.runNewReducer()
                final RawKeyValueIterator rawIter = new RawKeyValueIterator(){};
                Context reducerContext = createReduceContext(reducer, job, getTaskID(), rIter, reduceInputKeyCounter, reduceInputValueCounter, 
                                               trackedRW,committer,reporter, comparator, keyClass,valueClass);
                
                reducer.run(reducerContext);{//reducer��ʵ�������Ǽ̳�ʵ�ֵ� ReducerImpl, ��������丸�� Reducer.run()
                    setup(context);
                    while (context.nextKey()) {
                        reduce(context.getCurrentKey(), context.getValues(), context);{//���������reduce()ʵ�ַ���
                            MyReducerImpl.reduce(){}
                            
                        }
                        
                        Iterator<VALUEIN> iter = context.getValues().iterator();
                        if(iter instanceof ReduceContext.ValueIterator) {
                            ((ReduceContext.ValueIterator<VALUEIN>)iter).resetBackupStore();        
                        }
                    }
                }
            }
        } 
    }
}













