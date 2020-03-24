

import java.io.File;

public class Test {

	public static void main(String[] args) {
		String logsDir = "/Users/sunyong/Developer/eclipse-mars/workspace/MyTools/src/main/java";
		File logdir = new File(logsDir);
		
		if (logdir.exists()) {
            File[] files = logdir.listFiles();
            for (File file : files) {
                if (!file.isDirectory()) {
                	String fileName = file.getName();
                	String filePath = file.getAbsolutePath();
                	System.out.println("fileName=" + fileName);
                	System.out.println("strFileName=" + filePath);
                }
            }
        }
	}

}
