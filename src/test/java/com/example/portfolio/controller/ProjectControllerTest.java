package com.example.portfolio.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.portfolio.dto.ProjectCreateDto;
import com.example.portfolio.security.LoginFailureHandler;
import com.example.portfolio.security.WebSecurityConfig;
import com.example.portfolio.service.AdminService;
import com.example.portfolio.service.CategoryService;
import com.example.portfolio.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@WebMvcTest({ProjectController.class, CategoryController.class})
@Import(WebSecurityConfig.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private AdminService adminService;

    @MockBean
    private LoginFailureHandler loginFailureHandler;

    @Test
    void projectReadsRemainPublic() throws Exception {
        when(projectService.getProjectList(any(), any(), any())).thenReturn(new SliceImpl<>(List.of()));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousProjectWritesAreRejected() throws Exception {
        mockMvc.perform(multipart("/api/projects")
                        .param("title", "Portfolio")
                        .param("categoryId", "1")
                        .param("subcategoryId", "2"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void authenticatedProjectWritesReachTheService() throws Exception {
        mockMvc.perform(multipart("/api/projects")
                        .param("title", "Portfolio")
                        .param("categoryId", "1")
                        .param("subcategoryId", "2")
                        .with(user("admin")))
                .andExpect(status().isOk());

        verify(projectService).createProject(any(ProjectCreateDto.class));
    }

    @Test
    void anonymousCategoryWritesAreRejected() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().is3xxRedirection());
    }
}
