使用说明：
一、将本文件夹拷贝到Tomcat的根目录下

二、修改config.properties属性文件的配置

三、运行
	Windows：
	1、修改restart.bat中路径的配置
	2、启动：双击startup.bat
	3、停止：关闭cmd窗口

	Linux：
	1、修改restart.sh中路径的配置
	2、授权sh文件的执行权限
		chmod 777 restart.sh
		chmod 777 startup.sh
		chmod 777 shutdown.sh
	3、启动执行：./startup.sh
	4、停止执行：./shutdown.sh