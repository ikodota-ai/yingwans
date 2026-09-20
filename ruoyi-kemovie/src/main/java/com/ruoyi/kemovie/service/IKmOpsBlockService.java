package com.ruoyi.kemovie.service;

import java.util.List;
import com.ruoyi.kemovie.domain.KmOpsBlock;

/**
 * 运营位 Service
 *
 * @author kemovie
 */
public interface IKmOpsBlockService
{
    public KmOpsBlock selectKmOpsBlockById(Long blockId);

    public List<KmOpsBlock> selectKmOpsBlockList(KmOpsBlock kmOpsBlock);

    public List<KmOpsBlock> selectEnabledByKey(String blockKey);

    public int insertKmOpsBlock(KmOpsBlock kmOpsBlock);

    public int updateKmOpsBlock(KmOpsBlock kmOpsBlock);

    public int deleteKmOpsBlockByIds(Long[] blockIds);
}
