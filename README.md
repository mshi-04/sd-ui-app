# SD UI

Kotlin と Jetpack Compose で構築する Android クライアントです。

## Architecture

単一の `app` モジュール内で、依存方向を `ui -> domain <- data` に限定しています。

- `ui`: `route`、`screen`、`state`、`viewmodel`、Compose テーマ
- `domain`: Android 非依存のモデル、値オブジェクト、Repository インターフェース、UseCase
- `data`: Retrofit API、DTO、mapper、Repository 実装、Hilt module

規模が大きくなった場合は、このパッケージ境界をそのまま Gradle モジュールへ分離できます。

## Server connection

`GET {baseUrl}ui/{surface}` で UI スキーマを取得し、サーバーが解決した component ツリーを描画します。

```json
{
  "surface": "dashboard",
  "version": "2026-06-01",
  "revision": "a1b2c3",
  "theme": "dark",
  "density": "compact",
  "components": [
    { "id": "billing-summary", "type": "Card", "props": { "title": "Billing" }, "children": [] }
  ]
}
```

未知の `type` の component はスキップします。200 は表示、304 は保持中のスキーマ、401 と 404 はそれぞれ専用の UI state、
その他の HTTP エラーや不正 JSON は通信失敗として、異なる UI state へ変換します。

サーバーのベース URL は `gradle.properties` の `API_BASE_URL` に置いています。一時的な差し替えは Gradle プロパティで上書きします。末尾の `/` はビルド時に補完されます。

```powershell
.\gradlew.bat :app:assembleDebug "-PAPI_BASE_URL=http://10.0.2.2:8080/api/"
```

`debug` ビルドだけはローカル開発向け HTTP 通信を許可し、`release` ビルドでは許可しません。`API_BASE_URL` と `API_SCOPE` に既定値はなく、未設定ならビルドが失敗します。

## Authentication

MSAL で Entra ID にサインインし、`Authorization: Bearer` でトークンを送ります。
起動時はサイレント取得を試み、失敗した場合と 401 を受け取った場合はログイン画面へ戻します。

```powershell
.\gradlew.bat installDebug "-PAPI_BASE_URL=https://sdui-api.example.invalid/api/"
```

ローカルのモックサーバーへ向ける場合は `DEV_OID` / `DEV_ROLES` を渡します。
指定するとサインイン不要の `X-Dev-*` ヘッダー経路になり、`Authorization` は送りません。

構成値、release 用 redirect URI の扱い、動作確認の手順は [`docs/authentication.md`](docs/authentication.md) を参照します。

## Test

JVM 単体テストには JUnit Jupiter、Compose の端末テストには AndroidX Test の JUnit4 ランナーを使用します。

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

テストでは、fake の Interceptor で応答を返した `GET /ui/{surface}` の JSON 変換と条件付き取得、成功・失敗に応じた ViewModel の UI state 遷移を確認します。
