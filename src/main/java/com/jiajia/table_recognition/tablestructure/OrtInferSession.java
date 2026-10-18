package com.jiajia.table_recognition.tablestructure;

import ai.onnxruntime.*;
import ai.onnxruntime.OrtSession.Result;
import ai.onnxruntime.OrtSession.SessionOptions;
import ai.onnxruntime.providers.OrtCUDAProviderOptions;
import com.jiajia.common_object.OrtInferConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class OrtInferSession {

    private static final Logger logger = LoggerFactory.getLogger(OrtInferSession.class);

    private final OrtEnvironment env;
    private final OrtSession session;

    private boolean useCuda;
    private boolean useDirectML;


    public OrtInferSession(OrtInferConfig ortInferConfig, byte[] tableRegBytes) {
        logger.info("Initializing OrtInferSession...");

        String modelPath = ortInferConfig.getModelPath();
        this.useCuda = ortInferConfig.isUseCuda();
        this.useDirectML = ortInferConfig.isUseDml();

        // 1、创建 ONNX Runtime 环境
        this.env = OrtEnvironment.getEnvironment("OrtInferSessionEnv");

        try {
            // 2. 初始化 SessionOptions
            SessionOptions sessionOptions = initSessionOptions(ortInferConfig);
            EnumSet<OrtProvider> availableProviders = env.getAvailableProviders();
            if (this.useCuda && availableProviders.contains(OrtProvider.CUDA)) {
                OrtCUDAProviderOptions providerOptions = new OrtCUDAProviderOptions(ortInferConfig.deviceId);
                // kNextPowerOfTwo（默认值）以 2 的幂数扩展，而 kSameAsRequested 每次扩展的大小与分配请求的大小相同。
                providerOptions.add("arena_extend_strategy", "kNextPowerOfTwo");
                providerOptions.add("cudnn_conv_algo_search", "EXHAUSTIVE");
                providerOptions.add("do_copy_in_default_stream", "1");
                sessionOptions.addCUDA(providerOptions);
                logger.info("Requested CUDA EP added to session options.");
            }

            if (this.useDirectML && availableProviders.contains(OrtProvider.DIRECT_ML)) {
                sessionOptions.addDirectML(ortInferConfig.deviceId);
                logger.info("Requested DirectML EP - might not be supported in certain Java packages.");
            }

            // 最后添加 CPU
            if (availableProviders.contains(OrtProvider.CPU)) {
                sessionOptions.addCPU(ortInferConfig.useArena);
                logger.info("CPU EP added to session options.");
            }

            // 4. 创建推理会话
            this.session = env.createSession(tableRegBytes, sessionOptions);
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
        logger.info("OrtInferSession initialization completed.");
    }

    public OrtInferSession(OrtInferConfig ortInferConfig) {
        logger.info("Initializing OrtInferSession...");

        String modelPath = ortInferConfig.getModelPath();
        this.useCuda = ortInferConfig.isUseCuda();
        this.useDirectML = ortInferConfig.isUseDml();

        // 1、创建 ONNX Runtime 环境
        this.env = OrtEnvironment.getEnvironment("OrtInferSessionEnv");

        try {
            // 2. 初始化 SessionOptions
            SessionOptions sessionOptions = initSessionOptions(ortInferConfig);
            EnumSet<OrtProvider> availableProviders = env.getAvailableProviders();
            if (this.useCuda && availableProviders.contains(OrtProvider.CUDA)) {
                OrtCUDAProviderOptions providerOptions = new OrtCUDAProviderOptions(ortInferConfig.deviceId);
                // kNextPowerOfTwo（默认值）以 2 的幂数扩展，而 kSameAsRequested 每次扩展的大小与分配请求的大小相同。
                providerOptions.add("arena_extend_strategy", "kNextPowerOfTwo");
                providerOptions.add("cudnn_conv_algo_search", "EXHAUSTIVE");
                providerOptions.add("do_copy_in_default_stream", "1");
                sessionOptions.addCUDA(providerOptions);
                logger.info("Requested CUDA EP added to session options.");
            }

            if (this.useDirectML && availableProviders.contains(OrtProvider.DIRECT_ML)) {
                sessionOptions.addDirectML(ortInferConfig.deviceId);
                logger.info("Requested DirectML EP - might not be supported in certain Java packages.");
            }

            // 最后添加 CPU
            if (availableProviders.contains(OrtProvider.CPU)) {
                sessionOptions.addCPU(ortInferConfig.useArena);
                logger.info("CPU EP added to session options.");
            }

            // 4. 创建推理会话
            Path path = Paths.get(modelPath);
            if (path.isAbsolute()) {
                if (!path.toFile().exists()) {
                    throw new RuntimeException("模型文件未找到: " + modelPath);
                }
                this.session = env.createSession(modelPath, sessionOptions);
            } else {
                InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(modelPath);
                this.session = env.createSession(loadModel(inputStream), sessionOptions);
            }
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
        logger.info("OrtInferSession initialization completed.");
    }

    /**
     * 初始化 SessionOptions
     */
    private SessionOptions initSessionOptions(OrtInferConfig ortInferConfig) throws OrtException {
        SessionOptions sessOpt = new SessionOptions();
        int cpuNums = Runtime.getRuntime().availableProcessors();

        int intraOpNumThreads = ortInferConfig.intraOpNumThreads;
        if (intraOpNumThreads >= 1 && intraOpNumThreads <= cpuNums) {
            sessOpt.setIntraOpNumThreads(intraOpNumThreads);
        }
        int interOpNumThreads = ortInferConfig.interOpNumThreads;
        if (interOpNumThreads >= 1 && interOpNumThreads <= cpuNums) {
            sessOpt.setInterOpNumThreads(interOpNumThreads);
        }
        // 禁用 arena 内存池的扩展策略
        sessOpt.setCPUArenaAllocator(ortInferConfig.useArena);
        // 启用图优化
        sessOpt.setOptimizationLevel(SessionOptions.OptLevel.ALL_OPT);
        // 日志等级
        sessOpt.setSessionLogVerbosityLevel(4);
        sessOpt.setSessionLogLevel(OrtLoggingLevel.ORT_LOGGING_LEVEL_FATAL);
        return sessOpt;
    }

    /**
     * 执行推理
     *
     * @param inputData 传入的输入张量数据(形状需根据模型而定)
     * @return 推理结果 (形状需根据模型而定)
     */
    public Object[] run(float[][][][] inputData) throws OrtException {
        String inputName = this.getInputNames().get(0);
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, inputData)) {
            try (Result result = session.run(Collections.singletonMap(inputName, tensor))) {
                int outputSize = result.size();
                Object[] outputs = new Object[outputSize];
                // 遍历所有输出并存储到outputs数组中
                for (int i = 0; i < outputSize; i++) {
                    // 提取实际的数据
                    outputs[i] = result.get(i).getValue();
                }
                return outputs;
            }
        }
    }

    /**
     * 获取模型的输入名字列表
     */
    public List<String> getInputNames() throws OrtException {
        List<String> names = new ArrayList<>();
        for (NodeInfo info : session.getInputInfo().values()) {
            names.add(info.getName());
        }
        return names;
    }

    /**
     * 获取模型的输出名字列表
     */
    public List<String> getOutputNames() throws OrtException {
        List<String> names = new ArrayList<>();
        for (NodeInfo info : session.getOutputInfo().values()) {
            names.add(info.getName());
        }
        return names;
    }

    /**
     * 在 ONNX 模型的 metadata 中提取指定 key 对应的数据，split line 后返回
     */
    public List<String> getCharacterList(String key) {
        try {
            Map<String, String> customMetadata = session.getMetadata().getCustomMetadata();
            if (customMetadata.containsKey(key)) {
                String content = customMetadata.get(key);
                // 按行拆分
                return Arrays.asList(content.split("\\r?\\n"));
            }
            return Collections.emptyList();
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断模型中是否包含指定 key
     */
    public boolean haveKey(String key) {
        try {
            Map<String, String> customMetadata = session.getMetadata().getCustomMetadata();
            return customMetadata.containsKey(key);
        } catch (OrtException e) {
            throw new RuntimeException(e);
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

    public void close() {
        logger.info("Closing OrtInferSession...");
        if (session != null) {
            try {
                session.close();
            } catch (Exception e) {
                logger.error("Error closing OrtSession", e);
            }
        }
        if (env != null) {
            try {
                env.close();
            } catch (Exception e) {
                logger.error("Error closing OrtEnvironment", e);
            }
        }
        logger.info("OrtInferSession closed.");
    }

}
