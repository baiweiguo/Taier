package com.dtstack.engine.master.component;

import com.dtstack.dtcenter.common.sftp.SFTPHandler;
import com.dtstack.engine.common.exception.EngineDefineException;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class SFTPComponet extends BaseComponent {
    public SFTPComponet(Map<String, Object> allConfig) {
        super(allConfig);
    }

    @Override
    public void testConnection() throws Exception {
        Map<String, String> config = new HashMap<>(allConfig.size());
        for (String key : allConfig.keySet()) {
            config.put(key, allConfig.get(key).toString());
        }
        SFTPHandler instance = null;
        try {
            instance = SFTPHandler.getInstance(config);
            String path = config.get("path");
            if (StringUtils.isBlank(path)) {
                throw new EngineDefineException("SFTP组件path配置不能为空");
            }
            //测试路径是否存在
            instance.listFile(path);
        } finally {
            if (instance != null) {
                instance.close();
            }
        }
    }
}