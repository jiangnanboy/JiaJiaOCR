---

## 📊 核心返回结果说明

JiaJiaOCR 2\.1\.0 / 2.2.0 提供标准化、结构化返回数据，适配各类业务解析需求，新版升级表格双格式输出：

|功能模块|核心返回内容|数据格式说明|
|---|---|---|
|通用 OCR / 手写 OCR|文本内容、坐标、置信度|`List<Pair<Text, Box>>`，Text含文本内容与置信度，Box含8个顶点坐标|
|版面检测|元素类型、坐标、置信度|`List<Layout>`，含label（标题/段落/表格等）、bbox矩形坐标、置信度|
|表格检测|表格区域、置信度|`List<DetectionResult>`，含表格矩形区域、识别置信度|
|表格识别（2\.1\.0及以上升级）|表格结构、坐标、行列信息|`TableResult`，支持**HTML格式**（网页渲染）\+**JSON格式**（行列结构化数据）双输出，含坐标、耗时、行列数|

---

## 🖼️ 效果展示

各功能模块识别效果预览：

### 📝 通用OCR效果
<img src="generalocr.png" alt="通用OCR效果" width="800"/>

### ✍️ 手写OCR效果
<img src="handwrittingocr.png" alt="手写OCR效果" width="800"/>

### 📋 版面检测效果
<img src="layout.png" alt="版面检测效果" width="800"/>

### 📊 表格检测效果
<img src="table_det.png" alt="表格检测效果" width="800"/>

### 📈 表格识别效果
<img src="table_rec.png" alt="表格识别效果" width="800"/>

---

## 📞 联系方式

如有功能建议、问题反馈或技术交流，欢迎随时联系：

- 🐙 **GitHub**：[jiangnanboy](https://github.com/jiangnanboy)

- 💬 **QQ**：2229029156

- 📧 **Email**：2229029156@qq\.com

## ⭐ 支持项目

如果本项目对你有帮助，欢迎点亮 **Star**，您的支持是项目持续迭代优化的最大动力 ❤️
---

[![GitHub stars](https://img.shields.io/github/stars/jiangnanboy/JiaJiaOCR?style=social)](https://github.com/jiangnanboy/JiaJiaOCR/stargazers)
