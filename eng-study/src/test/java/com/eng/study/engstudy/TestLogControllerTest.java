package com.eng.study.engstudy;

import com.eng.study.engstudy.controller.TestLogController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestLogController.class) // TestLogController만 로드하여 테스트
public class TestLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("CRITICAL 로그 트리거 API 테스트")
    void triggerCriticalLogTest() throws Exception {
        mockMvc.perform(get("/test/critical"))
                .andExpect(status().isOk()) // HTTP 200 OK
                .andExpect(content().string("CRITICAL 로그 발생 완료"))
                .andDo(print());
    }
}
