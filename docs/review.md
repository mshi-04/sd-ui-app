# Review

この文書は SD UI でコードレビューを行うときの形式をまとめます。

## 比較範囲

- ユーザーが base branch、commit、merge-base、対象ファイルを指定した場合は、その比較基準を使う
- 比較基準が未指定の場合は、現在の branch、追跡先、PR 情報から妥当な base を確認してから差分を読む
- 対象を限定したレビューでは、確認した範囲と確認していない範囲を分けて報告する

## 指摘の分類

指摘は以下の3段階で分類します。

| ラベル | 意味 |
|--------|------|
| **Critical** | マージ前に必ず直す。再現可能なバグ、セキュリティ・データ損失、ビルド不能、重大な依存方向の破壊など |
| **Suggestion** | 直すことを推奨。品質・保守性・一貫性に関わるが、マージをブロックするかは判断による |
| **Nitpick** | 好みや細部の改善。マージをブロックしない |

Clean Architecture の方針との差異は、依存方向、責務、変更容易性への具体的な影響を確認して分類します。
方針から外れているという理由だけで、自動的に Critical にはしません。

## 指摘の形式

- 指摘には上から通し番号を振る
- 各指摘に `[Critical]` / `[Suggestion]` / `[Nitpick]` のラベルを先頭に付ける
- 各指摘に対象ファイルと行番号、問題による影響、問題である理由、修正方針を含める
- 重要度が高い順に並べる

例:

```markdown
1. [Critical] `app/src/main/java/.../DefaultExampleRepository.kt:20` — `CancellationException` を握りつぶすため、画面破棄後も処理が続き UI state が不整合になる。キャンセル例外を再送出して他の例外だけ `Result.failure` に畳む。
2. [Suggestion] `app/src/main/java/.../ExampleViewModel.kt:35` — ViewModel が DTO を直接参照し、通信仕様の変更が UI へ波及する。domain model へ変換してから UI state を組み立てる。
3. [Nitpick] `app/src/main/java/.../ExampleScreen.kt:42` — 変数名 `s` から用途を判断できない。`statusText` など意図を表す名前を検討する。
```

## 重点的に見る観点

- `ui -> domain <- data` の依存方向が崩れていないか
- ViewModel、UseCase、Repository interface、Repository 実装、mapper の責務が混ざっていないか
- 通信の失敗・欠損フィールド・HTTP エラーが UI state へ正しく分類されているか（[`docs/server-contract.md`](server-contract.md)）
- 値オブジェクトの制約を迂回した値が domain へ流れていないか
- `CancellationException` の再送出と、広すぎる例外捕捉がないか
- 変更した振る舞いに対するテストがあるか、テスト観点が妥当か
- 表示文字列が `strings.xml` に寄せられているか、ライト / ダークで破綻しないか

## 終了時の報告

指摘の有無にかかわらず、最後に次を報告します。

- Critical / Suggestion / Nitpick の全件数
- レビューした比較基準、ファイル、機能などの範囲
- 実行したテスト、ビルド、目視確認などの検証
- 未確認事項と残るリスク
- 確認範囲に応じたマージ可否

差分全体を確認した場合だけ、全体のマージ可否を示します。
対象限定レビューや静的レビューでは、確認範囲内の判断を示したうえで「PR 全体のマージ可否は未評価」としてよいものとします。

例:

```markdown
Critical: 0件、Suggestion: 1件、Nitpick: 0件。
確認範囲: `ui/viewmodel` の差分と対応する Unit Test。
検証: `:app:testDebugUnitTest` 成功。instrumented test は未実施。
残るリスク: Compose UI の実機上の表示と操作は未確認。
マージ可否: 確認範囲内にブロッカーなし。PR 全体のマージ可否は未評価。
```
