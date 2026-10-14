# Architecture

SD UI は Clean Architecture をベースに、UI 層は MVVM で構成します。
現在は単一の `:app` モジュール内をパッケージで分割しており、規模が大きくなった時点で
同じ境界のまま Gradle モジュールへ分離できる粒度を保ちます。

## 基本方針

- 機能コードの依存方向は `ui -> domain <- data` を守る
- `ui` は `data` の実装詳細に依存せず、必要なビジネスルールと抽象を `domain` 経由で利用する
- `data` は `domain` の Repository interface を実装し、Retrofit などのデータ源を扱う
- Domain 層は Android Framework に依存しない
- UseCase はビジネスルール、複数処理の調停、再利用する操作の境界として置き、単純委譲のためだけには増やさない
- 依存注入は Hilt で行い、`SdUiApplication` と `di` / `data.di` を composition boundary とする。
  `di` はアプリ全体の構成値（`BuildConfig` 由来の値など）、`data.di` は data 層の binding を担う。
  API のベース URL は注入しない。読む場所は [`docs/server-contract.md`](server-contract.md) を正本とする
- app entry と DI module は依存を組み立てる composition root のため、配線に必要な具象型を参照できる。
  `MainActivity` が `SignInActivityHolder` へ自身を登録するのはこの位置づけによる

## レイヤー構成

以下は現在の主なパッケージです。網羅的な固定一覧ではなく、新しいファイルは責務が最も近い既存パッケージへ追加します。
`ui` は機能別に分けず、主要な役割別パッケージを保ちます。
型の接尾語だけを根拠に `data` / `type` / `event` などの補助パッケージを増やしません。

- `ui`: Compose 画面、ViewModel、UI state
  - `SdUiApp`: `AuthViewModel` の認証状態を収集し、サインアウト中画面、サインアウト失敗画面、`LoginRoute`、`MainRoute` を出し分けるアプリレベルの entry
  - `route`: ViewModel と画面を接続し、`collectAsStateWithLifecycle()` で state を収集する `XxxRoute`。
  - `screen`: state と callback を受け取る `XxxScreen`。メニュー開閉など短命な UI element state は保持してよい
  - `viewmodel`: 画面ごとの `XxxViewModel` と、アプリレベルの認証状態を持つ `AuthViewModel`
  - `state`: 画面ごとのフラットな UI state（`XxxUiState`）と、その状態を表す enum（`XxxUiStatus`）
  - `model`: UI state の部品になる表示用モデル（`DashboardUiModel`）。ViewModel がコンストラクタで直接組み立てる
  - `theme`: Compose / Material 3 テーマ
- `domain`: Android Framework に依存しないビジネスロジック
  - `model`: ドメインモデル（`UiSchema`、`UiComponent`、`UiSchemaResolution`、`AuthSession`、`AuthAccount`）
  - `model/value`: 値オブジェクト（`SurfaceId`、`UiComponentId`、`AccountId`、`AccountName`）
  - `usecase`: ビジネス上の操作を表す UseCase（`LoadUiSchemaUseCase`、`SignInUseCase` など）
  - `repository`: Repository interface（`UiSchemaRepository`、`AuthRepository`）
- `data`: domain の interface を実装し、データ源を扱う
  - `remote`: Retrofit の API interface、remote data source、OkHttp interceptor
  - `auth`: MSAL を包む認証クライアントと、対話サインインに必要な Activity の保持
  - `model`: DTO など data 層固有のモデル
  - `mapper`: data model から domain model への変換
  - `repository`: Repository 実装（`DefaultUiSchemaRepository`、`DefaultAuthRepository`）
  - `di`: OkHttp、Retrofit、Json、API、認証、Repository の Hilt module
- app 直下
  - `SdUiApplication` / `MainActivity`: アプリと Android 画面の entry point
  - `di`: アプリ全体の構成値を提供する Hilt module と qualifier（`ApiScope`）

