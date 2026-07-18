# 弘股助手 — Android 股票助手应用

## 功能架构

```
com.hong.xin.stock/
├── MainActivity.java              # 主页：自选股列表
├── StockDetailActivity.java       # 个股详情：实时行情 + K线/分时图 + 盈亏计算
├── StockSearchActivity.java       # 股票/ETF搜索
├── DeepSeekChatActivity.java      # AI智能分析 (DeepSeek)
├── StrategyListActivity.java      # 交易策略管理
├── MinuteChartView.java           # 自定义分时图控件
├── KlineChartView.java            # 自定义K线图控件
├── StrategyAlarmScheduler.java    # 策略告警定时任务
├── StrategyCheckReceiver.java     # 策略条件检测广播接收器
├── StrategyNotificationHelper.java # 策略触发推送通知
├── ChatAdapter.java               # 聊天消息适配器
├── SearchResultAdapter.java       # 搜索结果适配器
├── SelectedStockAdapter.java      # 自选股列表适配器
├── data/
│   ├── SelectedStockManager.java  # 自选股管理 (SharedPreferences)
│   ├── StrategyManager.java       # 策略管理 (SharedPreferences)
│   ├── PurchaseRecordManager.java # 买入记录管理 (SharedPreferences)
│   ├── ChatHistoryManager.java    # 聊天历史管理 (SharedPreferences)
│   ├── PromptTemplateManager.java # AI提示词模板管理 (SharedPreferences)
│   ├── StockListCache.java        # 股票/ETF列表本地缓存
│   ├── api/
│   │   ├── EastMoneyApi.java      # 主数据接口 (新浪→腾讯→东方财富多源降级)
│   │   ├── TencentApi.java        # 腾讯数据源
│   │   ├── DeepSeekApi.java       # DeepSeek AI接口
│   │   ├── StockNewsApi.java      # 新浪财经新闻
│   │   ├── StockDataCache.java    # 内存LRU缓存
│   │   └── HttpClientFactory.java # OkHttp客户端工厂
│   └── model/
│       ├── RealtimeQuote.java     # 实时行情（不可变，Builder模式）
│       ├── KlineData.java         # K线数据
│       ├── MinuteLineData.java    # 分时数据
│       ├── Stock.java             # 股票/ETF基本信息
│       ├── Strategy.java          # 交易策略
│       ├── ChatMessage.java       # 聊天消息
│       └── PurchaseRecord.java    # 买入记录
└── util/
    └── DebugLogger.java           # 调试日志
```

## 核心功能

| 模块 | 功能说明 |
|------|---------|
| **自选股** | 添加/删除 A股 & ETF，持久化存储 |
| **个股详情** | 实时价、开高低收、PE/PB、市值、换手率、量比、涨跌停、EPS、股息率、均线(MA5/10/20/30/60)、ETF IOPV/溢价率 |
| **图表** | 分时图（量价+均价线）、日K/周K/月K 蜡烛图（MA5/10/20叠加），支持手势缩放与十字光标 |
| **盘中自动刷新** | 交易日 09:30-15:00 每1秒刷新，非交易时段每3秒 |
| **盈亏计算** | 录入买入价和日期，实时显示浮动盈亏 |
| **AI分析 (DeepSeek)** | 自动注入实时行情+K线+分时+大盘指数上下文，流式Markdown渲染，自动识别交易信号 |
| **策略管理** | 创建/暂停/删除价格、均线、成交量、涨跌幅条件策略，定时检测并推送通知 |
| **策略告警** | 每日 09:50、14:45 定时检测策略条件，匹配时推送系统通知（4小时去重） |

## 使用方法

### 1. 构建安装

