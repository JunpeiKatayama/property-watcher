package com.property.watcher.config

import com.property.watcher.model.SearchCondition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * アプリケーション設定を表すクラス
 */
@Serializable
data class AppConfig(
    val searchConditions: List<SearchCondition> = emptyList(),
    val lineConfig: LineConfig = LineConfig(),
    val dataStorePath: String = "data",
    val scheduleConfig: ScheduleConfig = ScheduleConfig()
) {
    companion object {
        fun loadFromFile(filePath: String): AppConfig {
            val configFile = File(filePath)
            if (!configFile.exists()) {
                throw IllegalArgumentException("設定ファイルが見つかりません: $filePath")
            }
            val jsonString = configFile.readText()
            return Json.decodeFromString(serializer(), jsonString)
        }
    }
}

/**
 * LINE通知の設定
 */
@Serializable
data class LineConfig(
    val channelToken: String = "",
    val userId: String = ""
)

/**
 * スケジュール実行の設定
 */
@Serializable
data class ScheduleConfig(
    val intervalHours: Int = 24,  // 実行間隔（時間）
    val startHour: Int = 9,       // 開始時刻（時）
    val startMinute: Int = 0      // 開始時刻（分）
) 