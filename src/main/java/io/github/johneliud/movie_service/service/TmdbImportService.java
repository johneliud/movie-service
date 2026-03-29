package io.github.johneliud.movie_service.service;

import io.github.johneliud.movie_service.client.TmdbClient;
import io.github.johneliud.movie_service.client.TmdbClient.DiscoverResult;
import io.github.johneliud.movie_service.client.TmdbClient.TmdbMovieRaw;
import io.github.johneliud.movie_service.entity.Movie;
import io.github.johneliud.movie_service.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmdbImportService {

    private final MovieRepository movieRepository;
    private final TmdbClient tmdbClient;

    @Value("${tmdb.import.pages}")
    private int importPages;

    @Async
    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void importMovies() {
        if (!tmdbClient.isConfigured()) {
            log.info("TMDB import skipped — API key not configured");
            return;
        }

        log.info("TMDB import started — fetching up to {} page(s)", importPages);

        Map<Integer, String> genreMap = tmdbClient.fetchGenreMap();
        if (genreMap.isEmpty()) {
            log.warn("TMDB import: genre map is empty, genre names will be omitted");
        }

        int imported = 0;
        int skipped = 0;

        for (int page = 1; page <= importPages; page++) {
            DiscoverResult result = tmdbClient.fetchDiscoverPage(page).orElse(null);
            if (result == null) {
                log.warn("TMDB import: page {} returned no data, stopping", page);
                break;
            }

            for (TmdbMovieRaw raw : result.movies()) {
                if (movieRepository.existsByTmdbId(raw.id())) {
                    skipped++;
                    continue;
                }

                Movie movie = Movie.builder()
                        .tmdbId(raw.id())
                        .title(raw.title())
                        .genres(resolveGenres(raw.genreIds(), genreMap))
                        .releaseYear(parseYear(raw.releaseDate()))
                        .description(raw.overview())
                        .posterUrl(raw.posterPath() != null && !raw.posterPath().isBlank()
                                ? tmdbClient.buildPosterUrl(raw.posterPath())
                                : null)
                        .build();

                movieRepository.save(movie);
                imported++;
            }

            int totalPages = result.totalPages();
            log.debug("TMDB import: page {}/{} processed", page, Math.min(importPages, totalPages));

            if (page >= totalPages) {
                log.info("TMDB import: reached last available page ({})", totalPages);
                break;
            }
        }

        log.info("TMDB import complete — imported: {}, skipped (already exist): {}", imported, skipped);
    }

    private List<String> resolveGenres(List<Integer> genreIds, Map<Integer, String> genreMap) {
        if (genreIds == null || genreIds.isEmpty()) return List.of();
        return genreIds.stream()
                .map(id -> genreMap.getOrDefault(id, null))
                .filter(name -> name != null)
                .toList();
    }

    private Integer parseYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}