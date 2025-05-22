package com.property.watcher.util

import com.property.watcher.config.AppConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

/**
 * 設定ファイルを読み込むユーティリティクラス
 */
object ConfigLoader {
    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    
    /**
     * 設定ファイルを読み込みます
     */
    fun loadConfig(filePath: String): AppConfig {
        val file = File(filePath)
        
        return if (file.exists()) {
            try {
                val content = file.readText()
                json.decodeFromString<AppConfig>(content)
            } catch (e: Exception) {
                logger.error("設定ファイルの読み込みに失敗しました: ${e.message}", e)
                createDefaultConfig(filePath)
            }
        } else {
            logger.warn("設定ファイルが存在しないため、デフォルト設定を作成します: $filePath")
            createDefaultConfig(filePath)
        }
    }
    
    /**
     * デフォルトの設定を作成し、ファイルに保存します
     */
    private fun createDefaultConfig(filePath: String): AppConfig {
        val config = AppConfig()
        
        try {
            val file = File(filePath)
            file.parentFile?.mkdirs()
            
            val configJson = json.encodeToString(AppConfig.serializer(), config)
            file.writeText(configJson)
            
            logger.info("デフォルト設定を保存しました: $filePath")
        } catch (e: Exception) {
            logger.error("デフォルト設定の保存に失敗しました: ${e.message}", e)
        }
        
        return config
    }
} 