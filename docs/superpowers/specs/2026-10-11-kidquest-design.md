# KidQuest — Design Spec

- **Date:** 2026-10-11
- **Author:** Gekkotron
- **Status:** Draft — awaiting review

## 1. Goal

A standalone family app (Android + iOS, Kotlin Multiplatform) where a parent awards and removes points for children's chores and behaviour, and children save toward goals — solo or shared — that are celebrated when reached and redeemed by the parent.

### Success criteria

- A parent can award points to a child in **one tap** (quick buttons) and undo a mis-tap within seconds.
- Children see their balance, level, streak and goal progress at a glance on the home screen.
- A goal's `cost` is **per child**. A shared goal is reached only when every member has at least `cost` points, and on redeem each member pays the full `cost`. Solo goals behave the same way with one member.
- Same app, same behaviour on Android and iOS from one codebase.

### Constraints & decisions

| Decision | Choice |
|---|---|
| Distribution | Standalone, single device, local data only. No backend, no accounts. |
| Operator | The parent operates the device. No PIN, no parent/child modes. |
| Stack | Compose Multiplatform (shared UI), Room KMP, Koin, kotlinx-datetime, Navigation (Compose Multiplatform navigation). |
| Languages | French and English via Compose resources; defaults to system language. |
| Attribution | `Gekkotron` everywhere. Package: `com.gekkotron.kidquest`. |

### Out of scope (v1)

Sync/backend, multiple devices, parent PIN, notifications/reminders, photo proof, recurring chore schedules, real-money allowance, store release.

## 2. Data model (Room)

| Entity | Fields | Notes |
|---|---|---|
| `Child` | `id`, `name`, `avatar` (emoji), `color` (ARGB int), `createdAt`, `archived` | Archived, never deleted, so history stays intact. Archived children are hidden from Home and goal member pickers. |
| `PointAction` | `id`, `label`, `points` (non-zero int, ±), `icon` (emoji), `sortOrder` | Configurable action list. |
| `QuickAmount` | `id`, `points` (non-zero int, ±), `sortOrder` | Global, shown on every child card. |
| `PointEvent` | `id`, `childId`, `delta`, `type`, `actionId?`, `goalId?`, `label`, `timestamp` (epoch ms), `undone` | **Source of truth for all point math.** `type ∈ {AWARD, PENALTY, REDEEM}`. AWARD has `delta > 0`, PENALTY `delta < 0`, REDEEM `delta ≤ 0` with `goalId` set. |
| `Goal` | `id`, `title`, `icon` (emoji), `cost` (> 0, points required from **each** member), `status`, `createdAt`, `achievedAt?`, `redeemedAt?` | `status ∈ {ACTIVE, ACHIEVED, REDEEMED}`. |
| `GoalMember` | `goalId`, `childId` (composite PK) | 1 member = solo goal, 2+ = shared goal. |

Room schema is exported (`room.schemaDirectory`) from version 1 so future migrations can be tested.

## 3. Derived values (never stored)

All computed by pure functions in `shared/domain`, from non-undone `PointEvent`s.

- **Balance(child)** = Σ `delta`. May be negative.
- **LifetimeEarned(child)** = Σ `delta` where `type = AWARD`. Never decreases on penalty or redeem.
- **Level(lifetime)**: level 1 starts at 0. Threshold to reach level *n* (n ≥ 2) = `floor(100 · (n − 1)^1.5)` → L2 = 100, L3 = 282, L4 = 519, L5 = 800, … The level is the highest *n* whose threshold ≤ lifetime. Level progress = (lifetime − threshold(n)) / (threshold(n+1) − threshold(n)).
- **Streak(child, today)**: the set of local dates (device time zone) that have at least one AWARD event. If neither today nor yesterday is in the set, the streak is 0. Otherwise it is the number of consecutive days ending on the most recent of today/yesterday that is in the set.
- **MemberProgress(goal, child)** = `max(0, Balance(child)) / cost`, clamped to [0, 1].
- **GoalProgress(goal)** = the mean of the members' MemberProgress, for display. It is 1 only when every member has reached the cost.
- **Reached(goal)** = the goal has members and every member has `Balance ≥ cost`. One member below the cost (including a negative balance) blocks the goal. Balances count toward **every** goal the child belongs to; nothing is reserved until redemption.

