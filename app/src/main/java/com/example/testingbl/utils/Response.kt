package com.example.testingbl.utils

sealed class Response<out T : Any> {
    data class Success<out T : Any>(val data: T) : Response<T>()
    data class Error(val errorMessage: String) : Response<Nothing>()
    data class Loading<out T : Any>(val data: T? = null, val message: String? = null) :
        Response<T>()
}