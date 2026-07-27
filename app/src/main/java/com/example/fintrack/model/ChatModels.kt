package com.example.fintrack.model

import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("query") val query: String
)
data class ChatResponse(
    val reply: String? = null
)