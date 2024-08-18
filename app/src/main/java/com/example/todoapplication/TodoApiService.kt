package com.example.todoapplication

import retrofit2.Call
import retrofit2.http.*

interface TodoApiService {

    @GET("todos")
    fun getTodos(): Call<TodoResponse>

    @GET("todos/{id}")
    fun getTodo(@Path("id") id: Int): Call<Todo>

    @POST("todos")
    fun createTodo(@Body todo: Todo): Call<Todo>

    @PUT("todos/{id}")
    fun updateTodo(@Path("id") id: Int, @Body todo: Todo): Call<Todo>

    @DELETE("todos/{id}")
    fun deleteTodo(@Path("id") id: Int): Call<Void>
}
