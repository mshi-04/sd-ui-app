# SD UI

Kotlin と Jetpack Compose で構築する Android クライアントです。

## Architecture

単一の `app` モジュール内で、依存方向を `ui -> domain <- data` に限定しています。

- `ui`: `route`、`screen`、`state`、`viewmodel`、Compose テーマ
- `domain`: Android 非依存のモデル、値オブジェクト、Repository インターフェース、UseCase
- `data`: Retrofit API、DTO、mapper、Repository 実装、Hilt module

規模が大きくなった場合は、このパッケージ境界をそのまま Gradle モジュールへ分離できます。

## Server connection

現在は `GET /health` が次の JSON を返す前提の仮契約です。

```json
{
  "status": "ok",
  "message": "ready"
}
```

`status` は必須です。`ok` は接続可能、`ok` 以外はサーバー都合の利用不可、HTTPエラーや不正JSONは通信失敗として、それぞれ異なる UI state へ変換します。

サーバーのベース URL は Gradle プロパティ `API_BASE_URL` で差し替えられます。末尾の `/` はビルド時に補完されます。

```powershell
.\gradlew.bat :app:assembleDebug "-PAPI_BASE_URL=http://10.0.2.2:8080/"
```

`debug` ビルドだけはローカル開発向け HTTP 通信を許可し、`release` ビルドでは許可しません。URL 未指定時は `https://example.com/` を使用します。

Azure 固有の URL、認証、証明書設定はサーバー契約確定後に追加します。現時点の通信層は通常の Retrofit API として分離しています。

## Test

JVM 単体テストには JUnit Jupiter、Compose の端末テストには AndroidX Test の JUnit4 ランナーを使用します。

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

テストでは MockWebServer による `GET /health` の JSON 変換と、成功・失敗に応じた ViewModel の UI state 遷移を確認します。
