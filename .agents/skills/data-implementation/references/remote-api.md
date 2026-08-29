# Retrofit API / DTO

サーバー契約をそのまま写した境界を作り、通信ライブラリの型を data 層に閉じる。

## 確認する対象

- `data/remote/` の `SdUiApi` と `ServerStatusRemoteDataSource`
- `data/model/` の DTO（`HealthResponseDto`）
- `data/di/NetworkModule` の `Json` / `Retrofit` / API の provide
- [`docs/server-contract.md`](../../../../docs/server-contract.md) のエンドポイントと応答

## 実装時の注意

- API interface のメソッドは `suspend` で宣言し、DTO をそのまま返す。
- パスは先頭に `/` を付けず、ベース URL の末尾 `/` と組み合わせる前提を保つ。
- DTO は `@Serializable` な `data class` にし、任意フィールドは nullable + 既定値で表す。
- 未知フィールドの無視は `Json { ignoreUnknownKeys = true }` に任せ、DTO 側で吸収しない。
- DTO に表示用の加工や判定ロジックを持たせない。
- remote data source は API 呼び出しの薄い委譲に留め、例外の変換は Repository 実装が担う。

## テスト判断

- MockWebServer で JSON 復号、リクエストの method / path、HTTP エラーを JVM Unit Test で押さえる。
- `@AfterEach` で `shutdown()` を呼び、サーバーを残さない。

## 検証観点

- 必須フィールドの欠損が失敗として扱われるか。
- ベース URL の末尾スラッシュとパスの組み合わせが意図どおりか。
