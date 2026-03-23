package com.johan.careerplanningagent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import com.johan.careerplanningagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;

/*
* 文件操作工具类（提供文件读写功能）
* */
public class FileOperationTool {
    //文件保存目录
    private final String FILE_DIR  = FileConstant.FILE_SAVE_PATH + "/file";

    //读取文件
    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of file to read") String fileName){
        String filePath = FILE_DIR + "/" + fileName;
        try {
            return FileUtil.readString(filePath, "utf-8");
        } catch (IORuntimeException e) {
            return "ERROR : " + e.getMessage();
        }

    }

    //写入文件
    @Tool(description = "Write content to a file")
    public String writeFile(@ToolParam(description = "Name of file to write") String fileName,
                           @ToolParam(description = "Content to write") String content){

        String filePath = FILE_DIR + "/" + fileName;

        //创建文件保存目录
        try {
            FileUtil.mkdir(FILE_DIR); //创建文件保存目录
            FileUtil.writeUtf8String(content,filePath); //写入文件
            return "FILE written successfully to :" + filePath; //返回文件保存路径
        } catch (IORuntimeException e) {
            return "ERROR :" +  e.getMessage();
        }

    }


}
