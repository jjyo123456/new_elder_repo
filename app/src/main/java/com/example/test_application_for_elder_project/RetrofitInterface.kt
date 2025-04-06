package com.example.googlemediapipe


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface RetrofitInterface {
    @Headers("Content-Type: application/json")
    @POST("predict")
    fun predictionInterface(@Body text: InputText): Call<ResponseText>
}