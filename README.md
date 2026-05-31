# 弘股助手 — Hong Stock Assistant

> A股智能交易助手 — 跟踪止盈止损监控 · AI 多维技术分析

---

## 功能特色

### 📊 智能跟踪止盈止损引擎
- **硬止损线**：基于买入价和固定止损比例
- **动态跟踪止盈**：防御线随最高价自动上移，锁定利润
- **梯度回撤收紧**：随盈利增加自动收紧回撤容忍度（10% → 6% → 4% → 2.5%）
- **里程碑提醒**：10% / 20% / 30% / 50% 盈利节点
- **回撤预警**：达到容忍度 80% 时发出告警
- **实时行情**：通过东方财富 API 获取实时价格

### 🤖 AI 多维技术分析（DeepSeek）
- **7 维评分体系**：趋势判断、关键价位、技术信号、短线预测、操作建议、风险提示、止盈建议
- **K 线数据**：自动获取区间涨跌、最高最低、近 10 日明细
- **关联新闻**：整合个股新闻 + 政策要闻
- **对话式追问**：支持多轮对话，持续深入分析

### 🔍 全市场股票搜索
- 内置 5000+ A股股票数据库
- 模糊搜索 + 自动补全
- 快速发起 AI 分析

### ⚙️ 灵活配置
- DeepSeek API Key 管理
- 模型切换：DeepSeek V4 Pro / DeepSeek Flash
- 买入参数自定义

---

## 技术栈

| 层级 | 技术选型 |
|---|---|
| 语言 | Java 11 |
| 架构 | MVVM（ViewModel + LiveData + Repository） |
| UI | XML 布局 · Material Design Components |
| 导航 | BottomNavigationView + FragmentTransaction |
| 网络 | OkHttp 4.12.0 |
| 序列化 | Gson 2.10.1 |
| 数据持久化 | SharedPreferences · CSV · JSON |
| 行情 API | 东方财富 |
| AI API | DeepSeek（兼容 OpenAI 格式） |
| 最低 SDK | Android 7.0 (API 24) |
| 目标 SDK | Android 16 (API 36) |

---

## 快速开始

### 前置要求
- Android Studio Hedgehog (2024.1.1+) 或更高版本
- JDK 11+
- Android SDK 36

### 构建运行

```bash
# 克隆项目
git clone https://github.com/your-username/stock-smartphone.git

# 使用 Android Studio 打开项目根目录
# 等待 Gradle Sync 完成

# 或使用命令行构建
./gradlew assembleDebug
```

### 配置 API Key

首次使用需在 **设置** 页面配置 DeepSeek API Key：

1. 前往 [DeepSeek 开放平台] 注册并获取 API Key
2. 在 App 设置页输入 API Key
3. 选择模型（推荐Deepseek V4 Flash）

---

## 使用指南

### 1. 交易监控
1. 搜索并选择监控股票
2. 输入买入价、买入日期、止损比例、止盈目标等参数
3. 点击「开始监控」— 引擎自动计算防御线和止盈止损线
4. 实时查看：买入以来最高价、防御线、硬止损线、盈利%、回撤%、距防御线距离、有效跟踪比例
5. 查看历史记录和价格变动明细

### 2. AI 分析
1. 选择或搜索股票
2. 点击「开始分析」— 自动获取 K 线数据和关联新闻
3. 查看 7 维评分结果
4. 通过聊天框追问技术细节

### 3. 快速搜索
- 在搜索页直接输入股票代码/名称
- 选中后立即发起 AI 分析或添加监控

---

## 项目结构

```
app/src/main/java/com/hong/xin/stock/
├── MainActivity.java              # 主界面 — 底部导航
├── StockApplication.java          # Application 入口
├── data/
│   ├── api/                       # EastMoney & DeepSeek API
│   ├── model/                     # 数据模型（Stock, Quote, Kline...）
│   └── repository/                # 数据仓库
├── domain/
│   └── TrailingStopEngine.java    # 核心：跟踪止盈止损引擎
├── ui/
│   ├── trade/                     # 交易监控（监控面板 + 历史记录）
│   ├── analysis/                  # AI 分析（7维评分 + 对话）
│   ├── search/                    # 股票搜索 + 快速分析
│   ├── settings/                  # 设置（API Key, 模型选择）
│   └── widget/                    # 自定义组件
└── util/                          # 工具类
```

---

## 截图

| 交易监控 | AI 分析 | 股票搜索 |
|---|---|---|
| *(截图待补充)* | *(截图待补充)* | *(截图待补充)* |

---

## 路线图

- [ ] 多股票同时监控
- [ ] 推送通知（回撤预警、里程碑到达）
- [ ] 持仓组合管理
- [ ] 历史回测
- [ ] 日/周 K 线图表展示
- [ ] 深股通/港股通支持

---

## 许可

详见 [LICENSE](LICENSE) 文件。

---

## 免责声明

本工具仅供学习和个人参考，不构成任何投资建议。股票交易有风险，投资需谨慎。
