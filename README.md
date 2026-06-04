# 弘股助手 — Hong Stock Assistant

> A股智能交易助手 — 动态跟踪止盈止损 · AI 多维技术分析

---

## 交易策略

本系统基于 **动态跟踪止盈止损策略**，核心思想是：买入后不断上调防守线，让利润奔跑的同时控制回撤风险。

### 核心公式

| 指标 | 公式 | 说明 |
|---|---|---|
| **硬止损线** | `买入价 × (1 − 止损比例%)` | 保底防线，跌破无条件离场 |
| **防守线** | `max(买入以来最高价 × (1 − 有效跟踪比例%), 硬止损线)` | 动态上移，只升不降 |
| **有效跟踪比例** | `基础跟踪比例 × 梯度系数` | 盈利越大，容忍度越紧 |
| **账面盈利** | `(现价 − 买入价) / 买入价 × 100%` | 当前浮动盈亏 |
| **回撤幅度** | `(最高价 − 现价) / 最高价 × 100%` | 距最高点的回落幅度 |

### 触发规则

| 触发类型 | 条件 | 行为 | 图标 |
|---|---|---|---|
| **硬止损** | 现价 ≤ 硬止损线 | 立即离场，记录交易 | ⛔ |
| **动态止盈** | 现价 ≤ 防守线 | 锁定利润，离场 | 🛑 |
| **目标止盈** | 盈利% ≥ 目标止盈比例 | 提醒用户（继续监控） | 🎯 |
| **里程碑提醒** | 盈利突破 10% / 20% / 30% / 50% | 阶段提示 | 🏆 |
| **回撤高危预警** | 回撤 ≥ 有效跟踪比例 × 80% | 提前警告，关注盘面 | ⚠️ |

### 梯度回撤收紧

随盈利增长自动收紧回撤容忍度，防止大幅回吐利润：

| 盈利区间 | 收紧系数 | 效果 |
|---|---|---|
| ≤ 10% | 100%（不收紧） | 给股价充分波动空间 |
| 10% ~ 20% | 60% | 适度收紧 |
| 20% ~ 30% | 40% | 加速收紧 |
| > 30% | 25% | 严控回撤，最低容忍 1.5% |

> 例如：基础跟踪比例 8%，盈利 15% 时，有效跟踪 = 8% × 60% = 4.8%

---

## 智能交易监控

### 监控流程

```
输入参数 → 启动监控 → 自动回溯K线 → 实时轮询行情 → 逐笔判断触发 → 弹窗/记录
```

1. **参数输入**：输入股票代码、买入价、买入日期、止损比例、目标止盈、跟踪比例
2. **K线回溯**：自动拉取买入日至今的K线数据，计算准确的买入以来最高价作为策略起点
3. **实时监控**：每 3 秒轮询实时行情，逐笔调用策略引擎判断触发条件
4. **分级预警**：根据触发类型弹窗提醒或自动记录离场

### 监控面板

实时展示以下指标，一目了然：

- **买入以来最高价** — 防守线计算的基准
- **当前防守线** — 跌破即触发动态止盈（仅梯度模式显示）
- **绝对止损线** — 硬止损保底价格
- **当前账面收益** — 浮动盈亏百分比
- **距最高点回撤** — 当前回撤幅度
- **距防守线空间** — 现价到防守线的安全距离
- **有效回撤容忍** — 当前生效的跟踪比例
- **梯度收紧状态** — 显示是否启用及当前收紧系数

### 记录与追溯

- **价格记录**：每次轮询的价格快照存入 JSON，事后可完整回溯监控过程
- **交易记录**：离场时自动生成 CSV 记录（时间、股票、买卖价格、盈利%、离场原因）
- **流水日志**：所有监控事件追加到 TXT 日志，带精确时间戳
- **交易预设**：常用参数保存为预设，一键加载复用

### 状态恢复

应用退出或重启后，监控状态自动从 SharedPreferences 恢复，无需重新配置。

---

## AI 多维技术分析（DeepSeek）

- **7 维评分体系**：趋势判断、关键价位、技术信号、短线预测、操作建议、风险提示、止盈建议
- **自动数据采集**：获取K线数据 + 个股新闻 + 政策要闻，构建完整分析上下文
- **上下文感知**：包含买入价、买入日期、止损止盈参数，分析更精准
- **对话式追问**：支持多轮对话，持续深入分析
- **Markdown 渲染**：AI 回复支持 Markdown 格式展示

---

## 全市场股票搜索

- 内置 5000+ A股股票数据库（CSV 文件，支持模糊搜索 + 自动补全）
- 支持 ETF 过滤
- 搜索结果快速发起 AI 分析或添加监控

---

## 技术架构

### 技术栈

| 层级 | 技术选型 |
|---|---|
| 语言 | Java 11 |
| 架构 | MVVM（ViewModel + LiveData + Repository） |
| UI | XML 布局 · Material Design Components · FragmentTransaction 导航 |
| 网络 | OkHttp 4.12.0（DNS 缓存 + 指数退避重试 + 日志拦截器） |
| 序列化 | Gson 2.10.1 |
| Markdown | commonmark 0.21.0 |
| 数据持久化 | SharedPreferences（设置） · CSV（股票列表、交易记录） · JSON（价格历史） |
| 最低 SDK | Android 7.0 (API 24) |
| 目标 SDK | Android 16 (API 36) |

### 网络层优化

