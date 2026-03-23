package com.johan.careerplanningagent.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceDownLoadToolTest {

    @Test
    void downloadResource() {
        ResourceDownLoadTool resourceDownLoadTool = new ResourceDownLoadTool();
        String url = "https://static.leetcode.cn/cn-frontendx-assets/production/_next/static/images/logo-ff2b712834cf26bf50a5de58ee27bcef.png?x-oss-process=image%2Fformat%2Cwebp";
        String filename = "logo.png";
        String result  = resourceDownLoadTool.downloadResource(url,filename);
        assertNotNull(result);
    }
}