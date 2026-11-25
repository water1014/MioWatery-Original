# MioWatery 原生页面入口清单

> 本表直接依据当前 `index.html` 中的 DOM 页面容器 ID（`v-*`）与设置子页面 ID（`settings-*`）提取。页面容器不等于独立应用，弹层/动态内容可能不以独立容器出现。

## 1. 页面容器（29 个）

| DOM ID | 识别名称 | 说明 |
|---|---|---|
| `v-home` | 首页 | 桌面与多个桌面页 |
| `v-wallet` | 钱包 | 钱包页面 |
| `v-mcp` | MCP | MCP 页面 |
| `v-life` | Time is Nothing | 生活模拟页面 |
| `v-music` | 音乐 | 音乐页面 |
| `v-coread` | 共读 | 共读页面 |
| `v-offline` | 线下 | 线下相关页面 |
| `v-preset` | 预设 | 预设管理 |
| `v-repo` | 仓库 | 作品仓库 |
| `v-youtube` | YouTube | 视频与媒体应用 |
| `v-crread` | 阅读 | 阅读相关页面，需继续核验其与共读的区别 |
| `v-couple` | 情侣空间 | 关系空间 |
| `v-msgs` | 讯息 | 消息应用 |
| `v-iff` | IF线 | 分支剧情 |
| `v-xinji` | 心迹 | 心迹互动 |
| `v-app` | 通用应用容器 | 聊天、联系人等标签式内容容器 |
| `v-postdetail` | 帖子详情 | 动态详情页面 |
| `v-cset` | 聊天设置 | 聊天级设置 |
| `v-css` | 美化 | 自定义样式管理 |
| `v-deco` | 编辑桌面帖子 | 桌面帖子装饰编辑 |
| `v-prof` | 桌面自定义美化 | 用户资料/桌面相关配置页，需继续确认实际流程 |
| `v-desktopcustom` | 桌面自定义美化 | 桌面壁纸、图标与组件 |
| `v-rolephones` | iPhone | 角色手机模拟 |
| `v-diary` | 日记 | 日记页面 |
| `v-stickers` | 表情包管理 | 表情包与贴纸 |
| `v-chat` | 聊天 | 聊天主界面 |
| `v-personas` | 联系人 | 联系人与角色管理 |
| `v-worlds` | 世界书 | 世界书管理 |
| `v-settings` | 设置 | 应用级设置中心 |

## 2. 设置子页面（12 个）

| DOM ID | 子页面 |
|---|---|
| `settings-main` | 设置首页 |
| `settings-fball` | 相关扩展设置（标题和业务需继续核验） |
| `settings-system` | 系统 |
| `settings-appearance` | 外观 |
| `settings-innervoice` | 内心语音/心声 |
| `settings-api` | API 与模型 |
| `settings-voice` | 语音 |
| `settings-prompt` | 提示词 |
| `settings-memory` | 记忆 |
| `settings-profile` | 资料 |
| `settings-imageapi` | 生图 API |
| `settings-data` | 数据管理 |

## 3. 原生导航要求

- 原生导航需要覆盖上述页面入口，但可以按 Android 交互习惯重新组织层级。
- 页面容器 `v-app` 中的聊天、联系人、动态等标签要继续向下拆成独立功能清单。
- 设置中心的 12 个子页面需要保留各自的功能，不得压成一个“设置”占位页。
- `v-prof` 与 `v-desktopcustom` 在源码中均存在，迁移前需核验它们各自职责，不能因标题相似而合并。
- `v-coread` 与 `v-crread` 均存在，迁移前需核验共读与阅读的差异。
- 页面清单只是路由覆盖基线；弹窗、菜单、编辑器、批处理、导入导出等交互仍需从事件绑定和函数调用继续盘点。


## 4. 设置子页面内容初步核验

从各子页面的实际 HTML 内容可确认：

| 子页面 ID | 已见内容 / 控件方向 | 原生化要求 |
|---|---|---|
| `settings-main` | 个人资料入口、系统设置入口 | 原生设置目录与资料入口 |
| `settings-fball` | 悬浮球开关、悬浮球图片更换/恢复 | Android 浮窗权限需单独申请；不能照搬网页浮球 |
| `settings-system` | 后台保活、消息通知、系统通知测试、主题、状态栏 | 通知权限、后台运行和状态栏适配按 Android 机制重做 |
| `settings-appearance` | 浅色/深色/跟随系统、状态栏避让、字体和预览 | Compose 主题令牌、系统栏和字体配置 |
| `settings-innervoice` | 心声预设的新建、重命名、删除、导入导出 | 预设 CRUD 与文件导入导出 |
| `settings-api` | OpenAI 兼容 API 地址、密钥、自定义请求头 | 多 Provider 配置与凭据安全存储 |
| `settings-voice` | MiniMax 真实语音开关与语音合成 | 语音服务配置、音频播放和权限 |
| `settings-prompt` | 全局角色扮演提示词入口，指向预设 App 中的 The-World 模块 | 避免重复维护同一份全局提示词 |
| `settings-memory` | 传统记忆、向量记忆、向量模型连接与检索配置 | 传统记忆和向量检索分层实现 |
| `settings-profile` | 头像、名字、用户名、个人简介 | 用户资料与头像媒体管理 |
| `settings-imageapi` | 角色主动发图开关、生图服务商、NAI/API 配置 | 独立图像 Provider 与生成参数 |
| `settings-data` | JSON 备份、ZIP 备份、导入 | 迁移预览、完整性校验、事务回滚 |

说明：这里只记录源码中能直接看到的内容。具体字段、按钮事件、网络请求和边界行为仍需逐项跟踪函数实现。
