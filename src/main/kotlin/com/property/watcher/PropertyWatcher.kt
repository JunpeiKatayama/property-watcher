package com.property.watcher

import com.property.watcher.config.AppConfig
import com.property.watcher.model.Property
import com.property.watcher.scraper.SuumoScraper
import com.property.watcher.notification.LineNotifier
import com.property.watcher.repository.PropertyRepository
import com.property.watcher.util.ConfigLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class PropertyWatcher {
    private val logger = LoggerFactory.getLogger(PropertyWatcher::class.java)
    
    /**
     * 監視処理を実行します
     */
    suspend fun execute(configFilePath: String) {
        // 設定ファイルを読み込む
        val config = ConfigLoader.loadConfig(configFilePath)
        logger.info("設定を読み込みました: ${config.searchConditions.size}件の検索条件")
        
        // リポジトリの初期化
        val repository = PropertyRepository(config.dataStorePath)
        
        // 各検索条件について処理
        config.searchConditions.forEach { searchCondition ->
            logger.info("検索条件[${searchCondition.name}]の処理を開始します")
            
            // スクレイピング実行
            val scraper = SuumoScraper()
            val properties = scraper.scrape(searchCondition)
            logger.info("${properties.size}件の物件情報を取得しました")
            
            // 新着物件を抽出
            val newProperties = filterNewProperties(properties, repository, searchCondition.name)
            logger.info("${newProperties.size}件の新着物件があります")
            
            if (newProperties.isNotEmpty()) {
                // 通知処理
                val notifier = LineNotifier(config.lineConfig)
                notifier.notify(newProperties, searchCondition)
                
                // 処理済み物件として保存
                repository.saveProperties(newProperties, searchCondition.name)
                logger.info("通知と保存が完了しました")
            }
        }
    }
    
    /**
     * 新着物件をフィルタリングします
     */
    private suspend fun filterNewProperties(
        properties: List<Property>,
        repository: PropertyRepository,
        conditionName: String
    ): List<Property> = withContext(Dispatchers.Default) {
        val existingProperties = repository.loadProperties(conditionName)
        val existingUrls = existingProperties.map { it.url }.toSet()
        
        properties.filterNot { it.url in existingUrls }
    }
} 