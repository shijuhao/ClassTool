# 表盘页（Dial Page）设计说明 v2

> 原型图：
> - `design/prototype/dial-prototype.png`：圆形表盘 6 状态总览
> - `design/prototype/dial-square.png`：方形表盘（无环，卡片更宽、时间放大）
> - `design/prototype/dial-annotated.png`：圆形表盘元素标注
>
> 渲染脚本：`design/prototype/render_prototype.py`（改坐标/文案后可重新出图）

## 1. 位置与手势

现在主页是 2 页 pager（`MainScreen.kt` 里的 `HorizontalPager`）：

| 现在 | 改后 |
| --- | --- |
| 0 当前课程 | 0 当前课程 |
| 1 更多 | **1 表盘（新增）** |
| - | 2 更多 |

- 手势、转轮（rotary）、页码点全部沿用现有 `HorizontalPagerScaffold`，不改交互模型。
- 表盘页把「剩余时间」当主信息，所以**这一页隐藏顶部状态栏时间**（否则和中心大号时间重复）。做法：`WearApp()` 里加一个 `statusTimeVisible` 状态，`AppScaffold(timeText = { if (statusTimeVisible) TimeText() })`，表盘页通过 `pagerState.currentPage` 回调切换；离开页面时恢复。

## 2. 数据来源（全部复用，无新增持久化）

- `ScheduleDataStore(context).scheduleFlow` → `events` + `adjustments`
- `eventsOnDate(events, adjustments, todayDate)`：今天生效的事件（含调休/调课）
- `effectiveWeekdayOnDate` / `todayWeekday`：显示用的星期
- `findCurrentEventOnDate`：当前进行中的事件（含课间 `BREAK`）
- `eventDisplayNameFor(event, weekday)`、`eventColorFor(event, weekday)`、`parseColor`
- `currentSecondOfDay()` + 1s tick：剩余 `mm:ss`
- 课前 3 分钟预备铃逻辑（`PREP_BELL_SECONDS`）与 `MainScreen` 保持一致

## 3. 状态

| 状态 | 卡片 | 环 | 页脚 |
| --- | --- | --- | --- |
| 上课中 | 课程名 + 时间段 / 剩余 mm:ss | 当前课高亮 + 「现在」圆点 | 下一节 10:00 · 英语 |
| 预备铃（≤3 min 前） | 即将开始 + 课程名 / 倒数 mm:ss | 下一节弧段高亮 | 今日 5 节 · 已完成 1 节 |
| 课间 | 课间休息 + 下一节 / 距上课 mm:ss | 间隙里放「现在」空心点 | 今日 5 节 · 已完成 2 节 |
| 今天没有课 | 今天没有课程 + 周几 | 只剩底轨 + 刻度 | 明天 08:00 · 高等数学 |
| 紧急（沿用动态主题 `eventUrgent`） | 卡片与背景转红 / 剩余 mm:ss | 当前课已上部分变暗、剩余段用 `colorScheme.error` | 下一节 … |

> 方屏没有环：上表「环」一列在方屏上不出现，信息由卡片右侧的剩余时间承担，其余元素完全一致。

> 红色不新增规则：`event.urgent == true` 且剩余 ≤ 10 分钟时，App 的 `WearAppTheme` 已经把整套色板切成红色，表盘直接吃主题色即可。

## 4. 视觉规格（454 × 454 dp 基准）

**圆屏**

| 元素 | 规格 |
| --- | --- |
| 课程环 | 圆心屏中心，R = 186 dp，stroke = 12 dp，全部圆头 |
| 刻度 | 环外 6°（小）/30°（大）装饰刻度，不表示具体时间 |
| 卡片 | 246 × 86 dp，圆角 24 dp，色底 = 课程色 19% 混合背景 |
| 大号时间 | 92 sp，`HH:mm` |
| 日期 | 19 sp，`M月d日 周X` |
| 页脚 | 13 sp，前带 3 dp 色点 |
| 页码点 | y = 430 dp，中间点用课程色 |

**方屏：也画环，但环是「可调圆角的矩形环」，元素与圆屏一一对应、时间放大**

| 元素 | 规格 |
| --- | --- |
| 课程环 | 圆角矩形环：距屏边 6%×短边，描边 2.4%×短边，圆角 = 设置值（默认 40 dp，0–120 dp） |
| 课程卡片 | 内容区整宽，高 = 内容区 24%；标题 6.5%×内容宽 / 时间段 3.2% / 剩余 8.5% |
| 大号时间 | **34%×内容宽**（454 方屏上约 127 sp，比圆屏的 92 sp 大） |
| 日期 | 5.5%×内容宽 |
| 页脚 | 4.2%×内容宽 |
| 内容内缩 | `环内缩 + 描边/2 + 圆角×0.293`（0.293 = 1 − 1/√2），圆角越大内容越往里让 |

