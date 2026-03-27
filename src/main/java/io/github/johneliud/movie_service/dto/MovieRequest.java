package io.github.johneliud.movie_service.dto;

import io.github.johneliud.movie_service.validation.MaxCurrentYearPlus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MovieRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        List<String> genres,

        @Min(value = 1990, message = "Release year must be 1990 or later")
        @MaxCurrentYearPlus(years = 10, message = "Release year must not exceed 10 years from the current year")
        Integer releaseYear,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        String posterUrl
) {}