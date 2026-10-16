# RWNoctiluna 架构

> RWPP（io.github.rwpp）的社区定制 fork · AGPL-3.0

## 模块结构

```
RWNoctiluna/
├── core-api/   注入框架抽象层：@Inject 注解体系、javassist 字节码改写器、
│               Game/Room/Player/World 接口、事件总线、设置系统
├── ksp/        KSP 编译期处理器：扫描注入注解 → 生成 InjectInfo
├── core/       Compose UI 层：多人大厅/房间界面、widget、i18n（月轨皮肤在此实现）
├── desktop/    Windows 桌面入口：Main.kt 启动游戏、20 个 *Inject Hook 类
├── docs/       设计文档与技术定位（游戏字段索引见 PHASE0-结论.md）
├── assets/     UI 资源（默认背景图等）
└── scripts/    辅助脚本（setup-lib.bat 复制游戏 jar）
```

依赖链：`desktop → core → core-api`，`ksp → core-api`。

## 注入机制（继承自 RWPP）

- 启动方式：classpath 注入——本程序自身为主类（`io.github.rwpp.desktop.MainKt`），
  游戏的 `game-lib.jar` 作为库加载
- 首次运行：javassist 按注解改写游戏类 → 输出 `generated_lib/` → 重启生效
- 菜单替换：自有 JFrame + ComposePanel 覆盖游戏 Canvas（RW 引擎为
  Slick2D + LWJGL2 + libRocket，Compose 无法嵌入 GL 画布，只能窗口叠加）

## 新增功能开发指引

游戏内字段/方法定位索引统一维护在 `docs/PHASE0-结论.md` 第三节（迷雾/聊天/
出兵队列/攻击范围的类名与 hook 点）。新功能 = 新增一个 Inject 类 + core 中的
UI/逻辑模块，禁止在现有 Hook 中堆功能。

## 构建与运行

```bat
scripts\setup-lib.bat        rem 首次：从游戏目录复制 game-lib.jar
gradlew :desktop:build       rem 编译
```
