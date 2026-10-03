package com.travel.ai.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class TravelAppTest {

    @Resource
    private TravelApp travelApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好";
        String answer = travelApp.doChat(message, chatId);
        // 第二轮
        message = "我想制定一份旅游计划";
        answer = travelApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我们上次聊的旅行目的地是哪里来着？帮我回忆一下";
        answer = travelApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我想制定一份旅游计划，但不知道该怎么做";
        TravelApp.TravelReport travelReport = travelApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(travelReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我想了解去云南旅游有哪些注意事项？";
        String answer = travelApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("周末想和朋友去上海玩，推荐几个适合自由行的小众打卡地？");

        // 测试网页抓取：旅游攻略分析
        testMessage("帮我制定一份南京两日游计划");

        // 测试资源下载：图片下载
        testMessage("直接下载一张适合做手机壁纸的星空风景图片为文件");

        // 测试终端操作：执行代码
        testMessage("执行 Python3 脚本来生成数据分析报告");

        // 测试文件操作：保存用户档案
        testMessage("保存我的旅行计划档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份‘国庆旅行计划’PDF，包含行程安排、住宿预订和景点清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = travelApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
//        String message = "我的朋友居住在上海静安区，请帮我找到 5 公里内合适的游玩地点";
//        String answer =  travelApp.doChatWithMcp(message, chatId);
//        Assertions.assertNotNull(answer);
        // 测试图片搜索 MCP
        String message = "帮我搜索一些适合做旅行攻略封面的风景图片";
        String answer =  travelApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }
}
