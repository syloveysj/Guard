

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class GuardMain {

	// 当前目录
	private static String rootPath = System.getProperty("user.dir");
	// 上级目录
	private static String parentRootPath = "";
	// 是否Windows
	private static boolean isWin = false;
	// 访问的URL地址
	private static String URLVISIT = "http://127.0.0.1";
	// 服务器端口
	private static String serverPort = "80";
	// 服务日志目录
	private static String serverLogsPath = "";
	// 检测间隔（单位：毫秒）
	private static long checkInterval = 0;
	// 检测失败重复次数
	private static int requestNum = 0;
	// 重复请求休眠时间（单位：毫秒）
	private static long requestInterval = 0;
	// 设置连接主机超时（单位：毫秒）
	private static int connectTimeout = 0;
	// 设置从主机读取数据超时（单位：毫秒）
	private static int readTimeout = 0;
	// 系统字符集编码
	private static String systemCharsetName = "GB2312";

	public static StringBuffer log = null;

	public static void main(String[] args) throws IOException {
		File dir = new File(rootPath);
		parentRootPath = dir.getParent();// 上一级目录

		Properties properties = new Properties();
		FileInputStream in = new FileInputStream(rootPath + File.separator + "config.properties");
		properties.load(in);

		// 获取key对应的value值
		URLVISIT = properties.getProperty("check_url");
		serverPort = String.valueOf(Integer.parseInt(properties.getProperty("server_port")));
		serverLogsPath = properties.getProperty("server_logs_path");
		if(serverLogsPath.trim().isEmpty()) serverLogsPath = parentRootPath + File.separator + "logs";
		
		checkInterval = Long.parseLong(properties.getProperty("check_interval")) * 1000;
		requestNum = Integer.parseInt(properties.getProperty("request_num"));
		requestInterval = Long.parseLong(properties.getProperty("request_interval")) * 1000;
		connectTimeout = Integer.parseInt(properties.getProperty("connect_timeout")) * 1000;
		readTimeout = Integer.parseInt(properties.getProperty("read_timeout")) * 1000;
		systemCharsetName = properties.getProperty("system_charset_name");

		String os = System.getProperty("os.name");
		if (os.toLowerCase().startsWith("win")) {
			isWin = true;
		}

		System.out.println("rootPath			=> " + rootPath);
		System.out.println("parentRootPath		=> " + parentRootPath);
		System.out.println("isWin				=> " + isWin);
		System.out.println("check_url			=> " + URLVISIT);
		System.out.println("server_port			=> " + serverPort);
		System.out.println("server_logs_path	=> " + serverLogsPath);
		System.out.println("checkInterval		=> " + checkInterval);
		System.out.println("requestNum			=> " + requestNum);
		System.out.println("requestInterval		=> " + requestInterval);
		System.out.println("connectTimeout		=> " + connectTimeout);
		System.out.println("readTimeout			=> " + readTimeout);
		System.out.println("systemCharsetName	=> " + systemCharsetName);

		while (true) {
			try {
				log = new StringBuffer();

				System.out.println("开始检测系统状态 <" + getDate() + ">");
				log.append("开始检测系统状态 <" + getDate() + ">\r\n");
				if (!checkGSSurvival()) {
					System.out.println("关闭tomcat");
					log.append("关闭tomcat\r\n");
					String processId = getProcessId();
					if (processId != null) {
						stopTomcat(processId);
					}

					bakLog();

					System.out.println("启动tomcat");
					log.append("启动tomcat\r\n");
					startTomcat();
				}
				System.out.println("系统检测结束 <" + getDate() + ">");
				log.append("系统检测结束 <" + getDate() + ">\r\n");
				System.out.println("Sleep...");
				log.append("Sleep...\r\n");

				writeFileContent(log.toString());
				Thread.sleep(checkInterval);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * 检查系统是否还存活
	 * 
	 * @return true 还存活， false 已经关闭
	 */
	public static boolean checkGSSurvival() {

		boolean result = true;

		for (int i = 0; i < requestNum; i++) {
			try {
				URL url = new URL(URLVISIT);
				HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
				// 设定传送的内容类型是可序列化的java对象
				// (如果不设此项,在传送序列化对象时,当WEB服务默认的不是这种类型时可能抛java.io.EOFException)
				httpURLConnection.setRequestProperty("Content-type", "application/x-java-serialized-object");
				// setConnectTimeout：设置连接主机超时（单位：毫秒）
				httpURLConnection.setConnectTimeout(connectTimeout);
				// setReadTimeout：设置从主机读取数据超时（单位：毫秒）
				httpURLConnection.setReadTimeout(readTimeout);
				httpURLConnection.connect();

				// 可以获取状态码
				System.out.println(httpURLConnection.getResponseCode());
				if (200 == (httpURLConnection.getResponseCode())) {
					System.out.println("系统运行正常！");
					log.append("系统运行正常！\r\n");
					return true;
				} else {
					System.out.println("异常！");
					log.append("异常！\r\n");
					result = false;
				}

				// 休眠10秒
				Thread.sleep(requestInterval);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				System.err.println("打开系统失败 <" + getDate() + ">");
				log.append("打开系统失败 <" + getDate() + ">\r\n");
				result = false;
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("访问系统打开失败 <" + getDate() + ">");
				log.append("访问系统打开失败 <" + getDate() + ">\r\n");
				result = false;
			} catch (InterruptedException e) {
				e.printStackTrace();
				result = false;
			}
		}
		return result;
	}

	/**
	 * 获取Tomcat进程ID
	 * 
	 * @return
	 */
	public static String getProcessId() {
		String processId = null;
		if (isWin) {
			Process p;
			String cmd = "cmd.exe /c netstat -ano | findstr " + serverPort;
			try {
				// 执行命令
				p = Runtime.getRuntime().exec(cmd);
				// 取得命令结果的输出流
				InputStream fis = p.getInputStream();

				// 用一个读输出流类去读
				// 用缓冲器读行
				LineNumberReader br = new LineNumberReader(new InputStreamReader(fis, systemCharsetName));
				String line = null, resultstr = null;
				// 直到读完为止
				while ((line = br.readLine()) != null) {
					if (line.contains("LISTENING")) { // 解析符合自己需要的內容，获取之后，直接返回。
						resultstr = line;
						break;
					}
				}

				if (resultstr != null) {
					int index = resultstr.indexOf("LISTENING");
					if (index > 0)
						processId = resultstr.substring(index + 9).trim();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Process p;
			String[] cmd = { "sh", "-c", "netstat -tunlp | grep " + serverPort };
			try {
				p = Runtime.getRuntime().exec(cmd);
				InputStream fis = p.getInputStream();
				LineNumberReader br = new LineNumberReader(new InputStreamReader(fis, systemCharsetName));
				String line = null, resultstr = null;
				while ((line = br.readLine()) != null) {
					if (line.contains("LISTEN") && line.contains(":::" + serverPort)) {
						resultstr = line;
						break;
					}
				}

				if (resultstr != null) {
					int sindex = resultstr.indexOf("LISTEN");
					int eindex = resultstr.indexOf("/");
					if (sindex > 0 && eindex > sindex)
						processId = resultstr.substring(sindex + 6, eindex).trim();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return processId;
	}

	/**
	 * 停止Tomcat
	 * 
	 * @param processId
	 */
	public static void stopTomcat(String processId) {
		if (isWin) {
			Process p;
			String cmd = "taskkill /F /PID " + processId;
			try {
				p = Runtime.getRuntime().exec(cmd);
				InputStream fis = p.getInputStream();

				LineNumberReader br = new LineNumberReader(new InputStreamReader(fis, systemCharsetName));
				String line = null;
				while ((line = br.readLine()) != null) {
					System.out.println(line);
					log.append(line + "\r\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Process p;
			String cmd = "kill -9 " + processId;
			try {
				p = Runtime.getRuntime().exec(cmd);
				InputStream fis = p.getInputStream();

				LineNumberReader br = new LineNumberReader(new InputStreamReader(fis, systemCharsetName));
				String line = null;
				while ((line = br.readLine()) != null) {
					System.out.println(line);
					log.append(line + "\r\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 启动Tomcat
	 */
	public static void startTomcat() {
		if (isWin) {
//			String cmd = "cmd.exe /c " + rootPath + File.separator + "restart.bat";//路径中不能含有括号
			String cmd = "cmd.exe /c restart.bat";
			try {
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
//			String[] cmd = { "sh", "-c", rootPath + File.separator + "restart.sh" };
			String[] cmd = { "sh", "-c", "restart.sh" };
			try {
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 获取系统时间
	 * 
	 * @return
	 */
	public static String getDate() {
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd :HH:mm:ss");
		return dateFormat.format(date);
	}

	public static String getDate2() {
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		return dateFormat.format(date);
	}

	public static String getDate3() {
		Date date = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormat.format(date);
	}

	public static void copyFileUsingFileChannels(File source, File dest) throws IOException {
		FileChannel inputChannel = null;
		FileChannel outputChannel = null;
		try {
			inputChannel = new FileInputStream(source).getChannel();
			outputChannel = new FileOutputStream(dest).getChannel();
			outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
		} finally {
			inputChannel.close();
			outputChannel.close();
		}
	}

	public static void bakLog() {
		try {
			// 服务器日志文件位置
			File logsDir = new File(serverLogsPath);

			// 备份日志文件位置
			String bakPath = rootPath + File.separator + "mlogs" + File.separator + "bak" + File.separator + getDate2();
			File bakDir = new File(bakPath);
			if (!bakDir.exists()) bakDir.mkdirs();
			
			String key = getDate3();
			if (logsDir.exists()) {
	            File[] files = logsDir.listFiles();
	            for (File file : files) {
	                if (!file.isDirectory()) {
	                	String fileName = file.getName();
//	                	String filePath = file.getAbsolutePath();
	                	if(fileName.contains(key)) {
	                		File bakFile = new File(bakPath + File.separator + fileName);
            				if (bakFile.exists()) bakFile.createNewFile();

            				copyFileUsingFileChannels(file, bakFile);
	                	}
	                }
	            }
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void writeFileContent(String content) {
		FileWriter fw = null;
		try {
			String dirPath = rootPath + File.separator + "mlogs";
			File dir = new File(dirPath);
			if (!dir.exists()) {
				dir.mkdirs();
			}

			String filePath = dirPath + File.separator + "rec." + getDate3() + ".log";
			File file = new File(filePath);
			fw = new FileWriter(file, true);

			PrintWriter pw = new PrintWriter(fw);
			pw.println(content);
			pw.flush();
			try {
				fw.flush();
				pw.close();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
