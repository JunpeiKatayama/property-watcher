package com.property.watcher.notification

import com.linecorp.bot.client.LineMessagingClient
import com.linecorp.bot.model.PushMessage
import com.linecorp.bot.model.message.FlexMessage
import com.linecorp.bot.model.message.Message
import com.linecorp.bot.model.message.TextMessage
import com.linecorp.bot.model.message.flex.component.Box
import com.linecorp.bot.model.message.flex.component.Button
import com.linecorp.bot.model.message.flex.component.Text
import com.linecorp.bot.model.message.flex.component.Text.TextWeight
import com.linecorp.bot.model.message.flex.component.Button.ButtonStyle
import com.linecorp.bot.model.message.flex.container.Bubble
import com.linecorp.bot.model.message.flex.unit.FlexLayout
import com.linecorp.bot.model.action.URIAction
import com.linecorp.bot.model.response.BotApiResponse
import com.property.watcher.config.LineConfig
import com.property.watcher.model.Property
import com.property.watcher.model.SearchCondition
import org.slf4j.LoggerFactory
import java.net.URI
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.CompletableFuture

/**
 * LINE通知機能を実装するクラス
 */
class LineNotifier(private val config: LineConfig) {
    private val logger = LoggerFactory.getLogger(LineNotifier::class.java)
    private val lineClient: LineMessagingClient by lazy {
        LineMessagingClient.builder(config.channelToken).build()
    }
    
    /**
     * 物件情報をLINEで通知します
     */
    suspend fun notify(properties: List<Property>, condition: SearchCondition) {
        if (properties.isEmpty()) {
            logger.info("通知対象の物件がありません")
            return
        }
        
        try {
            // サマリーメッセージを送信
            val summaryMessage = createSummaryMessage(properties, condition)
            sendMessage(summaryMessage)
            
            // 各物件の詳細を送信（最大10件まで）
            val maxNotifyCount = minOf(properties.size, 10)
            properties.take(maxNotifyCount).forEach { property ->
                val flexMessage = createPropertyFlexMessage(property)
                sendMessage(flexMessage)
            }
            
            logger.info("${maxNotifyCount}件の物件情報を通知しました")
        } catch (e: Exception) {
            logger.error("LINE通知に失敗しました: ${e.message}", e)
        }
    }
    
    /**
     * サマリーメッセージを作成します
     */
    private fun createSummaryMessage(properties: List<Property>, condition: SearchCondition): Message {
        val text = """
            🏠 新着物件のお知らせ
            検索条件: ${condition.name}
            ${properties.size}件の新着物件があります
        """.trimIndent()
        
        return TextMessage(text)
    }
    
    /**
     * 物件情報のFlexメッセージを作成します
     */
    private fun createPropertyFlexMessage(property: Property): Message {
        val formatter = NumberFormat.getNumberInstance(Locale.JAPAN)
        // 空値対策
        val propertyName = property.name.ifBlank { "物件名未設定" }
        val propertyAddress = property.address.ifBlank { "住所未設定" }
        val propertyStation = property.station.ifBlank { "駅未設定" }
        val propertyWalkMinutes = if (property.walkMinutes > 0) property.walkMinutes else 0
        val propertyRent = property.rent
        val propertyManagementFee = property.managementFee
        val propertyDeposit = property.deposit
        val propertyKeyMoney = property.keyMoney
        val propertyLayout = property.layout.ifBlank { "間取り未設定" }
        val propertySize = if (property.size > 0.0) property.size else 0.0
        val propertyAgeYears = if (property.ageYears > 0) property.ageYears else 0
        val propertyUrl = property.url.ifBlank { "https://example.com" }

        // ヘッダー（物件名）
        val headerText = Text.builder()
            .text(propertyName)
            .weight(TextWeight.BOLD)
            .size("lg")
            .build()
        
        // 物件情報のコンポーネントリスト
        val contentComponents = mutableListOf<com.linecorp.bot.model.message.flex.component.FlexComponent>()
        
        // 住所
        contentComponents.add(createInfoRow("📍 住所", propertyAddress))
        
        // 最寄り駅・徒歩
        contentComponents.add(createInfoRow("🚶 アクセス", "$propertyStation 徒歩${propertyWalkMinutes}分"))
        
        // 賃料・管理費
        val rentText = "${formatter.format(propertyRent)}万円"
        val managementFeeText = if (propertyManagementFee > 0) "(管理費${formatter.format(propertyManagementFee)}円)" else ""
        contentComponents.add(createInfoRow("💰 賃料", "$rentText $managementFeeText"))
        
        // 敷金・礼金
        val depositText = if (propertyDeposit > 0) "${formatter.format(propertyDeposit)}万円" else "なし"
        val keyMoneyText = if (propertyKeyMoney > 0) "${formatter.format(propertyKeyMoney)}万円" else "なし"
        contentComponents.add(createInfoRow("💴 敷金/礼金", "$depositText / $keyMoneyText"))
        
        // 間取り・面積・築年数
        contentComponents.add(createInfoRow("🏠 物件情報", "$propertyLayout / ${propertySize}m² / 築${propertyAgeYears}年"))
        
        // 詳細を見るボタン（URLが空の場合はダミーURLを使用）
        val footerContents = mutableListOf<com.linecorp.bot.model.message.flex.component.FlexComponent>()
        if (!propertyUrl.isNullOrBlank()) {
            val button = Button.builder()
                .style(ButtonStyle.PRIMARY)
                .action(URIAction("詳細を見る", URI.create(propertyUrl), null))
                .build()
            footerContents.add(button)
        }
        
        // メッセージを構築
        val bubble = Bubble.builder()
            .header(Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(*listOf(headerText).toTypedArray())
                .paddingAll("md")
                .build())
            .body(Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(contentComponents)
                .spacing("sm")
                .paddingAll("md")
                .build())
            .footer(Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(footerContents)
                .spacing("sm")
                .paddingAll("md")
                .build())
            .build()
        
        return FlexMessage("物件情報", bubble)
    }
    
    /**
     * 情報行のコンポーネントを作成します
     */
    private fun createInfoRow(label: String, value: String): Box {
        val labelComponent = Text.builder()
            .text(label)
            .size("sm")
            .color("#555555")
            .flex(1)
            .build()
        
        val valueComponent = Text.builder()
            .text(value)
            .size("sm")
            .color("#111111")
            .flex(3)
            .wrap(true)
            .build()
        
        return Box.builder()
            .layout(FlexLayout.HORIZONTAL)
            .contents(listOf(labelComponent, valueComponent))
            .build()
    }
    
    /**
     * LINEメッセージを送信します
     */
    private fun sendMessage(message: Message): CompletableFuture<BotApiResponse> {
        val pushMessage = PushMessage(config.userId, message)
        return lineClient.pushMessage(pushMessage)
    }
} 