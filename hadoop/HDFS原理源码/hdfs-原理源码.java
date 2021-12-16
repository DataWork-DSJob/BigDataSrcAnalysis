

// �������ػ�cluster��FileSystem ?
FileSystem.get(conf){
    URI url=getDefaultUri(conf);{
        return URI.create(fixName(conf.get(FS_DEFAULT_NAME_KEY, DEFAULT_FS)));{
            conf.get(FS_DEFAULT_NAME_KEY, DEFAULT_FS);{
                String[] names = handleDeprecation(deprecationContext.get(), name);
                for(String n : names) {
                    //n =="fs.defaultFS",��ӦgetProps()�е�props�еĸ�key��ֵ,Ҳ��Ĭ��ֵ: file:/// ; defaultValueҲ��file:///
                    result = substituteVars(getProps().getProperty(n, defaultValue));
                }
                return result;//����,��ConfigurationΪ��ʱ,���ص�fs.defaultFS��Ϊ: file:///
            }
        }
    }
        
    return get(url, conf);{
        String scheme = uri.getScheme();
        return CACHE.get(uri, conf);{//FileSystem.CACHE.get
            Key key = new Key(uri, conf);
            return getInternal(uri, conf, key);{
                FileSystem fs = map.get(key);
                if (fs != null) return fs; // ����Key(Url��Ӧ��Key)�л����FSʵ��,ֱ�ӷ���;����ģʽ;
                
                fs = createFileSystem(uri, conf);{
                    Tracer tracer = FsTracer.get(conf);
                    Class<?> clazz = getFileSystemClass(uri.getScheme(), conf);{
                        if (!FILE_SYSTEMS_LOADED) {
                            /* �����˸���FileSystem:
                            *   "file" -> "class org.apache.hadoop.fs.LocalFileSystem"
                            *   "hdfs" -> "class org.apache.hadoop.hdfs.DistributedFileSystem"
                            *   "s3" -> "class org.apache.hadoop.fs.s3.S3FileSystem"
                            *   "ftp" -> "class org.apache.hadoop.fs.ftp.FTPFileSystem"
                            *   "webhdfs" -> "class org.apache.hadoop.hdfs.web.WebHdfsFileSystem"
                            */
                            loadFileSystems();{ //������FileSystem��ʵ����,���ص�SERVICE_FILE_SYSTEMS: Map<String,Class> 
                                ServiceLoader<FileSystem> serviceLoader = ServiceLoader.load(FileSystem.class);
                                Iterator<FileSystem> it = serviceLoader.iterator();
                                while (it.hasNext()) { //������FileSystem��ʵ����,���ص�SERVICE_FILE_SYSTEMS: Map<String,Class> 
                                    fs = it.next();
                                    SERVICE_FILE_SYSTEMS.put(fs.getScheme(), fs.getClass());
                                }
                            }
                        }
                        if (clazz == null) {
                            clazz = SERVICE_FILE_SYSTEMS.get(scheme);//����file���scheme, ������ class org.apache.hadoop.fs.LocalFileSystem ����;
                        }
                        return clazz;
                    }
                }
                
            }
        }
    }
}


