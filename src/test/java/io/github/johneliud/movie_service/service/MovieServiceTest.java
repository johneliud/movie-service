package io.github.johneliud.movie_service.service;

import io.github.johneliud.movie_service.dto.MovieRequest;
import io.github.johneliud.movie_service.dto.MovieResponse;
import io.github.johneliud.movie_service.dto.PagedResponse;
import io.github.johneliud.movie_service.entity.Movie;
import io.github.johneliud.movie_service.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    private static final String ID = UUID.randomUUID().toString();

    private Movie movie() {
        return Movie.builder()
                .id(ID)
                .title("Inception")
                .genres(List.of("Sci-Fi", "Thriller"))
                .releaseYear(2010)
                .description("A thief who steals corporate secrets")
                .posterUrl("https://example.com/inception.jpg")
                .averageRating(8.8)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private MovieRequest request() {
        return new MovieRequest("Inception", List.of("Sci-Fi", "Thriller"), 2010,
                "A thief who steals corporate secrets", "https://example.com/inception.jpg");
    }

    @Test
    void create_savesAndReturnsMovieResponse() {
        when(movieRepository.save(any(Movie.class))).thenReturn(movie());

        MovieResponse response = movieService.create(request());

        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.title()).isEqualTo("Inception");
        assertThat(response.releaseYear()).isEqualTo(2010);
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void findById_returnsMovieResponse() {
        when(movieRepository.findById(ID)).thenReturn(Optional.of(movie()));

        MovieResponse response = movieService.findById(ID);

        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.title()).isEqualTo("Inception");
    }

    @Test
    void findById_throwsIllegalArgumentException_whenNotFound() {
        when(movieRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.findById(ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ID);
    }

    @Test
    void findAll_returnsPagedResponse() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Movie> page = new PageImpl<>(List.of(movie()), pageable, 1);
        when(movieRepository.findAll(pageable)).thenReturn(page);

        PagedResponse<MovieResponse> response = movieService.findAll(pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.page()).isEqualTo(0);
    }

    @Test
    void search_withAllFilters_delegatesToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Movie> page = new PageImpl<>(List.of(movie()), pageable, 1);
        when(movieRepository.search("Inception", "Sci-Fi", 2000, 2020, pageable)).thenReturn(page);

        PagedResponse<MovieResponse> response = movieService.search("Inception", "Sci-Fi", 2000, 2020, pageable);

        assertThat(response.content()).hasSize(1);
        verify(movieRepository).search("Inception", "Sci-Fi", 2000, 2020, pageable);
    }

    @Test
    void search_withBlankStrings_passesNullsToRepository() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Movie> page = new PageImpl<>(List.of(), pageable, 0);
        when(movieRepository.search(null, null, null, null, pageable)).thenReturn(page);

        movieService.search("  ", "  ", null, null, pageable);

        verify(movieRepository).search(null, null, null, null, pageable);
    }

    @Test
    void update_updatesAndReturnsMovieResponse() {
        Movie existing = movie();
        when(movieRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(movieRepository.save(existing)).thenReturn(existing);

        MovieResponse response = movieService.update(ID, request());

        assertThat(response.title()).isEqualTo("Inception");
        verify(movieRepository).save(existing);
    }

    @Test
    void update_throwsIllegalArgumentException_whenNotFound() {
        when(movieRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.update(ID, request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ID);
    }

    @Test
    void delete_deletesMovie() {
        when(movieRepository.existsById(ID)).thenReturn(true);

        movieService.delete(ID);

        verify(movieRepository).deleteById(ID);
    }

    @Test
    void delete_throwsIllegalArgumentException_whenNotFound() {
        when(movieRepository.existsById(ID)).thenReturn(false);

        assertThatThrownBy(() -> movieService.delete(ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ID);
    }

    @Test
    void updateAverageRating_updatesRating() {
        Movie existing = movie();
        when(movieRepository.findById(ID)).thenReturn(Optional.of(existing));
        when(movieRepository.save(existing)).thenReturn(existing);

        movieService.updateAverageRating(ID, 9.0);

        assertThat(existing.getAverageRating()).isEqualTo(9.0);
        verify(movieRepository).save(existing);
    }

    @Test
    void updateAverageRating_throwsIllegalArgumentException_whenNotFound() {
        when(movieRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movieService.updateAverageRating(ID, 9.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ID);
    }
}