画面固有の callback 集約、補助 data class、enum、test tag は、
所有する `route` / `screen` / `viewmodel` / `state` へ置きます。
UI state / status / model / result / event は、それぞれ `XxxUiState` / `XxxUiStatus` / `XxxUiModel` / `XxxUiResult` / `XxxUiEvent` と命名します。
複数箇所から参照する契約は主要な役割パッケージ内で独立ファイルにし、
所有者だけが使う小型型は所有者ファイルへまとめます。

## MVVM

- ViewModel は画面全体の状態を `StateFlow` で公開する
- Route は `StateFlow` を `collectAsStateWithLifecycle()` で収集し、state と callback を Screen に渡す
- Screen は state と callback だけを受け取り、ViewModel を直接参照しない
- 実行中の再入を防ぐガードは、UI state の値を根拠に ViewModel 側で早期リターンする
- プロセス再生成後に復元する最小限の状態は、ViewModel 側では `SavedStateHandle`、UI 側では `rememberSaveable` を使う
- 再生成後の復元が不要な UI element state は、必要な Composable の近くで `remember` により保持する
- ViewModel に Android View や Context 依存を直接持ち込まない
- 画面固有の状態とドメインモデルを混ぜすぎない
- 失ってはいけない処理結果は UI state に保持し、UI から確認済み callback を受けて消費済みにする
- UI 操作だけで完結する画面遷移は UI callback として扱い、ViewModel の Channel を必須経路にしない

## UseCase

- 画面から直接 Repository implementation を呼ばない
- ViewModel は UseCase または domain の Repository interface に依存する
- UseCase は、できるだけ1つの明確な操作を表す
- ビジネスルール、複数 Repository の調停、複数画面からの再利用がない単純委譲は、層の形をそろえるためだけに UseCase 化しない
- サーバー応答を業務上の意味へ解釈する判定のようなビジネスルールは、UseCase または domain model に置く

`LoadUiSchemaUseCase` は UI スキーマ取得の操作境界を持ち、対象 surface の既定値
（`SurfaceId.DASHBOARD`）を所有します。ViewModel は surface を決めず、UseCase を呼びます。
認証は `SignInUseCase` / `SignOutUseCase` の2つに分け、
1つの UseCase が1つの操作を表す形をそろえます。
`SignOutUseCase` は `AuthRepository` のサインアウトと `UiSchemaRepository` の保持スキーマ破棄を調停します。

## Value Object

- Domain の意味を持つ値は、必要に応じて値オブジェクトで表す
- 値オブジェクトは生成時に制約を満たすようにし、不正な状態を後段へ流さない
- 現在の値オブジェクトは `@JvmInline value class` + private constructor + `companion object` の `invoke` で生成を制約している
- DTO へ変換するときは、data 層で mapper を使う

## Data

- 通信は Retrofit の API interface を境界とし、`suspend` 関数として公開する
- JSON 変換は kotlinx.serialization に寄せ、DTO は `data` 層に閉じる
- Repository 実装は失敗を `Result` として返し、例外を UI / domain へ素通ししない
- `CancellationException` は再送出し、広い例外捕捉で coroutine のキャンセルを握りつぶさない
- data 層の model と domain model がずれる場合は mapper を置く
- 通信の契約、ベース URL、応答の分類は [`docs/server-contract.md`](server-contract.md) を正本とする

## エラーの流れ

サーバー応答は次の経路で UI state へ変換します。層をまたぐ度に、扱う型を1段ずつドメイン寄りへ移します。

1. `data/remote`: Retrofit が `Response` を返し、契約に書かれたステータスを `UiSchemaResponse` へ分類する。
   契約外の HTTP エラー、復号失敗、component の入れ子の上限超過は例外
2. `data/repository`: 例外を捕捉して `Result.failure` に畳み、成功時は mapper で domain model へ変換して
   `UiSchemaResolution` へ解釈する
3. `domain/usecase`: 対象 surface を決めて操作を呼び、`Result` を握りつぶさずそのまま返す
4. `ui/viewmodel`: `Result` の成功 / 失敗と `UiSchemaResolution` を UI state へ変換する

この分担を崩さず、UI へ `Response`、例外型、DTO を直接渡しません。
