package com.property.watcher

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) = runBlocking {
    try {
        logger.info("物件監視通知システムを開始します")
        
        // 設定ファイルを読み込む
        val configFile = args.firstOrNull() ?: "config.json"
        logger.info("設定ファイル: $configFile を読み込みます")
        
        // 監視処理を実行
        val propertyWatcher = PropertyWatcher()
        propertyWatcher.execute(configFile)
        
        logger.info("処理が完了しました")
    } catch (e: Exception) {
        logger.error("エラーが発生しました: ${e.message}", e)
    }
} 