package com.example.data.repository

import android.content.Context
import com.example.data.model.*
import com.example.data.remote.SupabaseApi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentRepository(context: Context) {
    private val api = SupabaseApi.instance
    private val prefs = context.getSharedPreferences("bijli_cache", Context.MODE_PRIVATE)
    private val moshi = SupabaseApi.moshi

    private val billLinksAdapter = moshi.adapter<List<BillLink>>(
        Types.newParameterizedType(List::class.java, BillLink::class.java)
    )

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

    var currentSelectedUrl: String = ""
    var currentSelectedTitle: String = ""

    init {
        // Instant millisecond loading from local storage
        loadCachedBillLinks()
    }

    private fun loadCachedBillLinks() {
        try {
            val cachedJson = prefs.getString("cached_bill_links", null)
            if (!cachedJson.isNullOrEmpty()) {
                val parsed = billLinksAdapter.fromJson(cachedJson)
                if (!parsed.isNullOrEmpty()) {
                    billLinks = parsed
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Default DISCO links as instant fallback if cache is empty
        billLinks = listOf(
            BillLink(1, "GEPCO (Gujranwala)", "گیپکو گوجرانوالہ", "Electricity", "https://bill.pitc.com.pk/gepcobill", null, 1, true),
            BillLink(2, "LESCO (Lahore)", "لیسکو لاہور", "Electricity", "https://bill.pitc.com.pk/lescobill", null, 2, true),
            BillLink(3, "FESCO (Faisalabad)", "فیسکو فیصل آباد", "Electricity", "https://bill.pitc.com.pk/fescobill", null, 3, true),
            BillLink(4, "IESCO (Islamabad)", "آئیسکو اسلام آباد", "Electricity", "https://bill.pitc.com.pk/iescobill", null, 4, true),
            BillLink(5, "MEPCO (Multan)", "میپکو ملتان", "Electricity", "https://bill.pitc.com.pk/mepcobill", null, 5, true),
            BillLink(6, "PESCO (Peshawar)", "پیسکو پشاور", "Electricity", "https://bill.pitc.com.pk/pescobill", null, 6, true),
            BillLink(7, "HESCO (Hyderabad)", "حیسکو حیدرآباد", "Electricity", "https://bill.pitc.com.pk/hescobill", null, 7, true),
            BillLink(8, "QESCO (Quetta)", "کیسکو کوئٹہ", "Electricity", "https://bill.pitc.com.pk/qescobill", null, 8, true),
            BillLink(9, "SEPCO (Sukkur)", "سیپکو سکھر", "Electricity", "https://bill.pitc.com.pk/sepcobill", null, 9, true),
            BillLink(10, "TESCO (Tribal)", "ٹیسکو قبائلی علاقہ", "Electricity", "https://bill.pitc.com.pk/tescobill", null, 10, true)
        )
    }

    private fun saveCachedBillLinks(list: List<BillLink>) {
        try {
            val json = billLinksAdapter.toJson(list)
            prefs.edit().putString("cached_bill_links", json).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchBillLinks(): List<BillLink> = withContext(Dispatchers.IO) {
        try {
            val list = api.getBillLinks().filter { it.isActive }.sortedBy { it.sortOrder }
            if (list.isNotEmpty()) {
                billLinks = list
                saveCachedBillLinks(list)
            }
            return@withContext billLinks
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext billLinks
        }
    }

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
            val onlineLinks = api.getBillLinks().filter { it.isActive }.sortedBy { it.sortOrder }
            if (onlineLinks.isNotEmpty()) {
                billLinks = onlineLinks
                saveCachedBillLinks(onlineLinks)
            }
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

    fun saveLastRefNumber(ref: String) {
        prefs.edit().putString("last_user_ref", ref.trim()).apply()
    }

    fun getLastRefNumber(): String {
        return prefs.getString("last_user_ref", "") ?: ""
    }

    fun resolveBillUrl(urlTemplate: String, refNumber: String): String {
        val trimmedRef = refNumber.trim()
        return if (urlTemplate.contains("{REF}")) {
            urlTemplate.replace("{REF}", trimmedRef)
        } else if (trimmedRef.isNotEmpty() && !urlTemplate.contains("ref=")) {
            val separator = if (urlTemplate.contains("?")) "&" else "?"
            "$urlTemplate${separator}ref=$trimmedRef"
        } else {
            urlTemplate
        }
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
