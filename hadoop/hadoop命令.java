


RunJar.main(String[] args) {
    
    new RunJar().run(args);{//������һ���� JarFile ;
        String usage = "RunJar jarFile [mainClass] args...";
        if (args.length < 1) {
          System.exit(-1);
        }
        int firstArg = 0;
        String fileName = args[firstArg++];
        File file = new File(fileName);
        if (!file.exists() || !file.isFile()) {
          System.err.println("Not a valid JAR: " + file.getCanonicalPath());
          System.exit(-1);
        }
        JarFile jarFile =new JarFile(fileName);
        
        // ȷ��mainClass, ���ȴ�Jar�е�Mainifest��ȡmainClass,��Ӵӵڶ�������
        String mainClassName = null;
        Manifest manifest = jarFile.getManifest();
        if (manifest != null) {
          mainClassName = manifest.getMainAttributes().getValue("Main-Class");
        }
        jarFile.close();

        if (mainClassName == null) { //��Jar.Mainifest û��ʱ,���Դӵڶ���args��������;
          if (args.length < 2) {
            System.err.println(usage);
            System.exit(-1);
          }
          mainClassName = args[firstArg++];
        }
        mainClassName = mainClassName.replaceAll("/", ".");

        File workDir = File.createTempFile("hadoop-unjar", "", new File(System.getProperty("java.io.tmpdir"))); //JavaӦ����ʱĿ¼;
        if (!workDir.delete()) {
          System.err.println("Delete failed for " + workDir);
          System.exit(-1);
        }
        ensureDirectory(workDir);
        ShutdownHookManager.get().addShutdownHook( //���Ӻ���,��֤wordDir�ܱ����ɾ��;
          new Runnable() {
            @Override
            public void run() {FileUtil.fullyDelete(workDir);}
          }, SHUTDOWN_HOOK_PRIORITY);
        
        
        unJar(file, workDir);
        
        ClassLoader loader = createClassLoader(file, workDir);
        Thread.currentThread().setContextClassLoader(loader);
        
        // ����mainClass �� Dirver��main()����, args����;
        Class<?> mainClass = Class.forName(mainClassName, true, loader);
        Method main = mainClass.getMethod("main", new Class[] {
          Array.newInstance(String.class, 0).getClass()
        });
        String[] newArgs = Arrays.asList(args).subList(firstArg, args.length).toArray(new String[0]);
        // ִ���û������UserDriver.main()����
        main.invoke(null, new Object[] { newArgs });
        
    }
}

