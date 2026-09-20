package com.ruoyi.kemovie.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.kemovie.domain.KmOpsBlock;
import com.ruoyi.kemovie.mapper.KmOpsBlockMapper;
import com.ruoyi.kemovie.service.IKmOpsBlockService;

/**
 * 运营位 Service 实现
 *
 * @author kemovie
 */
@Service
public class KmOpsBlockServiceImpl implements IKmOpsBlockService
{
    @Autowired private KmOpsBlockMapper mapper;

    @Override
    public KmOpsBlock selectKmOpsBlockById(Long blockId) { return mapper.selectKmOpsBlockById(blockId); }

    @Override
    public List<KmOpsBlock> selectKmOpsBlockList(KmOpsBlock kmOpsBlock) { return mapper.selectKmOpsBlockList(kmOpsBlock); }

    @Override
    public List<KmOpsBlock> selectEnabledByKey(String blockKey) { return mapper.selectEnabledByKey(blockKey); }

    @Override
    public int insertKmOpsBlock(KmOpsBlock kmOpsBlock) { return mapper.insertKmOpsBlock(kmOpsBlock); }

    @Override
    public int updateKmOpsBlock(KmOpsBlock kmOpsBlock) { return mapper.updateKmOpsBlock(kmOpsBlock); }

    @Override
    public int deleteKmOpsBlockByIds(Long[] blockIds) { return mapper.deleteKmOpsBlockByIds(blockIds); }
}
