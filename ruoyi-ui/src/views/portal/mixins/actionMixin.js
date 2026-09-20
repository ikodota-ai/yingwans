import { like, wish, watched, subscribe } from '@/api/kemovie/me'
import { getPortalToken } from '../portalAuth'

// 影片行为（点赞/想看/看过/订阅）乐观更新 + 需登录守卫
export default {
  methods: {
    ensureLogin() {
      if (!getPortalToken()) {
        this.$router.push('/portal-login?redirect=' + encodeURIComponent(this.$route.fullPath))
        return false
      }
      return true
    },
    async handleAction({ type, media }) {
      if (!this.ensureLogin()) return
      try {
        if (type === 'like') {
          await like(media.mediaId)
          this.$toggleAct(media, 'liked')
        } else if (type === 'wish') {
          await wish(media.mediaId)
          this.$toggleAct(media, 'wished')
        } else if (type === 'watched') {
          await watched(media.mediaId)
          this.$toggleAct(media, 'watched')
        } else if (type === 'subscribe') {
          const res = await subscribe(media.mediaId)
          const sub = res && typeof res.subscribed !== 'undefined' ? res.subscribed : !media.subscribed
          this.$set(media, 'subscribed', sub)
          const msg = sub
            ? (res && res.docReady ? '已订阅，资源链接已发送至站内通知' : '已订阅，资源更新后将通过站内通知发送链接')
            : '已取消订阅'
          this.$modal && this.$modal.msgSuccess(msg)
        }
      } catch (e) {
        this.$modal && this.$modal.msgError('操作失败')
      }
    },
    $toggleAct(media, key) {
      if (!media.myAction) this.$set(media, 'myAction', {})
      const cur = media.myAction[key] === '1' || media.myAction[key] === 1
      this.$set(media.myAction, key, cur ? '0' : '1')
    }
  }
}
