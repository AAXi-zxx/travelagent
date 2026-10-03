package com.travel.ai.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TravelAppDocumentLoaderTest {

    @Resource
    private TravelAppDocumentLoader travelAppDocumentLoader;

    @Test
    void loadMarkdowns() {
        travelAppDocumentLoader.loadMarkdowns();
    }
}