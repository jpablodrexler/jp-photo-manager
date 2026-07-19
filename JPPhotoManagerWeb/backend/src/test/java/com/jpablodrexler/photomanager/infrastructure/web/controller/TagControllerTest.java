package com.jpablodrexler.photomanager.infrastructure.web.controller;

import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.domain.port.in.tag.ListTagsUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TagController.class)
@ActiveProfiles("test")
class TagControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ListTagsUseCase listTagsUseCase;

    @Test
    void searchTags_withQuery_returnsMatchingTagNames() throws Exception {
        when(listTagsUseCase.execute("vac")).thenReturn(List.of(
                Tag.builder().tagId(1L).name("vacation").build(),
                Tag.builder().tagId(2L).name("vaccinated").build()
        ));

        mockMvc.perform(get("/api/tags").param("q", "vac"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("vacation"))
                .andExpect(jsonPath("$[1]").value("vaccinated"))
                .andExpect(jsonPath("$.length()").value(2));

        verify(listTagsUseCase).execute("vac");
    }

    @Test
    void searchTags_withoutQuery_callsUseCaseWithNull() throws Exception {
        when(listTagsUseCase.execute(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(listTagsUseCase).execute(null);
    }

    @Test
    void searchTags_noResults_returnsEmptyArray() throws Exception {
        when(listTagsUseCase.execute("xyz")).thenReturn(List.of());

        mockMvc.perform(get("/api/tags").param("q", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
