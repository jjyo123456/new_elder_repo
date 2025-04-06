package com.example.googlemediapipe

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback

// ✅ Correct Response Data Class (Matches Flask API Output)
data class ResponseText(
    @SerializedName("prediction") val prediction: String
)

data class InputText(
    @SerializedName("text") val text: String
)

// ✅ Retrofit Interface
interface ApiService {
    @POST("/predict")
    fun predictionInterface(@Body input: InputText): Call<ResponseText> // ✅ Fix: Call<ResponseText>
}


class PredictionViewModel : ViewModel() {
    private val predictionLiveData = MutableLiveData<String>()

    fun getPredictionLiveData(): LiveData<String> = predictionLiveData

    fun sendRequest(input: InputText) {
        MainActivity.RetrofitClient.instance.predictionInterface(input)
            .enqueue(object : Callback<ResponseText> {
                override fun onResponse(call: Call<ResponseText>, response: Response<ResponseText>) {
                    val result = if (response.isSuccessful && response.body() != null) {
                        response.body()!!.prediction  // ✅ Extracting prediction correctly
                    } else {
                        "Error: ${response.code()} - ${response.message()}"
                    }
                    predictionLiveData.postValue(result)
                }

                override fun onFailure(call: Call<ResponseText>, t: Throwable) {
                    predictionLiveData.postValue("Failed: ${t.message}")
                }
            })
    }
}

