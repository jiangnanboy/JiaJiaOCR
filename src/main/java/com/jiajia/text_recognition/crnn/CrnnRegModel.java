package com.jiajia.text_recognition.crnn;

import ai.onnxruntime.OrtException;
import com.jiajia.common_onnx.Model;
import com.jiajia.utils.common.CollectionUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author jiangnanboy
 * @date 2024/7/5 21:25
 */
public class CrnnRegModel extends Model {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrnnRegModel.class);

    private Map<Long, String> labelMapping = CollectionUtil.newHashMap();
    public CrnnRegModel(byte[] recBytes, byte[] vocabBytes) throws OrtException {
        super(recBytes);
        loadVocab(vocabBytes);
    }

    private void loadVocab(byte[] vocabBytes) {
        LOGGER.info("Loading recognition vocab...");
        // 1. 使用 ByteArrayInputStream 将 byte[] 包装成流
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(vocabBytes);
             // 2. 使用 InputStreamReader 指定字符编码，并包装成 BufferedReader
             BufferedReader br = new BufferedReader(new InputStreamReader(byteStream, StandardCharsets.UTF_8))) {

            String line;
            long cnt = 1L;
            // 3. 读取逻辑与原来完全一样
            while ((line = br.readLine()) != null) {
                line = line.trim();
                labelMapping.put(cnt, line);
                cnt++;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load vocab from byte array", e);
        }
    }

    /**
     * @return
     */
    public Map<Long, String> getLabelMapping() {
        return this.labelMapping;
    }
}
