# Hadoop和spark集群的搭建

## 第一章 搭建centos7集群环境

### 一、安装centos7虚拟机

1.配置虚拟机的NAT网络模式
https://www.cnblogs.com/yychnbt/p/5175273.html
https://www.cnblogs.com/zejin2008/p/5935934.html

### 二、克隆虚拟机

1.使用vmware自带的“克隆”功能

2.克隆后更改网络配置
--参考链接：https://blog.csdn.net/seven_zhao/article/details/43429571

--修改网络：
--1.删除网卡
--2.添加新的网卡 -->HWADDR（mac地址）将自动更新
	添加新网卡，选择NAT模式，点击“高级”-->MAC地址：点击“生成” ，并复制此时生成的MAC地址（大小写敏感） 00:50:56:3C:65:3D
	
--3.修改IP地址
	开机，登录为root用户，切换目录至网卡配置文件
		cd /etc/sysconfig/network-scripts
	打开配置文件： vi ifcfg-ens33	
		HWADDR=00:50:56:2A:D9:D6 #MAC地址，与刚生成的新MAC地址一致
		IPADDR=192.168.80.10     #静态IP
		GATEWAY=192.168.80.2     #默认网关
		NETMASK=255.255.255.0    #子网掩码
		DNS1=192.168.80.2        #DNS配置
		DNS2=222.246.129.80      #DNS配置
		DNS3=221.228.225.1       #DNS配置
--4.刷新网卡或重启电脑，验证是否成功连上外网：
		刷新网卡：service network restart
		重启电脑：reboot 
		验证是否联网：ping www.baidu.com  或者 curl www.baidu.com

### 三、安装Java

--参考链接：https://www.cnblogs.com/wangmo/p/7880521.html

--1.下载安装包，并复制到安装目录（例如，将java安装到这个目录： /home/hadoop/app）
--2.配置环境变量:4个
	1）使用vim命令打开系统的环境变量配置文件：
			vim /etc/profile
	2)添加4个环境变量
    JAVA_HOME  PATH   JRE_HOME   CLASSPATH 
    例如：
    	export JAVA_HOME=/home/hadoop/app/jdk1.8  #jdk的绝度路径
		export PATH=$PATH:$JAVA_HOME/bin
		export JRE_HOME=$JAVA_HOME/jre
		export CLASS_PATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar:$JRE_HOME/lib
	3)使修改生效：source /etc/profile

--3.验证是否安装成功
    回到根目录： cd /
    验证命令：java 和 javac ,java -version



## 第二章 搭建Hadoop集群

### 一、搭建集群

安装及克隆虚拟机，上述第一章已完成该步骤。

本集群包含三个节点，主机名及IP如下：

​		master	192.168.80.10
​		slave1	 192.168.80.11
​		slave2 	192.168.80.12

所选Hadoop版本：**hadoop2.8.4**



### 二、更改网络配置

​	1.右键点击虚拟机->设置：删除网卡，重新添加新网卡，并记录新生成的MAC地址
​	2.修改ip地址，并更新为新生成的MAC地址
​	3.修改hosts文件：
​		3台机器分别执行相同操作
​			1)在文件添加ip与主机名的映射关系：
​				打开hosts文件：vim /etc/hosts
​				三台机都添加如下三行
​					192.168.80.10  master
​					192.168.80.11  slave1
​					192.168.80.12  slave2
​			2）修改主机名：vim /etc/hostname
​				删除localhost.localdomain
​				三台机器分别修改为：master，slave1，slave2
​			3）重启虚拟机，验证
​				输入验证命令：hostname



### 三、建立三台机器间免密登录

