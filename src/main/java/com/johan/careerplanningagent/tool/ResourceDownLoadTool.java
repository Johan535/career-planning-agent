package com.johan.careerplanningagent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import com.johan.careerplanningagent.constant.FileConstant;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 资源下载类
 */

public class ResourceDownLoadTool {
    @Tool(description = "Download a resource from a given URL")
    public String downloadResource(@ToolParam(description = "URL of the resource to download") String url, @ToolParam(description = "Name of the file to save the downloaded resource") String fileName) {
        String fileDir = FileConstant.FILE_SAVE_PATH + "/download";
        String filePath = fileDir + "/" + fileName;
        try {
            //创建保存目录
            FileUtil.mkdir(fileDir);
            //使用Hutool工具包的downloadFile的方法下载资源
            HttpUtil.downloadFile(url, new File(filePath));
            return "Resource downloaded successfully to: " + filePath;
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }
}
