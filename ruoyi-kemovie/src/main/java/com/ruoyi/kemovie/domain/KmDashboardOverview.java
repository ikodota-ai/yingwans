package com.ruoyi.kemovie.domain;

import java.io.Serializable;

/**
 * 后台首页运营概览统计
 *
 * @author kemovie
 */
public class KmDashboardOverview implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 影片总数（未删除） */
    private Integer mediaTotal;
    /** 电影数 */
    private Integer movieTotal;
    /** 剧集数 */
    private Integer tvTotal;
    /** 已上架数（visible=0） */
    private Integer visibleTotal;
    /** 演职人员数 */
    private Integer personTotal;
    /** 前台会员数 */
    private Integer memberTotal;
    /** 片单数 */
    private Integer collectionTotal;
    /** MDL 抓取源数 */
    private Integer mdlTotal;
    /** 即将上线数 */
    private Integer upcomingTotal;
    /** 想看/点赞总数 */
    private Long likeTotal;
    /** 订阅/期待总数 */
    private Long wishTotal;
    /** 看过总数 */
    private Long watchTotal;

    public Integer getMediaTotal() { return mediaTotal; }
    public void setMediaTotal(Integer mediaTotal) { this.mediaTotal = mediaTotal; }
    public Integer getMovieTotal() { return movieTotal; }
    public void setMovieTotal(Integer movieTotal) { this.movieTotal = movieTotal; }
    public Integer getTvTotal() { return tvTotal; }
    public void setTvTotal(Integer tvTotal) { this.tvTotal = tvTotal; }
    public Integer getVisibleTotal() { return visibleTotal; }
    public void setVisibleTotal(Integer visibleTotal) { this.visibleTotal = visibleTotal; }
    public Integer getPersonTotal() { return personTotal; }
    public void setPersonTotal(Integer personTotal) { this.personTotal = personTotal; }
    public Integer getMemberTotal() { return memberTotal; }
    public void setMemberTotal(Integer memberTotal) { this.memberTotal = memberTotal; }
    public Integer getCollectionTotal() { return collectionTotal; }
    public void setCollectionTotal(Integer collectionTotal) { this.collectionTotal = collectionTotal; }
    public Integer getMdlTotal() { return mdlTotal; }
    public void setMdlTotal(Integer mdlTotal) { this.mdlTotal = mdlTotal; }
    public Integer getUpcomingTotal() { return upcomingTotal; }
    public void setUpcomingTotal(Integer upcomingTotal) { this.upcomingTotal = upcomingTotal; }
    public Long getLikeTotal() { return likeTotal; }
    public void setLikeTotal(Long likeTotal) { this.likeTotal = likeTotal; }
    public Long getWishTotal() { return wishTotal; }
    public void setWishTotal(Long wishTotal) { this.wishTotal = wishTotal; }
    public Long getWatchTotal() { return watchTotal; }
    public void setWatchTotal(Long watchTotal) { this.watchTotal = watchTotal; }
}