## 4. Goal lifecycle

```
ACTIVE ──reached──▶ ACHIEVED ──redeem──▶ REDEEMED
   ▲                        │  ▲                  │
   └──not reached ──────────┘  └──undo redeem─────┘
```

- **GoalStatusUpdater** runs after every write that creates or undoes a `PointEvent`. It recomputes progress for all ACTIVE and ACHIEVED goals that contain an affected child and flips status in both directions (sets or clears `achievedAt`).
- The ACTIVE → ACHIEVED transition emits `AppEvent.GoalAchieved(goalId)`. A level increase caused by an award emits `AppEvent.LevelUp(childId, newLevel)`. Both go on an app-wide `SharedFlow` that the UI turns into the celebration overlay. Reverse transitions emit nothing.
- **Redeem(goal)** is allowed only when `status = ACHIEVED`. In one transaction: insert one REDEEM event per member with `delta = −cost`, set `status = REDEEMED` and `redeemedAt`, then run GoalStatusUpdater for those members so their other ACHIEVED goals can drop back to ACTIVE. A member's balance may go negative because of a redeem.
- **Redeem confirmation:** a simple dialog listing each member with `−cost` and their resulting balance, plus Confirm and Cancel. There is no split to edit.
- **Startup re-evaluation:** status only changes after writes, so on app start `GoalRepository.reevaluateOpenGoals()` recomputes every ACTIVE and ACHIEVED goal silently (no `GoalAchieved` events). A goal that was ACHIEVED under the old pooled rule drops back to ACTIVE if some member is below the cost.
- **Restart** (offered after redeem): creates a new ACTIVE goal copying title, icon, cost and members.
- **Undo of a REDEEM event** undoes **all** REDEEM events with that `goalId` in one transaction and sets the goal back to ACHIEVED (clearing `redeemedAt`), then runs GoalStatusUpdater.

## 5. Undo

- Undo sets `undone = true`. Events are never deleted.
- After a quick-button or action award, the Home screen shows a snackbar: "+20 → Léa · Undo" (about 5 s).
- Any event can also be undone or restored later from the Child detail history. Undone entries are shown struck through and can be re-applied (`undone = false`).

## 6. Screens

Bottom navigation: **Home · Goals · Settings**.

1. **Home**
   - **Active goals strip** at the top: horizontal cards with icon, title, overall progress bar and, for shared goals, each member's avatar with their own small progress bar and `balance/cost`.
   - **One card per child** (non-archived, ordered by `createdAt`):
     - avatar, name and colour;
     - large balance, shown in red when negative;
     - level badge, 🔥 streak, and a mini progress bar for that child's closest active goal (highest progress < 1, else any achieved one);
     - a row of **QuickAmount** buttons (one tap applies it);
     - a **"More"** button that opens a bottom sheet listing the **PointActions** (one tap applies it).
   - Tapping the card body opens **Child detail**.
   - **Empty state:** "Add your first child" leads to the add-child form.
2. **Child detail**
   - Balance, lifetime earned, level with progress bar, and streak.
   - History grouped by local day, newest first, with undo/restore per entry.
3. **Goals**
   - Tabs: Active / Achieved / Redeemed. A FAB opens **Create goal**.
   - Goal form: title, icon, cost (labelled "Points per child" when 2+ members are selected), and member children (multi-select, at least 1).
   - Editing members or cost re-runs GoalStatusUpdater for that goal.
   - Achieved goals show **Redeem**, which opens the confirmation dialog, then confirm, then the option to restart.
