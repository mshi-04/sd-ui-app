# Project Overview

この文書は SD UI の概要、主要な確認先、技術スタックをまとめます。

技術スタックの具体値は執筆時点のスナップショットです。バージョンの正確な値は、必ず
`app/build.gradle.kts` と `gradle/libs.versions.toml` を確認してください。

## 概要

SD UI は、サーバーと接続する Android クライアントです。

現在の基本情報:

- パッケージ名: `com.example.sdui`
- ルートプロジェクト名: `SD UI`
- メインモジュール: `:app`
- ビルドフレーバー: なし（`debug` / `release` の build type のみ）
- minSdk 24 / targetSdk 36 / compileSdk 36

## 現在の主な機能

- MSAL による Entra ID サインインと、サインイン状態に応じたログイン画面 / ダッシュボードの出し分け
- `GET /ui/{surface}` による UI スキーマの取得と、取得結果に応じた画面表示の切り替え
- サーバーが解決した component ツリーの描画（未知の `type` はスキップ）
- `ETag` と `If-None-Match` による条件付き取得（プロセス内のメモリ保持のみ）

契約の正本はサーバー側リポジトリ (`sd-ui-infra`) の `docs/client_integration.md` です。
クライアント側の受け取り方は [`docs/server-contract.md`](server-contract.md) を正本とします。

## 主な確認先

- `app/`: Android アプリ本体
- `app/src/main/java/com/example/sdui/`: Kotlin ソース
  - `ui/`: Compose 画面（`route` / `screen`）、`viewmodel`、`state`、`theme`
  - `domain/`: model、値オブジェクト、UseCase、Repository interface
  - `data/`: Retrofit API、DTO、mapper、MSAL クライアント、Repository 実装、data 層の Hilt module
  - `di/`: アプリ全体の構成値を組み立てる Hilt module と qualifier
  - `MainActivity` / `SdUiApplication`: Android 画面とアプリの entry point
- `app/src/main/res/`: Android リソース（表示文字列は `strings.xml` に集約）
- `app/src/debug/AndroidManifest.xml`: `debug` ビルドだけの平文 HTTP 許可
- `app/src/debug/java/com/example/sdui/data/di/`: `debug` ビルドだけのローカル検証用ヘッダー送信
- `app/src/release/java/com/example/sdui/data/di/`: `release` ビルドの構成（ローカル検証用ヘッダーを持たない）
- `app/src/main/res/raw/msal_config.json`: MSAL の構成
- `app/src/test/`: JVM Unit Test
- `app/src/androidTest/`: instrumented test / Compose UI Test
- `gradle/libs.versions.toml`: 依存関係とプラグインのバージョン管理

## 技術スタック

### Android / UI

- Kotlin
- Jetpack Compose（BOM 管理）
- Material 3
- Dynamic Color（Android 12 以上）とライト / ダークモード
- Compose Preview

### Auth

- MSAL Android（単一アカウント構成）
- 構成と動作確認は [`docs/authentication.md`](authentication.md) を正本とします。

### DI

- Hilt
- `hilt-lifecycle-viewmodel-compose`（Compose から ViewModel を注入）

### State / Async

- StateFlow
- Coroutines
- `collectAsStateWithLifecycle()`（Route での state 収集）

### Network

- Retrofit
- OkHttp（interceptor は Hilt の multibinding で組み立てる）
- kotlinx.serialization（`Json` converter factory、`ignoreUnknownKeys = true`）
- ベース URL は Gradle プロパティ `API_BASE_URL` から `BuildConfig` 経由で注入

通信の契約と応答の扱いは [`docs/server-contract.md`](server-contract.md) を正本とします。

### Test

- JUnit Jupiter（JVM Unit Test。`useJUnitPlatform()` を設定済み）
- kotlinx-coroutines-test（`runTest`）
- MockK（モック / 呼び出しの検証）
- Turbine（`StateFlow` の emission 順序の検証）
- OkHttp MockWebServer（Retrofit 経由の JSON 変換と失敗系の検証）
- JUnit 4 / AndroidX Test / Espresso / Compose UI Test（instrumented test）

テストの方針は [`docs/unit-test.md`](unit-test.md) を正本とします。

### Build

- Android Gradle Plugin / KSP / Kotlin Serialization plugin
- JDK 17（`jvmToolchain(17)`）
- Gradle Build Cache と Configuration Cache を有効化
- `release` は現時点で minify 無効

## 未導入のもの

以下は未導入です。実装済みとして扱わず、必要になった時点で導入を提案します。

- ビルドフレーバー
- Navigation Compose（ログイン画面とダッシュボードの出し分けは `SessionRoute` の状態分岐で行う）
- ローカル DB / 永続化（Room、DataStore。MSAL 自身のトークンキャッシュは除く）
- release ビルド用の redirect URI 登録
- 静的解析（ktlint、detekt、Android Lint の運用設定）
- Git フック、fastlane、GitHub Actions などの CI
- カバレッジ計測
- クラッシュ収集などの Observability
- 英語ローカライズ

## 判断の前提

- まず Gradle とソースを見て、現在導入済みのものを確認する
- 既存構成を優先し、必要以上に大きな再編をしない
- 構造方針は [`docs/architecture.md`](architecture.md) を確認する
