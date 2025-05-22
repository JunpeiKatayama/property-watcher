package com.property.watcher.scraper

import com.property.watcher.model.Property
import com.property.watcher.model.SearchCondition
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * SUUMOサイトから物件情報を取得するスクレイパークラス
 */
class SuumoScraper {
    private val logger = LoggerFactory.getLogger(SuumoScraper::class.java)
    private val baseUrl = "https://suumo.jp/jj/chintai/ichiran/FR301FC001/"
    private val httpClient = HttpClient(CIO) {
        engine {
            requestTimeout = 30_000 // 30秒
        }
    }
    
    /**
     * 検索条件に基づいて物件情報を取得します
     */
    suspend fun scrape(condition: SearchCondition): List<Property> {
        val properties = mutableListOf<Property>()
        var page = 1
        var hasNextPage = true
        
        while (hasNextPage) {
            logger.info("ページ ${page} の取得を開始します")
            
            val url = buildUrl(condition, page)
            val html = fetchHtml(url)
            
            if (html.isEmpty()) {
                logger.warn("ページの取得に失敗しました: $url")
                break
            }
            
            val pageProperties = parseHtml(html)
            properties.addAll(pageProperties)
            
            logger.info("ページ ${page} から ${pageProperties.size} 件の物件情報を取得しました")
            
            // 次のページがあるかチェック
            hasNextPage = pageProperties.isNotEmpty() && checkNextPage(html)
            
            if (hasNextPage) {
                page++
                // サーバー負荷軽減のため少し待機
                delay(2000)
            }
        }
        
        return properties
    }
    
