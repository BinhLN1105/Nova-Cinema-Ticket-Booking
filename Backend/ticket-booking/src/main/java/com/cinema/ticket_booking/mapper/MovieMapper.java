package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.request.MovieRequest;
import com.cinema.ticket_booking.dto.response.MovieResponse;
import com.cinema.ticket_booking.model.Movie;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = { GenreMapper.class })
public interface MovieMapper {

    @Mapping(target = "id", expression = "java(movie.getId().toString())")
    MovieResponse toResponse(Movie movie);

    @Mapping(target = "id", expression = "java(movie.getId().toString())")
    @Mapping(target = "genres", source = "genres")
    MovieResponse.Summary toSummary(Movie movie);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "genres", ignore = true) // service tự set genres
    @Mapping(target = "avgRating", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Movie toEntity(MovieRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "genres", ignore = true)
    @Mapping(target = "avgRating", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(MovieRequest request, @MappingTarget Movie movie);
}
