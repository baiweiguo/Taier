package com.dtstack.rdos.engine.execution.sparkyarn;

import com.dtstack.rdos.engine.execution.base.IClient;
import com.dtstack.rdos.engine.execution.base.restart.ARestartService;
import com.dtstack.rdos.engine.execution.base.restart.IJobRestartStrategy;
import com.dtstack.rdos.engine.execution.sparkyarn.enums.ExceptionInfoConstrant;
import com.dtstack.rdos.engine.execution.sparkyarn.restart.SparkUndoRestart;
import com.dtstack.rods.engine.execution.base.resource.EngineResourceInfo;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Reason:
 * Date: 2018/1/25
 * Company: www.dtstack.com
 * @author xuchao
 */

public class SparkRestartService extends ARestartService {

    private static final Logger LOG = LoggerFactory.getLogger(SparkRestartService.class);

    private final static List<String> unrestartExceptionList = Lists.newArrayList(EngineResourceInfo.LIMIT_RESOURCE_ERROR);
    private final static List<String> restartExceptionList = ExceptionInfoConstrant.getNeedRestartException();

    @Override
    public boolean checkFailureForEngineDown(String msg) {
        if(msg != null && msg.contains(ExceptionInfoConstrant.SPARK_ENGINE_DOWN_RESTART_EXCEPTION)){
            return true;
        }
        return false;
    }

    @Override
    public boolean checkNOResource(String msg) {
        return false;
    }

    @Override
    public IJobRestartStrategy getAndParseErrorLog(String jobId, String engineJobId, String appId, IClient client) {
        // default undo  restart strategy
        return  new SparkUndoRestart();
    }
}
