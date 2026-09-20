package com.ruoyi.kemovie.mapper;

import java.util.List;
import com.ruoyi.kemovie.domain.KmOpsBlock;

/**
 * 运营位 Mapper
 *
 * @author kemovie
 */
public interface KmOpsBlockMapper
{
    public KmOpsBlock selectKmOpsBlockById(Long blockId);

    public List<KmOpsBlock> selectKmOpsBlockList(KmOpsBlock kmOpsBlock);

    /** 前台按位置取启用的运营位 */
    public List<KmOpsBlock> selectEnabledByKey(String blockKey);

    public int insertKmOpsBlock(KmOpsBlock kmOpsBlock);

    public int updateKmOpsBlock(KmOpsBlock kmOpsBlock);

    public int deleteKmOpsBlockByIds(Long[] blockIds);
}
