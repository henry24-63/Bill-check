package com.example.data.remote

import com.example.data.Constants
import com.example.data.model.*
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface SupabaseApi {
    @GET("rest/v1/app_config")
    suspend fun getAppConfig(
        @Query("id") id: String = "eq.1",
        @Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"
    ): List<AppConfig>

    @GET("rest/v1/bill_links")
    suspend fun getBillLinks(@Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY, @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"): List<BillLink>

    @GET("rest/v1/helplines")
    suspend fun getHelplines(@Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY, @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"): List<Helpline>

    @GET("rest/v1/package_codes")
    suspend fun getPackageCodes(@Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY, @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"): List<PackageCode>

    @GET("rest/v1/tariff_slabs")
    suspend fun getTariffSlabs(@Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY, @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"): List<TariffSlab>

    @GET("rest/v1/tariff_charges")
    suspend fun getTariffCharges(@Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY, @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"): List<TariffCharge>

    @GET("rest/v1/appliances")
    suspend fun getAppliances(@Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY, @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"): List<Appliance>

    @GET("rest/v1/tips")
    suspend fun getTips(@Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY, @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"): List<Tip>

    @GET("rest/v1/feature_flags")
    suspend fun getFeatureFlags(@Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY, @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"): List<FeatureFlag>

    @GET("rest/v1/ad_config")
    suspend fun getAdConfig(@Query("id") id: String = "eq.1", @Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY, @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"): List<AdConfig>

    @GET("rest/v1/announcements")
    suspend fun getAnnouncements(@Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY, @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"): List<Announcement>

    @GET("rest/v1/site_settings")
    suspend fun getSiteSettings(@Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY, @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"): List<SiteSetting>

    @POST("rest/v1/rpc/increment_stat")
    suspend fun incrementStat(
        @Body body: Map<String, String>,
        @Header("apikey") apiKey: String = Constants.SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer ${Constants.SUPABASE_ANON_KEY}"
    )

    companion object {
        val instance: SupabaseApi by lazy {
            Retrofit.Builder()
                .baseUrl(Constants.SUPABASE_URL + "/")
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(SupabaseApi::class.java)
        }
    }
}
