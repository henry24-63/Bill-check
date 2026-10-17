package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppConfig(
    @Json(name = "id") val id: Int = 1,
    @Json(name = "latest_version_code") val latestVersionCode: Int = 1,
    @Json(name = "latest_version_name") val latestVersionName: String = "1.0.0",
    @Json(name = "min_version_code") val minVersionCode: Int = 1,
    @Json(name = "apk_url") val apkUrl: String? = null,
    @Json(name = "apk_size_mb") val apkSizeMb: Double? = null,
    @Json(name = "changelog") val changelog: String? = null,
    @Json(name = "force_update_message") val forceUpdateMessage: String? = null,
    @Json(name = "content_version") val contentVersion: Int = 1,
    @Json(name = "maintenance_mode") val maintenanceMode: Boolean = false,
    @Json(name = "maintenance_message") val maintenanceMessage: String? = null
)

@JsonClass(generateAdapter = true)
data class BillLink(
    @Json(name = "id") val id: Int,
    @Json(name = "name_en") val nameEn: String,
    @Json(name = "name_ur") val nameUr: String?,
    @Json(name = "category") val category: String,
    @Json(name = "url") val url: String,
    @Json(name = "logo_url") val logoUrl: String?,
    @Json(name = "sort_order") val sortOrder: Int = 0,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class Helpline(
    @Json(name = "id") val id: Int,
    @Json(name = "org_name_en") val orgNameEn: String,
    @Json(name = "org_name_ur") val orgNameUr: String?,
    @Json(name = "category") val category: String,
    @Json(name = "number") val number: String,
    @Json(name = "notes") val notes: String?,
    @Json(name = "sort_order") val sortOrder: Int = 0,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class PackageCode(
    @Json(name = "id") val id: Int,
    @Json(name = "telco") val telco: String,
    @Json(name = "title_en") val titleEn: String,
    @Json(name = "title_ur") val titleUr: String?,
    @Json(name = "ussd_code") val ussdCode: String,
    @Json(name = "description") val description: String?,
    @Json(name = "sort_order") val sortOrder: Int = 0,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class TariffSlab(
    @Json(name = "id") val id: Int,
    @Json(name = "tariff_name") val tariffName: String,
    @Json(name = "from_unit") val fromUnit: Int,
    @Json(name = "to_unit") val toUnit: Int,
    @Json(name = "rate_per_unit") val ratePerUnit: Double,
    @Json(name = "calc_mode") val calcMode: String = "incremental",
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class TariffCharge(
    @Json(name = "id") val id: Int,
    @Json(name = "tariff_name") val tariffName: String,
    @Json(name = "fixed_charge") val fixedCharge: Double = 0.0,
    @Json(name = "gst_percent") val gstPercent: Double = 0.0,
    @Json(name = "other_percent") val otherPercent: Double = 0.0,
    @Json(name = "other_flat") val otherFlat: Double = 0.0,
    @Json(name = "notes") val notes: String?,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class Appliance(
    @Json(name = "id") val id: Int,
    @Json(name = "name_en") val nameEn: String,
    @Json(name = "name_ur") val nameUr: String?,
    @Json(name = "default_watts") val defaultWatts: Int,
    @Json(name = "icon") val icon: String?,
    @Json(name = "sort_order") val sortOrder: Int = 0,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class Tip(
    @Json(name = "id") val id: Int,
    @Json(name = "title_en") val titleEn: String,
    @Json(name = "title_ur") val titleUr: String?,
    @Json(name = "body_en") val bodyEn: String,
    @Json(name = "body_ur") val bodyUr: String?,
    @Json(name = "sort_order") val sortOrder: Int = 0,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class FeatureFlag(
    @Json(name = "key") val key: String,
    @Json(name = "label") val label: String,
    @Json(name = "enabled") val enabled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class AdConfig(
    @Json(name = "id") val id: Int = 1,
    @Json(name = "ads_enabled") val adsEnabled: Boolean = true,
    @Json(name = "banner_enabled") val bannerEnabled: Boolean = true,
    @Json(name = "interstitial_enabled") val interstitialEnabled: Boolean = true,
    @Json(name = "interstitial_interval_minutes") val interstitialIntervalMinutes: Int = 5,
    @Json(name = "banner_screens") val bannerScreens: List<String>? = null,
    @Json(name = "interstitial_after_calculation") val interstitialAfterCalculation: Boolean = true
)

@JsonClass(generateAdapter = true)
data class Announcement(
    @Json(name = "id") val id: Int,
    @Json(name = "title_en") val titleEn: String,
    @Json(name = "title_ur") val titleUr: String?,
    @Json(name = "body_en") val bodyEn: String,
    @Json(name = "body_ur") val bodyUr: String?,
    @Json(name = "type") val type: String = "info",
    @Json(name = "link_url") val linkUrl: String?,
    @Json(name = "starts_at") val startsAt: String?,
    @Json(name = "ends_at") val endsAt: String?,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SiteSetting(
    @Json(name = "key") val key: String,
    @Json(name = "value") val value: String?
)
