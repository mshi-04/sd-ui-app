---
name: data-implementation
description: サーバー通信、JSON 変換、Repository 実装、DI 配線が絡む依頼で使う。SD UI の Data 層の Retrofit API、DTO、mapper、Repository 実装、Hilt module を担当する。
---

# 目的

SD UI の data 層で、domain の interface を既存データ源に接続し、実装詳細を UI / domain へ漏らさない。

# 最初に確認するもの

- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/server-contract.md`](../../../docs/server-contract.md)
- [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md)
- [`docs/unit-test.md`](../../../docs/unit-test.md)
- [`app/src/main/java/com/example/sdui/data/`](../../../app/src/main/java/com/example/sdui/data/)
- 関連する [`app/src/test/java/com/example/sdui/data/`](../../../app/src/test/java/com/example/sdui/data/)

# 役割クラスと参照先

変更対象の役割クラスを見分け、該当する reference を読んでから実装する。
境界を跨ぐ変更（例: エンドポイント追加が DTO・mapper・Repository 実装へ波及する）では、関連する reference を複数読む。
薄い役割は reference を作らず docs に委ねる。

| 役割クラス | 参照 |
| --- | --- |
| Retrofit API / DTO / remote data source | [`references/remote-api.md`](references/remote-api.md) |
| Repository 実装 | [`references/repository-implementation.md`](references/repository-implementation.md) |
| mapper | [`references/mapper.md`](references/mapper.md) |
| Hilt module の単純な binding / provide | reference なし。`docs/architecture.md` の DI 方針に従う |

# 手順

1. 変更対象の役割クラスを判定し、該当する reference（複数に跨る場合は関連するものすべて）を読む。
2. domain interface の期待値とサーバー契約の制約を分けて読む。契約の正本は [`docs/server-contract.md`](../../../docs/server-contract.md)。
3. reference の観点に沿って実装し、変換は mapper、Android / 通信ライブラリ依存は data 層に閉じる。
4. 複数の役割クラスに跨る場合は、DTO・mapper・domain interface の整合を確認する。
5. 振る舞いを JVM Unit Test で押さえる。API 定義は応答を返す fake の Interceptor、呼び出し側は `SdUiApi` の MockK で検証する。
6. 変更内容に該当する検証をすべて実行する。Kotlin 変更は `.\gradlew.bat :app:testDebugUnitTest` で検証し、Manifest やリソースへ波及する場合は `.\gradlew.bat :app:lintDebug` を組み合わせる。task 一覧の正本は [`docs/development-setup.md`](../../../docs/development-setup.md)。
7. 変更内容、実行した task と結果、未実施の検証と理由を簡潔に報告する。

# 注意事項

- data 層から UI state や Compose の型を参照しない。
- domain model の不変条件を壊す値を mapper から流さない。
- サーバー契約を変える場合は [`docs/server-contract.md`](../../../docs/server-contract.md) を先に更新する。
- 設定値は data 層へハードコードせず `BuildConfig` から受け取る。`BuildConfig` を読むのは Hilt module に限り（ベース URL は `NetworkModule`、`X-Dev-*` は `DevIdentityModule`、API スコープは `di` の `AppConfigModule`）、data 層の実装クラスは module が組み立てた依存や constructor 引数で受け取る。
- 平文 HTTP の許可は `debug` ビルドに閉じたまま保つ。
