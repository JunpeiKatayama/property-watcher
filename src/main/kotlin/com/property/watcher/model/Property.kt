package com.property.watcher.model

import kotlinx.serialization.Serializable

/**
 * 物件情報を表すクラス
 */
@Serializable
data class Property(
    val id: String = "",             // 物件ID（URLから抽出）
    val name: String = "",           // 物件名
    val address: String = "",        // 住所
    val station: String = "",        // 最寄り駅
    val walkMinutes: Int = 0,        // 徒歩分数
    val rent: Int = 0,               // 賃料（万円）
    val managementFee: Int = 0,      // 管理費（円）
    val deposit: Int = 0,            // 敷金（万円）
    val keyMoney: Int = 0,           // 礼金（万円）
    val layout: String = "",         // 間取り
    val size: Double = 0.0,          // 専有面積（m²）
    val ageYears: Int = 0,           // 築年数（年）
    val floor: String = "",          // 階数
    val url: String = "",            // 物件詳細URL
    val imageUrl: String = "",       // 画像URL
    val updatedAt: Long = System.currentTimeMillis() // 更新日時
) {
    /**
     * 物件情報の文字列表現を返します（LINE通知用）
     */
    fun toNotificationText(): String {
        return """
            🏠 ${name}
            📍 ${address}
            🚶 ${station} 徒歩${walkMinutes}分
            💰 家賃${rent}万円 (管理費${managementFee}円)
            💴 敷金${deposit}万円 / 礼金${keyMoney}万円
            🏠 ${layout} / ${size}m² / 築${ageYears}年
            🔍 ${url}
        """.trimIndent()
    }
} 