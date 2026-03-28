package io.github.johneliud.movie_service.service;

import io.github.johneliud.movie_service.client.TmdbClient;
import io.github.johneliud.movie_service.entity.Movie;
import io.github.johneliud.movie_service.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PosterSyncService {

    private final MovieRepository movieRepository;
    private final TmdbClient tmdbClient;

    @Async
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void syncMissingPosters() {
        List<Movie> movies = movieRepository.findAllWithoutPoster();
        if (movies.isEmpty()) {
            log.info("Poster sync: all movies already have posters");
            return;
        }

        log.info("Poster sync: fetching posters for {} movies", movies.size());

        List<Movie> toUpdate = new ArrayList<>();
        for (Movie movie : movies) {
            tmdbClient.findPosterUrl(movie.getTitle(), movie.getReleaseYear()).ifPresent(url -> {
                movie.setPosterUrl(url);
                toUpdate.add(movie);
            });
        }

        if (!toUpdate.isEmpty()) {
            movieRepository.saveAll(toUpdate);
        }

        log.info("Poster sync complete — updated: {}, skipped: {}", toUpdate.size(), movies.size() - toUpdate.size());
    }
}