-- 智能写作辅助平台种子数据（MERGE 幂等，可重复执行）
-- 默认账号：admin / admin123（管理员），writer / user123（普通用户）

MERGE INTO app_user (id, username, password, nickname, role, status, tokens_used, cost_incurred, created_at)
  KEY(id) VALUES
(1, 'admin', 'f00ed028494dfb67c5895d1cedeeb56578295cabedbb8e3b6f814e42105035e9', '平台管理员', 'ADMIN', 'NORMAL', 0, 0.0, CURRENT_TIMESTAMP);

MERGE INTO app_user (id, username, password, nickname, role, status, tokens_used, cost_incurred, created_at)
  KEY(id) VALUES
(2, 'writer', '18f6db6ff91646f274a4e56e6d3871e95c96c1d704d5e3627903525cf38d41e8', '演示作者', 'USER', 'NORMAL', 0, 0.0, CURRENT_TIMESTAMP);

-- 多场景写作模板
MERGE INTO writing_template (id, name, category, description, system_prompt, prompt_template, variables, builtin, enabled, usage_count, created_at)
  KEY(id) VALUES
(1, '工作总结', '办公', '季度/年度个人与部门工作总结，结构清晰、重点突出',
 '你是一位资深职场写作专家，擅长撰写条理清晰、数据支撑、重点突出的工作总结。',
 '请为我撰写一篇{{period}}工作总结。岗位：{{role}}。本期主要工作成果与要点：{{achievements}}。要求语言务实、分条陈述、有理有据。',
 '[{"key":"period","label":"周期(如2026年第二季度)","required":true},{"key":"role","label":"岗位角色","required":true},{"key":"achievements","label":"主要工作成果","required":true}]',
 TRUE, TRUE, 0, CURRENT_TIMESTAMP);

MERGE INTO writing_template (id, name, category, description, system_prompt, prompt_template, variables, builtin, enabled, usage_count, created_at)
  KEY(id) VALUES
(2, '通知公告', '公文', '会议通知、活动通知、事务公告等正式文体',
 '你是一位机关公文写作秘书，熟悉《党政机关公文格式》与通知、公告的规范写法。',
 '请撰写一篇{{noticeType}}。发布单位：{{org}}。核心事项：{{content}}。时间地点等要素：{{details}}。要求格式规范、用语庄重、要素齐全。',
 '[{"key":"noticeType","label":"通知类型(如会议通知)","required":true},{"key":"org","label":"发布单位","required":true},{"key":"content","label":"核心事项","required":true},{"key":"details","label":"时间地点等补充信息","required":false}]',
 TRUE, TRUE, 0, CURRENT_TIMESTAMP);

MERGE INTO writing_template (id, name, category, description, system_prompt, prompt_template, variables, builtin, enabled, usage_count, created_at)
  KEY(id) VALUES
(3, '营销文案', '营销', '产品种草、活动推广、品牌故事等新媒体营销文案',
 '你是一位4A广告公司首席文案，擅长洞察用户心理，写出有感染力、转化率高的营销文案。',
 '请为产品「{{product}}」撰写一篇{{channel}}风格的营销文案。目标人群：{{audience}}。核心卖点：{{sellingPoints}}。要求有吸引力标题、场景化表达、行动号召。',
 '[{"key":"product","label":"产品名称","required":true},{"key":"channel","label":"投放渠道(如小红书/公众号)","required":true},{"key":"audience","label":"目标人群","required":true},{"key":"sellingPoints","label":"核心卖点","required":true}]',
 TRUE, TRUE, 0, CURRENT_TIMESTAMP);

MERGE INTO writing_template (id, name, category, description, system_prompt, prompt_template, variables, builtin, enabled, usage_count, created_at)
  KEY(id) VALUES