```ssh

	1.关闭防火墙（centos7和centos6的操作不一样）
		1）查看防火墙状态：firewall-cmd --state   (关闭状态：not running)
		2）关闭防火墙
			第一步，临时关闭：systemctl stop firewalld.service
			第二步，禁止开机启动：systemctl disable firewalld.service
			第三步，重启计算机，验证防火墙的状态是否为not running
	2.关闭selinux
		1)查看selinux状态：
			(1)getenforce：disabled
			(2)/usr/sbin/sestatus -v :disabled
		2）临时关闭：setenforce 0
		3）永久关闭：修改配置文件（root权限）
			vi /etc/selinux/config
			将SELINUX=enforcing改为SELINUX=disabled 
		4）重启计算机，让修改生效。

	3.修改sshd的配置（root权限）
		1)vim /etc/ssh/sshd_config
			找到以下内容，并去掉注释符“#”
			　　RSAAuthentication yes
			　　PubkeyAuthentication yes
			　　AuthorizedKeysFile      .ssh/authorized_keys
		2)重启sshd服务（root权限）
			/sbin/service sshd restart
		3）验证：查看sshd服务是否开启
			service sshd status  （开启状态：active(running))
			
	4.本机生成公钥/私钥对
		到这一步时，也可以通过ssh slave1命令+slave1的密码登录到slave1。
			也就是说，目前可以远程登录，只是不能免密登录。
		进一步配置免密登录如下
			参考链接：
				（1）RSA公钥，私钥和数字签名这样最好理解
				https://blog.csdn.net/cut001/article/details/53189645
				（2）Hadoop系列之（一）CentOS7安装配置及SSH无密码验证配置
				https://blog.csdn.net/triumphao/article/details/53264190
				（3）《ssh免密码登陆及其原理 by 代码如诗》
				https://www.cnblogs.com/kex1n/p/6017963.html?utm_source=itdadao&utm_medium=referral#top
		1）本机生成公钥/私钥对
			(1)从root切换回要免密码登录的用户hadoop
		 		su hadoop 
			(2)执行生成公钥/私钥对的命令
				ssh-keygen -t rsa -P ''
				默认在当前用户的家目录（~/.ssh/）生成两个文件：
				id_rsa: 私钥
				id_rsa.pub:公钥
				
		2）将公钥复制到本机和其他机器，并修改文件权限为600
			（1）复制master的公钥到本机，并修改文件权限
		 		cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys
		 		chmod 600 ~/.ssh/authorized_keys

		 	（2）复制公钥到其他机器，并修改文件权限
		 		第一步，复制master01的公钥
			 		scp ~/.ssh/id_rsa.pub hadoop@slave1:~/.ssh/authorized_keys
			 		scp ~/.ssh/id_rsa.pub hadoop@slave2:~/.ssh/authorized_keys
		 		第二步，修改文件权限  --> 完成这步之后，master01已经能够免密登录到slave1和slave2 
			 		分别切换到slave1和slave2，修改文件权限
			 		chmod 600 ~/.ssh/authorized_keys
				第三步，将slave1和slave2的公钥分别复制到master01，并修改master01的文件权限
					在slave1上执行：
						scp ~/.ssh/id_rsa.pub hadoop@master01:~/id_rsa.pub1 #先复制到master01，后追加到master01的authorized_keys中
					在slave2上执行：
						scp ~/.ssh/id_rsa.pub hadoop@master01:~/id_rsa.pub2
					在master01上执行：
		 				cat ~/id_rsa.pub1 >> ~/.ssh/authorized_keys
		 				cat ~/id_rsa.pub2 >> ~/.ssh/authorized_keys
		 			修改master01的文件权限：
		 				chmod 600 ~/.ssh/authorized_keys

		3)验证免密登录
			在master01上执行：ssh slave1   #退出远程登录：exit
			在master01上执行：ssh slave2
			在slav1上执行：ssh master
			在slav2上执行：ssh master
			查看master01的authorized_keys：cat ~/.ssh/authorized_keys  #公钥的末尾：@slave1、@slave2	
				
				
```



