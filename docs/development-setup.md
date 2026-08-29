# Development Setup

クローン後の前提と、手元で実行する検証コマンドをまとめます。

## 前提

- JDK 17（Gradle の `jvmToolchain` は 17）
- Android SDK（compileSdk 36 / minSdk 24 / targetSdk 36）
- ビルドフレーバーはなく、開発・動作確認は `debug` build type を使う
- `local.properties` の `sdk.dir` はローカル環境の値のため、コミットしない

Gradle は Build Cache と Configuration Cache を有効にしています（`gradle.properties`）。
task の入力を変えるプロパティを渡すと configuration cache は作り直されます。

## ベース URL の指定

`API_BASE_URL` を渡さない場合は `https://example.com/` が使われます。
ローカルサーバーへ向ける場合は Gradle プロパティで差し替えます。

```powershell
.\gradlew.bat :app:assembleDebug "-PAPI_BASE_URL=http://10.0.2.2:8080/"
```

エミュレーターからホストの `localhost` を指す場合は `10.0.2.2` を使います。
平文 HTTP は `debug` ビルドだけで許可されます。詳細は [`docs/server-contract.md`](server-contract.md) を参照します。

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
