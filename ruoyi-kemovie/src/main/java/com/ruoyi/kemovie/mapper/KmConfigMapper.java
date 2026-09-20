package com.ruoyi.kemovie.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 读取 sys_config 参数（免责声明等后台可配文案）
 *
 * @author kemovie
 */
public interface KmConfigMapper
{
    @Select("select config_value from sys_config where config_key = #{key} limit 1")
    public String selectConfigByKey(@Param("key") String key);
}