### 四、安装hadoop

		1.下载安装包并解压
			#master01节点（注意：slave节点的暂时不安装，后续复制配置好的文件即可）
			cd ~/app
			wget http://mirror.bit.edu.cn/apache/hadoop/common/hadoop-2.8.4/hadoop-2.8.4.tar.gz
			tar -zxvf hadoop-2.8.4.tar.gz
	
			解压之后，确认版本是32位还是64位
				cd hadoop-2.8.4/lib/native
			使用file命令：
				file libhadoop.so.1.0.0
			得到验证结果
				 ELF 64-bit LSB shared object, x86-64, version 1 (SYSV) -->64位
	
		2.修改Hadoop配置文件
			#Master
			cd hadoop-2.8.4/etc/hadoop
			1)修改hadoop-env.sh
				vim hadoop-env.sh 
				export JAVA_HOME=/home/hadoop/app/jdk1.8
	
			2)修改yarn-env.sh
				vim yarn-env.sh
				export JAVA_HOME=/home/hadoop/app/jdk1.8
	
			3）修改slaves
				vim slaves
				删除原内容，添加如下内容：
					slave01
					slave02
	
			4)创建临时目录和文件目录
				mkdir /home/hadoop/app/hadoop-2.8.4/tmp
				mkdir -p /home/hadoop/app/hadoop-2.8.4/dfs/name
				mkdir -p /home/hadoop/app/hadoop-2.8.4/dfs/data
	
			5)修改core-site.xml
				vim core-site.xml
				添加如下内容：
				<configuration>
					<!-- 指定hdfs的namenode为master01 -->
					<property>
						<name>fs.defaultFS</name>
						<value>hdfs://master01:9000</value>
					</property>
					<!-- 指定hadoop临时目录,需要自行创建 -->
					<property>
						<name>hadoop.tmp.dir</name>
						<value>/home/hadoop/app/hadoop-2.8.4/tmp</value>
					</property>
				</configuration>
	
			6）修改hdfs-site.xml
				vim hdfs-site.xml
					<configuration>
						<!--指定secondary namenode所在节点及端口-->
						<property>
							<name>dfs.namenode.secondary.http-address</name>
							<value>master01:9001</value>
						</property>
						<!--指定hdfs中namenode的存储位置-->
						<property>
							<name>dfs.namenode.name.dir</name>
							<value>file:/home/hadoop/app/hadoop-2.8.4/dfs/name</value>
						</property>
						<!--指定hdfs中datanode的存储位置-->
						<property>
							<name>dfs.datanode.data.dir</name>
							<value>file:/home/hadoop/app/hadoop-2.8.4/dfs/data</value>
						</property>
						<!-- DataNode存储block的副本数量：不大于DataNode的个数,默认为3。此处data节点只有2个，故改为2-->
						<property>
							<name>dfs.repliction</name>
							<value>2</value>
						</property>
					</configuration>
	
			7）修改mapred-site.xml
				vim mapred-site.xml
					<configuration>
					<!--指定MR框架为Yarn方式，默认是local-->
						<property>
							<name>mapreduce.framework.name</name>
							<value>yarn</value>
						</property>
					</configuration>
	
			8）修改yarn-site.xml
				vim yarn-site.xml
					<configuration>
						<property>
							<name>yarn.nodemanager.aux-services</name>
							<value>mapreduce_shuffle</value>
						</property>
						<property>
							<name>yarn.nodemanager.aux-services.mapreduce.shuffle.class</name>
							<value>org.apache.hadoop.mapred.ShuffleHandler</value>
						</property>
						<property>
							<name>yarn.resourcemanager.address</name>
							<value>master01:8032</value>
						</property>
						<property>
							<name>yarn.resourcemanager.scheduler.address</name>
							<value>master01:8030</value>
						</property>
						<property>
							<name>yarn.resourcemanager.resource-tracker.address</name>
							<value>master01:8035</value>
						</property>
						<property>
							<name>yarn.resourcemanager.admin.address</name>
							<value>master01:8033</value>
						</property>
						<property>
							<name>yarn.resourcemanager.webapp.address</name>
							<value>master01:8088</value>
						</property>
					</configuration>


		3. 配置环境变量
		#Master、Slave1、Slave2
		vim ~/.bashrc
			HADOOP_HOME=/home/hadoop/app/hadoop-2.8.4
			export PATH=$PATH:$HADOOP_HOME/bin
		#刷新环境变量
			source ~/.bashrc
	
		4. 拷贝安装包
		#Master
			scp -r /home/hadoop/app/hadoop-2.8.4 hadoop@slave01:/home/hadoop/app/hadoop-2.8.4
			scp -r /home/hadoop/app/hadoop-2.8.4 hadoop@slave02:/home/hadoop/app/hadoop-2.8.4
	
		5. 启动集群
		#Master
		#初始化Namenode
			hadoop namenode -format
		#启动集群
			cd /home/hadoop/app/hadoop-2.8.4/sbin
			./start-all.sh
	
		6. 集群状态
		jps
		#Master
		#Slave1,Slave2
	
		7.监控网页（上述yarn-site.xml文件中配置的RM网页地址）
		http://192.168.80.10:8088
	
		8. 操作命令
			启动集群后，输入hadoop fs即显示常用命令的用法
			参考链接：
				CSDN:https://www.cnblogs.com/zhaosk/p/4391294.html#top
				官网：http://hadoop.apache.org/docs/r2.8.4/hadoop-project-dist/hadoop-common/FileSystemShell.html
			hadoop fs 和 hadoop dfs的区别：
				hadoop fs：适用于任何不同的文件系统，比如本地文件系统和HDFS文件系统；
				hadoop dfs：只能适用于HDFS文件系统；
				hdfs dfs：跟hadoop dfs命令的作用一样，也只能适用于HDFS文件系统。
				参考链接：林子雨老师 http://dblab.xmu.edu.cn/blog/1625-2/
				
			1)打印文件列表（ls）
				（1）完整写法
				#和Hadoop1.0操作命令是一样的
				./hadoop fs -ls hdfs:/  #明确说明是hdfs系统路径
	
				（2）简写
				./hadoop fs -ls  /      #默认是hdfs系统路径
	
				（3）打印指定目录
				./hadoop fs -ls /pycode #hdfs系统下某个目录
	
				（4）选项-R：连同子目录的文件一起列出
				
			2）创建/删除文件夹（mkdir、rmdir)
				（1）创建文件夹
				hadoop fs -mkdir -p /mycode/pycode
					#mkdir的选项-p:如果上层目录不存在，递归建立所需目录
	
				（2）删除空文件夹
				hadoop fs -rmdir /某个空文件夹  
					#该命令不能删除非空文件夹
	
			3）上传文件或目录（put,copyFromLocal)
				（1）put的用法
					A.上传文件夹
					hdfs fs -put 文件夹路径 /
						举例：上传centos的本地文件到hdfs：
							hadoop fs -put file:/home/hadoop/pycode hdfs:/mycode
				（2）copyFromLocal的用法
	
			#./hadoop fs -text /passwd


		9. 关闭集群
		./sbin/hadoop stop-all.sh



