package com.travel.ai.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 旅游大师向量数据库配置（初始化基于内存的向量数据库 Bean）
 */
@Configuration
public class TravelAppVectorStoreConfig {

    @Resource
    private TravelAppDocumentLoader travelAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    // 关键词增强会调用 LLM，中科大免费 API 限流 20 次/分钟，启动批量调用会触发 429，故暂时跳过
    // @Resource
    // private MyKeywordEnricher myKeywordEnricher;

    @Bean
    VectorStore travelAppVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        // 加载文档
        List<Document> documentList = travelAppDocumentLoader.loadMarkdowns();
        // 自主切分文档
        List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documentList);
        // 跳过关键词增强（避免启动时批量调用 LLM 触发中科大 API 限流 429）
        simpleVectorStore.add(splitDocuments);
        return simpleVectorStore;
    }
}
