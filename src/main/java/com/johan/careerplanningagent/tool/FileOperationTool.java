package com.johan.careerplanningagent.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import com.johan.careerplanningagent.constant.FileConstant;
import com.johan.careerplanningagent.manus.ManusSseContext;
import com.johan.careerplanningagent.service.GeneratedFileStore;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 文件读写；写入成功后会注册 fileId 并通过 Manus SSE 推送 artifact，供前端下载。
 */
@Component
public class FileOperationTool {

    private final GeneratedFileStore generatedFileStore;
    private final String fileDir = FileConstant.FILE_SAVE_PATH + "/file";

    public FileOperationTool(GeneratedFileStore generatedFileStore) {
        this.generatedFileStore = generatedFileStore;
    }

    @Tool(description = "Read content from a file under the agent file directory")
    public String readFile(@ToolParam(description = "Name of file to read") String fileName) {
        String safe = safeBasename(fileName);
        String filePath = fileDir + "/" + safe;
        try {
            return FileUtil.readString(filePath, "utf-8");
        } catch (IORuntimeException e) {
            return "ERROR : " + e.getMessage();
        }
    }

    @Tool(description = "Write content to a file (UTF-8). Prefer .md or .txt for user-facing documents.")
    public String writeFile(
            @ToolParam(description = "Name of file to write (basename only, e.g. plan.md)") String fileName,
            @ToolParam(description = "Content to write") String content) {
        String safe = safeBasename(fileName);
        String filePath = fileDir + "/" + safe;
        try {
            FileUtil.mkdir(fileDir);
            FileUtil.writeUtf8String(content == null ? "" : content, filePath);
            Path absolute = Path.of(filePath).toAbsolutePath().normalize();
            String owner = ManusSseContext.currentSessionKey();
            String fileId = generatedFileStore.register(absolute, owner, safe);
            ManusSseContext.sendArtifact(fileId, safe);
            return "文件已写入，fileId=" + fileId + "，展示文件名=" + safe
                    + "。请提示用户在页面「生成文件」区域点击「下载到本地」保存。"
                    + " 磁盘路径（内部）: " + filePath;
        } catch (IORuntimeException e) {
            return "ERROR :" + e.getMessage();
        }
    }

    private static String safeBasename(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "output.txt";
        }
        String n = fileName.trim().replace('\\', '/');
        int last = n.lastIndexOf('/');
        String base = last >= 0 ? n.substring(last + 1) : n;
        if (base.isBlank() || ".".equals(base) || "..".equals(base)) {
            return "output.txt";
        }
        return base;
    }
}
