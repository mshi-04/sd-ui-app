# Development Setup

クローン後の前提と、手元で実行する検証コマンドをまとめます。

## 前提

- JDK 17（Gradle の `jvmToolchain` は 17）
- Android SDK（compileSdk 37.1 / minSdk 24 / targetSdk 37。SDK Manager で Android SDK Platform 37.1 を導入する）
- ビルドフレーバーはなく、開発・動作確認は `debug` build type を使う
- `local.properties` の `sdk.dir` はローカル環境の値のため、コミットしない

Gradle は Build Cache と Configuration Cache を有効にしています（`gradle.properties`）。
task の入力を変えるプロパティを渡すと configuration cache は作り直されます。

## ベース URL の指定

`API_BASE_URL` と `API_SCOPE` は `gradle.properties` に置いてあり、Android Studio の
Run ボタンからのビルドにも効きます。既定値は dev の実 API です。

どちらも未設定または空の場合はビルドが失敗します。既定値へ暗黙に落ちないため、
設定漏れが実行時の 404 ではなくビルド時のエラーとして出ます。
`local.properties` は `providers.gradleProperty` の対象外なので使えません。

ベース URL は `/api/` で終わり、エンドポイントはその下の `ui/{surface}` になります。
ローカルサーバーへ一時的に向ける場合は Gradle プロパティで上書きします。

```powershell
.\gradlew.bat :app:assembleDebug "-PAPI_BASE_URL=http://10.0.2.2:8080/api/"
```

エミュレーターからホストの `localhost` を指す場合は `10.0.2.2` を使います。
平文 HTTP は `debug` ビルドだけで許可されます。詳細は [`docs/server-contract.md`](server-contract.md) を参照します。

## 実 API へ向ける

MSAL でサインインして実 API へ接続します。`DEV_OID` / `DEV_ROLES` は指定しません。

```powershell
.\gradlew.bat installDebug "-PAPI_BASE_URL=https://sdui-api.example.invalid/api/"
```

要求するスコープは Gradle プロパティ `API_SCOPE` で差し替えられます。
既定値と Entra ID 側の設定は [`docs/authentication.md`](authentication.md) を正本とします。

## ローカルサーバーと利用者の指定

サーバー側リポジトリ (`sd-ui-infra`) の `scripts/dev_server.py` は、トークンを検証せず
seed のスキーマを返します。利用者は `X-Dev-Oid` と `X-Dev-Roles` で切り替えます。
`debug` ビルドだけが、次の Gradle プロパティの値をこのヘッダーとして送ります。

```powershell
.\gradlew.bat :app:assembleDebug `
  "-PAPI_BASE_URL=http://10.0.2.2:8080/api/" `
  "-PDEV_OID=00000000-0000-0000-0000-000000000000" `
  "-PDEV_ROLES=SupportAgent,UiAdmin"
```

指定した場合は `Authorization` を送らないため、MSAL のサインインは不要です。
指定しない場合はヘッダーを送らず、サーバーは権限を持たない利用者として応答します。
`release` ビルドにはこのヘッダーの送信経路がありません。

## ローカルでの検証

```powershell
# JVM Unit Test
.\gradlew.bat :app:testDebugUnitTest

# Android Lint
.\gradlew.bat :app:lintDebug

# debug ビルド
.\gradlew.bat :app:assembleDebug

# release ビルド
.\gradlew.bat :app:assembleRelease

# 端末またはエミュレーターが必要
.\gradlew.bat :app:connectedDebugAndroidTest
```

JVM Unit Test は JUnit Platform で実行します（`useJUnitPlatform()`）。
Compose UI Test は `app/src/androidTest/` にあり、実端末またはエミュレーターが必要です。

## 未導入の検証

以下は未導入です。導入されていない前提で作業し、必要になった時点で提案します。

- ktlint / detekt などのフォーマッタ・静的解析
- pre-commit などの Git フック
- fastlane
- GitHub Actions などの CI
- カバレッジ計測

現時点では、コード整形は既存ファイルの記述にそろえる形で保ちます（[`docs/implementation-guidelines.md`](implementation-guidelines.md) の Kotlin を参照）。
