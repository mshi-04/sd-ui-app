# Repository 実装

domain の Repository interface を既存データ源に接続し、実装詳細を UI / domain へ漏らさない。

## 確認する対象

- `data/repository/` の既存実装（`DefaultServerStatusRepository`）
- 対応する domain interface の期待値と、remote data source の失敗の種類
- `data/di/RepositoryModule` の binding

## 実装時の注意

- interface が期待する不変条件と、data source 側の制約を分けて扱う。
- 失敗は `Result.failure` に畳み、例外型や DTO を domain / UI へ漏らさない。
- `CancellationException` は先に捕捉して再送出し、その後で他の例外を扱う。
- data model と domain model がずれる場合は変換を mapper に委ね、実装本体へ散らさない。
- 実装クラスは `@Inject constructor` で構築し、binding は `data/di` の module に置く。

## テスト判断

- 振る舞いは JVM Unit Test を基本にし、通信を跨ぐ経路は MockWebServer で再現する。

## 検証観点

- domain へ data 由来の型・例外が漏れていないか。
- キャンセルが失敗として扱われていないか。
