package io.github.johneliud.movie_service.repository;

import io.github.johneliud.movie_service.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends Neo4jRepository<Movie, String> {

    @Query(value = """
            MATCH (m:Movie)
            WHERE ($title IS NULL OR toLower(m.title) CONTAINS toLower($title))
              AND ($genre IS NULL OR $genre IN m.genres)
              AND ($releaseYear IS NULL OR m.releaseYear = $releaseYear)
            RETURN m
            ORDER BY m.title ASC
            SKIP $skip LIMIT $limit
            """,
           countQuery = """
            MATCH (m:Movie)
            WHERE ($title IS NULL OR toLower(m.title) CONTAINS toLower($title))
              AND ($genre IS NULL OR $genre IN m.genres)
              AND ($releaseYear IS NULL OR m.releaseYear = $releaseYear)
            RETURN count(m)
            """)
    Page<Movie> search(
            @Param("title") String title,
            @Param("genre") String genre,
            @Param("releaseYear") Integer releaseYear,
            Pageable pageable);
}