```bash
# 编译 APK 并安装到设备
gradlew.bat assembleDebug && adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### 2. 自选股管理

- 主页点击 **+** 按钮进入搜索页
- 输入股票代码或名称搜索（500ms 防抖）
- 点击结果右侧 **+** 添加到自选股，或点击行进入详情
- 长按自选股列表项可删除

### 3. 个股详情

- 点击自选股进入详情页
- 顶部显示实时价格（红涨绿跌）
- **分时图** 标签：当日盘中走势
- **5日** 标签：最近5天日K线
- **20日** 标签：最近20天日K线
- 输入买入价格可计算浮盈/浮亏

### 4. AI 分析

- 在个股详情页点击 **AI分析** 进入聊天页
- 首次使用需配置 DeepSeek API Key（在聊天页菜单中设置）
- 系统自动将股票实时数据、K线、大盘指数注入上下文
- 支持自定义系统提示词、提示词模板
- AI 回复自动检测交易信号（加仓/减仓/止盈/止损/入场/退场），点击 **保存策略** 一键创建策略

### 5. 策略管理

- 主页点击 **策略管理** 进入策略列表
- 支持的条件类型：
  - 价格高于/低于指定值
  - 涨跌幅超过指定百分比
  - MA金叉/死叉（MA5/10/20/30/60）
  - 量比区间
  - 指定目标价、止损价
- 策略可暂停/恢复/删除
- 每日 09:50 和 14:45 自动检测，满足条件时推送通知

---

## 股票信息 API

### 数据源架构

| 数据源 | 用途 | 优先级 |
|--------|------|--------|
| **新浪 (Sina)** | 实时行情（主） | 1 |
| **腾讯 (Tencent)** | 实时行情（备1）、K线（主） | 2 |
| **东方财富 (EastMoney)** | 实时行情（备2）、K线（备）、分时线、股票列表、ETF列表、搜索 | 3 |
| **DeepSeek** | AI 对话分析 | — |
| **新浪财经** | 财经新闻 | — |

所有数据通过 **OkHttp** 异步请求获取，内置自动重试（指数退避 1s/2s/4s，最多3次）和 DNS 缓存。

### 缓存策略

| 数据类型 | TTL | 最大条目 |
|---------|-----|---------|
| 实时行情 | 3秒 | 50 |
| K线数据 | 5分钟 | 30 |
| 分时数据 | 1分钟 | 20 |

---

## 数据模型

### RealtimeQuote — 实时行情

通过 `EastMoneyApi.fetchRealtime(code, callback)` 获取。

| 字段 | 类型 | 含义 | EastMoney字段 |
|------|------|------|-------------|
| name | String | 股票名称 | f58 |
| code | String | 股票代码 | — |
| price | double | 最新价 | f43/100 |
| open | double | 开盘价 | f46/100 |
| high | double | 最高价 | f44/100 |
| low | double | 最低价 | f45/100 |
| preClose | double | 昨收价 | f60/100 |
| volume | double | 成交量（股） | f47 |
| amount | double | 成交额（元） | f48 |
| pctChg | double | 涨跌幅（%） | f170 |
| change | double | 涨跌额 | f169/100 |
| pe | double | 静态市盈率 | f162 |
| peTTM | double | 市盈率TTM | f163 |
| pb | double | 市净率 | f167 |
| turnoverRate | double | 换手率（%） | f168 |
| volumeRatio | double | 量比 | f50/100 |
| totalMarketCap | double | 总市值 | f116 |
| circulatingMarketCap | double | 流通市值 | f117 |
| limitUp | double | 涨停价 | f51/100 |
| limitDown | double | 跌停价 | f52/100 |
| eps | double | 每股收益 | f228 |
| dividendYield | double | 股息率（%） | f188 |
| ma5 | double | 5日均价 | f172 |
| ma10 | double | 10日均价 | f173 |
| ma20 | double | 20日均价 | f174 |
| ma30 | double | 30日均价 | f175 |
| ma60 | double | 60日均价 | f171 |
| iopv | double | IOPV（仅ETF） | f289 |
| premiumRate | double | 溢价率（仅ETF） | — |

**数据来源优先级**: Sina → Tencent → EastMoney（按返回结果降级）

---

### KlineData — K线数据

通过 `EastMoneyApi.fetchKline(code, days, callback)` 或 `fetchKlineWithPeriod(code, count, klt, fqt, callback)` 获取。

| 字段 | 类型 | 含义 |
|------|------|------|
| date | String | 日期/时间 |
| open | double | 开盘价 |
| close | double | 收盘价 |
| high | double | 最高价 |
| low | double | 最低价 |
| volume | double | 成交量（股） |
| amount | double | 成交额（元） |
| amplitude | double | 振幅（%） |
| pctChg | double | 涨跌幅（%） |
| change | double | 涨跌额 |
| turnover | double | 换手率（%，仅EastMoney） |

#### 多周期参数

| 参数 | 说明 |
|------|------|
| **count** | 数据条数 |
| **klt** | K线周期: 1=1分钟, 5=5分钟, 15=15分钟, 30=30分钟, 60=60分钟, **101=日线**, 102=周线, 103=月线 |
| **fqt** | 复权类型: 0=不复权, **1=前复权**, 2=后复权 |

**数据来源优先级**: Tencent（主）→ EastMoney（备）

---

### MinuteLineData — 分时数据

通过 `EastMoneyApi.fetchMinuteLine(code, callback)` 或 `fetchMinuteLineDays(code, ndays, callback)` 获取。

| 字段 | 类型 | 含义 |
|------|------|------|
| time | String | 时间 |
| price | double | 当前价 |
| avgPrice | double | 均价 |
| volume | double | 成交量（股） |
| amount | double | 成交额（万元） |
| preClose | double | 昨收价 |
| pctChg | double | 实时涨跌幅（计算值） |

**参数**: `ndays` — 最近几天（默认1），可获取多日连续分时

**数据来源**: EastMoney `trends2` 接口

---

### Stock — 股票/ETF基本信息

通过 `fetchStockList` / `fetchEtfList` / `searchSuggest` 获取。

| 字段 | 类型 | 含义 |
|------|------|------|
| code | String | 证券代码 |
| name | String | 证券名称 |
| type | String | 类型: "stock" / "etf" |
| isEtf() | boolean | 是否为ETF |

---

## API 接口详情

### 东方财富 API 端点

| 端点 | 用途 |
|------|------|
| `https://hq.sinajs.cn/list={code}` | 新浪实时行情（主） |
| `https://push2.eastmoney.com/api/qt/stock/get?secid={code}&fields=...` | 东方财富实时行情 + 扩展字段（PE/PB/市值/均线等） |
| `https://push2his.eastmoney.com/api/qt/stock/kline/get?secid={code}&klt={period}&fqt={fq}&fields1=...&fields2=...` | 多周期K线（单次请求） |
| `https://push2.eastmoney.com/api/qt/stock/trends2/get?secid={code}&ndays={n}&fields1=...&fields2=...` | 分时线（支持多日） |
| `https://searchadapter.eastmoney.com/api/suggest/get?type=14&input={kw}&count=20` | 搜索建议 |
| `http://vip.stock.finance.sina.com.cn/.../Market_Center.getHQNodeData?node=hs_a&page={p}&num=80` | 全量A股列表（分页） |
| `http://vip.stock.finance.sina.com.cn/.../Market_Center.getHQNodeData?node=etf_hq_fund&page={p}&num=80` | 全量ETF列表（分页） |

