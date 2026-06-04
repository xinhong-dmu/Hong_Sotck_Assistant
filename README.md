# 弘股助手 — Hong Stock Assistant

> A股智能交易助手 — 跟踪止盈止损监控 · AI 多维技术分析

---

## 功能特色

### 智能跟踪止盈止损引擎

基于 `TrailingStopEngine` 实现的智能交易监控：

- **硬止损线**：基于买入价和固定止损比例，跌破即触发止损
- **动态跟踪止盈**：防御线随买入以来最高价自动上移，锁定浮动利润
- **梯度回撤收紧**：随盈利增加自动收紧回撤容忍度（100% → 60% → 40% → 25% 基础比例，最低 1.5%）
  - 盈利 ≤ 10%：宽松容忍
  - 盈利 10%~20%：收紧至 60%
  - 盈利 20%~30%：收紧至 40%
  - 盈利 > 30%：收紧至 25%
- **里程碑提醒**：盈利突破 10% / 20% / 30% / 50% 时自动提示
- **回撤预警**：当前回撤达到容忍度 80% 时触发高危警告
- **目标止盈**：支持自定义目标止盈比例，到达即提醒
- **K线数据回溯**：自动获取买入以来的历史最高价，确保防御线准确

### AI 多维技术分析（DeepSeek）

- **7 维评分体系**：趋势判断、关键价位、技术信号、短线预测、操作建议、风险提示、止盈建议
- **自动数据采集**：获取K线数据 + 个股新闻 + 政策要闻，构建完整分析上下文
- **上下文感知**：包含买入价、买入日期、止损止盈参数，分析更精准
- **对话式追问**：支持多轮对话，持续深入分析
- **Markdown 渲染**：AI 回复支持 Markdown 格式展示

### 全市场股票搜索

- 内置 5000+ A股股票数据库（CSV 文件，支持模糊搜索 + 自动补全）
- 支持 ETF 过滤
- 搜索结果快速发起 AI 分析或添加监控

### 交易预设管理

- 保存/加载交易参数预设，快速切换监控配置
- 支持删除预设

### 灵活配置

- DeepSeek API Key 管理
- 模型切换：DeepSeek V4 Pro（`deepseek-chat`）/ DeepSeek Flash（`deepseek-reasoner`）
- 买入参数自定义：止损比例、目标止盈比例、跟踪比例、梯度模式开关

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
3. 点击「开始监控」— 引擎自动计算防御线和止损线
4. 实时查看：最高价、防御线、硬止损线、盈利%、回撤%、距防御线距离、有效跟踪比例
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
│   │   └── PriceRecord.java          # 价格监控记录
│   └── repository/
│       ├── StockRepository.java      # 股票搜索（CSV 资产文件）
│       ├── TradeRepository.java      # 交易记录持久化（CSV）
│       └── PriceHistoryRepository.java # 价格历史持久化（JSON）
├── domain/
│   └── TrailingStopEngine.java       # 核心：跟踪止盈止损引擎
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
