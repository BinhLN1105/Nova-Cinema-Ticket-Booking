package com.cinema.ticket_booking.di;

import android.content.Context;
import androidx.room.Room;
import com.cinema.ticket_booking.data.local.AppDatabase;
import com.cinema.ticket_booking.data.local.TokenManager;
import com.cinema.ticket_booking.data.local.dao.BookingDao;
import com.cinema.ticket_booking.data.local.dao.MovieDao;
import com.cinema.ticket_booking.data.repository.*;
import com.cinema.ticket_booking.network.ApiService;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides @Singleton
    public TokenManager provideTokenManager(@ApplicationContext Context ctx) {
        return new TokenManager(ctx);
    }

    @Provides @Singleton
    public AppDatabase provideDatabase(@ApplicationContext Context ctx) {
        return Room.databaseBuilder(ctx, AppDatabase.class, "cinema_db")
                .fallbackToDestructiveMigration().build();
    }

    @Provides @Singleton
    public MovieDao provideMovieDao(AppDatabase db) { return db.movieDao(); }

    @Provides @Singleton
    public BookingDao provideBookingDao(AppDatabase db) { return db.bookingDao(); }

    @Provides @Singleton
    public AuthRepository provideAuthRepository(ApiService api, TokenManager tm) {
        return new AuthRepository(api, tm);
    }

    @Provides @Singleton
    public MovieRepository provideMovieRepository(ApiService api) {
        return new MovieRepository(api);
    }

    @Provides @Singleton
    public BookingRepository provideBookingRepository(ApiService api) {
        return new BookingRepository(api);
    }

    @Provides @Singleton
    public ShowtimeRepository provideShowtimeRepository(ApiService api) {
        return new ShowtimeRepository(api);
    }

    @Provides @Singleton
    public CinemaRepository provideCinemaRepository(ApiService api) {
        return new CinemaRepository(api);
    }

    @Provides @Singleton
    public VoucherRepository provideVoucherRepository(ApiService api) {
        return new VoucherRepository(api);
    }

    @Provides @Singleton
    public ComboRepository provideComboRepository(ApiService api) {
        return new ComboRepository(api);
    }

    @Provides @Singleton
    public UserRepository provideUserRepository(ApiService api) {
        return new UserRepository(api);
    }

    @Provides @Singleton
    public NotificationRepository provideNotificationRepository(ApiService api) {
        return new NotificationRepository(api);
    }
}
