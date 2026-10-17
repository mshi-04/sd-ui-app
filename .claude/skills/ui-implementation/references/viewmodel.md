# viewmodel

画面ごとの ViewModel で UI state を公開し、UseCase または domain の抽象へ依存する。

## 確認する対象

- `ui/viewmodel/` の対象 ViewModel と、公開する `XxxUiState`（StateFlow）
- 使用する UseCase / domain abstraction と、その戻り値（`Result` を含む）

## 実装時の注意

- UI state は `MutableStateFlow` を private に持ち、`asStateFlow()` で公開する。
- 必要な UseCase または domain の Repository interface だけに依存し、data 層の具象型を参照しない。
- Android View / Context 依存を ViewModel に直接持ち込まない。
- 実行中の再入は、UI state の値を根拠に早期リターンで防ぐ。
- `Result` の成功 / 失敗と domain の解釈結果を、UI state の型へ1箇所で変換する。
- 失ってはいけない処理結果は UI state に保持して確認済み callback で消費し、失われても影響しない通知だけ Channel event に流す。
- ViewModel 固有の補助型は `ui/viewmodel` の所有者付近へ置き、`event` / `data` パッケージへ分散させない。

## テスト判断

- 状態遷移・分岐・再入防止を ViewModel test で押さえる。`runTest` と `StandardTestDispatcher` を使い、`Dispatchers.setMain` / `resetMain` で `viewModelScope` を制御する。

## 検証観点

- 初期値・再収集で状態が破綻しないか。
- domain 操作の失敗時に UI state が適切に反映されるか。
- 連続操作で重複リクエストが発生しないか。
