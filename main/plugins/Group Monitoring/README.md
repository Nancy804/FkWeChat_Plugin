# 群成员监控插件 v2.0

实时监控群聊成员变动，有成员加入或退出时自动通知。

## 功能特性

- 🔍 **实时监控**：基于系统消息，即时检测成员变动
- 📥 **进群监控**：成员加入时发送欢迎消息
- 📤 **退群监控**：成员退出时发送通知
- 📝 **自定义消息**：可自定义欢迎语和退群通知语
- 🎛️ **可视化控制面板**：选择要监控的群聊

## 使用方法

### 打开控制面板

在微信聊天界面长按消息，选择 **群监控** 菜单。

### 面板功能

| 功能 | 说明 |
|------|------|
| 启用监控 | 开启/关闭总开关 |
| 退群监控 | 开启后，有成员退群时发送通知 |
| 进群监控 | 开启后，有成员进群时发送欢迎消息 |
| 消息设置 | 自定义欢迎语和退群通知语 |
| 群列表 | 显示所有群聊，可单独开启/关闭监控 |

### 消息模板

支持变量替换：

| 变量 | 说明 | 示例 |
|------|------|------|
| `@name` | 成员的群昵称 | `欢迎 @name 加入群聊！` |

## 配置说明

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `gm_enabled` | false | 总开关 |
| `gm_watch_{群ID}` | false | 单个群的监控开关 |
| `gm_notify` | true | 退群监控开关 |
| `gm_at_join` | false | 进群监控开关 |
| `gm_welcome` | `欢迎 @name 加入群聊！` | 欢迎语模板 |
| `gm_leave` | `@name 退出了群聊` | 退群通知语模板 |

## 文件结构

```
群成员监控/
├── info.prop          # 插件元数据
├── main.java          # 入口 + 消息监听
├── README.md          # 使用文档
├── UI开发文档.md      # UI 开发参考
└── views/
    ├── helper.java    # 核心逻辑
    └── ui.java        # UI 面板
```

## 技术实现

### 消息格式

成员变动时，微信会发送系统消息，包含XML格式数据：

```xml
<sysmsg type="sysmsgtemplate">
    <sysmsgtemplate>
        <content_template type="tmpl_type_profile">
            <template><![CDATA[你将"$kickoutname$"移出了群聊]]></template>
            <link_list>
                <link name="kickoutname" type="link_profile">
                    <memberlist>
                        <member>
                            <username><![CDATA[wxid_xxx]]></username>
                            <nickname><![CDATA[昵称]]></nickname>
                        </member>
                    </memberlist>
                </link>
            </link_list>
        </content_template>
    </sysmsgtemplate>
</sysmsg>
```

### 类型判断

| content_template type | 场景 |
|----------------------|------|
| `tmpl_type_profile` | 移出群聊 |
| `tmpl_type_profilewithrevoke` | 邀请加入 |

### 昵称获取

使用 `getUserName(groupId, wxid)` 获取群内昵称：
- 优先显示群昵称
- 如果没有设置群昵称，显示备注或微信昵称

## 注意事项

- 已退出的群聊不会显示在列表中
- 需要重新加载插件才能生效
- 静默运行，无日志输出
