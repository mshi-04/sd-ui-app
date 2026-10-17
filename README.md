# SD UI

日本語 | [English](README.en.md)

サーバー駆動 UI（Server-Driven UI）を学ぶために作成した Android クライアントです。Kotlin と Jetpack Compose による実装と開発履歴を、学習資料として保管します。

対応する API とインフラは [sd-ui-infra](https://github.com/mshi-04/sd-ui-infra) にあります。動作させるには、ご自身の API と Microsoft Entra ID 環境の設定が必要です。この README の構成例は例示値です。

詳細文書には開発当時の環境を前提とした記録も含まれます。公開版の設定値はサンプルとして、ご自身の環境に合わせて読み替えてください。

## 主な機能

- MSAL による Entra ID サインインと、認証状態に応じたログイン画面 / ダッシュボードの切り替え
- `GET {API_BASE_URL}ui/dashboard` による、サーバーが解決した UI スキーマの取得
- `Card` / `List` / `Chart` / `Button` / `Banner` / `Panel` のツリー表示と、theme / density の反映
- 未知の component type のスキップと、HTTP エラー・不正な応答に応じた表示の切り替え
- `ETag` / `If-None-Match` による条件付き取得と、304 応答時の保持中スキーマの再利用

## 構成

単一の `app` モジュール内で、Clean Architecture + MVVM のパッケージ境界を設けています。基本の依存方向は `ui -> domain <- data` です。

- `ui`: Compose の Route / Screen、ViewModel、UI state、テーマ
- `domain`: Android 非依存のモデル、値オブジェクト、Repository インターフェース、UseCase
- `data`: Retrofit API、DTO、mapper、Repository 実装、MSAL クライアント、Hilt module

主な技術は Jetpack Compose / Material 3、Hilt、Coroutines / StateFlow、Retrofit / OkHttp、kotlinx.serialization です。依存関係のバージョンは [`gradle/libs.versions.toml`](gradle/libs.versions.toml) で管理しています。

## 開発環境

| 項目 | リポジトリの設定 |
| --- | --- |
| JDK | 17（`jvmToolchain(17)`） |
| Android SDK | compileSdk 37.1 / targetSdk 37 / minSdk 24 |
| Android Gradle Plugin | 9.4.1 |
| Gradle | 付属の Wrapper 9.8.0 を使用 |

上記のビルド構成に対応した Android Studio と、Android SDK Platform 37.1 を用意してください。端末での実行には Android 7.0（API 24）以上が必要です。ローカルの SDK パスは `local.properties` の `sdk.dir` に設定し、このファイルはコミットしません。

## 自分の環境での設定

1. [sd-ui-infra](https://github.com/mshi-04/sd-ui-infra) を参照し、API と Entra ID の API 用 / Android 用アプリ登録を用意します。API の `UiSchema.Read` スコープと、Android クライアントからの利用許可を設定します。
2. `gradle.properties` の `API_BASE_URL` と `API_SCOPE` を、自分の環境の値に変更します。
3. [`app/src/main/res/raw/msal_config.json`](app/src/main/res/raw/msal_config.json) の `client_id`、`authorities[].audience.tenant_id`、`redirect_uri` を、自分の Android アプリ登録と署名証明書に合わせます。
4. [`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml) の `BrowserTabActivity` にも、同じパッケージ名と署名ハッシュを設定し、対応する redirect URI を Entra ID に登録します。

Gradle プロパティの例です。`example.com` と `<api-application-id>` は、自分の値へ置き換えてください。

```properties
API_BASE_URL=https://example.com/api/
API_SCOPE=api://<api-application-id>/UiSchema.Read
```

`API_BASE_URL` は `/api/` を含めます。末尾の `/` はビルド時に補完されます。両プロパティは `-PAPI_BASE_URL=...` / `-PAPI_SCOPE=...` でも上書きできます。未設定または空ならビルドが失敗し、`local.properties` に書いても API 設定としては読み込まれません。

MSAL の redirect URI は `msauth://com.example.sdui/<URLエンコード済み署名ハッシュ>` の形です。JSON 側はハッシュを URL エンコードし、Manifest の `android:path` は `/<エンコード前の署名ハッシュ>` とします。掲載の設定例をそのまま認証に使わず、自分の署名証明書から取得した値にそろえてください。詳しくは [認証の構成](docs/authentication.md) を参照してください。

## 認証の動作

起動時にはキャッシュに残る前回のアプリ内セッションをサインアウトし、毎回ログイン画面からサインインします。ブラウザーの Entra ID セッションは残るため、SSO によってパスワード入力なしで完了することがあります。

サインイン後にダッシュボードの UI スキーマを取得します。API が 401 を返すとサインアウトしてログイン画面へ戻り、サインアウトに失敗した場合は再試行画面を表示します。

アクセストークンは、ベース URL と host / port が一致する HTTPS リクエストにだけ `Authorization: Bearer` として付けます。

## ローカル API との接続

[sd-ui-infra](https://github.com/mshi-04/sd-ui-infra) の `scripts/dev_server.py` は、トークンを検証せずに seed のスキーマを返す開発用サーバーです。サーバーを起動し、上記の MSAL と `API_SCOPE` の設定を済ませたうえで、エミュレーター向けの接続先を指定します。

```powershell
.\gradlew.bat :app:installDebug "-PAPI_BASE_URL=http://10.0.2.2:8080/api/"
```

`10.0.2.2` は Android エミュレーターからホストへ接続するためのアドレスです。平文 HTTP は `debug` ビルドの `10.0.2.2` と `localhost` にだけ許可しています。

ローカル API を使う場合も、アプリ側では MSAL サインインが必要です。HTTP リクエストにはアクセストークンを付けず、開発用サーバーは役割のない利用者向けの `usage-summary` を返します。

## ビルドと検証

設定後、リポジトリのルートで実行します。以下は Windows PowerShell の例です。他の環境では `.\gradlew.bat` を `./gradlew` に読み替えてください。

```powershell
# debug ビルド
.\gradlew.bat :app:assembleDebug

# JVM 単体テスト
.\gradlew.bat :app:testDebugUnitTest

# Android Lint
.\gradlew.bat :app:lintDebug

# 端末またはエミュレーターへのインストール
.\gradlew.bat :app:installDebug

# 端末またはエミュレーターが必要な Compose UI テスト
.\gradlew.bat :app:connectedDebugAndroidTest
```

JVM テストは JUnit Jupiter / MockK / Turbine / kotlinx-coroutines-test、端末テストは AndroidX Test の JUnit4 ランナーを使います。JSON 変換、条件付き取得、認証・画面状態の遷移などを対象にしています。

## サンプルとしての制約

- component はラベルとコンテナーを中心に表示します。`Chart` / `List` の実データ取得や、component action の実行は実装しておらず、スキーマ由来の `Button` は無効です。
- `props` は primitive 値を文字列として保持し、object / 配列 / `null` は取り込みません。
- UI スキーマと ETag はプロセス内メモリだけに保持し、サインアウト時に破棄します。永続化や `Cache-Control` に基づく HTTP キャッシュはありません。
- release 用の認証・配布構成は用意していません。利用する署名証明書に合わせた redirect URI 登録と構成が別途必要です。
- ビルドフレーバー、ローカル DB、CI は未導入です。アプリの表示は日本語で、英語 README の追加はアプリの英語対応を意味しません。

## 関連資料

- [sd-ui-infra: 対応するサーバーとインフラ](https://github.com/mshi-04/sd-ui-infra)
- [サーバー側のクライアント連携契約](https://github.com/mshi-04/sd-ui-infra/blob/main/docs/client_integration.md)
- [プロジェクト概要](docs/project-overview.md)
- [アーキテクチャ](docs/architecture.md)
- [開発環境と検証手順](docs/development-setup.md)
- [認証](docs/authentication.md)
- [クライアント側のサーバー契約](docs/server-contract.md)
- [単体テストの方針](docs/unit-test.md)
