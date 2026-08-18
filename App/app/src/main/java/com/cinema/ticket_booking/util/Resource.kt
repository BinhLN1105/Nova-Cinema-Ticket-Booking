package com.cinema.ticket_booking.util

class Resource<T> private constructor(
    @JvmField val status: Status,
    @JvmField val data: T?,
    @JvmField val message: String?
) {
    enum class Status {
        LOADING, SUCCESS, ERROR
    }

    val isSuccess: Boolean get() = status == Status.SUCCESS
    val isError: Boolean get() = status == Status.ERROR
    val isLoading: Boolean get() = status == Status.LOADING

    companion object {
        @JvmStatic
        fun <T> loading(): Resource<T> {
            return Resource(Status.LOADING, null, null)
        }

        @JvmStatic
        fun <T> success(data: T?): Resource<T> {
            return Resource(Status.SUCCESS, data, null)
        }

        @JvmStatic
        fun <T> error(msg: String?): Resource<T> {
            return Resource(Status.ERROR, null, msg)
        }

        @JvmStatic
        fun <T> error(msg: String?, data: T?): Resource<T> {
            return Resource(Status.ERROR, data, msg)
        }
    }
}
