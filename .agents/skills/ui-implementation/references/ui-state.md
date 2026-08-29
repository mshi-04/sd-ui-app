# UI state

画面の表示に必要な情報だけを UI の言葉で表し、domain model をそのまま持ち込まない。

## 確認する対象

- `ui/state/` の対象 `XxxUiState` と、その構成要素になる sealed interface
- state を生成する ViewModel と、state を消費する Screen

## 実装時の注意

- 画面全体の state は既定値を持つ `data class` にし、追加時に既存の呼び出しを壊さない。
- 排他的な表示分岐は `sealed interface` + `data object` / `data class` で表し、boolean を複数並べない。
- domain の値オブジェクトは、UI へ渡す時点で表示に使う primitive へ落とす。
- 表示文言そのものは UI state に持たず、`strings.xml` の resource を Screen 側で解決する。
- UI state / result / event は、それぞれ `XxxUiState` / `XxxUiResult` / `XxxUiEvent` と命名する。

## テスト判断

- state の生成規則は ViewModel test で押さえる。state 単体の data class にテストは基本的に不要。

## 検証観点

- 表現できない状態（読み込み中かつ失敗など）を型で排除できているか。
- 分岐が増えたときに Screen 側の `when` が網羅されるか。
