package com.nchuy099.ecommerce.notification;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.nchuy099.ecommerce.common.event.NotificationChannel;
import com.nchuy099.ecommerce.notification.controller.NotificationAdminController;
import com.nchuy099.ecommerce.notification.dto.NotificationDlqResponse;
import com.nchuy099.ecommerce.notification.dto.NotificationReplayResponse;
import com.nchuy099.ecommerce.notification.exception.NotificationDlqNotFoundException;
import com.nchuy099.ecommerce.notification.service.NotificationDlqService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationAdminController.class)
class NotificationAdminControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationDlqService dlqService;

    @Test
    void listsDlqEntries() throws Exception {
        when(dlqService.findAll()).thenReturn(List.of(new NotificationDlqResponse(
                1L,
                "campaign-1",
                100L,
                NotificationChannel.EMAIL,
                "provider down",
                3,
                "OPEN",
                Instant.parse("2026-09-05T00:00:00Z")
        )));

        mockMvc.perform(get("/v1/admin/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].channel").value("EMAIL"));
    }

    @Test
    void replaysDlqEntry() throws Exception {
        when(dlqService.replay(1L)).thenReturn(new NotificationReplayResponse(1L, "REPLAYED", false));

        mockMvc.perform(post("/v1/admin/dlq/1/replay"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.dlqId").value(1))
                .andExpect(jsonPath("$.data.status").value("REPLAYED"))
                .andExpect(jsonPath("$.data.alreadySent").value(false));
    }

    @Test
    void missingDlqReturnsProblemDetail() throws Exception {
        when(dlqService.replay(404L)).thenThrow(new NotificationDlqNotFoundException(404L));

        mockMvc.perform(post("/v1/admin/dlq/404/replay"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Notification DLQ entry not found"));
    }
}
