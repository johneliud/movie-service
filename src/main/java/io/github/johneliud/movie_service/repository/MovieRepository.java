package io.github.johneliud.movie_service.repository;

import io.github.johneliud.movie_service.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieRepository extends Neo4jRepository<Movie, String> {

    boolean existsByTmdbId(Integer tmdbId);

    @Query("MATCH (m:Movie) WHERE m.posterUrl IS NULL RETURN m")
    List<Movie> findAllWithoutPoster();

    @Query("MATCH (m:Movie) UNWIND m.genres AS genre RETURN DISTINCT genre ORDER BY genre")
    List<String> findAllDistinctGenres();

    @Query(value = """
            MATCH (m:Movie)
            WHERE ($title IS NULL OR toLower(m.title) CONTAINS toLower($title))
              AND ($genre IS NULL OR $genre IN m.genres)
              AND ($releaseYearFrom IS NULL OR m.releaseYear >= $releaseYearFrom)
              AND ($releaseYearTo IS NULL OR m.releaseYear <= $releaseYearTo)
            RETURN m
            ORDER BY m.title ASC
            SKIP $skip LIMIT $limit
            """,
           countQuery = """
            MATCH (m:Movie)
            WHERE ($title IS NULL OR toLower(m.title) CONTAINS toLower($title))
              AND ($genre IS NULL OR $genre IN m.genres)
              AND ($releaseYearFrom IS NULL OR m.releaseYear >= $releaseYearFrom)
              AND ($releaseYearTo IS NULL OR m.releaseYear <= $releaseYearTo)
            RETURN count(m)
            """)
    Page<Movie> search(
            @Param("title") String title,
            @Param("genre") String genre,
            @Param("releaseYearFrom") Integer releaseYearFrom,
            @Param("releaseYearTo") Integer releaseYearTo,
            Pageable pageable);
}