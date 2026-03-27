package io.github.johneliud.movie_service.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MovieResponse(
        String id,
        String title,
        List<String> genres,
        Integer releaseYear,
        String description,
        String posterUrl,
        Double averageRating,
        LocalDateTime createdAt
) {}