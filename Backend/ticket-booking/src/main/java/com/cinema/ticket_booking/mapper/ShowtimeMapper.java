package com.cinema.ticket_booking.mapper;

import com.cinema.ticket_booking.dto.response.ShowtimeResponse;
import com.cinema.ticket_booking.model.Showtime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShowtimeMapper {

    @Mapping(target = "id", expression = "java(showtime.getId().toString())")
    @Mapping(target = "movieId", expression = "java(showtime.getMovie().getId().toString())")
    @Mapping(target = "movieTitle", source = "movie.title")
    @Mapping(target = "moviePosterUrl", source = "movie.posterUrl")
    @Mapping(target = "movieDuration", source = "movie.duration")
    @Mapping(target = "movieRated", source = "movie.rated")
    @Mapping(target = "movieGenres", expression = "java(showtime.getMovie().getGenres().stream().map(g -> g.getName()).toList())")
    @Mapping(target = "screenId", expression = "java(showtime.getScreen().getId().toString())")
    @Mapping(target = "screenName", source = "screen.name")
    @Mapping(target = "screenType", expression = "java(showtime.getScreen().getScreenType().name())")
    @Mapping(target = "cinemaId", expression = "java(showtime.getScreen().getCinema().getId().toString())")
    @Mapping(target = "cinemaName", source = "screen.cinema.name")
    @Mapping(target = "cinemaCity", source = "screen.cinema.city")
    @Mapping(target = "availableSeats", ignore = true) // service tự tính
    ShowtimeResponse toResponse(Showtime showtime);
}
