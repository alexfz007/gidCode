package ai.ocs.wechat.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blade.kit.json.JSONObject;

import ai.ocs.wechat.model.WechatMeta;
import ai.ocs.wechat.service.WechatService;

public class WechatListener {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(WechatListener.class);

  int playWeChat = 0;

  public void start(final WechatService wechatService,
      final WechatMeta wechatMeta) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        LOGGER.warn("进入消息监听模式 ...");
        wechatService.choiceSyncLine(wechatMeta);
        int iTime = 0, iSync = 0;
        while (true) {
          int[] arr = wechatService.syncCheck(wechatMeta);
          if (iSync > 300) {
            LOGGER.warn("retcode={}, selector={}", arr[0], arr[1]);
            iSync = 0;
          }
          iSync++;
          if (arr[0] == 1100 || arr[0] == 1101) {
            LOGGER.warn("你在手机上登出了微信，再见");
            break;
          }
          if (arr[0] == 0) {
            if (arr[1] == 2) {
              JSONObject data = wechatService.webwxsync(wechatMeta);
              wechatService.handleMsg(wechatMeta, data);
            } else if (arr[1] == 6) {
              JSONObject data = wechatService.webwxsync(wechatMeta);
              wechatService.handleMsg(wechatMeta, data);
            } else if (arr[1] == 7) {
              playWeChat += 1;
              LOGGER.info("你在手机上玩微信被我发现了 {} 次", playWeChat);
              wechatService.webwxsync(wechatMeta);
            } else if (arr[1] == 3) {
              JSONObject data = wechatService.webwxsync(wechatMeta);
              wechatService.handleMsg(wechatMeta, data);
              continue;
            } else if (arr[1] == 0) {
              continue;
            }
          } else {
            //
          }
          try {
            // LOGGER.info("等待2000ms...");
            Thread.sleep(2000);
            if (iTime > 300) {
              LOGGER.warn("retcode={}, selector={}", arr[0], arr[1]);
              LOGGER.warn("等待2000ms...");
              iTime = 0;
            }
            iTime++;
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }, "wechat-listener-thread").start();
  }

}
