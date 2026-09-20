package com.ruoyi.kemovie.service;

/**
 * 影片资源文档（飞书/腾讯文档）Service
 *
 * @author kemovie
 */
public interface IKmDocService
{
    /**
     * 生成或更新影片的飞书资源文档：读取影片全部下载资源，按分组写入文档，
     * 并设置链接分享。已有文档（feishu_doc_token）时清空重写，链接保持不变。
     *
     * @return 飞书文档链接
     */
    public String generateFeishuDoc(Long mediaId);

    /**
     * 飞书连接测试：验证 app-id/app-secret 能否换取 tenant_access_token
     *
     * @return 正常返回 null，否则返回失败原因
     */
    public String testFeishuConnection();
}
