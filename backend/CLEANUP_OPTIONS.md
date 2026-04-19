# 清理重构中间状态

## 选项 1: 保守清理（推荐）

**只删除旧的模块目录，保留当前文件位置**

```bash
cd C:\Users\HP\Desktop\think\backend

# 删除旧的 common 模块
rm -rf codereview-common
rm -rf codereview-ai
rm -rf knowledge-base-common  # 如果存在
rm -rf codereview-domain
rm -rf codereview-infrastructure
```

保留：km-common, ci-domain（继续使用）

## 选项 2: 完全重构（彻底）

**移动文件到新目录结构，完全符合 Java 规范**

```bash
cd C:\Users\HP\Desktop\think\backend

# 创建新的目录结构并移动文件
mkdir -p ci-domain/src/main/java/com/think/platform/ci
mkdir -p km-domain/src/main/java/com/think/platform/km

# 移动文件（需要手动或脚本）
# 这会比较复杂，建议使用 IDE 重构功能
```

## 选项 3: 接受现状（最简单）

**不做任何物理移动，保持现状**

优点：
- 文件内容已经正确（package 声明已更新）
- 代码可以正常编译运行
- Git 历史完整

缺点：
- 目录结构与包名不一致
- IDE 可能显示警告（不影响运行）

---

## 建议

**推荐：选项 1 + 选项 3**

1. 删除旧的模块目录（避免混淆）
2. 接受当前的目录结构（文件内容正确即可）

Java 项目中，**包名正确比目录结构更重要**。编译器看的是 package 声明，不是目录名。

你要执行哪个选项？
