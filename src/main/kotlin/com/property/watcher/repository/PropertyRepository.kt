package com.property.watcher.repository

import com.property.watcher.model.Property
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Paths

/**
 * 物件情報を保存・取得するリポジトリクラス
 */
class PropertyRepository(private val basePath: String) {
    private val logger = LoggerFactory.getLogger(PropertyRepository::class.java)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    
    init {
        // 基本ディレクトリの作成
        val baseDir = File(basePath)
        if (!baseDir.exists()) {
            baseDir.mkdirs()
            logger.info("データストアディレクトリを作成しました: $basePath")
        }
    }
    
    /**
     * 物件情報を保存します
     */
    suspend fun saveProperties(properties: List<Property>, conditionName: String) = withContext(Dispatchers.IO) {
        if (properties.isEmpty()) {
            return@withContext
        }
        
        try {
            // 既存の物件情報を読み込む
            val existingProperties = loadProperties(conditionName).toMutableList()
            
            // 新しい物件を追加
            existingProperties.addAll(properties)
            
            // URLで重複排除
            val uniqueProperties = existingProperties.distinctBy { it.url }
            
            // ファイルに保存
            val file = getPropertiesFile(conditionName)
            file.parentFile?.mkdirs()
            
            val jsonString = json.encodeToString(uniqueProperties)
            file.writeText(jsonString)
            
            logger.info("${properties.size}件の物件情報を保存しました: ${file.absolutePath}")
        } catch (e: Exception) {
            logger.error("物件情報の保存に失敗しました: ${e.message}", e)
        }
    }
    
    /**
     * 物件情報を読み込みます
     */
    suspend fun loadProperties(conditionName: String): List<Property> = withContext(Dispatchers.IO) {
        val file = getPropertiesFile(conditionName)
        
        if (!file.exists()) {
            logger.info("保存済み物件情報がありません: ${file.absolutePath}")
            return@withContext emptyList()
        }
        
        try {
            val jsonString = file.readText()
            val properties = json.decodeFromString<List<Property>>(jsonString)
            logger.info("${properties.size}件の物件情報を読み込みました: ${file.absolutePath}")
            properties
        } catch (e: Exception) {
            logger.error("物件情報の読み込みに失敗しました: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * 物件情報ファイルのパスを取得します
     */
    private fun getPropertiesFile(conditionName: String): File {
        // ファイル名を常に properties.json に統一
        return File(Paths.get(basePath, "properties.json").toString())
    }
} 