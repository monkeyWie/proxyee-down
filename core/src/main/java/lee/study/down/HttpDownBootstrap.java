package lee.study.down;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.ssl.SslContext;
import io.netty.resolver.NoopAddressResolverGroup;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import lee.study.down.constant.HttpDownStatus;
import lee.study.down.dispatch.HttpDownCallback;
import lee.study.down.handle.HttpDownInitializer;
import lee.study.down.io.LargeMappedByteBuffer;
import lee.study.down.model.ChunkInfo;
import lee.study.down.model.HttpDownInfo;
import lee.study.down.model.HttpRequestInfo;
import lee.study.down.model.TaskInfo;
import lee.study.down.util.FileUtil;
import lee.study.down.util.HttpDownUtil;
import lee.study.proxyee.util.ProtoUtil.RequestProto;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Data
@AllArgsConstructor
public class HttpDownBootstrap {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpDownBootstrap.class);
  public static final String ATTR_CHANNEL = "channel";
  public static final String ATTR_FILE_CHANNEL = "fileChannel";
  public static final String ATTR_MAP_BUFFER = "mapBuffer";

  private HttpDownInfo httpDownInfo;
  private SslContext clientSslContext;
  private NioEventLoopGroup clientLoopGroup;
  private HttpDownCallback callback;
  private final Map<Integer, Map<String, Object>> attr = new HashMap<>();

  public void startDown() throws Exception {
    TaskInfo taskInfo = httpDownInfo.getTaskInfo();
    taskInfo.buildChunkInfoList();
    FileUtil.deleteIfExists(taskInfo.buildTaskFilePath());
    FileUtil.createDirSmart(taskInfo.getFilePath());
    try (
        RandomAccessFile randomAccessFile = new RandomAccessFile(taskInfo.buildTaskFilePath(), "rw")
    ) {
      randomAccessFile.setLength(taskInfo.getTotalSize());
    }
    //文件下载开始回调
    taskInfo.reset();
    taskInfo.setStatus(HttpDownStatus.RUNNING);
    taskInfo.setStartTime(System.currentTimeMillis());
    for (int i = 0; i < taskInfo.getChunkInfoList().size(); i++) {
      ChunkInfo chunkInfo = taskInfo.getChunkInfoList().get(i);
      //设置状态和时间
      chunkInfo.setStartTime(System.currentTimeMillis());
      startChunkDown(chunkInfo, HttpDownStatus.CONNECTING_NORMAL);
    }
    if (callback != null) {
      callback.onStart(httpDownInfo);
    }
  }

  public void startChunkDown(ChunkInfo chunkInfo, int updateStatus) {
    HttpRequestInfo requestInfo = (HttpRequestInfo) httpDownInfo.getRequest();
    RequestProto requestProto = requestInfo.requestProto();
    LOGGER.debug("开始下载：" + chunkInfo);
    Bootstrap bootstrap = new Bootstrap()
        .channel(NioSocketChannel.class)
        .group(clientLoopGroup)
        .handler(new HttpDownInitializer(requestProto.getSsl(), this, chunkInfo));
    if (httpDownInfo.getProxyConfig() != null) {
      //代理服务器解析DNS和连接
      bootstrap.resolver(NoopAddressResolverGroup.INSTANCE);
    }
    ChannelFuture cf = bootstrap.connect(requestProto.getHost(), requestProto.getPort());
    chunkInfo.setStatus(updateStatus);
    cf.addListener((ChannelFutureListener) future -> {
      if (future.isSuccess()) {
        synchronized (chunkInfo){
          setAttr(chunkInfo, HttpDownBootstrap.ATTR_CHANNEL,future.channel());
        }
        synchronized (requestInfo) {
          LOGGER.debug("下载连接成功：channelId[" + future.channel().id() + "]\t" + chunkInfo);
          if (httpDownInfo.getTaskInfo().isSupportRange()) {
            requestInfo.headers()
                .set(HttpHeaderNames.RANGE,
                    "bytes=" + chunkInfo.getNowStartPosition() + "-" + chunkInfo.getEndPosition());
          } else {
            requestInfo.headers().remove(HttpHeaderNames.RANGE);
          }
          future.channel().writeAndFlush(httpDownInfo.getRequest());
        }
        if (requestInfo.content() != null) {
          //请求体写入
          HttpContent content = new DefaultLastHttpContent();
          content.content().writeBytes(requestInfo.content());
          future.channel().writeAndFlush(content);
        }
      } else {
        LOGGER.debug("下载连接失败：" + chunkInfo);
        chunkInfo.setStatus(HttpDownStatus.FAIL);
        future.channel().close();
      }
    });
  }

  /**
   * 下载重试
   */
  public void retryChunkDown(ChunkInfo chunkInfo, int updateStatus)
      throws Exception {
    TaskInfo taskInfo = httpDownInfo.getTaskInfo();
    synchronized (chunkInfo) {
      close(chunkInfo);
      //已经下载完成
      if (chunkInfo.getDownSize() == chunkInfo.getTotalSize()) {
        chunkInfo.setStatus(HttpDownStatus.DONE);
        if (callback != null) {
          callback.onChunkDone(httpDownInfo, chunkInfo);
        }
        return;
      }
      if (taskInfo.isSupportRange()) {
        chunkInfo.setNowStartPosition(chunkInfo.getOriStartPosition() + chunkInfo.getDownSize());
      }
      startChunkDown(chunkInfo, updateStatus);
    }
  }

  /**
   * 下载重试
   */
  public void retryChunkDown(ChunkInfo chunkInfo) throws Exception {
    retryChunkDown(chunkInfo, HttpDownStatus.CONNECTING_FAIL);
  }

  /**
   * 暂停下载
   */
  public void pauseDown() throws Exception {
    TaskInfo taskInfo = httpDownInfo.getTaskInfo();
    synchronized (taskInfo) {
      taskInfo.setStatus(HttpDownStatus.PAUSE);
      for (ChunkInfo chunkInfo : taskInfo.getChunkInfoList()) {
        synchronized (chunkInfo) {
          close(chunkInfo);
          if (chunkInfo.getStatus() != HttpDownStatus.DONE) {
            chunkInfo.setStatus(HttpDownStatus.PAUSE);
          }
        }
      }
    }
    if (callback != null) {
      callback.onPause(httpDownInfo);
    }
  }

  /**
   * 继续下载
   */
  public void continueDown()
      throws Exception {
    TaskInfo taskInfo = httpDownInfo.getTaskInfo();
    synchronized (taskInfo) {
      //如果文件被删除重新开始下载
      if (!FileUtil.exists(taskInfo.buildTaskFilePath())) {
        close();
        startDown();
      } else {
        taskInfo.setStatus(HttpDownStatus.RUNNING);
        long curTime = System.currentTimeMillis();
        taskInfo.setPauseTime(
            taskInfo.getPauseTime() + (curTime - taskInfo.getLastTime()));
        taskInfo.setLastTime(curTime);
        for (ChunkInfo chunkInfo : taskInfo.getChunkInfoList()) {
          synchronized (chunkInfo) {
            if (chunkInfo.getStatus() == HttpDownStatus.PAUSE) {
              chunkInfo.setPauseTime(taskInfo.getPauseTime());
              chunkInfo.setLastTime(curTime);
              retryChunkDown(chunkInfo, HttpDownStatus.CONNECTING_NORMAL);
            }
          }
        }
      }
    }
    if (callback != null) {
      callback.onContinue(httpDownInfo);
    }
  }

  public void close(ChunkInfo chunkInfo) {
    try {
      Channel channel = (Channel) getAttr(chunkInfo, ATTR_CHANNEL);
      FileChannel fileChannel = (FileChannel) getAttr(chunkInfo, ATTR_FILE_CHANNEL);
      LargeMappedByteBuffer mapBuffer = (LargeMappedByteBuffer) getAttr(chunkInfo, ATTR_MAP_BUFFER);
      LOGGER.debug(
          "下载连接关闭：channelId[" + (channel != null ? channel.id() : "null") + "]\t" + chunkInfo);
      HttpDownUtil.safeClose(channel, fileChannel, mapBuffer);
    } catch (Exception e) {
      LOGGER.error("closeChunk error", e);
    }
  }

  public void close() {
    TaskInfo taskInfo = httpDownInfo.getTaskInfo();
    synchronized (taskInfo) {
      for (ChunkInfo chunkInfo : httpDownInfo.getTaskInfo().getChunkInfoList()) {
        synchronized (chunkInfo) {
          close(chunkInfo);
        }
      }
    }
  }

  public void setAttr(ChunkInfo chunkInfo, String key, Object object) {
    Map<String, Object> map = attr.get(chunkInfo.getIndex());
    if (map == null) {
      map = new HashMap<>();
      attr.put(chunkInfo.getIndex(), map);
    }
    map.put(key, object);
  }

  public Object getAttr(ChunkInfo chunkInfo, String key) {
    Map<String, Object> map = attr.get(chunkInfo.getIndex());
    if (map == null) {
      return null;
    } else {
      return map.get(key);
    }
  }
}