(4, '学术论文摘要', '学术', '论文中英文摘要与关键词建议，逻辑严谨',
 '你是一位高校科研写作导师，熟悉学术论文摘要的结构：研究背景、问题、方法、结果、结论。',
 '请根据以下论文信息撰写学术摘要。题目：{{title}}。研究问题：{{problem}}。研究方法：{{method}}。主要发现：{{findings}}。要求学术规范用语，300-500字，并在文末给出5个关键词。',
 '[{"key":"title","label":"论文题目","required":true},{"key":"problem","label":"研究问题","required":true},{"key":"method","label":"研究方法","required":true},{"key":"findings","label":"主要发现","required":true}]',
 TRUE, TRUE, 0, CURRENT_TIMESTAMP);

MERGE INTO writing_template (id, name, category, description, system_prompt, prompt_template, variables, builtin, enabled, usage_count, created_at)
  KEY(id) VALUES
(5, '散文随笔', '文学', '抒情散文与生活随笔，文笔优美、情理交融',
 '你是一位散文作家，文字清雅凝练，善于从日常细节中生发情感与哲思。',
 '请以「{{topic}}」为主题写一篇抒情散文或随笔。可融入的意象或片段：{{imagery}}。要求意境完整、语言有质感、避免空洞口号。',
 '[{"key":"topic","label":"主题","required":true},{"key":"imagery","label":"意象或素材片段","required":false}]',
 TRUE, TRUE, 0, CURRENT_TIMESTAMP);

MERGE INTO writing_template (id, name, category, description, system_prompt, prompt_template, variables, builtin, enabled, usage_count, created_at)
  KEY(id) VALUES
(6, '演讲稿', '办公', '开幕致辞、主题演讲、颁奖发言等，富有感染力',
 '你是一位专业演讲撰稿人，擅长设计金句、排比与情绪曲线，让演讲既有观点又有温度。',
 '请撰写一篇在{{occasion}}上的演讲稿。演讲人身份：{{speaker}}。核心观点：{{keyMessage}}。听众对象：{{audience}}。要求开场抓人、层层递进、结尾有力。',
 '[{"key":"occasion","label":"场合","required":true},{"key":"speaker","label":"演讲人身份","required":true},{"key":"keyMessage","label":"核心观点","required":true},{"key":"audience","label":"听众对象","required":false}]',
 TRUE, TRUE, 0, CURRENT_TIMESTAMP);

MERGE INTO writing_template (id, name, category, description, system_prompt, prompt_template, variables, builtin, enabled, usage_count, created_at)
  KEY(id) VALUES
(7, '新闻稿', '公文', '会议报道、活动新闻、企业动态等消息类稿件',
 '你是一位资深新闻记者，熟悉消息写作的倒金字塔结构，5W1H要素齐全。',
 '请撰写一篇新闻报道。事件：{{event}}。时间地点：{{whenWhere}}。关键人物与引语：{{people}}。要求标题点睛、导语总括、客观第三人称叙述。',
 '[{"key":"event","label":"事件","required":true},{"key":"whenWhere","label":"时间地点","required":true},{"key":"people","label":"关键人物与引语","required":false}]',
 TRUE, TRUE, 0, CURRENT_TIMESTAMP);

MERGE INTO writing_template (id, name, category, description, system_prompt, prompt_template, variables, builtin, enabled, usage_count, created_at)
  KEY(id) VALUES
(8, '短视频脚本', '新媒体', '口播/剧情类短视频脚本，含分镜与台词',
 '你是一位百万粉丝短视频团队的编剧，精通黄金三秒开头、节奏控制与完播率优化。',
 '请为账号「{{account}}」撰写一篇{{duration}}内的短视频脚本。选题：{{topic}}。风格：{{style}}。要求给出分镜表（镜头/画面/台词/时长），开头三秒强钩子。',
 '[{"key":"account","label":"账号定位","required":true},{"key":"duration","label":"时长(如60秒)","required":true},{"key":"topic","label":"选题","required":true},{"key":"style","label":"风格","required":false}]',
 TRUE, TRUE, 0, CURRENT_TIMESTAMP);
