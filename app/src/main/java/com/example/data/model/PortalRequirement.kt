package com.example.data.model

data class PortalRequirement(
    val label: String,
    val shortLabel: String,
    val placeholder: String,
    val sampleValue: String,
    val minLength: Int,
    val maxLength: Int,
    val hintLocation: String,
    val category: String = "Electricity"
)

object PortalRequirementHelper {
    fun getRequirement(link: BillLink?): PortalRequirement {
        if (link == null) {
            return PortalRequirement(
                label = "14-Digit Reference Number",
                shortLabel = "Reference #",
                placeholder = "01123456789012",
                sampleValue = "01123456789012",
                minLength = 10,
                maxLength = 14,
                hintLocation = "Printed on the top-left of your bill (14 digits)",
                category = "Electricity"
            )
        }

        val name = link.nameEn.lowercase()
        val url = link.url.lowercase()
        val category = link.category.lowercase()

        return when {
            // Karachi K-Electric (Account Number 13-digit or Consumer Number 8-digit)
            name.contains("k-electric") || name.contains("kelectric") || url.contains("ke.com.pk") -> {
                PortalRequirement(
                    label = "13-Digit Account # / 8-Digit Consumer #",
                    shortLabel = "Account #",
                    placeholder = "0400012345678",
                    sampleValue = "0400012345678",
                    minLength = 8,
                    maxLength = 13,
                    hintLocation = "Printed on top-right of your K-Electric bill",
                    category = "Electricity"
                )
            }
            // SNGPL Sui Northern Gas Pipelines Ltd (11-digit Consumer ID)
            name.contains("sngpl") || url.contains("sngpl") -> {
                PortalRequirement(
                    label = "11-Digit Consumer Number (SNGPL)",
                    shortLabel = "Consumer ID",
                    placeholder = "12345678901",
                    sampleValue = "12345678901",
                    minLength = 10,
                    maxLength = 11,
                    hintLocation = "Printed on top of your SNGPL Gas bill",
                    category = "Gas"
                )
            }
            // SSGC Sui Southern Gas Company (10-digit Customer Number)
            name.contains("ssgc") || url.contains("ssgc") -> {
                PortalRequirement(
                    label = "10-Digit Customer Number (SSGC)",
                    shortLabel = "Customer #",
                    placeholder = "1234567890",
                    sampleValue = "1234567890",
                    minLength = 10,
                    maxLength = 10,
                    hintLocation = "Printed on top-left of your SSGC Gas bill",
                    category = "Gas"
                )
            }
            // PTCL Landline / Broadband (10-digit Account ID or Phone Number)
            name.contains("ptcl") || url.contains("ptcl") -> {
                PortalRequirement(
                    label = "Account ID or Phone Number (PTCL)",
                    shortLabel = "Account ID",
                    placeholder = "1012345678",
                    sampleValue = "1012345678",
                    minLength = 8,
                    maxLength = 11,
                    hintLocation = "Printed on your PTCL invoice / e-bill",
                    category = "Telephone"
                )
            }
            // WASA Water & Sanitation Agency (7 to 10-digit Consumer Number)
            name.contains("wasa") || url.contains("wasa") -> {
                PortalRequirement(
                    label = "8-10 Digit Consumer Number (WASA)",
                    shortLabel = "Consumer #",
                    placeholder = "12345678",
                    sampleValue = "12345678",
                    minLength = 7,
                    maxLength = 10,
                    hintLocation = "Printed on your WASA water bill",
                    category = "Water"
                )
            }
            // Generic Gas utilities
            category.contains("gas") -> {
                PortalRequirement(
                    label = "10-11 Digit Consumer Number",
                    shortLabel = "Consumer ID",
                    placeholder = "12345678901",
                    sampleValue = "12345678901",
                    minLength = 10,
                    maxLength = 11,
                    hintLocation = "Printed on top of your gas bill",
                    category = "Gas"
                )
            }
            // Generic Water utilities
            category.contains("water") -> {
                PortalRequirement(
                    label = "8-10 Digit Consumer Number",
                    shortLabel = "Consumer #",
                    placeholder = "12345678",
                    sampleValue = "12345678",
                    minLength = 7,
                    maxLength = 10,
                    hintLocation = "Printed on your water utility bill",
                    category = "Water"
                )
            }
            // Pakistan DISCOs (GEPCO, LESCO, FESCO, IESCO, MEPCO, PESCO, HESCO, QESCO, SEPCO, TESCO)
            else -> {
                val discoCode = when {
                    name.contains("gepco") -> "GEPCO"
                    name.contains("lesco") -> "LESCO"
                    name.contains("fesco") -> "FESCO"
                    name.contains("iesco") -> "IESCO"
                    name.contains("mepco") -> "MEPCO"
                    name.contains("pesco") -> "PESCO"
                    name.contains("hesco") -> "HESCO"
                    name.contains("qesco") -> "QESCO"
                    name.contains("sepco") -> "SEPCO"
                    name.contains("tesco") -> "TESCO"
                    else -> link.nameEn.split(" ").firstOrNull() ?: "DISCO"
                }
                PortalRequirement(
                    label = "14-Digit Reference Number ($discoCode)",
                    shortLabel = "14-Digit Ref #",
                    placeholder = "01123456789012",
                    sampleValue = "01123456789012",
                    minLength = 10,
                    maxLength = 14,
                    hintLocation = "Printed on top-left of $discoCode bill (14 digits without space)",
                    category = "Electricity"
                )
            }
        }
    }
}
