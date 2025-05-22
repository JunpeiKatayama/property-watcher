package com.property.watcher.model

import kotlinx.serialization.Serializable

/**
 * 検索条件を表すクラス
 */
@Serializable
data class SearchCondition(
    val name: String,                        // 検索条件の名前
    val prefecture: String,                  // 都道府県
    val city: String? = null,                // 市区町村
    val district: String? = null,            // 町名
    val minRent: Int? = null,                // 最小賃料（万円）
    val maxRent: Int? = null,                // 最大賃料（万円）
    val layouts: List<String> = emptyList(), // 間取り（1K, 1LDK, 2Kなど）
    val maxWalkMinutes: Int? = null,         // 最大徒歩分数
    val maxAgeYears: Int? = null,            // 最大築年数
    val hasParking: Boolean? = null,         // 駐車場有無
    val hasPetAllowed: Boolean? = null,      // ペット可否
    val otherConditions: Map<String, String> = emptyMap() // その他の条件
) {
    /**
     * SUUMOのURLクエリパラメータに変換します
     */
    fun toSuumoQueryParams(): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        // 地域パラメータ
        params["ar"] = convertPrefectureToAreaCode(prefecture)
        city?.let { params["bs"] = convertCityToCode(prefecture, it) }
        district?.let { params["ta"] = convertDistrictToCode(prefecture, city, it) }
        
        // 賃料範囲
        minRent?.let { params["cb"] = convertRentToCode(it) }
        maxRent?.let { params["ct"] = convertRentToCode(it) }
        
        // 間取り
        if (layouts.isNotEmpty()) {
            layouts.forEachIndexed { index, layout ->
                params["md${index + 1}"] = convertLayoutToCode(layout)
            }
        }
        
        // 駅徒歩
        maxWalkMinutes?.let { params["ts"] = convertWalkMinutesToCode(it) }
        
        // 築年数
        maxAgeYears?.let { params["kb"] = convertAgeYearsToCode(it) }
        
        // その他条件
        hasParking?.let { if (it) params["kj"] = "9" }
        hasPetAllowed?.let { if (it) params["ks"] = "1" }
        
        // 固定パラメータ
        params["pc"] = "50" // 1ページに表示する件数
        
        // その他の条件をマージ
        params.putAll(otherConditions)
        
        return params
    }
    
    /**
     * 都道府県をSUUMOのエリアコードに変換（簡易実装）
     */
    private fun convertPrefectureToAreaCode(prefecture: String): String {
        return when (prefecture) {
            "東京都" -> "030"
            "神奈川県" -> "040"
            "埼玉県" -> "050"
            "千葉県" -> "060"
            "大阪府" -> "070"
            "京都府" -> "080"
            "兵庫県" -> "090"
            else -> "030" // デフォルトは東京
        }
    }
    
    // 以下、実際のSUUMOのコード体系に合わせて実装する必要があります
    // 実装の詳細はSUUMOサイトの調査が必要です
    private fun convertCityToCode(prefecture: String, city: String): String = "040"
    private fun convertDistrictToCode(prefecture: String, city: String?, district: String): String = "11217"
    private fun convertRentToCode(rent: Int): String = rent.toString()
    private fun convertLayoutToCode(layout: String): String = "1"
    private fun convertWalkMinutesToCode(minutes: Int): String = "3"
    private fun convertAgeYearsToCode(years: Int): String = "1"
} 