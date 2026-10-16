# RWNoctiluna

《铁锈战争》(Rusted Warfare) 社区增强客户端 —— 基于 [RWPP](https://github.com/wty-hub/RWPP) 的 fork。

以 AGPL-3.0 开源（继承自 RWPP，见 [LICENSE](LICENSE)）。

## 功能规划

- **Phase 1 (MVP)**：多人菜单 UI（月轨设计）· 中文输入修复 · 一键连服/服务器列表 · 公告
- **Phase 2**：工厂出兵智能分散 · 攻击范围显示 · 房间配置增强 · 迷雾反作弊（巡逻+重算）
- **Phase 3**：崩溃诊断打包 · 对局统计 · 社区反馈迭代

## 文档

- [架构说明](docs/ARCHITECTURE.md)
- [Phase 0 预研结论（游戏字段定位索引）](docs/PHASE0-结论.md)
- [UI 原型（月轨 Lunar Orbital）](docs/ui-prototype-v4.html)

## 构建

需要 JDK 17+。首次构建先运行 `scripts\setup-lib.bat`（从本地游戏安装目录复制 game-lib.jar，
游戏 jar 为版权物，不入库）。

```bat
gradlew :desktop:build
```

## 致谢

- [RWPP](https://github.com/wty-hub/RWPP) 及其贡献者 —— 注入框架与 UI 替换架构
- [coolomet/RustedWarfare](https://github.com/coolomet/RustedWarfare) —— RW 1.15 反编译参考
