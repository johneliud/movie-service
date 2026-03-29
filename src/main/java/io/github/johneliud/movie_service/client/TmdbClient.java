package io.github.johneliud.movie_service.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TmdbClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String imageBaseUrl;

    public TmdbClient(
            @Value("${tmdb.api.key}") String apiKey,
            @Value("${tmdb.base-url}") String baseUrl,
            @Value("${tmdb.image-base-url}") String imageBaseUrl) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("TMDB API key is not configured — TMDB features will be skipped");
        }
        this.apiKey = apiKey;
        this.imageBaseUrl = imageBaseUrl;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Optional<String> findPosterUrl(String title, Integer year) {
        if (!isConfigured()) return Optional.empty();
        try {
            TmdbSearchResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/search/movie")
                                .queryParam("api_key", apiKey)
                                .queryParam("query", title)
                                .queryParam("include_adult", "false");
                        if (year != null) builder = builder.queryParam("year", year);
                        return builder.build();
                    })
                    .retrieve()
                    .body(TmdbSearchResponse.class);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                return Optional.empty();
            }

            String posterPath = response.results().getFirst().posterPath();
            if (posterPath == null || posterPath.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(buildPosterUrl(posterPath));
        } catch (Exception ex) {
            log.error("TMDB search failed for title='{}' year={}: {}", title, year, ex.getMessage());
            return Optional.empty();
        }
    }

    public Map<Integer, String> fetchGenreMap() {
        try {
            TmdbGenreListResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/genre/movie/list")
                            .queryParam("api_key", apiKey)
                            .build())
                    .retrieve()
                    .body(TmdbGenreListResponse.class);

            if (response == null || response.genres() == null) return Map.of();

            return response.genres().stream()
                    .collect(Collectors.toMap(TmdbGenre::id, TmdbGenre::name));
        } catch (Exception ex) {
            log.error("TMDB genre fetch failed: {}", ex.getMessage());
            return Map.of();
        }
    }

    public Optional<DiscoverResult> fetchDiscoverPage(int page) {
        try {
            TmdbDiscoverResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/discover/movie")
                            .queryParam("api_key", apiKey)
                            .queryParam("page", page)
                            .queryParam("sort_by", "popularity.desc")
                            .queryParam("include_adult", "false")
                            .queryParam("include_video", "false")
                            .build())
                    .retrieve()
                    .body(TmdbDiscoverResponse.class);

            if (response == null || response.results() == null) return Optional.empty();

            return Optional.of(new DiscoverResult(response.results(), response.totalPages()));
        } catch (Exception ex) {
            log.error("TMDB discover page {} failed: {}", page, ex.getMessage());
            return Optional.empty();
        }
    }

    public String buildPosterUrl(String posterPath) {
        return imageBaseUrl + posterPath;
    }

    record TmdbSearchResponse(List<TmdbPosterResult> results) {}
    record TmdbPosterResult(@JsonProperty("poster_path") String posterPath) {}

    record TmdbGenreListResponse(List<TmdbGenre> genres) {}
    record TmdbGenre(int id, String name) {}

    record TmdbDiscoverResponse(
            int page,
            List<TmdbMovieRaw> results,
            @JsonProperty("total_pages") int totalPages
    ) {}

    public record TmdbMovieRaw(
            int id,
            String title,
            String overview,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("genre_ids") List<Integer> genreIds,
            @JsonProperty("poster_path") String posterPath
    ) {}

    public record DiscoverResult(List<TmdbMovieRaw> movies, int totalPages) {}
}