### 腾讯 API 端点

| 端点 | 用途 |
|------|------|
| `http://qt.gtimg.cn/q={code}` | 实时行情 |
| `https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?param={code},day,,,{days},qfq` | 日K线（前复权） |

### DeepSeek AI API

| 端点 | 用途 |
|------|------|
| `https://api.deepseek.com/chat/completions` | 对话补全（非流式 + SSE流式） |
| 认证 | `Authorization: Bearer {api_key}` |
| 默认模型 | `deepseek-v4-pro` |
| 备选模型 | `deepseek-v4-flash`, `deepseek-chat`, `deepseek-reasoner` |

### 新浪财经新闻 API

| 端点 | 用途 |
|------|------|
| `https://feed.mix.sina.com.cn/api/roll/get?pageid=153&lid=2509&num={count}&page=1` | 财经新闻滚动列表 |

---

## EastMoneyApi 方法一览

| 方法 | 功能 | 异步 |
|------|------|------|
| `fetchRealtime(code, callback)` | 获取实时行情+完整基本面 | 是 |
| `fetchLatestCloseSync(code)` | 同步获取最新价 | 同步 |
| `fetchKline(code, days, callback)` | 获取日K线数据 | 是 |
| `fetchKlineWithPeriod(code, count, klt, fqt, callback)` | 获取多周期K线 | 是 |
| `fetchMinuteLine(code, callback)` | 获取当日分时线 | 是 |
| `fetchMinuteLineDays(code, ndays, callback)` | 获取多日分时线 | 是 |
| `calculateMA(klines, period, callback)` | 计算移动平均线 | 是 |
| `calculateVolumeRatio(dailyKlines, todayVolume, callback)` | 计算量比 | 是 |
| `fetchStockList(callback)` | 获取全部A股列表 | 是 |
| `fetchEtfList(callback)` | 获取全部ETF列表 | 是 |
| `searchSuggest(keyword, callback)` | 搜索股票/ETF建议 | 是 |

### TencentApi

| 方法 | 功能 |
|------|------|
| `fetchKline(code, days, callback)` | K线（供EastMoneyApi内部调用） |
| `fetchRealtime(code, callback)` | 实时行情（供EastMoneyApi内部调用） |
| `fetchLatestCloseSync(code)` | 同步最新价 |

### DeepSeekApi

| 方法 | 功能 |
|------|------|
| `chat(messages, model, callback)` | 非流式对话 |
| `chatStream(messages, model, callback)` | SSE流式对话 |

### StockNewsApi

| 方法 | 功能 |
|------|------|
| `fetchStockNews(stockName, stockCode, todayOnly, count, callback)` | 获取个股相关新闻 |
| `fetchMarketNews(count, callback)` | 获取市场整体新闻 |

### StockDataCache

| 方法 | 功能 |
|------|------|
| `getQuote(code)` | 获取缓存行情 |
| `putQuote(code, quote)` | 写入缓存行情 |
| `getKline(code, cacheKey)` | 获取缓存K线 |
| `putKline(code, cacheKey, data)` | 写入缓存K线 |
| `getMinuteLine(code, cacheKey)` | 获取缓存分时 |
| `putMinuteLine(code, cacheKey, data)` | 写入缓存分时 |
| `clear()` | 清空所有缓存 |

