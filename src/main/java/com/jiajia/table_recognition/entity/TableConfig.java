package com.jiajia.table_recognition.entity;

public class TableConfig {

    public String modelPath = "models/t.onnx"; // 模型路径
    public TableModelType modelType = TableModelType.SLANET_PLUS; // 模型类型
    public boolean useCuda = false; // 是否使用 CUDA
    public int deviceId = 0; // 显卡编号
    public boolean useArena = false; // arena内存池的扩展策略（速度有提升，但内存会剧增，且持续占用，不释放，默认关闭）

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public TableModelType getModelType() {
        return modelType;
    }

    public void setModelType(TableModelType modelType) {
        this.modelType = modelType;
    }

    public boolean isUseCuda() {
        return useCuda;
    }

    public void setUseCuda(boolean useCuda) {
        this.useCuda = useCuda;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public boolean isUseArena() {
        return useArena;
    }

    public void setUseArena(boolean useArena) {
        this.useArena = useArena;
    }

    public TableConfig(String modelPath) {
        this.modelPath = modelPath;
    }

    public TableConfig() {
    }
}
