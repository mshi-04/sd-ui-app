# Implementation Guidelines

この文書は実装時の基本方針をまとめます。
詳細設計は、必要になった時点で別文書またはコード上で明確にしてください。

## UI

- UI は Jetpack Compose と Material 3 を基本にする
- Compose Preview を壊さないように、UI コンポーネントは小さく分ける
- ライト / ダーク両対応を前提にし、固定色の直書きを避ける
- 画面の色は `SDUITheme` の `MaterialTheme.colorScheme` から取る
- システム UI との重なりは `enableEdgeToEdge()` と `safeDrawingPadding()` で扱う
- Screen は state と callback だけを受け取り、ViewModel や domain の型を引数に取らない

## Architecture

- 構造は Clean Architecture + MVVM を基本にする
- DI は Hilt を使う
- 依存方向、レイヤーの責務、パッケージ構成、UseCase の粒度は [`docs/architecture.md`](architecture.md) を正本とする

## Kotlin

- 関数名は英語にする
- インデントは半角スペース2つにする（既存ファイルの記述にそろえる）
- Route、Screen、ViewModel、主要な UiState は、役割と同名の独立ファイルを基本とする
- UI state / result / event は、それぞれ `XxxUiState` / `XxxUiResult` / `XxxUiEvent` と命名する
- 同じ責務を構成する小型の class / interface / object / enum は、ファイルが読みやすい範囲で関連する所有者ファイルへトップレベル宣言としてまとめてよい。300行程度を分割検討の目安とするが、機械的な上限にはしない
- sealed hierarchy の直接の子型や、親に強く従属する private 実装型は親型へネストしてよい
- ガード条件は早期リターンで扱い、ネストを深くしない
- 意味のある値は primitive のまま広げず、必要に応じて値オブジェクトにする（設計方針は [`docs/architecture.md`](architecture.md) の Value Object を正本とする）
- 取り得る種類だけを閉じ、実装型に別の superclass や複数 interface の実装余地を残す場合は `sealed interface` で表す
- 親型に共通の状態・実装・constructor 制約を持たせる場合や、class としての継承関係が必要な場合は `sealed class` で表す
- 定数は所有者の `private companion object` に置き、意味の分かる名前を付ける

## Navigation

- 現在は Navigation Compose を導入しておらず、画面は `MainRoute` の単一構成
- 画面が増えて遷移が必要になった時点で Navigation Compose の導入を提案する
- 画面追加時は route 名、引数、戻る挙動を明確にする

## State / Data

- ViewModel から UI へは StateFlow を中心に状態を公開する
- 非同期処理は Coroutines を使い、公開する suspend API を main-safe にする
- 実行中の再入防止は UI state を根拠に早期リターンする
- 通信・ファイル操作など blocking I/O を行う実装が、必要な dispatcher への切り替えを所有する
- `CancellationException` は再送出し、広い例外捕捉で coroutine のキャンセルを握りつぶさない
- サーバー通信の契約と応答の分類は [`docs/server-contract.md`](server-contract.md) を正本とする

## Error

- data 層は失敗を `Result` に畳み、例外型を domain / UI へ漏らさない
- 失ってはいけない処理結果は UI state に保持し、UI から確認済み callback を受けて消費済みにする
- Channel event は、collector 不在、再生成、送信失敗で失われても処理結果や整合性に影響しない best-effort の通知に限る
- 一回限り event の Channel は、`latest wins`（最新だけ届けばよい）が成立する場合に限り `Channel.CONFLATED` を使う
- `Channel.BUFFERED` は一時的な backpressure を吸収する手段であり、取りこぼしのない永続的な配送手段として扱わない

## Test

- Unit Test の方針は [`docs/unit-test.md`](unit-test.md) を確認する

## Localization

- 表示文字列は `strings.xml` に寄せ、Composable では `stringResource` から取得する
- 現在は既定 locale のみで、英語リソースは未導入
- 値を埋め込む文言は書式指定子付きの string resource にし、文字列連結で組み立てない
- UI 実装時は長い文字列でも崩れない余白を確保する
