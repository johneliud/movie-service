package io.github.johneliud.movie_service.controller;

import io.github.johneliud.movie_service.dto.AverageRatingUpdateRequest;
import io.github.johneliud.movie_service.dto.MovieRequest;
import io.github.johneliud.movie_service.dto.MovieResponse;
import io.github.johneliud.movie_service.dto.PagedResponse;
import io.github.johneliud.movie_service.service.MovieService;

import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MovieResponse> create(@Valid @RequestBody MovieRequest request) {
        log.info("POST /api/movies - Creating movie: {}", request.title());
        MovieResponse response = movieService.create(request);
        log.info("POST /api/movies - Movie created with id: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<MovieResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        log.info("GET /api/movies - page: {}, size: {}, sort: {} {}", page, size, sort, direction);
        Sort.Direction dir = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));
        return ResponseEntity.ok(movieService.findAll(pageable));
    }

    @GetMapping("/genres")
    public ResponseEntity<List<String>> getGenres() {
        log.info("GET /api/movies/genres");
        return ResponseEntity.ok(movieService.getGenres());
    }

    @GetMapping("/search")
    public ResponseEntity<PagedResponse<MovieResponse>> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer releaseYearFrom,
            @RequestParam(required = false) Integer releaseYearTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/movies/search - title: {}, genre: {}, releaseYearFrom: {}, releaseYearTo: {}", title, genre, releaseYearFrom, releaseYearTo);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(movieService.search(title, genre, releaseYearFrom, releaseYearTo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> findById(@PathVariable String id) {
        log.info("GET /api/movies/{} - Fetching movie", id);
        return ResponseEntity.ok(movieService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MovieResponse> update(@PathVariable String id,
                                                @Valid @RequestBody MovieRequest request) {
        log.info("PUT /api/movies/{} - Updating movie", id);
        MovieResponse response = movieService.update(id, request);
        log.info("PUT /api/movies/{} - Update successful", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("DELETE /api/movies/{} - Deleting movie", id);
        movieService.delete(id);
        log.info("DELETE /api/movies/{} - Deletion successful", id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/average-rating")
    public ResponseEntity<Void> updateAverageRating(@PathVariable String id,
                                                    @RequestBody AverageRatingUpdateRequest request) {
        log.info("PATCH /api/movies/{}/average-rating - value: {}", id, request.averageRating());
        movieService.updateAverageRating(id, request.averageRating());
        return ResponseEntity.noContent().build();
    }
}