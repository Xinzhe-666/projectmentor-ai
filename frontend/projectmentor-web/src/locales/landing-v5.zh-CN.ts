export default {
  nav: {
    system: '项目证据审计系统',
    login: '登录',
    workspace: '进入工作台',
    start: '开始免费审计',
    more: '更多',
    feedback: '反馈',
    support: '请作者喝咖啡'
  },
  meta: {
    release: 'PMAI V5',
    environment: '公开预览',
    rules: '规则引擎在线',
    privacy: '请勿上传机密'
  },
  hero: {
    title: '让每一句项目主张，都经得住追问。',
    description: 'ProjectMentor AI 把 README、源码与配置放进同一条证据链，判断你的项目描述是否有据可查，并把薄弱处转成更稳妥的简历表达与面试深挖。',
    primary: '开始审计项目',
    secondary: '查看判断方法',
    note: '规则扫描免费；AI 深度解读按 credits 使用。'
  },
  audit: {
    title: '项目真实性审计 / PMAI-WEB',
    illustrative: '示意审计',
    illustrativeNote: '界面示例使用本仓库真实路径，不构成项目认证。',
    phase: {
      searching: '正在定位证据',
      found: '已找到来源',
      verified: '主张已核验'
    },
    claim: '主张',
    evidence: '证据来源',
    verdict: '判断',
    selected: '当前检查项',
    sourceMatch: '来源命中',
    sourceReason: '初始化逻辑拒绝少于 32 字符的密钥，并由配置构造 HMAC 签名密钥。',
    line: '行 27–30',
    confidence: '规则置信度 0.96',
    claims: {
      jwt: '项目使用 JWT 签名并校验登录身份',
      async: '审计报告通过异步任务执行',
      preview: '产品当前处于 Public Preview',
      redis: '缓存层已使用 Redis',
      scale: '系统可支撑高并发生产负载'
    },
    paths: {
      jwt: 'backend/.../util/JwtUtil.java',
      async: 'backend/.../analysis/service/AnalysisTaskAsyncExecutor.java',
      preview: 'README.md',
      redis: '未找到 Redis 配置或实现',
      scale: '未提供压测或部署证据'
    }
  },
  status: {
    supported: 'SUPPORTED',
    partial: 'PARTIAL',
    docOnly: 'DOC_ONLY',
    noEvidence: 'NO_EVIDENCE',
    risky: 'RISKY'
  },
  method: {
    title: '从主张到判断，不跳过中间证据。',
    description: 'PMAI 的核心不是替项目打一个漂亮分数，而是把每个结论还原成可检查的来源。',
    claimTitle: 'Claim / 主张',
    claimDesc: '拆解 README、简历表述与项目说明中的可验证陈述。',
    evidenceTitle: 'Evidence / 证据',
    evidenceDesc: '回到源码、配置、SQL 与部署材料，记录路径与命中原因。',
    verdictTitle: 'Verdict / 判断',
    verdictDesc: '用明确状态说明支持程度、材料缺口与表达风险。'
  },
  taxonomy: {
    title: '五种状态，分别回答“有多少证据”。',
    description: '颜色只是辅助；每个判断都保留状态名称、来源与解释。',
    supported: '实现证据可以直接支撑主张。',
    partial: '存在相关实现，但不足以支持完整表述。',
    docOnly: '只在文档中出现，尚未发现实现证据。',
    noEvidence: '当前材料未找到可验证来源。',
    risky: '表述强于现有材料，面试中容易被追问。'
  },
  layers: {
    title: '稳定判断交给规则，深度解释再调用 AI。',
    description: '基础扫描与生成能力保持分离：先用可复查规则核对材料，再决定是否消耗 credits。',
    ruleTitle: '规则证据链扫描',
    ruleLabel: '免费 / 可重复',
    ruleDesc: '定位风险词、文件路径、配置命中与 Claim–Evidence 基础矩阵。结果稳定，可回到原始材料复查。',
    aiTitle: 'AI 深度增强',
    aiLabel: '按 credits 使用',
    aiDesc: '基于已有证据生成更深入的解释、简历改写建议、项目问答与模拟面试追问。失败调用会退款。'
  },
  boundary: {
    title: '先保护代码，再开始审计。',
    description: 'Public Preview 适合学习、项目复盘和面试准备；它不是代码安全审计或简历真实性认证。',
    warning: '不要上传真实密钥、公司内部代码、商业机密或敏感文件。',
    details: '查看完整试用边界',
    item1: '材料不完整时，证据判断也可能不完整。',
    item2: 'AI 输出仅供参考，仍需你理解并核对自己的代码。',
    item3: '较大的 ZIP 上传可能需要更长处理时间。',
    primary: '安全地开始审计',
    secondary: '先登录账户'
  }
}
