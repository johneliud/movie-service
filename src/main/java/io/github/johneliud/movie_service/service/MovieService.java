package io.github.johneliud.movie_service.service;

import io.github.johneliud.movie_service.dto.MovieRequest;
import io.github.johneliud.movie_service.dto.MovieResponse;
import io.github.johneliud.movie_service.dto.PagedResponse;
import io.github.johneliud.movie_service.entity.Movie;
import io.github.johneliud.movie_service.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovieService {

    private final MovieRepository movieRepository;

    @Transactional
    public MovieResponse create(MovieRequest request) {
        log.debug("Creating movie with title: {}", request.title());

        Movie movie = Movie.builder()
                .title(request.title())
                .genres(request.genres())
                .releaseYear(request.releaseYear())
                .description(request.description())
                .posterUrl(request.posterUrl())
                .build();

        Movie saved = movieRepository.save(movie);
        log.info("Movie created - id: {}, title: {}", saved.getId(), saved.getTitle());
        return toResponse(saved);
    }

    public MovieResponse findById(String id) {
        log.debug("Fetching movie by id: {}", id);
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Movie not found - id: {}", id);
                    return new IllegalArgumentException("Movie not found with id: " + id);
                });
        return toResponse(movie);
    }

    public PagedResponse<MovieResponse> findAll(Pageable pageable) {
        log.debug("Fetching all movies - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Movie> page = movieRepository.findAll(pageable);
        return toPagedResponse(page);
    }
}