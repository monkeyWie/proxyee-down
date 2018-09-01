export default {
  nav: {
    tasks: '任務管理',
    extension: '擴充功能管理',
    setting: '軟體設定',
    about: '關於專案',
    support: '支援我們'
  },
  tip: {
    ok: '確定',
    cancel: '取消',
    notNull: '不能為空',
    fmtErr: '格式不正確',
    choose: '選擇',
    save: '儲存'
  },
  tasks: {
    createTask: '建立任務',
    continueDownloading: '繼續下載',
    pauseDownloads: '暫停下載',
    deleteTask: '刪除任務',
    deleteTaskTip: '是否刪除任務和檔案？',
    url: '链接',
    fileName: '檔案名稱',
    fileSize: '大小',
    connections: '連接數',
    filePath: '路徑',
    status: '狀態',
    operate: '操作',
    downloadAddress: '下載位址',
    downloadSpeed: '下載速度',
    createTime: '開始時間',
    taskProgress: '任務進度',
    wait: '待下載',
    unknowLeft: '未知',
    statusPause: '暫停',
    statusFail: '失敗',
    statusDone: '完成',
    option: '附加',
    head: '請求頭',
    body: '請求體',
    detail:'下載細節'
  },
  extension: {
    conditions: '使用須知',
    conditionsContent:
      '首次使用擴充模組時，必須安裝由 Proxyee Down 隨機產生的一個 CA 證書，點擊下面的安裝按鈕並按系統的引導進行確認安裝。(注意：程式會在安裝前檢測作業系統中是否有安裝過證書，當檢測到有安裝的情況會提示刪除對應的舊 CA 證書)',
    install: '安裝',
    globalProxy: '全域代理',
    proxyTip1:
      'Proxyee Down 會修改系統全域的代理設定，可能會與相同機制的軟體發生衝突(例如：SS、SSR)',
    proxyTip2: '若關閉全域代理，需配合瀏覽器代理插件來使用(例如：SwitchyOmega)',
    title: '名稱',
    description: '描述',
    currVersion: '目前版本',
    newVersion: '最新版本',
    installStatus: '狀態',
    installStatusTrue: '已安装',
    installStatusFalse: '未安装',
    action: '操作',
    actionUpdate: '更新',
    actionInstall: '安裝',
    actionDetail: '細節',
    switch: '開關',
    downloadingTip: '下載中...[伺服器(',
    downloadOk: '下載成功',
    downloadErr: '下載失敗',
    downloadErrTip: '自動切換伺服器'
  },
  setting: {
    downSetting: '下載設定',
    path: '路徑',
    pathTip: '預設下載路徑',
    connections: '連接數',
    connectionsTip: '預設連接數',
    taskLimit: '同時下載任務數',
    taskSpeedLimit: '單任務限速',
    globalSpeedLimit: '全域限速',
    speedLimitTip: '0 為不限速',
    appSetting: '系統設定',
    language: '語言',
    uiMode: 'UI模式',
    uiModeWindows: '視窗',
    uiModeBrowser: '瀏覽器',
    checkUpdate: '檢查更新',
    checkUpdateWeek: '每周',
    checkUpdateStartup: '每次啟動',
    checkUpdateNever: '從不'
  },
  update: {
    checkNew: '檢測到新版本',
    version: '版本號',
    changeLog: '更新內容',
    update: '更新',
    done: '更新完畢',
    restart: '是否重新啟動？',
    error: '更新失敗，請重試'
  },
  alert: {
    refused: '程式異常，拒絕存取',
    timeout: '程式異常，連接逾時',
    error: '程式出錯',
    notFound: '任務不存在',
    '/tasks': {
      post: {
        4000: '參數解析錯誤',
        4001: '請求對象不能為空',
        4002: '請求位址不能為空',
        4003: '檔案儲存路徑不能為空',
        4004: '建立資料夾失敗',
        4005: '無寫入權限',
        4006: '磁碟空間不足',
        4007: '檔案已存在'
      }
    },
    '/util/resolve': {
      put: {
        4000: '參數解析錯誤',
        4001: '請求位址不能為空',
        4002: '回應狀態碼異常'
      }
    },
    '/config': {
      put: {
        4000: '參數解析錯誤'
      }
    }
  }
}
