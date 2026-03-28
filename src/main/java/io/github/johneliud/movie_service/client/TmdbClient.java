package io.github.johneliud.movie_service.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class TmdbClient {

    private static final String IMAGE_BASE = "https://image.tmdb.org/t/p/w500";

    private final RestClient restClient;
    private final String apiKey;

    public TmdbClient(@Value("${tmdb.api.key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("TMDB API key is not configured — poster sync will be skipped");
        }
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.themoviedb.org/3")
                .build();
    }

    public Optional<String> findPosterUrl(String title, int year) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        try {
            TmdbSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search/movie")
                            .queryParam("api_key", apiKey)
                            .queryParam("query", title)
                            .queryParam("year", year)
                            .queryParam("include_adult", "false")
                            .build())
                    .retrieve()
                    .body(TmdbSearchResponse.class);

            if (response == null || response.results() == null || response.results().isEmpty()) {
                return Optional.empty();
            }

            String posterPath = response.results().getFirst().posterPath();
            if (posterPath == null || posterPath.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(IMAGE_BASE + posterPath);
        } catch (Exception ex) {
            log.error("TMDB search failed for title='{}' year={}: {}", title, year, ex.getMessage());
            return Optional.empty();
        }
    }

    record TmdbSearchResponse(List<TmdbMovie> results) {}

    record TmdbMovie(@JsonProperty("poster_path") String posterPath) {}
}