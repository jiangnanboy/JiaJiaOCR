package com.jiajia.text_recognition.convnext;

import ai.onnxruntime.OrtException;
import com.jiajia.common_onnx.Model;
import com.jiajia.utils.common.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/**
 * @author jiangnanboy
 * @date 2024/7/5 21:25
 */
public class ConvRegModel extends Model {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConvRegModel.class);

    private Map<Long, String> labelMapping = CollectionUtil.newHashMap();
    public ConvRegModel(String modelPath, String vocabPath) throws OrtException {
        super(modelPath);
        loadVocab(vocabPath, 2L);
    }

    public ConvRegModel(byte[] recBytes, byte[] vocabBytes) throws OrtException {
        super(recBytes);
        loadVocab(vocabBytes, 2L);
    }

    /**
     * @param vocabPath
     */
    private void loadVocab(String vocabPath) {
        loadVocab(vocabPath, 1L);
    }

    private void loadVocab(String vocabPath, long cnt) {
        LOGGER.info("load recognition vocab...");
        try(BufferedReader br = Files.newBufferedReader(Paths.get(vocabPath), Charset.forName("utf-8"))) {
            String line = null;
            while(null != (line = br.readLine())) {
                line = line.trim();
                labelMapping.put(cnt, line);
                cnt ++;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load vocab from path: {}", vocabPath, e);
        }
    }

    private void loadVocab(byte[] vocabBytes) {
        loadVocab(vocabBytes, 1L);
    }

    private void loadVocab(byte[] vocabBytes, long cnt) {
        LOGGER.info("Loading recognition vocab...");
        // 1. 使用 ByteArrayInputStream 将 byte[] 包装成流
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(vocabBytes);
             // 2. 使用 InputStreamReader 指定字符编码，并包装成 BufferedReader
             BufferedReader br = new BufferedReader(new InputStreamReader(byteStream, StandardCharsets.UTF_8))) {

            String line;
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
