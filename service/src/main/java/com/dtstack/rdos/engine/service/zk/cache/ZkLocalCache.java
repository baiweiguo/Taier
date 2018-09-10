package com.dtstack.rdos.engine.service.zk.cache;

import com.dtstack.rdos.engine.execution.base.enums.RdosTaskStatus;
import com.dtstack.rdos.engine.execution.base.queue.ClusterQueueInfo;
import com.dtstack.rdos.engine.service.node.WorkNode;
import com.dtstack.rdos.engine.service.zk.ShardConsistentHash;
import com.dtstack.rdos.engine.service.zk.ZkDistributed;
import com.dtstack.rdos.engine.service.zk.data.BrokerDataNode;
import com.dtstack.rdos.engine.service.zk.data.BrokerDataShard;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * company: www.dtstack.com
 * author: toutian
 * create: 2018/9/6
 */
public class ZkLocalCache implements CopyOnWriteCache<String, BrokerDataNode> {

    private volatile Map<String, BrokerDataNode> core;
    private volatile Map<String, BrokerDataNode> view;

    private volatile BrokerDataNode localDataCache;
    private volatile AtomicBoolean requiresCopyOnWrite;
    private String localAddress;
    private static ZkLocalCache zkLocalCache = new ZkLocalCache();
    public static ZkLocalCache getInstance() {
        return zkLocalCache;
    }

    private ZkDistributed zkDistributed = ZkDistributed.getZkDistributed();
    private WorkNode workNode = WorkNode.getInstance();
    private ClusterQueueInfo clusterQueueInfo = ClusterQueueInfo.getInstance();
    private ShardConsistentHash shardsCsist = ShardConsistentHash.getInstance();

    private ZkLocalCache() {
        this.requiresCopyOnWrite = new AtomicBoolean(false);
    }

    public void init() {
        core = zkDistributed.initMemTaskStatus();
        localAddress = zkDistributed.getLocalAddress();
        localDataCache = core.get(localAddress);
    }

    public void updateLocalMemTaskStatus(String zkTaskId, Integer status) {
        if (zkTaskId == null || status == null) {
            throw new UnsupportedOperationException();
        }
        copy();
        String shard = shardsCsist.get(zkTaskId);
        localDataCache.getShards().get(shard).put(zkTaskId, status.byteValue());
    }

    public Map<String, BrokerDataNode> getLocalCache() {
        return getView();
    }

    public BrokerDataNode getBrokerData() {
        return getView().get(localAddress);
    }


    public String getJobLocationAddr(String zkTaskId) {
        //todo 先查缓存，没有再查zk
        String shard = shardsCsist.get(zkTaskId);
        for (Map.Entry<String, BrokerDataNode> entry : core.entrySet()) {
            String addr = entry.getKey();
            Map<String, BrokerDataShard> shardMap = entry.getValue().getShards();
            if (shardMap.containsKey(shard)) {
                if (shardMap.get(shard).containsKey(zkTaskId)) {
                    return addr;
                }
            }
        }
        return null;
    }

    /**
     * 选择节点间（队列负载+已提交任务 加权值）+ 误差 符合要求的node，做任务分发
     */
    public String getDistributeNode(String engineType,List<String> excludeNodes) {
        int def = Integer.MAX_VALUE;
        String node = null;

        Set<Map.Entry<String, BrokerDataNode>> entrys = core.entrySet();
        for (Map.Entry<String, BrokerDataNode> entry : entrys) {
            String targetNode = entry.getKey();
            if (excludeNodes.contains(targetNode)) {
                continue;
            }
            int size = 0;
            for (Map.Entry<String, BrokerDataShard> shardEntry : entry.getValue().getShards().entrySet()) {

                size += getDistributeJobCount(shardEntry.getValue());
            }
            if (size < def) {
                def = size;
                node = targetNode;
            }
        }
        return node;
    }

    private int getDistributeJobCount(BrokerDataShard brokerDataShard) {
        int count = 0;
        for (byte status : brokerDataShard.getView().values()) {
            if (status == RdosTaskStatus.RESTARTING.getStatus()
                    || status == RdosTaskStatus.WAITCOMPUTE.getStatus()
                    || status == RdosTaskStatus.WAITENGINE.getStatus()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public Map<String, BrokerDataNode> cloneData() {
        try {
            return new ConcurrentHashMap<String, BrokerDataNode>(core);
        } finally {
            requiresCopyOnWrite.set(true);
        }
    }

    private void copy() {
        if (requiresCopyOnWrite.compareAndSet(true, false)) {
            core = new ConcurrentHashMap<>(core);
            localDataCache = core.get(localAddress);
            view = null;
        }
    }

    public void cover(Map<String, BrokerDataNode> otherNode) {
        Map<String, BrokerDataNode> coverCore = new ConcurrentHashMap<>(core);
        coverCore.remove(localAddress);
        coverCore.putAll(otherNode);
        core = coverCore;
    }

    private Map<String, BrokerDataNode> getView() {
        if (view == null) {
            view = Collections.unmodifiableMap(core);
        }
        return view;
    }

}
