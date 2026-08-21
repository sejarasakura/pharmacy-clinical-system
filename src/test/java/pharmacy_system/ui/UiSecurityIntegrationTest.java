package pharmacy_system.ui;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UiSecurityIntegrationTest {
    @Autowired MockMvc mockMvc;

    @Test
    void publicLoginRouteResolvesTheLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void protectedRouteRedirectsExpiredSessionToLogin() throws Exception {
        mockMvc.perform(get("/doctor/prescriptions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/login?returnTo=**"));
    }

    @Test
    void mutationWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/password/recovery").param("identity", "someone@example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mutationWithCsrfPassesTheSecurityBoundary() throws Exception {
        mockMvc.perform(post("/password/recovery").with(csrf())
                        .param("identity", "someone@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/password-recovery"));
    }
}