    /**
     * 検索URLを構築します
     */
    private fun buildUrl(condition: SearchCondition, page: Int): String {
        val params = condition.toSuumoQueryParams().toMutableMap()
        
        // ページ番号設定
        if (page > 1) {
            params["pn"] = page.toString()
        }
        
        // クエリパラメータ構築
        val queryString = params.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value, StandardCharsets.UTF_8)}"
        }
        
        return "$baseUrl?$queryString"
    }
    
    /**
     * HTMLを取得します
     */
    private suspend fun fetchHtml(url: String): String {
        return try {
            val response = httpClient.get(url) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    append("Accept-Language", "ja,en-US;q=0.9,en;q=0.8")
                    append("Referer", "https://suumo.jp/")
                }
            }
            response.bodyAsText()
        } catch (e: Exception) {
            logger.error("HTMLの取得に失敗しました: ${e.message}", e)
            ""
        }
    }
    
    /**
     * HTMLから物件情報を解析します
     */
    private fun parseHtml(html: String): List<Property> {
        val properties = mutableListOf<Property>()
        val document = Jsoup.parse(html)
        
        // SUUMOの物件リスト要素を取得
        val propertyElements = document.select("div.cassetteitem")
        
        propertyElements.forEach { element ->
            try {
                // 物件名と住所
                val name = element.select("div.cassetteitem_content-title").text()
                val address = element.select("li.cassetteitem_detail-col1").text()
                
                // 各部屋情報を取得
                val roomElements = element.select("tbody tr")
                
                roomElements.forEach { roomElement ->
                    try {
                        // 階数
                        val floor = roomElement.select("td.cassetteitem_other-floor").text()
                        
                        // 賃料・管理費
                        val rentText = roomElement.select("span.cassetteitem_other-emphasis").text()
                        val rent = extractRent(rentText)
                        val managementFeeText = roomElement.select("span.cassetteitem_price--administration").text()
                        val managementFee = extractManagementFee(managementFeeText)
                        
                        // 敷金・礼金
                        val depositText = roomElement.select("span.cassetteitem_price--deposit").text()
                        val deposit = extractDeposit(depositText)
                        val keyMoneyText = roomElement.select("span.cassetteitem_price--gratuity").text()
                        val keyMoney = extractKeyMoney(keyMoneyText)
                        
                        // 間取り・面積
                        val layout = roomElement.select("span.cassetteitem_madori").text()
                        val sizeText = roomElement.select("span.cassetteitem_menseki").text()
                        val size = extractSize(sizeText)
                        
                        // 最寄り駅・徒歩時間
                        val stationInfo = element.select("li.cassetteitem_detail-col2").first()
                        val stationText = stationInfo?.text() ?: ""
                        val (station, walkMinutes) = extractStationInfo(stationText)
                        
                        // 築年数
                        val ageText = element.select("li.cassetteitem_detail-col3").text()
                        val ageYears = extractAge(ageText)
                        
                        // URL
                        val detailUrl = roomElement.select("td.cassetteitem_other a").attr("abs:href")
                        
                        // 画像URL
                        val imageUrl = element.select("div.cassetteitem_object-item img").attr("abs:src")
                        
                        // 物件IDを抽出
                        val id = extractPropertyId(detailUrl)
                        
                        val property = Property(
                            id = id,
                            name = name,
                            address = address,
                            station = station,
                            walkMinutes = walkMinutes,
                            rent = rent,
                            managementFee = managementFee,
                            deposit = deposit,
                            keyMoney = keyMoney,
                            layout = layout,
                            size = size,
                            ageYears = ageYears,
                            floor = floor,
                            url = detailUrl,
                            imageUrl = imageUrl
                        )
                        
                        properties.add(property)
                        logger.info("物件情報を取得しました: ID=${property.id}, 名前=${property.name}")
                    } catch (e: Exception) {
                        logger.warn("部屋情報の解析に失敗しました: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                logger.warn("物件情報の解析に失敗しました: ${e.message}")
            }
        }
        
        return properties
    }
    
    /**
     * 次のページがあるかチェックします
     */
    private fun checkNextPage(html: String): Boolean {
        val document = Jsoup.parse(html)
        return document.select("div.pagination p.pagination-parts a:contains(次へ)").isNotEmpty()
    }
    
    /**
     * 物件URLからIDを抽出します
     */
    private fun extractPropertyId(url: String): String {
        val regex = "jnc_([0-9]+)".toRegex()
        val matchResult = regex.find(url)
        return matchResult?.groupValues?.getOrNull(1) ?: ""
    }
    
    /**
     * 賃料を抽出します（万円単位）
     */
    private fun extractRent(text: String): Int {
        val regex = "([0-9.]+)万円".toRegex()
        val matchResult = regex.find(text)
        return matchResult?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.toInt() ?: 0
    }
    
    /**
     * 管理費を抽出します（円単位）
     */
    private fun extractManagementFee(text: String): Int {
        val regex = "([0-9,]+)円".toRegex()
        val matchResult = regex.find(text)
        return matchResult?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull() ?: 0
    }
    
    /**
     * 敷金を抽出します（万円単位）
     */
    private fun extractDeposit(text: String): Int {
        if (text == "-") return 0
        val regex = "([0-9.]+)万円".toRegex()
        val matchResult = regex.find(text)
        return matchResult?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.toInt() ?: 0
    }
    
    /**
     * 礼金を抽出します（万円単位）
     */
    private fun extractKeyMoney(text: String): Int {
        if (text == "-") return 0
        val regex = "([0-9.]+)万円".toRegex()
        val matchResult = regex.find(text)
        return matchResult?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.toInt() ?: 0
    }
    
    /**
     * 面積を抽出します（m²）
     */
    private fun extractSize(text: String): Double {
        val regex = "([0-9.]+)m".toRegex()
        val matchResult = regex.find(text)
        return matchResult?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
    }
    
    /**
     * 最寄り駅と徒歩時間を抽出します
     */
    private fun extractStationInfo(text: String): Pair<String, Int> {
        val stationPattern = "(.+?)駅".toRegex()
        val walkPattern = "歩([0-9]+)分".toRegex()
        
        val stationMatch = stationPattern.find(text)
        val walkMatch = walkPattern.find(text)
        
        val station = stationMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
        val minutes = walkMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        
        return Pair(station, minutes)
    }
    
    /**
     * 築年数を抽出します
     */
    private fun extractAge(text: String): Int {
        val regex = "築([0-9]+)年".toRegex()
        val matchResult = regex.find(text)
        return matchResult?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    }
} 