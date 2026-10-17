package com.example.data.repository

import android.content.Context
import com.example.data.model.*
import com.example.data.remote.SupabaseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentRepository(context: Context) {
    private val api = SupabaseApi.instance
    private val prefs = context.getSharedPreferences("bijli_cache", Context.MODE_PRIVATE)

    var cachedAppConfig: AppConfig? = null
    var billLinks: List<BillLink> = emptyList()
    var helplines: List<Helpline> = emptyList()
    var packageCodes: List<PackageCode> = emptyList()
    var tariffSlabs: List<TariffSlab> = emptyList()
    var tariffCharges: List<TariffCharge> = emptyList()
    var appliances: List<Appliance> = emptyList()
    var tips: List<Tip> = emptyList()
    var featureFlags: List<FeatureFlag> = emptyList()
    var adConfig: AdConfig? = null
    var announcements: List<Announcement> = emptyList()
    var siteSettings: List<SiteSetting> = emptyList()

    suspend fun fetchAppConfig(): AppConfig? = withContext(Dispatchers.IO) {
        try {
            val list = api.getAppConfig()
            if (list.isNotEmpty()) {
                cachedAppConfig = list[0]
                prefs.edit().putInt("cached_content_version", list[0].contentVersion).apply()
                return@withContext list[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext cachedAppConfig
    }

    suspend fun fetchAllContent() = withContext(Dispatchers.IO) {
        try {
            billLinks = api.getBillLinks().filter { it.isActive }.sortedBy { it.sortOrder }
            helplines = api.getHelplines().filter { it.isActive }.sortedBy { it.sortOrder }
            packageCodes = api.getPackageCodes().filter { it.isActive }.sortedBy { it.sortOrder }
            tariffSlabs = api.getTariffSlabs().filter { it.isActive }
            tariffCharges = api.getTariffCharges().filter { it.isActive }
            appliances = api.getAppliances().filter { it.isActive }.sortedBy { it.sortOrder }
            tips = api.getTips().filter { it.isActive }.sortedBy { it.sortOrder }
            featureFlags = api.getFeatureFlags()
            val adList = api.getAdConfig()
            if (adList.isNotEmpty()) adConfig = adList[0]
            announcements = api.getAnnouncements().filter { it.isActive }
            siteSettings = api.getSiteSettings()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCachedContentVersion(): Int {
        return prefs.getInt("cached_content_version", 1)
    }

    suspend fun incrementStat(key: String) {
        try {
            withContext(Dispatchers.IO) {
                api.incrementStat(mapOf("p_key" to key))
            }
        } catch (e: Exception) {
            // Silently ignore
        }
    }
}
