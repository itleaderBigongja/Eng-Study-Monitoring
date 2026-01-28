//package com.eng.study.engstudy;
//
//import com.eng.study.engstudy.controller.MainController;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
//
//@WebMvcTest(MainController.class) // MainController만 로드하여 테스트
//public class MainControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Test
//    @DisplayName("메인 페이지 로드 테스트")
//    void getIndex() throws Exception {
//        mockMvc.perform(get("/"))
//                .andExpect(status().isOk()) // HTTP 200 OK
//                .andExpect(view().name("index")) // "index" 뷰 반환 확인
//                .andDo(print()); // 요청/응답 내용 출력
//    }
//}
