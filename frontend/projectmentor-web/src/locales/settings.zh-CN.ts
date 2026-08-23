export default {
  navigation: '设置',
  title: '设置',
  description: '管理只影响当前浏览器的工作区展示偏好。',
  experience: {
    title: '体验模式',
    current: '当前体验',
    description: '选择 PMAI 的界面体验。切换不会退出登录，也不会改变项目数据。',
    classic: {
      title: 'Classic Experience',
      description: 'PMAI 原始界面'
    },
    workbench: {
      title: 'Evidence Workbench',
      description: '以证据为先的工作空间'
    },
    selected: '当前',
    disabled: '当前环境未启用 Evidence Workbench。',
    unavailable: '此页面暂未提供该体验。',
    fallbackDescription: '已自动使用 Classic Experience；当前路由与登录状态保持不变。',
    onlyWorkbench: '此功能仅在 Evidence Workbench 中提供。',
    onlyClassic: '此功能仅在 Classic Experience 中提供。',
    unavailableFeature: '当前体验暂未提供此功能。',
    switchToWorkbench: '切换到 Evidence Workbench',
    switchToClassic: '切换到 Classic Experience'
  }
}
