package com.studioos.server.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import com.studioos.server.dashboard.dto.DashboardOverviewResponse;
import com.studioos.server.shared.enums.Role;
import com.studioos.server.user.User;

@ExtendWith(MockitoExtension.class)
class DashboardOverviewControllerTest {

    @Mock
    private DashboardOverviewService dashboardOverviewService;

    @InjectMocks
    private DashboardOverviewController controller;

    @Test
    void returnsOverviewForAuthenticatedDashboardUser() {
        User user = User.builder().id(7).role(Role.ARTIST).build();
        DashboardOverviewResponse overview = DashboardOverviewResponse.builder().build();
        when(dashboardOverviewService.getOverview(user)).thenReturn(overview);

        var response = controller.getOverview(user);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(overview);
        verify(dashboardOverviewService).getOverview(user);
    }

    @Test
    void restrictsOverviewToRegularDashboardRoles() throws NoSuchMethodException {
        PreAuthorize annotation = DashboardOverviewController.class
                .getMethod("getOverview", User.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasAnyRole('USER', 'ARTIST', 'PRODUCER')");
    }
}
