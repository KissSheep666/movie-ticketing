package com.movieticket.movie.service;

import com.movieticket.movie.entity.Seat;
import com.movieticket.movie.entity.Session;

import java.util.List;

public interface SessionService {
    List<Session> listByMovieId(Long movieId);

    Session getById(Long id);

    List<Seat> getSeatsBySessionId(Long sessionId);

    void lockSeats(Long userId,Long sessionId, List<Long> seatIds);
    void releaseSeats(Long sessionId, List<Long> seatIds);
}
