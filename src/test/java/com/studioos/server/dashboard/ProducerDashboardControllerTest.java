package com.studioos.server.dashboard;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studioos.server.dashboard.dto.ProducerDashboardResponse;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProducerDashboardControllerTest {

    @Mock
    private ProducerDashboardService producerDashboardService;

    @InjectMocks
    private ProducerDashboardController controller;

    @Test
    void rejectsNonProducers() {
        User user = User.builder().id(10).role(Role.USER).email("user@example.com").name("User").build();

        assertThatThrownBy(() -> controller.getMyResponse(user))
                .isInstanceOf(StudioosException.class)
                .hasMessageContaining("Only producers");
    }

    @Test
    void allowsProducers() {
        User user = User.builder().id(10).role(Role.PRODUCER).email("producer@example.com").name("Producer").build();
        ProducerDashboardResponse response = ProducerDashboardResponse.builder().totalBeats(1).build();

        when(producerDashboardService.getDashboard(10)).thenReturn(response);

        ProducerDashboardResponse actual = controller.getMyResponse(user);

        verify(producerDashboardService).getDashboard(10);
        org.assertj.core.api.Assertions.assertThat(actual).isSameAs(response);
    }
}