---

## 数据验证与调试

所有API类均使用 `android.util.Log` 输出以下级别的日志：

- **Log.v / Log.d**: 请求URL、缓存命中/未命中、返回数据摘要（前200~300字符）
- **Log.i**: 请求成功、返回条目数、核心摘要字段
- **Log.w**: 降级切换、数据为空、非严重异常
- **Log.e**: 网络失败、解析异常、请求异常（含堆栈）

日志 Tag 前缀：

| Tag | 范围 |
|-----|------|
| `EastMoneyApi` | 东方财富/新浪接口 |
| `TencentApi` | 腾讯接口 |
| `StockDataCache` | 缓存操作 |
| `HttpClientFactory` | 网络请求层 |
| `DeepSeekApi` | AI对话 |
| `StockNewsApi` | 财经新闻 |
| `DebugLogger` | 调试日志（写入文件） |

---

## 使用示例

```java
// 1. 获取实时行情（含量比、换手率、均线、PE等完整字段）
EastMoneyApi.fetchRealtime("000001", result -> {
    if (result.getPrice() > 0) {
        Log.d("Demo", "最新价: " + result.getPrice());
        Log.d("Demo", "涨跌幅: " + result.getPctChg() + "%");
        Log.d("Demo", "量比: " + result.getVolumeRatio());
        Log.d("Demo", "换手率: " + result.getTurnoverRate() + "%");
        Log.d("Demo", "市盈率: " + result.getPe());
        Log.d("Demo", "5日均线: " + result.getMa5());
        Log.d("Demo", "总市值: " + result.getTotalMarketCap());
    }
});

// 2. 获取日K线（最近120天）
EastMoneyApi.fetchKline("000001", 120, klines -> {
    for (KlineData k : klines) {
        Log.d("Demo", k.getDate() + " O:" + k.getOpen()
                + " H:" + k.getHigh() + " L:" + k.getLow()
                + " C:" + k.getClose() + " V:" + k.getVolume());
    }
});

// 3. 获取30分钟K线（前复权）
EastMoneyApi.fetchKlineWithPeriod("000001", 100, 30, 1, klines -> {
    Log.d("Demo", "30分钟K线共 " + klines.size() + " 条");
});

// 4. 获取当日分时线
EastMoneyApi.fetchMinuteLine("000001", minuteData -> {
    for (MinuteLineData m : minuteData) {
        Log.d("Demo", m.getTime() + " 价:" + m.getPrice()
                + " 量:" + m.getVolume() + " 均价:" + m.getAvgPrice());
    }
});

// 5. 计算MA5
EastMoneyApi.calculateMA(klines, 5, maValues -> {
    double latestMA5 = maValues[maValues.length - 1];
    Log.d("Demo", "最新MA5: " + latestMA5);
});

// 6. 获取全部A股列表
EastMoneyApi.fetchStockList(stocks -> {
    Log.d("Demo", "共 " + stocks.size() + " 只股票");
});

// 7. 获取全部ETF列表
EastMoneyApi.fetchEtfList(etfs -> {
    Log.d("Demo", "共 " + etfs.size() + " 只ETF");
});

// 8. 搜索
EastMoneyApi.searchSuggest("茅台", results -> {
    for (Stock s : results) {
        Log.d("Demo", s.getCode() + " " + s.getName());
    }
});

// 9. DeepSeek AI 对话（流式）
List<ChatMessage> messages = new ArrayList<>();
messages.add(new ChatMessage("system", "你是一个股票分析专家"));
messages.add(new ChatMessage("user", "分析一下000001平安银行"));
DeepSeekApi.getInstance().chatStream(messages, "deepseek-chat",
    new DeepSeekApi.StreamCallback() {
        @Override public void onToken(String token) { /* 增量文本 */ }
        @Override public void onComplete(String fullContent) { /* 完成 */ }
        @Override public void onError(String error) { /* 错误 */ }
    });

// 10. 获取财经新闻
StockNewsApi.getInstance().fetchStockNews("平安银行", "000001", true, 20, news -> {
    for (Map<String, String> item : news) {
        Log.d("Demo", item.get("title") + " - " + item.get("url"));
    }
});
```

---

## 项目配置

| 项 | 值 |
|----|-----|
| **应用ID** | `com.hong.xin.stock` |
| **compileSdk** | 36 |
| **minSdk** | 24 (Android 7.0) |
| **targetSdk** | 36 |
| **版本** | 1.0 (versionCode: 1) |
| **Java** | 11 |
| **权限** | `android.permission.INTERNET` |
| **HTTP库** | OkHttp 4.12.0 |
| **JSON库** | Gson 2.10.1 |
| **Markdown渲染** | Markwon 4.6.2 (core + tables + strikethrough) |
