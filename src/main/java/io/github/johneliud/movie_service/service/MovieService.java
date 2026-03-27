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

    public PagedResponse<MovieResponse> search(String title, String genre, Integer releaseYear, Pageable pageable) {
        log.debug("Searching movies - title: {}, genre: {}, releaseYear: {}", title, genre, releaseYear);
        Page<Movie> page = movieRepository.search(
                blankToNull(title),
                blankToNull(genre),
                releaseYear,
                pageable);
        log.debug("Search returned {} results", page.getTotalElements());
        return toPagedResponse(page);
    }

    @Transactional
    public MovieResponse update(String id, MovieRequest request) {
        log.debug("Updating movie id: {}", id);
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Update failed - movie not found: {}", id);
                    return new IllegalArgumentException("Movie not found with id: " + id);
                });

        movie.setTitle(request.title());
        movie.setGenres(request.genres());
        movie.setReleaseYear(request.releaseYear());
        movie.setDescription(request.description());
        movie.setPosterUrl(request.posterUrl());

        Movie saved = movieRepository.save(movie);
        log.info("Movie updated - id: {}, title: {}", saved.getId(), saved.getTitle());
        return toResponse(saved);
    }

    @Transactional
    public void delete(String id) {
        log.debug("Deleting movie id: {}", id);
        if (!movieRepository.existsById(id)) {
            log.warn("Delete failed - movie not found: {}", id);
            throw new IllegalArgumentException("Movie not found with id: " + id);
        }
        movieRepository.deleteById(id);
        log.info("Movie deleted - id: {}", id);
    }

    @Transactional
    public void updateAverageRating(String id, double averageRating) {
        log.debug("Updating averageRating for movie id: {} to {}", id, averageRating);
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found with id: " + id));
        movie.setAverageRating(averageRating);
        movieRepository.save(movie);
        log.info("AverageRating updated - id: {}, rating: {}", id, averageRating);
    }

    private MovieResponse toResponse(Movie movie) {
        return new MovieResponse(
                movie.getId(),
                movie.getTitle(),
                movie.getGenres(),
                movie.getReleaseYear(),
                movie.getDescription(),
                movie.getPosterUrl(),
                movie.getAverageRating(),
                movie.getCreatedAt());
    }

    private PagedResponse<MovieResponse> toPagedResponse(Page<Movie> page) {
        return new PagedResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}