## 第三章 搭建spark集群

### 一、预备知识

#### 1.参考文章

​		1）看了之后不再迷糊-Spark多种运行模式：https://www.jianshu.com/p/65a3476757a5
​		2）从源码上看spark yarn-lient和yarn-cluster模式的本质区别 http://bigdata.51cto.com/art/201709/552622.htm#topx
​				首先区分下AppMaster和Driver，任何一个yarn上运行的任务都必须有一个AppMaster，而任何一个Spark任务都会有一个Driver。
​				所以Driver和AppMaster是两个完全不同的东西，Driver是控制Spark计算和任务资源的，而AppMaster是控制yarn app运行和任务资源的，只不过在Spark on Yarn上，这两者就出现了交叉。
​				而在standalone模式下，资源则由Driver管理。在Spark on Yarn上，Driver会和AppMaster通信，资源的申请由AppMaster来完成，而任务的调度和执行则由Driver完成，Driver会通过与AppMaster通信来让Executor的执行具体的任务。



#### 2.spark的三种运行模式

​	1）本地模式
​		本质：非集群模式，该模式被称为Local[N]模式，是用单机的多个线程来模拟Spark分布式计算。
​			 通常用来验证应用程序的逻辑是否有问题。其中N代表可以使用N个线程，每个线程拥有一个core。
​			 如果不指定N，则默认是1个线程（该线程有1个core）。
​		验证： ./bin/run-example SparkPi 10 --master local[2]

​	2）集群模式：spark standalone
​		本质：集群模式，集群仅供spark使用，不依赖hadoop

​	3）集群模式：spark on yarn
​		本质：集群模式，使用yarn作为资源管理器
​		spark on yarn分为两种模式：client和cluster模式，二者的区别如下。
​			(1)应用场景不同
​				yarn cluster用于生产环境，yarn client用于交互与调试

