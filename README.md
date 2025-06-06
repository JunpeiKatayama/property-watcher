# 物件監視通知システム

SUUMO から指定した条件に合致する新着物件情報を取得し、LINE 通知するシステムです。

## 機能概要

- SUUMO ウェブサイトから物件情報をスクレイピング
- JSON 形式で柔軟に検索条件を指定可能（SUUMO の仕様に依存）
- 新着物件のみを LINE Messaging API を使用して通知
- 定期実行可能なバッチシステム

## 必要環境

- Java 21 以上
- Kotlin 1.9.x
- Gradle 8.x
- LINE Developers アカウント（LINE Messaging API 用）

## セットアップ

### 1. リポジトリのクローン

```bash
git clone https://github.com/yourusername/property_watcher.git
cd property_watcher
```

### 2. LINE Messaging API の設定

1. [LINE Developers](https://developers.line.biz/)にアクセスし、新規プロバイダーとチャネルを作成
2. Messaging API のチャネルアクセストークンを発行
3. LINE のユーザー ID を取得（1:1 チャット開始後、Bot からのメッセージで取得可能）

### 3. 設定ファイルの作成

`config.json.sample`をコピーして`config.json`を作成し、必要な情報を入力します。

```bash
cp config.json.sample config.json
```

`config.json`を編集し、以下の情報を設定します：

- LINE Messaging API のチャネルアクセストークン
- LINE のユーザー ID
- 物件検索条件

## 使い方

### ビルド

```bash
./gradlew build
```

### 実行

```bash
./gradlew run
```

または、fat jar ファイルを直接実行：

```bash
java -jar build/libs/property_watcher-1.0-SNAPSHOT-all.jar
```

設定ファイルのパスを指定して実行：

```bash
java -jar build/libs/property_watcher-1.0-SNAPSHOT-all.jar path/to/config.json
```

### 定期実行

システムを定期的に実行するには、OS のタスクスケジューラを使用します。

#### Windows の場合（タスクスケジューラ）

1. タスクスケジューラを開く
2. 「基本タスクの作成」を選択
3. タスクの名前と説明を入力
4. トリガーを設定（毎日など）
5. アクションとして「プログラムの開始」を選択
6. プログラムのパスに`java`を、引数に`-jar "C:\path\to\property_watcher-1.0-SNAPSHOT-all.jar"`を設定

#### Linux の場合（cron）

cron に以下のような設定を追加します：

```
0 9,21 * * * cd /path/to/property_watcher && java -jar build/libs/property_watcher-1.0-SNAPSHOT-all.jar
```

## GCP Cloud Run Jobs でのデプロイ・定期実行

Google Cloud Run Jobs と Cloud Scheduler を使って、バッチ処理をクラウド上で定期実行することも可能です。

### デプロイ・定期実行の手順

1. 必要に応じて `deploy_and_schedule.sh` 内の変数（`PROJECT_ID` など）を編集してください。
2. 実行権限を付与します：
   ```sh
   chmod +x deploy_and_schedule.sh
   ```
3. スクリプトを実行します：
   ```sh
   ./deploy_and_schedule.sh
   ```

このスクリプトを実行するだけで、

- Java アプリの fat jar ビルド
- Docker イメージのビルド・push
- Cloud Run Job の作成・更新
- Cloud Scheduler による定期実行設定
  まで一括で自動化されます。

#### 注意事項

- `PROJECT_ID`、`REGION`、`IMAGE_NAME`、`CRON_SCHEDULE` などはご自身の環境に合わせて**必ず書き換えてください**。
- 必要に応じてサービスアカウントに Cloud Run Job 実行権限を付与してください。
- Dockerfile では `property_watcher-1.0-SNAPSHOT-all.jar` を使用しています。

## 設定ファイルの詳細

`config.json` の各設定項目について説明します：

```json
{
  // 検索条件（複数指定可能）
  "searchConditions": [
    {
      "name": "検索条件の名前", // 任意の条件名
      "prefecture": "都道府県名", // 例: "埼玉県"
      "city": "市区町村名", // 例: "鴻巣市"（省略可）
      "district": "町名", // 例: "北新宿"（省略可）
      "minRent": 6, // 最小賃料（万円, 省略可）
      "maxRent": 8, // 最大賃料（万円, 省略可）
      "layouts": ["1LDK", "2LDK"], // 間取り（省略可）
      "maxWalkMinutes": 15, // 最大徒歩分数（省略可）
      "maxAgeYears": 30, // 最大築年数（省略可）
      "hasParking": true, // 駐車場あり（省略可）
      "hasPetAllowed": true, // ペット可（省略可）
      "otherConditions": {
        // SUUMOのURLパラメータを直接指定（省略可）
        "key": "value" // 例: "ek": "98287" など
      }
    }
  ],
  "lineConfig": {
    // LINE通知設定
    "channelToken": "LINEチャネルアクセストークン", // LINEチャネルアクセストークン
    "userId": "LINEユーザーID" // LINEユーザーID
  },
  "dataStorePath": "data", // データ保存先ディレクトリ
  "scheduleConfig": {
    // スケジュール設定
    "intervalHours": 12, // 実行間隔（時間）
    "startHour": 9, // 開始時刻（時）
    "startMinute": 0 // 開始時刻（分）
  }
}
```

> ※ `hasPetAllowed` や `hasParking` などは SUUMO の仕様変更で効かなくなる場合があります。
> ※ `otherConditions` には SUUMO の URL パラメータ（例: `"ek": "98287"` など）を直接指定できます。
> ※ district（町名）は省略可能です。

## 注意事項

- SUUMO サイトのスクレイピングは利用規約に準拠した頻度で行ってください。
- LINE Messaging API の利用上限に注意してください。
- サンプル設定のままでは LINE 通知は動作しません。必ずご自身の LINE チャネル情報を設定してください。
- SUUMO の仕様変更により一部の検索条件が効かなくなる場合があります。
