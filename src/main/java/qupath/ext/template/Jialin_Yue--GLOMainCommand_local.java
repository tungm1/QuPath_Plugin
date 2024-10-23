package qupath.ext.template;

import javafx.application.Platform;
import javafx.concurrent.Task;
import qupath.lib.io.PathIO;
import java.io.InputStream;
import java.io.FileInputStream; 
import java.util.ArrayList;
import java.util.List;

import qupath.lib.gui.QuPathGUI;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.objects.PathObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class GLOMainCommand {

    private static final Logger logger = LoggerFactory.getLogger(GLOMainCommand.class);
    private final QuPathGUI qupath; // QuPath GUI 实例
    private String serverURL; // 服务器URL
    private String targetDir; // 目标目录
    private String pythonScriptPath; // Python 脚本路径

    public GLOMainCommand(QuPathGUI qupath, String pythonScriptPath, String targetDir) {
        this.qupath = qupath;
        this.pythonScriptPath = pythonScriptPath; // 设置 Python 脚本路径
        this.targetDir = targetDir; // 设置目标目录
    }

    // 提交检测任务
    public void submitDetectionTask() {

	// 获取全景图像的文件路径
	String rawPath = qupath.getViewer().getImageData().getServer().getPath(); // 获取包含前缀的路径
	String wholeSlideImagePath;

	// Check if the path contains the "file:" prefix
	if (rawPath.contains("file:")) {
	    wholeSlideImagePath = rawPath.split("file:")[1].trim(); // 提取 'file:' 后面的部分，并去掉空格
	} else {
	    wholeSlideImagePath = rawPath; // 如果没有 "file:"，直接使用原始路径
	}

	System.out.println("Extracted Whole Slide Image Path: " + wholeSlideImagePath);

        // 根据 WSI 名称生成 GeoJSON 文件名
        String wsiName = new File(wholeSlideImagePath).getName();
        if (wsiName.endsWith(".svs")) {
            wsiName = wsiName.replace(".svs", ".geojson");
        } else if (wsiName.endsWith(".scn")) {
            wsiName = wsiName.replace(".scn", ".geojson");
        } else {
            logger.warn("Unsupported WSI format for file: " + wholeSlideImagePath);
            return;
        }

        // 调用生成 GeoJSON 路径的方法
        String geojsonDir = generateGeoJsonPath(targetDir);  // targetDir 替换 xml_dir

	// 准备好 Python 解释器的绝对路径和 Python 脚本的路径
	String pythonInterpreter = "/home/yuej2/anaconda3/envs/CircleNet/bin/python3.7";  // 使用 Conda 虚拟环境中的 Python
	String pythonScriptPath = "/data/CircleNet/src/run_detection_for_scn.py";  // 您的 Python 脚本路径
	



	// 准备命令行参数，传递给 Python 脚本的其他参数
	List<String> command = new ArrayList<>();
	
	//command.add("sudo");  // Prepend 'sudo' to the command
	command.add(pythonInterpreter);  // 第一个参数是 Python 解释器
	command.add(pythonScriptPath);   // 第二个参数是 Python 脚本路径
	command.add("circledet");  // 下面的参数是传递给 Python 脚本的
	command.add("--circle_fusion");
	command.add("--generate_geojson");
	command.add("--arch");
	command.add("dla_34");
	command.add("--demo");
	command.add(wholeSlideImagePath);  // WSI 路径
	command.add("--load_model_dir");
	command.add("/data/CircleNet/exp/circledet/SPIE_model_v3");
	command.add("--filter_boarder");
	command.add("--demo_dir");
	command.add("/data/CircleNet/data/new_data_for_miccai_paper/test/test_geojson/test_result");
	command.add("--target_dir");
	command.add(targetDir);  // 目标目录

	// 使用 ProcessBuilder 启动进程
	ProcessBuilder processBuilder = new ProcessBuilder(command);
	
	Map<String, String> env = processBuilder.environment();
	
	// Explicitly add the Python interpreter's directory to the PATH
	env.put("PATH", "/home/yuej2/anaconda3/envs/CircleNet/bin:" + env.get("PATH"));

	// Set the PYTHONPATH to include the directory where pycocotools is installed
	env.put("PYTHONPATH", "/home/yuej2/.local/lib/python3.7/site-packages");
	
	System.out.println("Environment PATH: " + env.get("PATH"));//check if the path is right

	processBuilder.redirectErrorStream(true);

	// Redirect error and output streams to capture the output
	processBuilder.redirectErrorStream(true);  // Merge error stream into output stream

	Process process = null;  // Initialize the process variable
	try {
	    process = processBuilder.start();

	    // Capture the output from the process
	    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    String line;
	    StringBuilder output = new StringBuilder();
	    
	    while ((line = reader.readLine()) != null) {
		output.append(line).append("\n");
		System.out.println(line);  // Optionally log output to console
	    }

	    // Wait for the process to complete and check the exit value
	    int exitCode = process.waitFor();
	    if (exitCode != 0) {
		logger.error("Python script exited with error code: " + exitCode);
		logger.error("Python script output: " + output.toString());
	    } else {
		logger.info("Python script executed successfully.");
		logger.info("Python script output: " + output.toString());
	    }

	} catch (IOException e) {
	    logger.error("Failed to start Python process", e);
	} catch (InterruptedException e) {
	    logger.error("Python process was interrupted", e);
	}
        // 读取Python脚本的输出
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        } catch (IOException e) {
            logger.error("Error reading Python output", e);
        }

        // 解析输出结果
        parsePythonOutput(output.toString(), geojsonDir, wsiName);
    }

    // 方法用于生成 GeoJSON 保存目录路径
    private String generateGeoJsonPath(String targetDir) {
        Path targetDirPath = Paths.get(targetDir);
        String baseName = targetDirPath.getFileName().toString();
        String geoJsonDir = targetDirPath.getParent().resolve(baseName + "_geojson").toString();
        return geoJsonDir;
    }

    // 方法用于解析 Python 输出并加载 GeoJSON 文件到 QuPath
    private void parsePythonOutput(String output, String geojsonDir, String wsiName) {
        // 此处可以添加对 Python 输出的解析逻辑

        // 加载生成的 GeoJSON 文件
        loadGeoJsonToQuPath(geojsonDir, wsiName);
    }

    // 方法用于加载 GeoJSON 文件到 QuPath
    private void loadGeoJsonToQuPath(String geojsonDir, String wsiName) {
        File geojsonFile = new File(geojsonDir, wsiName); // 使用生成的 GeoJSON 文件名
        if (geojsonFile.exists()) {
            // 获取文件的输入流
            try (InputStream inputStream = new FileInputStream(geojsonFile)) {
                // 使用 PathIO 提供的 readObjectsFromGeoJSON 方法读取 GeoJSON 文件
                List<PathObject> objects = PathIO.readObjectsFromGeoJSON(inputStream);
                // 将对象添加到 QuPath 的层次结构中
                qupath.getViewer().getImageData().getHierarchy().addPathObjects(objects);
                logger.info("GeoJSON file loaded successfully: " + geojsonFile.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Failed to read GeoJSON file: " + geojsonFile.getAbsolutePath(), e);
            }
        } else {
            logger.warn("GeoJSON file not found: " + geojsonFile.getAbsolutePath());
        }
    }
}

