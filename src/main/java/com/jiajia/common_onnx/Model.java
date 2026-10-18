package com.jiajia.common_onnx;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

/**
 * @author jiangnanboy
 * @date 2025/12/2 9:10
 */
public class Model {
    private static final Logger LOGGER = LoggerFactory.getLogger(Model.class);

    private OrtSession session;
    private OrtEnvironment env;
    private int gpuDeviceId = -1;
    private long inputHeight;
    private long inputWidth;
    private int numInputElements;
    private String inputName;
    private String outputName;
    private long[] inputShape;
    private OnnxJavaType inputType;

    // ... 所有 getter 方法保持不变 ...
    public OrtSession getSession() { return this.session; }
    public OrtEnvironment getEnv() { return this.env; }
    public int getGpuDeviceId() { return this.gpuDeviceId; }
    public long getInputHeight() { return this.inputHeight; }
    public long getInputWidth() { return this.inputWidth; }
    public int getNumInputElements() { return this.numInputElements; }
    public String getInputName() { return this.inputName; }
    public String getOutputName() { return this.outputName; }
    public long[] getInputShape() { return this.inputShape; }
    public OnnxJavaType getInputType() { return this.inputType; }

    /**
     * 构造函数：从内存中的字节数组加载模型。
     * 这是推荐的安全加载方式。
     * @param modelBytes 模型的字节数组
     * @throws OrtException
     */
    public Model(byte[] modelBytes) throws OrtException {
        initModel(modelBytes);
    }

    /**
     * 构造函数：从内存中的字节数组加载模型，并指定GPU设备。
     * @param modelBytes 模型的字节数组
     * @param gpuDeviceId GPU设备ID，-1表示使用CPU
     * @throws OrtException
     */
    public Model(byte[] modelBytes, int gpuDeviceId) throws OrtException {
        this.gpuDeviceId = gpuDeviceId;
        initModel(modelBytes);
    }

    /**
     * @deprecated 推荐使用 {@link #Model(byte[])} 以获得更好的安全性。
     * 此方法为了兼容性而保留。
     */
    @Deprecated
    public Model(String path) throws OrtException {
        initModel(path);
    }

    /**
     * @deprecated 推荐使用 {@link #Model(byte[], int)} 以获得更好的安全性。
     * 此方法为了兼容性而保留。
     */
    @Deprecated
    public Model(String path, int gpuDeviceId) throws OrtException {
        this.gpuDeviceId = gpuDeviceId;
        initModel(path);
    }

    /**
     * 核心初始化逻辑：从字节数组创建会话。
     * 已针对CPU推理进行性能优化。
     * @param modelBytes 模型的字节数组
     * @throws OrtException
     */
    private void initModel(byte[] modelBytes) throws OrtException {
        LOGGER.info("Initializing model from byte array...");
        this.env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();

        // ========== 性能优化配置 ==========
        // 1. 启用所有图优化（包括算子融合、常量折叠等）
        sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);

        // 2. 根据CPU核心数自动设置线程数
        int cpuCores = Runtime.getRuntime().availableProcessors();
        int intraOpThreads = Math.max(1, cpuCores / 2);
        int interOpThreads = Math.max(1, cpuCores / 4);
        sessionOptions.setIntraOpNumThreads(intraOpThreads);
        sessionOptions.setInterOpNumThreads(interOpThreads);
        LOGGER.info("CPU cores: {}, IntraOp threads: {}, InterOp threads: {}", cpuCores, intraOpThreads, interOpThreads);

        // 3. 内存优化配置：启用 arena allocator 复用内存，降低连续推理时的分配开销
        sessionOptions.setCPUArenaAllocator(true);

        // 4. 启用内存模式优化（减少内存拷贝）
        sessionOptions.setMemoryPatternOptimization(true);

        // 5. 禁用顺序执行（允许算子并行）
//        sessionOptions.disableSequentialExecution();

        // 6. 设置执行模式为并行（默认是顺序）
        sessionOptions.setExecutionMode(OrtSession.SessionOptions.ExecutionMode.PARALLEL);

        // GPU配置
        if (this.gpuDeviceId >= 0) {
            sessionOptions.addCPU(false);
            sessionOptions.addCUDA(this.gpuDeviceId);
        }

        // *** 关键修改点：使用 createSession(byte[], SessionOptions) 重载方法 ***
        this.session = this.env.createSession(modelBytes, sessionOptions);

        // 后续的元数据获取逻辑保持不变
        Map<String, NodeInfo> inputMetaMap = this.session.getInputInfo();
        this.inputName = this.session.getInputNames().iterator().next();
        NodeInfo inputMeta = inputMetaMap.get(this.inputName);
        this.inputType = ((TensorInfo)inputMeta.getInfo()).type;
        this.inputShape = ((TensorInfo)inputMeta.getInfo()).getShape();
        this.numInputElements = (int)(this.inputShape[1] * this.inputShape[2] * this.inputShape[3]);
        this.inputHeight = this.inputShape[2];
        this.inputWidth = this.inputShape[3];
        this.outputName = this.session.getOutputNames().iterator().next();
        LOGGER.info("Input meta info: {}", inputMeta.getInfo());
        LOGGER.info("Output meta info: {}", this.session.getOutputInfo().get(this.outputName));
    }

    /**
     * @deprecated 此方法为了兼容性而保留。
     */
    @Deprecated
    private void initModel(String path) throws OrtException {
        Path paths = Paths.get(path);
        if (paths.isAbsolute()) {
            if (!paths.toFile().exists()) {
                throw new RuntimeException("模型文件未找到: " + path);
            }
            try (java.io.InputStream fis = new java.io.FileInputStream(path)) {
                byte[] modelBytes = fis.readAllBytes();
                initModel(modelBytes);
            } catch (java.io.IOException e) {
                throw new OrtException("Failed to read model file: " + e);
            }
        } else {
            InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
            initModel(loadModel(inputStream));
        }
    }

    private byte[] loadModel(InputStream modelInputStream) {
        try (
                InputStream inputStream = modelInputStream;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream()
        ) {
            int nRead;
            byte[] data = new byte[1024];

            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeModel() {
        LOGGER.info("Closing model...");
        if (this.session != null) {
            try {
                this.session.close();
            } catch (OrtException e) {
                LOGGER.error("Failed to close model session", e);
            }
            this.session = null;
        }
        // 注意：OrtEnvironment 由全局单例管理，不在此处关闭
        // 应用退出时调用 Model.closeGlobalEnv() 统一释放
    }
}