圆角设置在 **更多 → 设置 → 主题 → 表盘圆角（方屏）**，改完立刻生效（DataStore Flow → 重组），不需要重启。

> 环的路径参数化在 `RoundedRectRing` 里：从 12 点方向顺时针，line/arc 分段按弧长取点。当圆角 = 短边一半时，它退化成圆环 —— 所以方环和圆环共用同一套分段逻辑。

## 5. 课程环的映射

把一天的课表压成 360°：

1. 每节课按**时长**分弧；
2. 相邻课之间的课间也占弧，但**压缩到最多 12 分钟等价**（否则午休会把环切掉一大块，见下面的取舍）；
3. 每段左右各留 2.4° 间隙；
4. 透明度：已上过 24%、下一节 66%、其余未上 46%、当前课「已上」100%、「剩余」28%；
5. 「现在」位置 = 当前段内按分钟比例插值，画一个「拇指」圆点（底色挖空 + 课程色内芯）。

**取舍**：环是「今天的课程顺序」而不是「24 小时钟面」。时间线性映射更"准确"，但午休/空课会把环切得七零八落，视觉上不像表盘；按时长压缩后环始终完整、每段长度≈课时长度，反而更像"今天有几节课、上到哪了"。若你更想要"真实时间"的环，改 `build_segments()` 即可，采样代码里有注释。

## 6. 实现落地（v2 已写入代码，尚未编译）

> 环境没装好，本次**只改代码、不构建**。构建通过后再按真机截图微调比例。

| 文件 | 改动 |
| --- | --- |
| `ui/dial/DialScreen.kt`（新增，约 1040 行） | 表盘页：`DialScreen()` 入口 + `RoundDial()` / `SquareDial()` 两套布局；`drawDialRing()`（圆环，`drawArc`）、`drawSquareRing()` + `RoundedRectRing`（圆角矩形环，按弧长采样）；`buildDialState()` / `buildRing()` 纯计算 |
| `ui/main/MainScreen.kt` | pager 页数 2 → 3；新增 `DIAL_PAGE = 1`；接入表盘页并传入 `dialCornerRadius`；`KeepScreenOn` 覆盖到表盘页；通过 `onStatusTimeVisibleChange` 回调控制状态栏时间 |
| `MainActivity.kt` | 新增 `statusTimeVisible`（`MutableState`），`AppScaffold.timeText` 按状态条件渲染；把回调传给 `GreetingScreen` |
| `datastore/SettingsDataStore.kt` | 新增 `dial_corner_radius` 偏好（默认 40，范围 0–160 dp） |
| `ui/settings/ThemeScreen.kt` | 「主题」页新增滑块「表盘圆角（方屏）」，0–120 dp，步进 8 |
| `ui/settings/SettingsScreen.kt` | `SliderSettingCard` 从 `private` 改为 `internal`，供主题页复用 |

已落实的规格：

- **全部尺寸按屏宽/屏高比例计算**（不写死 dp），所以 454×454 圆屏、368×448 的 DW99 都能自适应；
  - 圆屏：环半径 ≈ 0.43×短边，描边 2.64%，装饰刻度 60 格（每 6°，每 30° 加长），卡片 54.5%×宽、18.5%×高，时间字号 20%×宽；
  - 方屏：圆角矩形环（内缩 6%、描边 2.4%、圆角来自设置），卡片整宽、高 24%×内容高，时间 34%×内容宽；
- 圆环与方环共用同一套「课程分段 + 压缩课间 + 现在标记」的分段数据（`buildRing()`），只有几何映射不同；
- 课程环分段、透明度、压缩课间、现在标记、预备铃、课间、无课、紧急（跟随动态主题）逻辑都已实现；
- 页脚「下一节 / 今日 N 节 · 已完成 M 节 / 今天没有安排」三态切换。

本次**未做**：表盘页开关设置项、无障碍 `contentDescription`、Roborazzi 截图测试。

## 7. 待确认

1. 环用「课程分段（压缩课间）」还是「24 小时 / 上课时间区间线性」？
2. 表盘页是否隐藏顶部状态栏时间？
3. 表盘页放在「当前课程」之后、「更多」之前，顺序 OK？
4. 课间是否需要单独显示 `BREAK` 类型事件的弧段（现在按空档处理）？
5. 方屏圆角默认 40 dp 合不合适？（范围 0–120 dp，可在主题页调；圆角拉到最大会变成圆环）