​			(2)driver运行的位置不同
​				cluster模式中，driver运行在集群的AM中（或者：运行driver的container就是AM），负责向yarn的RM申请资源，并监督作业运行状况。
​				client模式中，driver在任务提交的机器上运行，AM仅向RM申请executor需要的资源，client通过和请求资源的container通信来调度任务。

​			(3)client运行持续时间不同
​				cluster模式中，用户提交作业后client就会关闭，作业会继续在yarn中运行；client模式中，client会和请求集群资源的container通信来调度任务，即client不会关闭。

​			(4)基于yarn时，spark-shell和pyspark必须要使用yarn-client模式



### 二、安装spark

	1.安装yarn
		参考hadoop2.8.4的安装文档安装完hadoop后，相关yarn的配置也已经完成，可以使用yarn了。
	2.安装Scala
		1）版本匹配问题
			请参考spark的官方文档，其中对hadoop、Scala的版本有指定。
			链接：http://spark.apache.org/downloads.html
	
		2)安装Scala
			# 先在master节点上执行如下安装步骤，后续会将配置好的安装文件复制到各slave节点。
			第一步，下载安装包，复制到~/app目录，解压
				解压命令：tar -zxvf 
	
			第二步，配置环境变量
				(1)打开配置文件，命令：sudo vim ~/.bashrc
					参考链接：bashrc和profile的区别 https://www.cnblogs.com/sddai/p/6534630.html
						bashrc和profile的差异在于：
						1. bashrc是在系统启动后就会自动运行。
						2. profile是在用户登录后才会运行。
						3. 进行设置后，可运用source bashrc命令更新bashrc，也可运用source profile命令更新profile。
						PS：通常我们修改bashrc,有些linux的发行版本不一定有profile这个文件
						4. /etc/profile中设定的变量(全局)的可以作用于任何用户，而~/.bashrc等中设定的变量(局部)只能继承/etc/profile中的变量，他们是"父子"关系。
				(2)配置SCALA_HOME，修改PATH，命令如下
				    SCALA_HOME=/home/hadoop/app/scala-2.11.12
					export PATH=$PATH:$SCALA_HOME/bin
	
				(3)使环境变量生效：source ~/.bashrc
				(4)验证：scala -version  
						如果出现如下信息，表示scala已经安装成功：
						Scala code runner version 2.11.12 -- Copyright 2002-2017, LAMP/EPFL
	3.安装spark
		第一步，下载安装包，复制到~/app目录，解压
			下载链接：http://spark.apache.org/downloads.html
			解压命令：tar -zxvf spark-2.3.1-bin-hadoop2.7.tgz
	
		第二步，修改文件名称
			命令： mv spark-2.3.1-bin-hadoop2.7 spark-2.3.1



		第三步，修改spark的配置文件
			（1）进入配置文件目录
				cd ~/app/spark-2.3.1/conf
	
			（2）配置spark-env.sh
				复制：cp spark-env.sh.template spark-env.sh
				打开：vim spark-env.sh
	
			    增加配置信息
				export JAVA_HOME=/home/hadoop/app/jdk1.8
				export SCALA_HOME=/home/hadoop/app/scala-2.11.12
				export HADOOP_HOME=/home/hadoop/app/hadoop-2.8.4
				export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
				#指定spark master的IP及端口
				#SPARK_MASTER_IP=master
				SPARK_MASTER_HOST=master01 
				SPARK_MASTER_PORT=7077
	
				#job history conf
				#注意：
				#1、因为hadoop的core-site.xml中配置的hdfs的默认端口为9000,所以spark.history.fs.logDirectory中hdfs的访问端口也是9000。
				#2、否则，会报错，如下：failed to launch: nice -n 0 /usr/local/bigdata/spark-2.3.1/bin/spark-class org.apache.spark.deploy.history.HistoryServer
				#3、参考链接：https://blog.csdn.net/kancy110/article/details/80374631
	
				export SPARK_HISTORY_OPTS="-Dspark.history.ui.port=18080 -Dspark.history.retainedApplications=5 -Dspark.history.fs.logDirectory=hdfs://master01:9000/spark/historyLog"


			（3）配置spark-default.conf
				#添加配置：保存应用运行的日志
				复制：cp spark-default.conf.template  spark-default.conf
				打开：vim spark-default.conf
	
				增加配置信息
					 spark.master                     spark://master01:7077
					 spark.eventLog.enabled           true
					 				 #注意：
					 #1、hdfs的访问端口应该与hadoop中core-site.xml中配置的一致，即9000
					 #2、日志存放的位置，应该与spark-env.sh中spark.history.fs.logDirectory的目录相同。
					 #3、日志存放的位置可以是本地（file://xxx)或hdfs，且该目录必须提前创建好。
					 #4、其他配置信息，见官网： http://spark.apache.org/docs/latest/monitoring.html
	
					 spark.eventLog.dir               hdfs://master01:9000/spark/historyLog  
					 spark.eventLog.compress 		  true
					 
			（4）配置从节点信息
				复制配置文件： cp slaves.template slaves
				打开文件： vim slaves
				增加从节点信息：
					首先，删除原有信息 localhost
					然后，添加从节点：
						slave01
						slave02
					最后，保存修改并退出：wq