- **多源容错**：实时行情 新浪 → 腾讯 → 东方财富 三级降级；K线 腾讯 → 东方财富 两级降级
- **指数退避重试**：500/429/408 状态码及连接超时自动重试，最多 3 次
- **DNS 缓存**：LRU 策略缓存最多 64 条 DNS 记录，TTL 5 分钟
- **连接池**：8 连接 / 5 分钟保活
- **数据缓存**：K线 & 实时行情内存缓存（TTL 机制）
- **请求去重**：并发相同请求合并为单次网络调用

### 状态管理

- UI 状态通过不可变 POJO（Builder 模式）+ LiveData 驱动
- 所有交易监控状态持久化到 SharedPreferences，应用重启后自动恢复
- 退出交易自动记录到 CSV，保留完整交易历史

---

## 快速开始

### 前置要求

- Android Studio Hedgehog (2024.1.1+) 或更高版本
- JDK 11+
- Android SDK 36

### 构建

```bash
# 克隆项目
git clone https://github.com/your-username/stock-smartphone.git

# 命令行构建
./gradlew assembleDebug        # macOS / Linux
gradlew.bat assembleDebug      # Windows
```

### 配置 API Key

1. 前往 [DeepSeek 开放平台](https://platform.deepseek.com) 注册并获取 API Key
2. 打开 App → 设置页面 → 输入 API Key → 选择模型 → 保存

---

## 使用指南

### 1. 交易监控
1. 搜索并选择监控股票
2. 输入买入价、买入日期、止损比例、止盈目标等参数
3. 点击「开始监控」— 引擎自动计算防守线和止损线
4. 实时查看：最高价、防守线、硬止损线、盈利%、回撤%、距防守线距离、有效跟踪比例
5. 价格历史记录自动保存，交易离场后生成完整记录

### 2. AI 分析
1. 选择或搜索股票
2. 点击「开始分析」— 自动获取 K 线数据、个股新闻、政策要闻
3. 查看 AI 7 维评分结果
4. 通过聊天框追问技术细节

### 3. 搜索与管理
- 搜索页直接输入股票代码/名称/拼音首字母，实时模糊匹配
- 选中股票后快速发起 AI 分析或开始监控
- 首页管理交易预设，一键加载常用配置

---

## 项目结构

```
app/src/main/java/com/hong/xin/stock/
├── MainActivity.java                 # 主界面 · 底部导航
├── StockApplication.java             # Application 入口
├── data/
│   ├── api/
│   │   ├── EastMoneyApi.java         # 东方财富 API（实时行情/K线/搜索）
│   │   ├── TencentApi.java           # 腾讯证券 API（备用数据源）
│   │   ├── DeepSeekApi.java          # DeepSeek AI 对话接口
│   │   ├── StockNewsApi.java         # 个股新闻 & 政策要闻
│   │   ├── StockDataCache.java       # 内存缓存（K线/行情 TTL）
│   │   └── HttpClientFactory.java    # OkHttp 工厂（重试/DNS缓存/日志）
│   ├── model/
│   │   ├── Stock.java                # 股票实体
│   │   ├── RealtimeQuote.java        # 实时行情（Builder 模式）
│   │   ├── KlineData.java            # 日K线数据
│   │   ├── TradeRecord.java          # 交易离场记录
│   │   ├── TradePreset.java          # 交易预设
│   │   └── PriceRecord.java          # 价格监控快照
│   └── repository/
│       ├── StockRepository.java      # 股票搜索（CSV 资产文件）
│       ├── TradeRepository.java      # 交易记录持久化（CSV）
│       └── PriceHistoryRepository.java # 价格历史持久化（JSON）
├── domain/
│   └── TrailingStopEngine.java       # ★ 核心：跟踪止盈止损策略引擎
├── ui/
│   ├── home/
│   │   └── HomeFragment.java         # 首页 · 预设管理
│   ├── trade/
│   │   ├── TradeFragment.java        # 交易监控面板
│   │   ├── TradeViewModel.java       # 交易逻辑（MVVM）
│   │   ├── TradeUiState.java         # UI 状态
│   │   ├── HistoryFragment.java      # 交易历史
│   │   ├── HistoryAdapter.java       # 历史记录适配器
│   │   └── PriceHistoryAdapter.java  # 价格历史适配器
│   ├── analysis/
│   │   ├── AnalysisActivity.java     # AI 分析页面
│   │   ├── AnalysisInputActivity.java # 分析输入页面
│   │   ├── AnalysisFragment.java     # AI 分析对话页面
│   │   ├── AiAnalysisViewModel.java  # AI 分析逻辑
│   │   └── AiAnalysisState.java      # 分析状态
│   ├── search/
│   │   ├── StockSearchFragment.java  # 股票搜索页面
│   │   ├── StockSearchViewModel.java # 搜索逻辑
│   │   └── StockSearchUiState.java   # 搜索状态
│   ├── settings/
│   │   └── SettingsFragment.java     # 设置页面
│   └── widget/
│       └── StockFilterAdapter.java   # 股票自动补全适配器
└── util/
    ├── SettingsManager.java          # SharedPreferences 封装
    └── DebugLogger.java              # 文件调试日志
```

---

## 许可

本项目基于 MIT 许可证开源，详见 [LICENSE](LICENSE) 文件。

---

## 免责声明

本工具仅供学习和个人参考，不构成任何投资建议。股票交易有风险，投资需谨慎。使用本工具产生的任何交易决策及结果由使用者自行承担。
