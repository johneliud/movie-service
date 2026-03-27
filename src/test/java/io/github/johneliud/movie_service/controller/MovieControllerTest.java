package io.github.johneliud.movie_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.johneliud.movie_service.dto.MovieRequest;
import io.github.johneliud.movie_service.dto.MovieResponse;
import io.github.johneliud.movie_service.dto.PagedResponse;
import io.github.johneliud.movie_service.exception.GlobalExceptionHandler;
import io.github.johneliud.movie_service.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MovieControllerTest {

    @Mock
    private MovieService movieService;

    @InjectMocks
    private MovieController movieController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final String ID = "test-movie-id";

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(movieController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
    }

    private MockHttpServletRequestBuilder withAdminPrincipal(MockHttpServletRequestBuilder builder) {
        var auth = new UsernamePasswordAuthenticationToken(
                "user-id", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        return builder.principal(auth);
    }

    private MovieResponse movieResponse() {
        return new MovieResponse(ID, "Inception", List.of("Sci-Fi"), 2010, "desc", "url", 8.8, LocalDateTime.now());
    }

    private MovieRequest movieRequest() {
        return new MovieRequest("Inception", List.of("Sci-Fi"), 2010, "desc", "url");
    }

    @Test
    void create_returnsCreated_withValidRequest() throws Exception {
        when(movieService.create(any(MovieRequest.class))).thenReturn(movieResponse());

        mockMvc.perform(withAdminPrincipal(
                        post("/api/movies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(movieRequest()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.title").value("Inception"));
    }

    @Test
    void create_returnsBadRequest_whenTitleBlank() throws Exception {
        MovieRequest invalid = new MovieRequest("", List.of("Sci-Fi"), 2010, "desc", "url");

        mockMvc.perform(withAdminPrincipal(
                        post("/api/movies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalid))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_returnsOk() throws Exception {
        PagedResponse<MovieResponse> paged = new PagedResponse<>(List.of(movieResponse()), 0, 20, 1, 1);
        when(movieService.findAll(any())).thenReturn(paged);

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Inception"));
    }

    @Test
    void search_returnsOk_withFilters() throws Exception {
        PagedResponse<MovieResponse> paged = new PagedResponse<>(List.of(movieResponse()), 0, 20, 1, 1);
        when(movieService.search(any(), any(), any(), any(), any())).thenReturn(paged);

        mockMvc.perform(get("/api/movies/search")
                        .param("title", "Inception")
                        .param("genre", "Sci-Fi")
                        .param("releaseYearFrom", "2000")
                        .param("releaseYearTo", "2020"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(ID));
    }

    @Test
    void findById_returnsOk() throws Exception {
        when(movieService.findById(ID)).thenReturn(movieResponse());

        mockMvc.perform(get("/api/movies/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID));
    }

    @Test
    void findById_returnsBadRequest_whenMovieNotFound() throws Exception {
        when(movieService.findById(ID)).thenThrow(new IllegalArgumentException("Movie not found with id: " + ID));

        mockMvc.perform(get("/api/movies/{id}", ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Movie not found with id: " + ID));
    }

    @Test
    void update_returnsOk() throws Exception {
        when(movieService.update(eq(ID), any(MovieRequest.class))).thenReturn(movieResponse());

        mockMvc.perform(withAdminPrincipal(
                        put("/api/movies/{id}", ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(movieRequest()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID));
    }

    @Test
    void update_returnsBadRequest_whenTitleBlank() throws Exception {
        MovieRequest invalid = new MovieRequest("", List.of("Sci-Fi"), 2010, "desc", "url");

        mockMvc.perform(withAdminPrincipal(
                        put("/api/movies/{id}", ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalid))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        doNothing().when(movieService).delete(ID);

        mockMvc.perform(withAdminPrincipal(delete("/api/movies/{id}", ID)))
                .andExpect(status().isNoContent());
    }
}