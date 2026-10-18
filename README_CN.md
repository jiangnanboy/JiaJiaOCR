# JiaJiaOCR

[![English README](https://img.shields.io/badge/English-README-blue?style=flat-square)](README.md)
[![GitHub stars](https://img.shields.io/github/stars/jiangnanboy/JiaJiaOCR?style=social)](https://github.com/jiangnanboy/JiaJiaOCR/stargazers)
[![JDK](https://img.shields.io/badge/JDK-11+-blue?style=flat-square)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-green?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-x86__64%20%7C%20ARM-orange?style=flat-square)]()
[![Inference](https://img.shields.io/badge/Inference-CPU%20Recommended-blueviolet?style=flat-square)]()

> **纯 Java 实现的 OCR 引擎** —— 覆盖通用 OCR、手写 OCR、版面检测、表格检测与识别、PDF 识别，轻量、跨平台、开箱即用。

## ✨ 核心亮点

- ☕ **纯 Java**：无需 JNI 调用 EXE/DLL，无需 Python 环境，一个 Jar 搞定。
- 🧠 **CPU 推理优先**：**本项目推荐使用 CPU 推理**，不依赖 GPU/CUDA，部署门槛极低。
- 💻 **全平台覆盖**：支持 **x86_64 与 ARM** 架构，适配 **Windows / Linux / macOS**。
- 🇨🇳 **信创适配**：支持**部分国产信创环境**（麒麟、统信等 x86_64 / ARM 平台）。
- ⚡ **懒加载设计**：按需加载模型，用到哪个功能才加载哪个，内存友好。
- 📦 **模型开箱即用**：模型已内置在 Jar 中，也支持用 `-Djiajia.model.dir` 指定外部模型目录。
- 🧵 **多线程友好**：实例可安全共享，支持并发批量识别。

## 🖥️ 平台支持

| 平台 | 架构 | 支持情况 |
| --- | --- | --- |
| Windows | x86_64 | ✅ 支持 |
| Linux | x86_64 | ✅ 支持 |
| Linux | ARM64 (aarch64) | ✅ 支持（树莓派、ARM 服务器等） |
| macOS | x86_64 / ARM64 | ✅ 支持 |
| 国产信创（麒麟、统信等） | x86_64 / ARM | ✅ 部分支持 |

> 推理默认走 **CPU**，当前版本**不依赖 GPU**。更多架构与依赖说明见 [ARM_SUPPORT.md](ARM_SUPPORT.md)。

## 🧩 功能总览

| 功能 | 说明 |
| --- | --- |
| 📝 通用 OCR | 中英文、数字、符号混合识别，返回文本、置信度、8 点坐标。 |
| ✍️ 手写 OCR | 针对中文手写体优化，标准样本识别准确率 **92%+**。 |
| 📋 版面检测 | 识别标题、段落、图片、表格等版面元素及坐标。 |
| 📐 文本行检测 | 仅定位文本行位置（不做内容识别）。 |
| 📊 表格检测 | 定位表格区域及置信度。 |
| 🧾 表格识别 | 表格结构解析，输出 **HTML** 与结构化 **JSON**（行列数据）。 |
| 📄 PDF 识别 | 多页 PDF 转图片后批量 OCR。 |
| ⏹️ 任务取消 | 长耗时识别可中途取消。 |
| 🧵 多线程批量 | 单实例共享并发处理，支持大批量任务。 |

## ⚡ 性能评估

以下为 **CPU 环境**下的参考耗时（**冷启动** = 首次调用、含模型加载；**热启动** = 模型已加载、纯推理）。除多线程批量外，各功能均为**单张图片**的识别耗时；多线程批量为 **3 个线程并发**：

| 功能 | 冷启动 | 热启动（纯推理） |
| --- | --- | --- |
| 通用 OCR | 928 ms | 426 ms |
| 手写 OCR | 1516 ms | 989 ms |
| 版面检测 | 1572 ms | 910 ms |
| 文本行检测 | 194 ms | 123 ms |
| 表格检测 | 748 ms | 478 ms |
| 表格检测 + 识别（一键） | 1986 ms | 1362 ms |
| 多线程批量（3 线程） | 预热 686 ms | 总耗时 1411 ms |

PDF 识别（27 页示例）：转图约 10.6s，OCR 约 38.8s（平均每页约 1.4s）。

> 以上数值仅供参考，实际耗时会因机器配置与图片内容不同而有所差异。

## 🚀 快速开始

### 1. 环境要求

- 轻量版 1.0+ / 全功能版 2.0.x：**JDK 8+**
- 全功能版 2.1.0 / 2.2.0：**JDK 11+**（推荐）
- 操作系统：Windows 10+、Linux x86_64、Linux ARM、国产信创系统
- 构建工具：Maven（推荐）

### 2. 引入依赖

在 `pom.xml` 中引入核心依赖：

```xml
<dependencies>
    <dependency>
        <groupId>com.microsoft.onnxruntime</groupId>
        <artifactId>onnxruntime</artifactId>
        <version>1.19.0</version>
    </dependency>
    <dependency>
        <groupId>org.openpnp</groupId>
        <artifactId>opencv</artifactId>
        <version>4.9.0-0</version>
    </dependency>
    <dependency>
        <groupId>org.apache.pdfbox</groupId>
        <artifactId>pdfbox</artifactId>
        <version>3.0.2</version>
    </dependency>
</dependencies>
```

> `pdfbox` 仅在需要 PDF 识别时引入。

### 3. 构建 Jar 包

[Releases](https://github.com/jiangnanboy/JiaJiaOCR/releases) 提供**源码包（含模型）**，解压后在项目根目录执行：

```bash
mvn clean package -DskipTests
```

构建产物位于 `target/JiaJia-2.2.0.jar`，将其放入你的项目依赖目录即可使用。

### 4. 模型说明

模型**已内置在 Jar 中**（classpath `models/`），默认开箱即用，无需额外配置。

如需使用**外部模型目录**（模型单独分发、热更新等场景），可通过系统属性指定：

```bash
java -Djiajia.model.dir=/path/to/models -jar yourapp.jar
```

外部模型目录结构需与内置一致：

```
models/
├── text_detection/det.onnx
├── direction_detection/cls.onnx
├── text_recognition/rec.onnx
├── text_recognition/dict.txt
├── handwriting_rec/rec.onnx
├── handwriting_rec/dict.txt
├── layout_analysis/layout.onnx
└── table_recognition/table_det.onnx
└── table_recognition/table_reg.onnx
```

### 5. 最小示例

```java
import com.jiajia.common_object.*;
import com.jiajia.core.JiaJiaOCR;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class QuickStart {
    public static void main(String[] args) throws Exception {
        // 创建实例（自动加载 OpenCV，模型按需懒加载）
        try (JiaJiaOCR ocr = JiaJiaOCR.builder()) {
            List<Pair<Text, Box>> results = ocr.recognizeGeneralText("general.png");
            for (Pair<Text, Box> item : results) {
                System.out.println(item.getLeft().getText());
            }
        }
    }
}
```

## 📖 功能调用

> 以下示例均基于 `JiaJiaOCR` 门面类，公共导入：
> `import com.jiajia.common_object.*;`、`import com.jiajia.core.JiaJiaOCR;`、`import org.apache.commons.lang3.tuple.Pair;`。
> 每个功能都支持 `String 路径 / Mat / BufferedImage` 三种入参形式，下面以路径为例。

### 1. 通用 OCR

```java
try (JiaJiaOCR ocr = JiaJiaOCR.builder()) {
    List<Pair<Text, Box>> results = ocr.recognizeGeneralText("general.png");
    for (Pair<Text, Box> item : results) {
        System.out.println(item.getLeft().getText());            // 文本内容
        System.out.println(item.getRight().getScore());          // 置信度
        System.out.println(item.getRight().getLinePosition());   // 8 点坐标
    }
}
```

### 2. 手写 OCR

```java
try (JiaJiaOCR ocr = JiaJiaOCR.builder()) {
    List<Pair<Text, Box>> results = ocr.recognizeHandwrittenText("handwriting.jpg");
    for (Pair<Text, Box> item : results) {
        System.out.println(item.getLeft().getText());
    }
}
```

### 3. 版面检测

```java
try (JiaJiaOCR ocr = JiaJiaOCR.builder()) {
    List<Layout> layouts = ocr.detectLayout("layout.png");
    for (Layout l : layouts) {
        System.out.println(l.getLabel() + "，置信度：" + l.getConfidence()); // 元素类型、置信度
        System.out.println(Arrays.toString(l.getBbox()));                     // 矩形坐标
    }
}
```

### 4. 文本行检测

```java
try (JiaJiaOCR ocr = JiaJiaOCR.builder()) {
    Boxes boxes = ocr.detectTextLines("general.png");
    for (Box box : boxes.getBoxes()) {
        System.out.println(Arrays.toString(box.getLinePosition()));           // 8 点坐标
    }
}
```

### 5. 表格检测

```java
try (JiaJiaOCR ocr = JiaJiaOCR.builder()) {
    List<DetectionResult> tables = ocr.detectTables("table.png");
    for (DetectionResult t : tables) {
        System.out.println(t.getClassLabel() + "，置信度：" + t.getConfidence());
        System.out.println(t.getRect());                                      // 表格区域
    }
}
```

### 6. 表格识别（两步：OCR + 结构识别）

```java
try (JiaJiaOCR ocr = JiaJiaOCR.builder()) {
    // 第一步：通用 OCR 提取文字
    List<Pair<Text, Box>> ocrResults = ocr.recognizeGeneralText("table.png");
    // 第二步：识别表格结构
    TableResult table = ocr.recognizeTableFromOCR("table.png", ocrResults);
    System.out.println(table.getHtml());        // HTML 结果
    System.out.println(table.getTableJson());   // 结构化 JSON（行列数据）
}
```

### 7. 表格一键检测 + 识别

```java
try (JiaJiaOCR ocr = JiaJiaOCR.builder()) {
    List<TableResult> tables = ocr.recognizeTables("table.png");
    for (TableResult t : tables) {
        System.out.println(t.getHtml());
        System.out.println(t.getTableJson());
    }
}
```

### 8. PDF 识别

```java
try (JiaJiaOCR ocr = JiaJiaOCR.builder()) {
    List<String> pageImages = PdfConvert.convertPdfToImages("doc.pdf", "pdf_images");
    for (String img : pageImages) {
        List<Pair<Text, Box>> results = ocr.recognizeGeneralText(img);
        for (Pair<Text, Box> item : results) {
            System.out.println(item.getLeft().getText());
        }
    }
}
```

### 9. 任务取消 & 多线程批量

```java
// 任务取消：传入 CancellationToken，在其它线程调用 cancel() 即可中断
CancellationToken token = new CancellationToken();
new Thread(token::cancel).start();
ocr.recognizeGeneralText("general.png", token);

// 多线程批量：共享同一个实例（预热后并发调用即可）
JiaJiaOCR sharedOcr = JiaJiaOCR.builder();
sharedOcr.recognizeGeneralText("general.png");   // 预热，完成模型加载
// 之后可在多个线程中并发调用 sharedOcr.recognizeGeneralText(...)
```

> 完整可运行示例见 [src/test/java/JiaJiaOCRFullTest.java](src/test/java/JiaJiaOCRFullTest.java)。

## 📦 版本说明

| 版本 | 大小 | 核心功能 | JDK |
| --- | --- | --- | --- |
| 🔥 轻量版（1.0+） | 21 MB+ | 通用 OCR、PDF 识别 | 8+ |
| ⚡ 全功能版（2.0.x / 2.1.0 / 2.2.0） | 200 MB+ | 通用 OCR + 手写 OCR + 版面检测 + 表格识别 + PDF 识别 | 2.0.x：8+；2.1.0 / 2.2.0：11+ |

**v2.2.0**（推荐）全面兼容 **x86_64 与 ARM**，深度适配国产信创环境。

### 更新日志

- **v1.0.1** — 英文 OCR 空白识别返回空串而非 `null`。
- **v1.0.2** — 新增多页 PDF 文字提取。
- **v2.0.x** — 模型懒加载、手写 OCR、版面检测、表格检测与识别、坐标输出、HTML 表格输出。
- **v2.1.0** — 内存优化、表格 **JSON** 结构化输出、任务取消、多线程批量处理。
- **v2.2.0** — x86_64 与 ARM 全架构支持、信创环境适配。

## 📊 返回数据

| 功能模块 | 返回类型 | 说明 |
| --- | --- | --- |
| 通用 / 手写 OCR | `List<Pair<Text, Box>>` | `Text`：文本内容；`Box`：置信度 + 8 点坐标。 |
| 版面检测 | `List<Layout>` | label（标题/段落/表格等）、bbox 坐标、置信度。 |
| 表格检测 | `List<DetectionResult>` | 表格矩形区域、类别、置信度。 |
| 表格识别 | `TableResult` | `getHtml()` + `getTableJson()` 双输出，含坐标、耗时。 |

## 🖼️ 效果展示

| 通用 OCR | 手写 OCR |
| --- | --- |
| <img src="docs/images/generalocr.png" alt="通用OCR效果" width="400"/> | <img src="docs/images/handwritingocr.png" alt="手写OCR效果" width="400"/> |

| 版面检测 | 表格检测 | 表格识别 |
| --- | --- | --- |
| <img src="docs/images/layout.png" alt="版面检测效果" width="260"/> | <img src="docs/images/table_det.png" alt="表格检测效果" width="260"/> | <img src="docs/images/table_rec.png" alt="表格识别效果" width="260"/> |

## 📞 联系方式

- 🐙 GitHub：[jiangnanboy](https://github.com/jiangnanboy)
- 💬 QQ：2229029156
- 📧 Email：2229029156@qq.com

## ⭐ 支持项目

如果本项目对你有帮助，欢迎点亮 **Star**，您的支持是项目持续迭代优化的最大动力！❤️
