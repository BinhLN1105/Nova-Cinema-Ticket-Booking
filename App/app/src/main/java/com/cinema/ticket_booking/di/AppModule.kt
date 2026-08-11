package com.cinema.ticket_booking.di

import android.content.Context
import androidx.room.Room
import com.cinema.ticket_booking.data.local.AppDatabase
import com.cinema.ticket_booking.data.local.TokenManager
import com.cinema.ticket_booking.data.local.dao.BookingDao
import com.cinema.ticket_booking.data.local.dao.MovieDao
import com.cinema.ticket_booking.data.repository.*
import com.cinema.ticket_booking.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTokenManager(@ApplicationContext ctx: Context): TokenManager {
        return TokenManager(ctx)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase {
        return Room.databaseBuilder(ctx, AppDatabase::class.java, "cinema_db")
            .fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideMovieDao(db: AppDatabase): MovieDao {
        return db.movieDao()
    }

    @Provides
    @Singleton
    fun provideBookingDao(db: AppDatabase): BookingDao {
        return db.bookingDao()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(api: ApiService, tm: TokenManager): AuthRepository {
        return AuthRepository(api, tm)
    }

    @Provides
    @Singleton
    fun provideMovieRepository(api: ApiService): MovieRepository {
        return MovieRepository(api)
    }

    @Provides
    @Singleton
    fun provideBookingRepository(api: ApiService, bookingDao: BookingDao): BookingRepository {
        return BookingRepository(api, bookingDao)
    }

    @Provides
    @Singleton
    fun provideShowtimeRepository(api: ApiService): ShowtimeRepository {
        return ShowtimeRepository(api)
    }

    @Provides
    @Singleton
    fun provideCinemaRepository(api: ApiService): CinemaRepository {
        return CinemaRepository(api)
    }

    @Provides
    @Singleton
    fun provideVoucherRepository(api: ApiService): VoucherRepository {
        return VoucherRepository(api)
    }

    @Provides
    @Singleton
    fun provideComboRepository(api: ApiService): ComboRepository {
        return ComboRepository(api)
    }

    @Provides
    @Singleton
    fun provideUserRepository(api: ApiService): UserRepository {
        return UserRepository(api)
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(api: ApiService): NotificationRepository {
        return NotificationRepository(api)
    }

    @Provides
    @Singleton
    fun provideStaffRepository(api: ApiService): StaffRepository {
        return StaffRepository(api)
    }
}
