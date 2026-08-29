# AGENTS.md

SD UI で Codex が最初に読む入口です。
ここには地図だけを書き、詳細は `docs/` 配下の文書へ分けます。

## 技術文書

- [`docs/project-overview.md`](docs/project-overview.md): プロジェクト概要 / 主要な確認先 / 技術スタック / 未導入のもの
- [`docs/architecture.md`](docs/architecture.md): Clean Architecture / MVVM の構造方針 / エラーの流れ
- [`docs/server-contract.md`](docs/server-contract.md): `GET /ui/{surface}` の契約 / ベース URL / 応答の分類と変換経路
- [`docs/implementation-guidelines.md`](docs/implementation-guidelines.md): 実装時の基本方針
- [`docs/development-setup.md`](docs/development-setup.md): 開発環境の前提 / ローカル検証コマンド
- [`docs/unit-test.md`](docs/unit-test.md): Unit Test の方針
- [`docs/review.md`](docs/review.md): コードレビューの形式

## AI作業用 Skill

作業の種類に応じて、必要な Skill を最初に確認します。
Codex 向け skill は `.agents/skills/` で管理する。Claude Code 向けの `.claude/skills/` とは自動同期しない。

- `ui-implementation`: Compose / Route / ViewModel / UI state / 画面テスト
  - [`.agents/skills/ui-implementation/SKILL.md`](.agents/skills/ui-implementation/SKILL.md)
- `domain-implementation`: model / value object / UseCase / Repository interface
  - [`.agents/skills/domain-implementation/SKILL.md`](.agents/skills/domain-implementation/SKILL.md)
- `data-implementation`: Retrofit API / DTO / mapper / Repository 実装 / Hilt module
  - [`.agents/skills/data-implementation/SKILL.md`](.agents/skills/data-implementation/SKILL.md)
- `test-implementation`: Unit Test / androidTest / coroutine / Flow 検証
  - [`.agents/skills/test-implementation/SKILL.md`](.agents/skills/test-implementation/SKILL.md)
- `implementation-review`: 実装後レビューと指摘整理
  - [`.agents/skills/implementation-review/SKILL.md`](.agents/skills/implementation-review/SKILL.md)

## コーディング規約

実装前に、対象へ該当する規約を確認します。

- [`.claude/rules/comments.md`](.claude/rules/comments.md): コメントの方針（本番コードは読み取れない制約のみ / テストは AAA ラベルと検証観点）

## 最低限の前提

- SD UI はサーバーと接続する Android クライアント
- パッケージ名は `com.example.sdui`
- メインモジュールは `:app`
- 単一モジュール内で `ui` / `domain` / `data` のパッケージ境界を保つ
- UI は Kotlin / Jetpack Compose / Material 3 を軸にする
- 構造は Clean Architecture + MVVM を軸にする
- サーバー契約の正本は `sd-ui-infra` の `docs/client_integration.md` で、受け取り方は `docs/server-contract.md` に書く
- 未導入の技術は、実装済みとして扱わない（ビルドフレーバー、Navigation Compose、ローカル DB、静的解析、CI は未導入）
