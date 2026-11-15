# RWNoctiluna

《铁锈战争》(Rusted Warfare) 社区增强客户端 —— 基于 [RWPP](https://github.com/Minxyzgo/RWPP) 修改，以 AGPL-3.0 开源（继承自上游，见 [LICENSE](LICENSE)）。

面向中文社区：绿色免安装、无需 Java 环境、内置反作弊与本土化界面。

该仓库将不再更新，有更多的功能建议请提issue或向RWPP直接提出issue。

## 功能

**界面与体验**
- 月轨（Lunar Orbital）主题 UI，主界面/加载屏布局为背景人物让位
- 原版游戏可正常进行：多人/遭遇战/回放/沙盒全部可用
- 中文输入修复、公告面板、右上角快捷按钮（公告 / GitHub / 关于 / 退出）

**游戏增强**
- 侦察单位（侦查者 / 无人侦察机）视野范围显示（设置开关）
- 选中敌方单位可单独显示其攻击范围与视野范围（额外按钮）
- 增强的出兵分散逻辑（多工厂轮转，避免排长队）
- 单位目标连线、改进血条等原 RWPP 增强项保留

**反作弊（迷雾巡逻）**
- 基线完整性巡逻：对局中翻转迷雾开关（透视的判定短路手法）立即写回
- 独立周期重扫：不依赖游戏原生机制，浓雾（LOS Fog）下同样有效
- 全部为本地数据校验，零联机同步风险，单机可选关闭

## 安装（玩家）

1. 下载发布包 `RWNoctiluna-x.x.x.zip`
2. 解压**全部内容**到《铁锈战争》游戏根目录
3. 双击 `启动-RWNoctiluna.bat`，首次启动按提示重启一次即进入游戏

无需安装 Java（包内附裁剪版运行时）；不修改任何游戏文件，删除 `generated_lib/` 即完全还原原版。

## 构建（开发者）

需要 JDK 17+。首次构建先运行 `scripts\setup-lib.bat`（从本地游戏安装目录复制 game-lib.jar，游戏 jar 为版权物，不入库）。

```bat
gradlew :desktop:packageReleaseUberJarForCurrentOS   rem 产出可运行 fat jar
```

发布包（jar + 随包 JRE + 启动脚本 + 说明）见 `dist/`，构建产物不入库。

## 文档

- [功能说明](docs/功能说明.md)
- [架构说明](docs/ARCHITECTURE.md)
- [Phase 0 预研结论（游戏字段定位索引）](docs/PHASE0-结论.md)
- [UI 原型（月轨 Lunar Orbital）](docs/ui-prototype-v4.html)

## 开源声明

本项目（RWNoctiluna）是 [RWPP](https://github.com/Minxyzgo/RWPP) 的修改版，两者均以
GNU AGPL-3.0 许可证发布。依据 AGPL 第 13 条，通过网络提供服务时须向用户提供
本修改版的完整对应源代码；本仓库即完整源码， forks 须保持同许可并保留上游版权声明。

## 致谢

- [RWPP](https://github.com/Minxyzgo/RWPP) 及其贡献者 —— 注入框架与 UI 替换架构
- [coolomet/RustedWarfare](https://github.com/coolomet/RustedWarfare) —— RW 1.15 反编译参考
