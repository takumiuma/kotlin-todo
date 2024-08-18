package com.example.todoapplication

data class TodoResponse(
    val todos: List<TodoRaw>
)

data class TodoRaw(
    val id: TodoId,
    val title: TodoTitle,
    val person: TodoPerson,
    val done: TodoDone
)

data class TodoId(
    val value: Int
)
data class TodoTitle(
    val value: String
)
data class TodoPerson(
    val value: String
)
data class TodoDone(
    val value: Boolean
)
