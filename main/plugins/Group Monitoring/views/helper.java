// ==================== helper.java - 实时监控核心 ====================

// 处理成员变动
void handleMemberChange(String gid, String content) {
    if (!getBoolean(PRE + gid, false)) return
    
    // 提取 wxid
    String wxid = extractWxid(content)
    if (wxid == null || wxid.isEmpty()) return
    
    // 提取昵称
    String nickname = extractNickname(content)
    
    // 判断加入/退出
    boolean isJoin = content.contains("tmpl_type_profilewithrevoke")
    boolean isLeave = content.contains("tmpl_type_profile") && !isJoin
    
    if (isJoin) {
        handleJoin(gid, wxid, nickname)
    } else if (isLeave) {
        handleLeave(gid, wxid, nickname)
    }
}

// 处理加入
void handleJoin(String gid, String wxid, String nickname) {
    // 获取群昵称
    String name = getUserName(gid, wxid)
    if (name == null || name.isEmpty()) {
        name = nickname  // 备用
    }
    // 进群监控开启时，发送欢迎消息
    if (getBoolean(KEY_AT_JOIN, false)) {
        String welcome = getString("gm_welcome", "欢迎 @name 加入群聊！")
        String msg = welcome.replace("@name", name)
        sendText(gid, msg)
    }
    
    setLong("gm_last", System.currentTimeMillis() / 1000)
}

// 处理退出
void handleLeave(String gid, String wxid, String nickname) {
    // 获取群昵称
    String name = getUserName(gid, wxid)
    if (name == null || name.isEmpty()) {
        name = nickname  // 备用
    }
    // 退群监控开启时，发送通知
    if (getBoolean(KEY_NOTIFY, false)) {
        String leaveMsg = getString("gm_leave", "@name 退出了群聊")
        String msg = leaveMsg.replace("@name", name)
        sendText(gid, msg)
    }
    
    setLong("gm_last", System.currentTimeMillis() / 1000)
}

// 从 XML 提取 wxid
String extractWxid(String xml) {
    try {
        // 匹配 revokemsg 中的 wxid
        int idx = xml.indexOf("<fromusername>")
        if (idx > 0) {
            int start = idx + 14
            int end = xml.indexOf("</fromusername>", start)
            if (end > start) {
                return xml.substring(start, end)
            }
        }
        
        // 匹配 profilewithrevoke 中的 wxid
        idx = xml.indexOf("wxid_")
        if (idx >= 0) {
            int end = idx
            while (end < xml.length()) {
                char c = xml.charAt(end)
                if (c == '<' || c == '&' || c == ' ' || c == '"') break
                end++
            }
            return xml.substring(idx, end)
        }
    } catch (Exception e) {
    }
    return null
}

// 从 XML 提取昵称
String extractNickname(String xml) {
    try {
        // 查找 nickname 标签内的 CDATA
        int nickIdx = xml.indexOf("<nickname>")
        if (nickIdx >= 0) {
            int cdataStart = xml.indexOf("<![CDATA[", nickIdx)
            if (cdataStart >= 0 && cdataStart - nickIdx < 20) {
                int start = cdataStart + 9  // "<![CDATA[".length()
                int end = xml.indexOf("]]>", start)
                if (end > start) {
                    String name = xml.substring(start, end).trim()
                    if (!name.isEmpty()) {
                        return name
                    }
                }
            }
            
            // 如果没有 CDATA，尝试普通内容
            int start = nickIdx + 10
            int end = xml.indexOf("</nickname>", start)
            if (end > start) {
                String name = xml.substring(start, end).trim()
                if (!name.isEmpty()) {
                    return name
                }
            }
        }
    } catch (Exception e) {
    }
    return "未知用户"
}

// 获取成员名称（优先用getUserName）
String getMemberName(String gid, String wxid, String fallback) {
    String name = getUserName(gid, wxid)
    if (name == null || name.isEmpty()) {
        return fallback
    }
    return name
}
