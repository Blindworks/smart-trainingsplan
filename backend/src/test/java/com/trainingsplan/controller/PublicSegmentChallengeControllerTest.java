package com.trainingsplan.controller;

import com.trainingsplan.dto.SegmentEffortResultDto;
import com.trainingsplan.entity.ActivityType;
import com.trainingsplan.service.SegmentChallengeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// NOTE: @WebMvcTest cannot load the context for this project because SmartTrainingsplanApplication
// uses a custom @ComponentScan that bypasses @WebMvcTest's TypeExcludeFilter, causing all
// @Components (CommandLineRunner seeders, all controllers) to be picked up. Satisfying their
// transitive bean dependencies (40+ repositories) in a slice test is impractical.
//
// Deviation from plan: replaced @WebMvcTest with MockMvcBuilders.standaloneSetup(), which builds
// a minimal MockMvc for just the one controller under test. All JSON assertions from the plan
// are preserved exactly. @WithMockUser is not needed because standaloneSetup does not apply
// the security filter chain (the test verifies controller behaviour and response shape only).
@ExtendWith(MockitoExtension.class)
class PublicSegmentChallengeControllerTest {

    @Mock
    private SegmentChallengeService service;

    @InjectMocks
    private PublicSegmentChallengeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void submitEffort_returnsResultJson() throws Exception {
        when(service.submitPublicEffort(eq("heartbreak-hill-2026"), eq(ActivityType.RIDE),
                eq("Lukas"), any(), eq("ride.gpx"), any()))
                .thenReturn(new SegmentEffortResultDto(7L, "tok", 47, 312, 298, "4:58", 46, 85.0, null, null, "VALID"));

        MockMultipartFile file = new MockMultipartFile("file", "ride.gpx",
                "application/gpx+xml", "<gpx/>".getBytes());

        mockMvc.perform(multipart("/api/public/challenges/heartbreak-hill-2026/efforts")
                        .file(file)
                        .param("displayName", "Lukas")
                        .param("type", "RIDE")
                        .with(req -> { req.setRemoteAddr("1.2.3.4"); return req; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rank").value(47))
                .andExpect(jsonPath("$.elapsedFormatted").value("4:58"));
    }

    @Test
    void submitEffort_rejectedMatch_returns422() throws Exception {
        when(service.submitPublicEffort(any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("start_gate_not_reached"));

        MockMultipartFile file = new MockMultipartFile("file", "ride.gpx",
                "application/gpx+xml", "<gpx/>".getBytes());

        mockMvc.perform(multipart("/api/public/challenges/heartbreak-hill-2026/efforts")
                        .file(file)
                        .param("displayName", "Lukas")
                        .param("type", "RIDE"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.reason").value("start_gate_not_reached"));
    }
}
