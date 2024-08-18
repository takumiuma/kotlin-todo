package com.example.todoapplication

data class Todo(
//    val title:String,
//    val detail:String
    val id: Int?,
    val title: String,
    val person: String,
    val done: Boolean,
)