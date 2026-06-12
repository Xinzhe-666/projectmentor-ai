package com.xinzhe.projectmentor.admin.interceptor;

import com.xinzhe.projectmentor.admin.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminInterceptorTests {

    @Test
    void ordinaryUserCannotAccessAdminCreditEndpoints() throws Exception {
        AdminService adminService = mock(AdminService.class);
        when(adminService.isCurrentUserAdmin()).thenReturn(false);
        AdminInterceptor interceptor = new AdminInterceptor(adminService);
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST", "/api/admin/credits/users/7/grant"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":40300");
    }
}