4. **Celebration overlay**
   - Full-screen confetti with "🎉 Goal reached: <title>!" for GoalAchieved.
   - A smaller banner for LevelUp.
   - Tap to dismiss.
5. **Settings**
   - **Children:** add/edit name, avatar emoji and colour; archive or unarchive.
   - **Point actions:** CRUD and reordering.
   - **Quick buttons:** CRUD and reordering.
   - **Language:** System / Français / English.

## 7. Architecture

```
composeApp/                 Compose Multiplatform app (androidMain, iosMain, commonMain)
  commonMain/ui/            screens, components, theme, ViewModels, navigation
  commonMain/di/            Koin modules
shared/                     KMP library, no Compose
  commonMain/domain/        models + pure calculators
  commonMain/data/          Room DB, entities, DAOs, repositories
  androidMain|iosMain/      expect/actual: RoomDatabase.Builder, Clock/TimeZone providers
iosApp/                     Xcode project hosting ComposeUIViewController
```

- **Domain:** `LevelCalculator`, `StreakCalculator`, `GoalProgressCalculator`. Pure, deterministic, and with time zone and "today" injected.
- **Data:**
  - `ChildRepository`;
  - `PointRepository` (`apply(childId, delta, label, actionId?)` — type is AWARD if `delta > 0`, PENALTY if `delta < 0`, and `delta = 0` is rejected; `undo(eventId)`, `restore(eventId)`, `history(childId): Flow`);
  - `GoalRepository` (`create`, `update`, `redeem(goalId)`, `reevaluateOpenGoals()`, `restart(goalId)`, `observe…`);
  - `SettingsRepository` (actions, quick amounts, language).
  - DAOs expose `Flow`, and multi-row writes are `@Transaction`.
- **GoalStatusUpdater:** called by `PointRepository` and `GoalRepository` after writes. It owns the `SharedFlow<AppEvent>`.
- **UI:** one ViewModel per screen exposing `StateFlow<UiState>` and intent functions. One-shot effects (snackbar with undo, navigation) go through a `Channel`.
- **Seed (first launch, in a Room `onCreate` callback):**
  - Actions: Wipe the table +50 · Put shoes away +25 · Vacuum +100 · Load/unload dishwasher +100 · Brush teeth without asking +20.
  - Quick amounts: +10, +20, +100, −10.
  - No children.
- **Validation (ViewModels):**
  - child name non-empty;
  - action label non-empty with non-zero points;
  - goal title non-empty with cost > 0 and at least 1 member.

## 8. Testing

- **Domain (`commonTest`, kotlin.test, written TDD):**
  - level thresholds at exact boundaries;
  - streaks that are empty, today-only, yesterday-only, have a gap, or cross midnight/DST;
  - member progress with negative balances clamped to 0;
  - mean progress for shared goals;
  - reached only when every member has at least the cost, and one negative member blocks the goal;
  - solo goals unchanged.
- **Repositories (in-memory Room, Android/JVM host tests):**
  - apply → balance;
  - undo/restore;
  - redeem charges every member the full cost, atomically, with the resulting status;
  - redeeming drops members' other ACHIEVED goals back to ACTIVE;
  - startup re-evaluation flips a pooled-only ACHIEVED goal back to ACTIVE with no event;
  - undoing a redeem → ACHIEVED;
  - status flips ACTIVE↔ACHIEVED;
  - GoalAchieved/LevelUp emitted only on forward transitions.
- **ViewModels (fakes + Turbine):**
  - a quick award emits the undo snackbar effect;
  - reaching a goal emits the celebration;
  - the redeem confirmation shows `−cost` for each member;
  - the cost field reads "Points per child" when 2+ members are selected.
- **Compose UI test (critical path):** add child → tap +20 → balance 20 → Undo → 0.
- **iOS:** shared tests run on `iosSimulatorArm64Test`, plus a manual simulator smoke test.
- **CI:** GitHub Actions on a macOS runner runs the Gradle build and all tests.