​	
​		
​	
		第四步，复制安装文件到从节点
				(1)复制spark文件
					scp -r ~/app/spark-2.3.1 hadoop@slave01:~/app
					scp -r ~/app/spark-2.3.1 hadoop@slave02:~/app	
	                
				(2)复制scala文件
				scp -r ~/app/scala-2.11.12 hadoop@slave01:~/app
				scp -r ~/app/scala-2.11.12 hadoop@slave02:~/app
	
				(3)备注：此处无需在从节点上配置scala和spark的环境变量
	
		第五步，启动集群
			(1)进入master节点的spark目录
				cd ~/app/spark-2.3.1
	
			(2)	启动集群
				./sbin/start-all.sh
	
			(3)验证：查看相关进程是否启动
				在每个节点上执行jps命令，如果在主从节点上分别看到Master、Worker进程则集群启动成功。
	
		第六步，查看监控网页
			master01:8080
	
			备注：如果windows系统上没有修改hosts文件，则无法显示。
			修改windows上hosts文件的方法：
				(1)进入如下目录
					C:\Windows\System32\drivers\etc
				(2)右键单击hosts文件，依次点击
					右键-->属性-->安全-->选择当前账户-->编辑-->勾选需要所有权限-->确定
				(3)打开hsots文件，添加master节点的IP
					192.168.80.10 master01
	
		第七步，运行spark自带的示例程序
			参考链接：
				官方文档：http://spark.apache.org/docs/latest/quick-start.html
				spark submit参数及调优：https://www.cnblogs.com/haoyy/p/6893943.html
				【帮助命令】./bin/spark-submit --help
	
			(0)切换到spark安装目录
				cd ~/app/spark-2.3.1  #在该目录下执行以下验证命令
				
			(1)本地模式
				./bin/run-example SparkPi 10 --master local[2]
	
			(2)集群模式--standalone
				./bin/spark-submit --class org.apache.spark.examples.SparkPi --master spark://master:7077 lib/spark-examples-1.6.3-hadoop2.6.0.jar 100
	
			(3)集群模式--yarn-cluster
				./bin/spark-submit --class org.apache.spark.examples.SparkPi --master yarn --name Pi /home/hadoop/app/spark-2.3.1/examples/jars/spark-examples_2.11-2.3.1.jar 100

	4.修改spark打印日志的级别，减少屏幕上打印的日志量，便于阅读
		修改spark的conf目录下的log4j.properties
		命令：
			cp log4j.properties.template log4j.properties
			vim log4j.properties
			修改第19行的配置项为WARN：
				# Set everything to be logged to the console
				log4j.rootCategory=WARN, console
	
		将修改后的文件发送到slave01和slave02节点：
			scp log4j.properties hadoop@slave01:/home/hadoop/app/spark-3.0.1/conf
			scp log4j.properties hadoop@slave02:/home/hadoop/app/spark-3.0.1/conf




