
// yarnNM.1: NodeManager �����ͳ�ʼ��

	// 1.1 ������ʱ�߳�, ��ʱ�� LocalDirsHandlerService.checkDirs()
	LocalDirsHandlerService.serviceStart(){
		if (isDiskHealthCheckerEnabled) {
			dirsHandlerScheduler = new Timer("DiskHealthMonitor-Timer", true);
			// �Թ̶�Ƶ��,��ʱcheckDirs(), 
			// diskHealthCheckInterval, yarn.nodemanager.disk-health-checker.interval-ms, Ĭ�� 2 * 60 * 1000 = 2����;
			dirsHandlerScheduler.scheduleAtFixedRate(monitoringTimerTask,diskHealthCheckInterval, diskHealthCheckInterval);{
				LocalDirsHandlerService.MonitoringTimerTask.run(){
					LocalDirsHandlerService.checkDirs();// Դ������; 
				}
			}
		}
	}


// yarnNM.2: NodeManager �ṩ Container ��ͣ�͹������




launchContainer:211, DefaultContainerExecutor (org.apache.hadoop.yarn.server.nodemanager)
call:302, ContainerLaunch (org.apache.hadoop.yarn.server.nodemanager.containermanager.launcher)
call:82, ContainerLaunch (org.apache.hadoop.yarn.server.nodemanager.containermanager.launcher)
run:266, FutureTask (java.util.concurrent)
runWorker:1149, ThreadPoolExecutor (java.util.concurrent)
run:624, ThreadPoolExecutor$Worker (java.util.concurrent)
run:748, Thread (java.lang)






// yarnNM.3: ��������Ͷ�ʱ����߳� : checkDir(), 

	// 3.1 ��ʱִ�� checkDir; 

	LocalDirsHandlerService.checkDirs(){
		Set<String> failedLocalDirsPreCheck =new HashSet<String>(localDirs.getFailedDirs());
		Set<String> failedLogDirsPreCheck =new HashSet<String>(logDirs.getFailedDirs());
		boolean isLocalDirsChanged = localDirs.checkDirs();{// DirectoryCollection.checkDirs()
			boolean setChanged = false;
			List<String> allLocalDirs =DirectoryCollection.concat(localDirs, failedDirs);
			// ���ÿ���ļ�Ŀ¼,�����Ƿ��� DISK_FULL ״̬; 
			Map<String, DiskErrorInformation> dirsFailedCheck = testDirs(allLocalDirs);{//DirectoryCollection.testDirs()
				for (final String dir : dirs) {
					File testDir = new File(dir);
					DiskChecker.checkDir(testDir);
					// ���ж� Usageʹ�����Ƿ񳬹� diskUtilizationPercentageCutoff (���� max-disk-utilization-per-disk-percentage,Ĭ�� 90)
					if (isDiskUsageOverPercentageLimit(testDir)) {
						ret.put(dir,new DiskErrorInformation(DiskErrorCause.DISK_FULL, msg));
						continue
					}else if(isDiskFreeSpaceUnderLimit(testDir)){
						ret.put(dir,new DiskErrorInformation(DiskErrorCause.DISK_FULL, msg));
						continue;
					}
					
					verifyDirUsingMkdir(testDir);
				}
				return ret;
			}
			fullDirs.clear();
			
			// ֻҪ���κ� dist���� DISK_FULL ״̬��, �ͱ�� setChanged=true; �����쳣���; 
			for (Map.Entry<String, DiskErrorInformation> entry : dirsFailedCheck.entrySet()) {
				DiskErrorInformation errorInformation = entry.getValue();	
				switch (entry.getValue().cause) {
					case DISK_FULL:fullDirs.add(entry.getKey());break;
					case OTHER:errorDirs.add(entry.getKey());break;
				}
				if (preCheckGoodDirs.contains(dir)) {
					LOG.warn("Directory " + dir + " error, " + errorInformation.message + ", removing from list of valid directories");
					setChanged = true;
					numFailures++;
				}
				
			}
			
			return setChanged;
		}
		if (localDirs.checkDirs()) {
			disksStatusChange = true;
		}
		if (logDirs.checkDirs()) {
			disksStatusChange = true;
		}
		
		
		if (!disksFailed) {
			disksFailed =disksTurnedBad(failedLogDirsPreCheck, failedLogDirsPostCheck);
		}
		if (!disksTurnedGood) {
		  disksTurnedGood =disksTurnedGood(failedLogDirsPreCheck, failedLogDirsPostCheck);
		}
		
		logDiskStatus(disksFailed, disksTurnedGood);
	}




