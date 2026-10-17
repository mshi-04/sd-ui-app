---
name: implementation-review
description: 実装差分をレビューする依頼で使う。SD UI の依存方向、責務分離、過剰 API、サーバー契約の扱い、UI state、Unit Test 不足を確認し、docs/review.md の形式で指摘する。
---

# 目的

SD UI の差分が既存方針に沿っているかを確認し、マージ前に直すべき問題を具体的に洗い出す。

# 最初に確認するもの

- [`docs/review.md`](../../../docs/review.md)
- [`docs/architecture.md`](../../../docs/architecture.md)
- [`docs/server-contract.md`](../../../docs/server-contract.md)
- [`docs/implementation-guidelines.md`](../../../docs/implementation-guidelines.md)
- [`docs/unit-test.md`](../../../docs/unit-test.md)
- 差分と関連コード

# 手順

1. 比較基準を確定する（判断手順は `docs/review.md` の「比較範囲」を正本とする）。
2. 差分の目的、変更範囲、既存実装との関係を確認する。
3. `ui -> domain <- data` の依存方向が崩れていないか見る。
4. ViewModel、UseCase、Repository interface、Repository 実装、mapper、Retrofit API の責務が混ざっていないか見る。
5. 通信の失敗、欠損フィールド、HTTP エラーが UI state へ正しく分類されているか確認する。
6. 新規 API、抽象化、画面構成が本当に必要か確認する。
7. 変更した振る舞いに対するテストがあるか、テスト観点が妥当か確認する。
8. 指摘の分類、形式、終了時の報告は `docs/review.md` を正本とする。

# 注意事項

- 好みではなく、バグ、依存方向、責務分離、保守性、テスト不足を優先する。
- テストコードだけの実装・修正は `test-implementation` を優先する。
- 問題がない場合も、残るリスクや未検証項目を明記する。
- サーバー契約の変更がある場合は `docs/server-contract.md` の更新有無を必ず確認する。
- `CancellationException` の再送出と、広すぎる例外捕捉を確認する。
- UI 変更がある場合は strings、Preview、ライト / ダーク、表示分岐の網羅を確認する。
