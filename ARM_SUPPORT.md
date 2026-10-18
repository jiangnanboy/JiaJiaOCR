# JiaJiaOCR ARM 平台支持说明

## 概述

JiaJiaOCR 已通过修改 Maven 依赖配置，实现了对 ARM 架构的支持。本文档说明如何在不同平台（x86 和 ARM）上构建和运行项目。

---

## 支持的架构

| 平台 | 架构 | 状态 | 说明 |
|------|------|------|------|
| Windows | x86_64 | ✅ 支持 | 默认支持 |
| Windows | ARM64 | ⚠️ 部分支持 | ONNX Runtime 需手动配置 |
| Linux | x86_64 | ✅ 支持 | 默认支持 |
| Linux | ARM64 (aarch64) | ✅ 支持 | 树莓派、ARM 服务器等 |
| macOS | x86_64 | ✅ 支持 | 默认支持 |
| macOS | ARM64 (Apple Silicon) | ✅ 支持 | M1/M2/M3 芯片 |

---

## 依赖变更说明

### 1. ONNX Runtime

**依赖：**
```xml
<dependency>
    <groupId>com.microsoft.onnxruntime</groupId>
    <artifactId>onnxruntime</artifactId>
    <version>1.19.0</version>
</dependency>
```

**支持情况：**
- ✅ Windows x86_64：自动支持
- ✅ Linux x86_64：自动支持
- ✅ Linux ARM64：自动支持（从 1.16.0+ 开始）
- ✅ macOS x86_64：自动支持
- ✅ macOS ARM64：自动支持
- ⚠️ Windows ARM64：需要手动配置（见下方说明）

### 2. OpenCV

**原依赖（仅支持 x86）：**
```xml
<dependency>
    <groupId>ai.djl.opencv</groupId>
    <artifactId>opencv</artifactId>
    <version>0.31.0</version>
</dependency>
```

**新依赖（支持 x86 + ARM）：**
```xml
<dependency>
    <groupId>org.openpnp</groupId>
    <artifactId>opencv</artifactId>
    <version>4.9.0-0</version>
</dependency>
```

**变更原因：**
- `ai.djl.opencv` 仅包含 x86 平台的 OpenCV 原生库
- `org.openpnp:opencv` 是社区维护的跨平台 OpenCV Java 绑定
- 支持 Windows/Linux/macOS 的 x86 和 ARM64 架构
- 通过 `nu.pattern.OpenCV.loadLocally()` 自动加载当前平台的原生库

---

## 构建配置

### 通用构建（推荐）

默认配置已支持多平台，直接构建即可：

```bash
mvn clean package
```

构建后的 JAR 包包含所有平台的原生库，可在任何支持的平台运行。

### 特定平台构建（减小包体积）

如果只需要在特定平台运行，可以修改 `pom.xml`，使用平台特定的依赖：

#### Linux ARM64（树莓派、ARM 服务器）

注释掉通用的 OpenCV 依赖，启用 Linux ARM64 特定依赖：

```xml
<!-- 注释掉通用依赖 -->
<!--
<dependency>
    <groupId>org.openpnp</groupId>
    <artifactId>opencv</artifactId>
    <version>${opencv.version}</version>
</dependency>
-->

<!-- 启用 Linux ARM64 特定依赖 -->
<dependency>
    <groupId>org.openpnp</groupId>
    <artifactId>opencv</artifactId>
    <version>${opencv.version}</version>
    <classifier>linux-arm64</classifier>
</dependency>
```

#### 其他平台

参考 `pom.xml` 中的注释，选择对应的平台依赖：
- `windows-x86_64`：Windows 64位
- `linux-x86_64`：Linux 64位
- `linux-arm64`：Linux ARM64
- `macosx-x86_64`：macOS Intel
- `macosx-arm64`：macOS Apple Silicon

---

## Windows ARM64 特殊配置

ONNX Runtime 官方 Java API 目前不直接支持 Windows ARM64。如需在 Windows ARM64（如 Snapdragon 设备）运行，需要：

### 步骤1：下载 Windows ARM64 版本的 ONNX Runtime

访问 [ONNX Runtime Releases](https://github.com/microsoft/onnxruntime/releases)，下载 Windows ARM64 版本的 `onnxruntime.dll`。

### 步骤2：配置 DLL 加载

将 `onnxruntime.dll` 放到以下位置之一：
- 系统 PATH 环境变量中的目录
- 项目资源目录（`src/main/resources`）
- 指定路径并通过代码加载

### 步骤3：代码中手动加载（如需要）

```java
public class WindowsArm64Loader {
    static {
        // 方式1：加载系统 PATH 中的 DLL
        System.loadLibrary("onnxruntime");
        
        // 方式2：加载指定路径的 DLL
        // System.load("C:/path/to/onnxruntime.dll");
    }
}
```

---

## 验证 ARM 支持

### 方法1：查看依赖树

```bash
mvn dependency:tree | grep opencv
```

应看到类似输出：
```
org.openpnp:opencv:jar:4.9.0-0:compile
```

### 方法2：运行时检测

```java
public class PlatformCheck {
    public static void main(String[] args) {
        String os = System.getProperty("os.name");
        String arch = System.getProperty("os.arch");
        System.out.println("操作系统: " + os);
        System.out.println("架构: " + arch);
        
        // 验证 OpenCV 加载
        try {
            nu.pattern.OpenCV.loadLocally();
            System.out.println("OpenCV 加载成功");
        } catch (Exception e) {
            System.err.println("OpenCV 加载失败: " + e.getMessage());
        }
    }
}
```

### 方法3：运行测试

```bash
mvn test
```

---

## 常见问题

### Q1: ARM 平台上运行时报 `UnsatisfiedLinkError`

**原因：** 原生库未正确加载。

**解决：**
1. 确认使用的是 `org.openpnp:opencv` 依赖
2. 检查 `nu.pattern.OpenCV.loadLocally()` 是否被调用
3. 确认 JAR 包中包含对应平台的原生库

### Q2: Linux ARM64 上 ONNX Runtime 加载失败

**原因：** ONNX Runtime 版本过旧，不支持 ARM64。

**解决：** 确保使用 ONNX Runtime 1.16.0 或更高版本。

### Q3: 包体积过大

**原因：** `org.openpnp:opencv` 包含所有平台的原生库。

**解决：** 使用平台特定的 classifier（见"特定平台构建"部分）。

### Q4: 与 `ai.djl.opencv` 冲突

**原因：** 项目中同时存在两个 OpenCV 依赖。

**解决：** 确保 `pom.xml` 中只有 `org.openpnp:opencv`，移除 `ai.djl.opencv`。

---

## 源码兼容性

**重要：** 本次修改仅涉及 Maven 依赖，**不需要修改任何 Java 源码**。

- `org.openpnp:opencv` 和 `ai.djl.opencv` 都使用 `org.opencv` 包名
- 现有的 `import org.opencv.*` 代码完全兼容
- `nu.pattern.OpenCV.loadLocally()` 加载方式不变

---

## 版本历史

| 版本 | 变更 |
|------|------|
| 2.1.0 | 将 OpenCV 依赖从 `ai.djl.opencv` 替换为 `org.openpnp:opencv`，支持 ARM 平台 |

---

## 参考链接

- [ONNX Runtime 平台支持](https://onnxruntime.ai/docs/execution-providers/)
- [org.openpnp:opencv Maven 仓库](https://central.sonatype.com/artifact/org.openpnp/opencv)
- [OpenCV Java 文档](https://docs.opencv.org/4.x/d9/d52/tutorial_java_dev_intro.html)
