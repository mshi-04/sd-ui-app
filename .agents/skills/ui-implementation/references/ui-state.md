# UI state

画面の表示に必要な情報だけを UI の言葉で表し、domain model をそのまま持ち込まない。

## 確認する対象

- `ui/state/` の対象 `XxxUiState` と、その状態を表す `XxxUiStatus`
- `ui/model/` の表示用モデル `XxxUiModel`
- state を生成する ViewModel と、state を消費する Screen

## 実装時の注意

- 画面全体の state は既定値を持つフラットな `data class` にし、追加時に既存の呼び出しを壊さない。
- 排他的な状態は `XxxUiStatus` enum の 1 フィールドで表し、boolean を複数並べない。
- status 以外のフィールドは、その値を持つ status のときだけ意味を持つ。status が変わるときは `XxxUiState(...)` を作り直し、使わないフィールドを既定値へ戻す。
- 一覧の要素など、画面 state の部品になる表示用モデルは `ui/model/XxxUiModel` に置く。
- `XxxUiState` / `XxxUiModel` は ViewModel でコンストラクタを呼んで直接組み立てる。`companion.fromDomain` や変換用の拡張関数（`toUiState()` / `toUiModel()`）は作らない。木構造のように再帰が要るときだけ、組み立て用の private 関数を ViewModel に置く。
- domain の値オブジェクトは、UI へ渡す時点で表示に使う primitive へ落とす。
- 表示文言そのものは UI state に持たず、`strings.xml` の resource を Screen 側で解決する。
- UI state / status / model / result / event は、それぞれ `XxxUiState` / `XxxUiStatus` / `XxxUiModel` / `XxxUiResult` / `XxxUiEvent` と命名する。

## テスト判断

- state の生成規則は ViewModel test で押さえる。state 単体の data class にテストは基本的に不要。

## 検証観点

- status が変わるときに、前の status のデータが state に残っていないか。
- status が増えたときに Screen 側の `when` が網羅されるか。
