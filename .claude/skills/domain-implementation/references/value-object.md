# value object

Domain の意味を持つ値を、不正状態を作れない型に閉じる。

## 確認する対象

- `domain/model/value/` の既存 value object（`SurfaceId`、`UiComponentId`）
- その値を受け渡す model / UseCase と、data 層の mapper 変換

## 実装時の注意

- 既存の書き方にそろえ、`@JvmInline value class` + private constructor + `companion object` の `invoke` で生成を制約する。
- 生成時に `require` で制約を検証し、不正な値を後段へ流さない。
- trim など正規化を行う場合は、生成時に1箇所で済ませる。
- 意味のある値は primitive のまま UI / data へ広げず、value object に寄せる。
- 制約違反の例外メッセージを UI 表示文言と混ぜない。

## テスト判断

- 生成の成功・境界（空白のみ、前後の空白）・失敗を JVM Unit Test で押さえる。
- primitive との往復変換が必要な場合、mapper 側のテストは data 層で扱う。

## 検証観点

- 不正値で確実に生成失敗するか。
- 既存の等価な value object と役割が重複していないか。
- 値オブジェクト化した結果、data 層で欠損値の扱いが必要になっていないか。
