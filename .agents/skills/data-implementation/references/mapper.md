# mapper

DTO から domain model への変換責務を mapper に閉じる。

## 確認する対象

- `data/mapper/` の既存 mapper（`ServerStatusMapper`）
- 変換元の DTO と、変換先の domain model / value object

## 実装時の注意

- domain model の不変条件を壊す値を流さない（value object の生成制約を尊重する）。
- 変換ロジックを Repository 実装や remote data source に散らさず mapper に集約する。
- null / 空白のみ / 欠損フィールドの扱いを明示し、意味のない値は `null` へ落とす。
- 拡張関数として書く場合も、公開範囲を必要最小限にする。

## テスト判断

- 代表値・境界（空白のみ、`null`、前後の空白）を含む変換を JVM Unit Test で押さえる。

## 検証観点

- DTO のフィールド追加・変更時に変換漏れがないか。
- 値オブジェクトの生成失敗が、意図した層で扱われているか。
