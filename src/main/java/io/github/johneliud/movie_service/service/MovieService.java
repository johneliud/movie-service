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
}