九、HDFS（hadoop分布式文件系统）
	一、HDFS的特点
		1.优点
			1）可存储超大文件
				（1）普通文件系统
					每个磁盘都有默认的数据块大小，这是磁盘在对数据进行读和写时要求的最小单位。
					文件系统是要构建于磁盘上的， 文件系统的也有块的逻辑概念，通常是磁盘块的整数倍，通常文件系统为几千个字节，而磁盘块一般为 512 个字节。
				（2）HDFS
					HDFS是一种文件系统，自身也有块（block）的概念，其文件块要比普通单一磁盘上文件系统大的多，默认是 64MB。
				（3）设计思想：最小化寻址开销
					HDFS上的块之所以设计的如此之大，其目的是为了最小化寻址开销。
					HDFS 文件的大小可以大于网络中任意一个磁盘的容量， 文件的所有块并不需要存储在一个磁盘上， 因此可以利用集群上任意一个磁盘进行存储， 由于具备这种分布式存储的逻辑，所以可以存储超大的文件，通常 G、T、P 级别。
			2）一次写入、多次读取
				一个文件经过创建、写入、关闭之后就无需改变，这种假设简化了数据一致性问题，同时提高了数据访问的吞吐量。
			3）可运行在普通廉价机器上
				hadoop的设计对硬件要求低，无需昂贵的高可用机器。

		2.HDFS不适用的场景
			1）数据量并不太大
				hadoop适用于PB/TP级别数据量，如果数据量只有几十GB，不建议使用hadoop，因为没有任何好处。
	
			2）大量小文件
				对于Hadoop系统，小文件通常定义为远小于HDFS的blocksize（默认64MB）的文件。由于每个文件都会产生各自的元数据，Hadoop 通过Namenode来存储这些信息，若小文件过多，容易导致Namenode存储出现瓶颈。
				且小文件过多，对数据寻址的时间开销可能会大于读取数据的时间，因而效率很低。
	
			3）低延时
				不适用于实时查询这种低延迟的场景，如股票实盘。但其他组件，如Hbase具有随机读、低延迟的特点。
	
			4）结构化数据
				HDFS适用于半结构、非结构化数据。结构化数据可以考虑用Hbase。
	
			5）多用户更新
				为了保证并发性，HDFS需要一次写入多次读取，目前不支持多用户写入。如需修改，也是通过追加的方式添加到文件的末尾处。出现太多文件需要更新的情况，Hadoop是不支持的。针对有多人写入数据的场景，可以考虑采用Hbase。


	二、HDFS体系架构
		1.主从架构
			HDFS 是一个主/从（Master/Slave）体系架构，由于分布式存储的性质，集群拥有两类节点 NameNode 和 DataNode。
	
		2.Namenode(名称节点))
			系统中通常只有一个namenode，中心服务器的角色，管理存储和检索多个DataNode的实际数据所需的所有元数据。
	
		3.DataNode（数据节点）
			系统中通常有多个datanode，是文件系统中真正存储数据的地方，在NameNode统一调度下进行数据块的创建、删除和复制。
	
		4.Client(客户端)
		 	Client是HDFS的客户端，应用程序可通过该模块与NameNode和DataNode进行交互，进行文件的读写操作。
	
	三、HDFS数据块复制
		参考链接：深刻理解HDFS工作机制
		https://www.cnblogs.com/wxisme/p/6270860.html
		
		1.多副本机制
			为了数据容错，文件系统会对所有数据块复制多份副本，默认3个副本。
	
		2.副本管理策略
			1）客户端节点上：默认放一个复本（若客户端运行在集群之外，会随机选择一个节点）。
			2）第二个复本：会放在与第一个不同且随机另外选择的机架中节点上，
			3）第三个复本：与第二个复本放在相同机架，切随机选择另一个节点。
			4）其他副本：所存在其他复本，则放在集群中随机选择的节点上，不过系统会尽量避免在相同机架上放太多复本。
			5）心跳和块报告：所有有关块复制的决策统一由 NameNode 负责，NameNode会周期性地接受集群中数据节点DataNode的心跳和块报告。一个心跳的到达表示这个数据节点是正常的。一个块报告包括该数据节点上所有块的列表。


	四、HDFS读取和写入流程
		参考《hadoop大数据入门与实战--电子书》p18~19
		1.读文件
	
		2.写文件


	五、操作HDFS的命令
		参考《hadoop大数据入门与实战--电子书》p20~21