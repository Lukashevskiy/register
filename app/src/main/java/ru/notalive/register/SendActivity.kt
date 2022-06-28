package ru.notalive.register


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import ru.notalive.register.databinding.ActivitySendBinding

class SendActivity : AppCompatActivity() {
    lateinit var binding: ActivitySendBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val url = intent.getStringExtra("barcodeData")
        binding.text.text = url
        binding.send.setOnClickListener {
            sendData()
        }
    }

    private fun sendData(){
        val apiService = RestApiService()
        val userInfo = UserInfo(binding.text.text as String?)
        apiService.addUser(userInfo){
            if(it?.barcodeData != null){

            }else{
                Log.d("mmm", "Fail to send")
            }
        }
    }
}

data class UserInfo(
    @SerializedName("barcodeData") val barcodeData: String?
)


interface RestApi{

    @Headers("Content-Type: aplication/json")
    @POST("user")

    fun addBarcode( @Body barcode:UserInfo): Call<UserInfo>
}

object ServiceBuilder {
    private val client = OkHttpClient.Builder().build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("")
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    fun<T> buildService(service: Class<T>): T{
        return retrofit.create(service)
    }
}


class RestApiService {
    fun addUser(userInfo: UserInfo, onResult: (UserInfo?) -> Unit){
        val retrofit = ServiceBuilder.buildService(RestApi::class.java)
        retrofit.addBarcode(userInfo).enqueue(
            object : Callback<UserInfo> {
                override fun onFailure(call: Call<UserInfo>, t: Throwable) {
                    TODO("Not yet implemented")
                    onResult(null)
                }

                override fun onResponse(call: Call<UserInfo>, response: Response<UserInfo>) {
                    TODO("Not yet implemented")
                    onResult(response.body())
                }
            }

        )
    }
}