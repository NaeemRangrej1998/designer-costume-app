package com.designer.costume.controller;

import com.designer.costume.dto.HealthResponse;
import com.designer.costume.service.HealthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.designer.costume.security.CustomUserDetailsService;
import com.designer.costume.security.JwtService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthService healthService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Test
    @WithMockUser
    void returnsApiHealthContract() throws Exception {
        when(healthService.getHealth())
                .thenReturn(new HealthResponse("UP"));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "success": true,
                          "message": "Backend is running",
                          "data": {
                            "status": "UP"
                          }
                        }
                        """, false));
    }
}
