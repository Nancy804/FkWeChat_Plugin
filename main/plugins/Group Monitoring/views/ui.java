// ==================== ui.java - 群成员监控面板 ====================
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

// 颜色常量
final String C_GREEN = "#07C160";
final String C_RED = "#FA5151";
final String C_GRAY = "#EAEAEA";
final String C_BLUE = "#576B95";
final String C_SUB = "#888888";

Dialog _dlg = null;

// 工具方法：生成圆角背景
GradientDrawable createRoundRect(String hexColor, float radius) {
    GradientDrawable drawable = new GradientDrawable();
    drawable.setColor(Color.parseColor(hexColor));
    drawable.setCornerRadius(radius);
    return drawable;
}

// 创建入场动画
AnimationSet createEnterAnimation() {
    AnimationSet animSet = new AnimationSet(true);
    animSet.setDuration(250);
    TranslateAnimation slide = new TranslateAnimation(
        Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0,
        Animation.RELATIVE_TO_SELF, 0.3f, Animation.RELATIVE_TO_SELF, 0
    );
    slide.setDuration(250);
    animSet.addAnimation(slide);
    AlphaAnimation alpha = new AlphaAnimation(0, 1);
    alpha.setDuration(250);
    animSet.addAnimation(alpha);
    return animSet;
}

// 安全获取群列表（过滤已退出的群）
java.util.List getGroupsSafe() {
    java.util.List gs = getGroupList();
    if (gs == null || gs.isEmpty()) return null;
    
    // 获取当前用户wxid
    String myWxid = getMyWxId();
    
    java.util.List result = new java.util.ArrayList();
    for (int i = 0; i < gs.size(); i++) {
        java.util.Map g = (java.util.Map) gs.get(i);
        String gid = (String) g.get("username");
        if (gid == null) continue;
        
        // 检查是否还在群里
        if (myWxid != null) {
            java.util.List members = getGroupMemberList(gid);
            if (members == null || !members.contains(myWxid)) continue;
        }
        
        result.add(g);
    }
    
    if (result.isEmpty()) return null;
    return result;
}

// 统计监控中的群数量
int countActiveGroups() {
    java.util.List gs = getGroupsSafe();
    int n = 0;
    if (gs != null) {
        for (int i = 0; i < gs.size(); i++) {
            java.util.Map g = (java.util.Map) gs.get(i);
            if (getBoolean("gm_watch_" + (String) g.get("username"), false)) n++;
        }
    }
    return n;
}

// 同步总开关
void syncMasterSwitch() {
    setBoolean("gm_enabled", countActiveGroups() > 0);
}

// 关闭旧弹窗
void dismissOld() {
    if (_dlg != null && _dlg.isShowing()) {
        try { _dlg.dismiss(); } catch (Exception e) {}
    }
}

// ==================== 主面板入口 ====================
void showMainPanel() {
    Activity context = getTopActivity();
    if (context == null) {
        toast("请在微信主界面操作");
        return;
    }
    new Handler(Looper.getMainLooper()).post(new Runnable() {
        public void run() {
            buildAndShowDialog(context);
        }
    });
}

// ==================== 构建UI ====================
void buildAndShowDialog(Activity context) {
    try {
        dismissOld();
        
        boolean running = getBoolean("gm_enabled", false);
        int ac = countActiveGroups();
        
        // 外层容器
        FrameLayout rootFrame = new FrameLayout(context);
        rootFrame.setPadding(50, 50, 50, 50);
        
        // 主面板卡片
        LinearLayout mainPanel = new LinearLayout(context);
        mainPanel.setOrientation(LinearLayout.VERTICAL);
        mainPanel.setBackground(createRoundRect("#F7F7F7", 40f));
        mainPanel.setPadding(60, 60, 60, 60);
        mainPanel.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        
        // 标题行
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, 40);
        
        TextView titleView = new TextView(context);
        titleView.setText("群成员监控");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        titleView.setTextColor(Color.parseColor("#111111"));
        titleView.getPaint().setFakeBoldText(true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleView.setLayoutParams(titleParams);
        headerRow.addView(titleView);
        
        // 状态标签
        TextView badge = new TextView(context);
        badge.setText(running ? "运行中·" + ac + "个群" : "已停止");
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        badge.setTextColor(Color.WHITE);
        badge.setBackground(createRoundRect(running ? C_GREEN : C_SUB, 20f));
        badge.setPadding(30, 10, 30, 10);
        headerRow.addView(badge);
        mainPanel.addView(headerRow);
        
        // 滚动区域
        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 850
        );
        scrollView.setLayoutParams(scrollParams);
        
        LinearLayout listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer);
        
        // ===== 群列表区背景 =====
        LinearLayout listSection = new LinearLayout(context);
        listSection.setOrientation(LinearLayout.VERTICAL);
        listSection.setBackground(createRoundRect("#F0F4F8", 20f));
        listSection.setPadding(25, 25, 25, 25);
        LinearLayout.LayoutParams listSectionParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        listSectionParams.setMargins(0, 0, 0, 20);
        listSection.setLayoutParams(listSectionParams);
        
        // 群列表标题
        TextView listTitle = new TextView(context);
        listTitle.setText("监控群列表");
        listTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        listTitle.setTextColor(Color.parseColor("#999999"));
        listTitle.setPadding(5, 0, 0, 15);
        listSection.addView(listTitle);
        
        // ===== 群列表 =====
        java.util.List groups = getGroupsSafe();
        if (groups != null && !groups.isEmpty()) {
            for (int i = 0; i < groups.size(); i++) {
                final java.util.Map g = (java.util.Map) groups.get(i);
                final String gid = (String) g.get("username");
                String nick = (String) g.get("nickname");
                if (gid == null) continue;
                
                boolean watched = getBoolean("gm_watch_" + gid, false);
                String displayName = (nick != null && nick.length() > 0) ? nick : gid;
                
                // 群卡片
                LinearLayout itemCard = new LinearLayout(context);
                itemCard.setOrientation(LinearLayout.HORIZONTAL);
                itemCard.setGravity(Gravity.CENTER_VERTICAL);
                itemCard.setBackground(createRoundRect("#FFFFFF", 24f));
                itemCard.setPadding(40, 30, 40, 30);
                LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                );
                itemParams.setMargins(0, 0, 0, 20);
                itemCard.setLayoutParams(itemParams);
                
                // 群名
                TextView nameText = new TextView(context);
                nameText.setText(displayName);
                nameText.setTextColor(Color.parseColor("#333333"));
                nameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                nameText.setSingleLine(true);
                LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                nameText.setLayoutParams(nameParams);
                itemCard.addView(nameText);
                
                // 监控状态文字
                TextView statusText = new TextView(context);
                statusText.setText(watched ? "监控中" : "未监控");
                statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                statusText.setTextColor(Color.parseColor(watched ? C_GREEN : C_SUB));
                statusText.setPadding(0, 0, 20, 0);
                itemCard.addView(statusText);
                
                // 切换按钮
                Button toggleBtn = new Button(context);
                toggleBtn.setText(watched ? "开启" : "关闭");
                toggleBtn.setTextColor(watched ? Color.WHITE : Color.parseColor(C_SUB));
                toggleBtn.setBackground(createRoundRect(watched ? C_GREEN : C_GRAY, 16f));
                toggleBtn.setPadding(30, 0, 30, 0);
                toggleBtn.setTag(gid);
                toggleBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        String id = (String) v.getTag();
                        boolean cur = getBoolean("gm_watch_" + id, false);
                        boolean newVal = !cur;
                        setBoolean("gm_watch_" + id, newVal);
                        syncMasterSwitch();
                        toast(newVal ? "已开启监控" : "已关闭监控");
                        dismissOld();
                        showMainPanel();
                    }
                });
                itemCard.addView(toggleBtn);
                listSection.addView(itemCard);
            }
        } else {
            TextView emptyText = new TextView(context);
            emptyText.setText("暂无群聊");
            emptyText.setTextColor(Color.parseColor("#999999"));
            emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, 30, 0, 30);
            listSection.addView(emptyText);
        }
        
        listContainer.addView(listSection);
        mainPanel.addView(scrollView);
        
        // ===== 功能区背景 =====
        LinearLayout funcSection = new LinearLayout(context);
        funcSection.setOrientation(LinearLayout.VERTICAL);
        funcSection.setBackground(createRoundRect("#F8F8F8", 20f));
        funcSection.setPadding(25, 25, 25, 25);
        LinearLayout.LayoutParams funcParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        funcParams.setMargins(0, 0, 0, 25);
        funcSection.setLayoutParams(funcParams);
        
        // 启用监控
        LinearLayout masterCard = new LinearLayout(context);
        masterCard.setOrientation(LinearLayout.HORIZONTAL);
        masterCard.setGravity(Gravity.CENTER_VERTICAL);
        masterCard.setBackground(createRoundRect("#FFFFFF", 16f));
        masterCard.setPadding(35, 28, 35, 28);
        LinearLayout.LayoutParams masterParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        masterParams.setMargins(0, 0, 0, 15);
        masterCard.setLayoutParams(masterParams);
        
        TextView masterLabel = new TextView(context);
        masterLabel.setText("启用监控");
        masterLabel.setTextColor(Color.parseColor("#333333"));
        masterLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        LinearLayout.LayoutParams mlParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        masterLabel.setLayoutParams(mlParams);
        masterCard.addView(masterLabel);
        
        Button masterBtn = new Button(context);
        masterBtn.setText(running ? "运行中" : "已停止");
        masterBtn.setTextColor(running ? Color.WHITE : Color.parseColor("#999999"));
        masterBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        masterBtn.setBackground(createRoundRect(running ? C_GREEN : C_GRAY, 18f));
        masterBtn.setPadding(25, 10, 25, 10);
        masterBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean cur = getBoolean("gm_enabled", false);
                setBoolean("gm_enabled", !cur);
                toast(!cur ? "监控已开启" : "监控已关闭");
                dismissOld();
                showMainPanel();
            }
        });
        masterCard.addView(masterBtn);
        funcSection.addView(masterCard);
        
        // 退群+进群 并排
        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnRowParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        btnRowParams.setMargins(0, 0, 0, 15);
        btnRow.setLayoutParams(btnRowParams);
        
        boolean notifyOn = getBoolean("gm_notify", true);
        boolean atJoinOn = getBoolean("gm_at_join", false);
        
        // 退群监控按钮
        Button notifyBtn = new Button(context);
        notifyBtn.setText("退群监控");
        notifyBtn.setTextColor(notifyOn ? Color.WHITE : Color.parseColor("#888888"));
        notifyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        notifyBtn.setBackground(createRoundRect(notifyOn ? C_RED : "#EEEEEE", 18f));
        notifyBtn.setPadding(20, 22, 20, 22);
        LinearLayout.LayoutParams notifyBtnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        notifyBtnParams.setMargins(0, 0, 8, 0);
        notifyBtn.setLayoutParams(notifyBtnParams);
        notifyBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean cur = getBoolean("gm_notify", true);
                setBoolean("gm_notify", !cur);
                toast(!cur ? "退群监控已开启" : "退群监控已关闭");
                dismissOld();
                showMainPanel();
            }
        });
        btnRow.addView(notifyBtn);
        
        // 进群监控按钮
        Button atBtn = new Button(context);
        atBtn.setText("进群监控");
        atBtn.setTextColor(atJoinOn ? Color.WHITE : Color.parseColor("#888888"));
        atBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        atBtn.setBackground(createRoundRect(atJoinOn ? C_GREEN : "#EEEEEE", 18f));
        atBtn.setPadding(20, 22, 20, 22);
        LinearLayout.LayoutParams atBtnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        atBtnParams.setMargins(8, 0, 0, 0);
        atBtn.setLayoutParams(atBtnParams);
        atBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean cur = getBoolean("gm_at_join", false);
                setBoolean("gm_at_join", !cur);
                toast(!cur ? "进群监控已开启" : "进群监控已关闭");
                dismissOld();
                showMainPanel();
            }
        });
        btnRow.addView(atBtn);
        funcSection.addView(btnRow);
        
        // 消息设置
        LinearLayout settingsCard = new LinearLayout(context);
        settingsCard.setOrientation(LinearLayout.HORIZONTAL);
        settingsCard.setGravity(Gravity.CENTER_VERTICAL);
        settingsCard.setBackground(createRoundRect("#FFFFFF", 16f));
        settingsCard.setPadding(35, 28, 35, 28);
        
        TextView settingsLabel = new TextView(context);
        settingsLabel.setText("消息设置");
        settingsLabel.setTextColor(Color.parseColor("#333333"));
        settingsLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        LinearLayout.LayoutParams slParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        settingsLabel.setLayoutParams(slParams);
        settingsCard.addView(settingsLabel);
        
        Button settingsBtn = new Button(context);
        settingsBtn.setText("设置 >");
        settingsBtn.setTextColor(Color.WHITE);
        settingsBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        settingsBtn.setBackground(createRoundRect(C_BLUE, 18f));
        settingsBtn.setPadding(25, 10, 25, 10);
        settingsBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismissOld();
                showWelcomePanel(context);
            }
        });
        settingsCard.addView(settingsBtn);
        funcSection.addView(settingsCard);
        
        mainPanel.addView(funcSection);
        
        // ===== 检测时间 =====
        long lastTime = getLong("gm_last", 0);
        String timeStr = "未检测";
        if (lastTime > 0) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM-dd HH:mm:ss");
                timeStr = sdf.format(new java.util.Date(lastTime * 1000));
            } catch (Exception e) {
                timeStr = String.valueOf(lastTime);
            }
        }
        
        TextView timeView = new TextView(context);
        timeView.setText("最后检测: " + timeStr);
        timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        timeView.setTextColor(Color.parseColor(C_SUB));
        timeView.setGravity(Gravity.CENTER);
        timeView.setPadding(0, 20, 0, 20);
        mainPanel.addView(timeView);
        
        // 保存并关闭按钮
        Button closeBtn = new Button(context);
        closeBtn.setText("保存并关闭");
        closeBtn.setTextColor(Color.WHITE);
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        closeBtn.setBackground(createRoundRect(C_BLUE, 24f));
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 120
        );
        closeParams.setMargins(0, 30, 0, 0);
        closeBtn.setLayoutParams(closeParams);
        mainPanel.addView(closeBtn);
        
        rootFrame.addView(mainPanel);
        
        // 创建弹窗
        _dlg = new AlertDialog.Builder(context).setView(rootFrame).create();
        
        closeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismissOld();
            }
        });
        
        // 透明背景 + 正方形
        Window window = _dlg.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int sz = context.getResources().getDisplayMetrics().widthPixels;
            window.setLayout(sz, sz);
        }
        _dlg.show();
        
        // 应用入场动画
        rootFrame.startAnimation(createEnterAnimation());
        
    } catch (Exception e) {
        log("[群监控] UI异常: " + e.getMessage());
        toast("界面加载失败");
    }
}

// ==================== 消息设置面板 ====================
void showWelcomePanel(Activity context) {
    try {
        dismissOld();
        
        String currentWelcome = getString("gm_welcome", "欢迎 @name 加入群聊！");
        String currentLeave = getString("gm_leave", "@name 退出了群聊");
        
        // 外层容器
        FrameLayout rootFrame = new FrameLayout(context);
        rootFrame.setPadding(40, 40, 40, 40);
        
        // 主面板
        LinearLayout mainPanel = new LinearLayout(context);
        mainPanel.setOrientation(LinearLayout.VERTICAL);
        mainPanel.setBackground(createRoundRect("#FFFFFF", 30f));
        mainPanel.setPadding(50, 50, 50, 50);
        mainPanel.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        
        // 标题
        TextView titleView = new TextView(context);
        titleView.setText("消息设置");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        titleView.setTextColor(Color.parseColor("#1A1A1A"));
        titleView.getPaint().setFakeBoldText(true);
        titleView.setPadding(0, 0, 0, 30);
        mainPanel.addView(titleView);
        
        // ===== 第一部分：欢迎语 =====
        TextView welcomeTitle = new TextView(context);
        welcomeTitle.setText("进群欢迎语");
        welcomeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        welcomeTitle.setTextColor(Color.parseColor("#333333"));
        welcomeTitle.getPaint().setFakeBoldText(true);
        welcomeTitle.setPadding(0, 0, 0, 10);
        mainPanel.addView(welcomeTitle);
        
        EditText welcomeInput = new EditText(context);
        welcomeInput.setText(currentWelcome);
        welcomeInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        welcomeInput.setHint("输入欢迎语...");
        welcomeInput.setBackground(createRoundRect("#F5F5F5", 12f));
        welcomeInput.setPadding(30, 25, 30, 25);
        LinearLayout.LayoutParams wiParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        wiParams.setMargins(0, 0, 0, 15);
        welcomeInput.setLayoutParams(wiParams);
        mainPanel.addView(welcomeInput);
        
        Button saveWelcomeBtn = new Button(context);
        saveWelcomeBtn.setText("保存欢迎语");
        saveWelcomeBtn.setTextColor(Color.WHITE);
        saveWelcomeBtn.setBackground(createRoundRect(C_GREEN, 20f));
        saveWelcomeBtn.setPadding(40, 15, 40, 15);
        saveWelcomeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String newWelcome = welcomeInput.getText().toString().trim();
                if (newWelcome.isEmpty()) { toast("不能为空"); return; }
                setString("gm_welcome", newWelcome);
                toast("欢迎语已保存");
            }
        });
        mainPanel.addView(saveWelcomeBtn);
        
        // 分割线
        View divider1 = new View(context);
        divider1.setBackgroundColor(Color.parseColor("#F0F0F0"));
        LinearLayout.LayoutParams d1Params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        d1Params.setMargins(0, 30, 0, 30);
        divider1.setLayoutParams(d1Params);
        mainPanel.addView(divider1);
        
        // ===== 第二部分：退群语 =====
        TextView leaveTitle = new TextView(context);
        leaveTitle.setText("退群通知语");
        leaveTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        leaveTitle.setTextColor(Color.parseColor("#333333"));
        leaveTitle.getPaint().setFakeBoldText(true);
        leaveTitle.setPadding(0, 0, 0, 10);
        mainPanel.addView(leaveTitle);
        
        EditText leaveInput = new EditText(context);
        leaveInput.setText(currentLeave);
        leaveInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        leaveInput.setHint("输入退群通知语...");
        leaveInput.setBackground(createRoundRect("#F5F5F5", 12f));
        leaveInput.setPadding(30, 25, 30, 25);
        LinearLayout.LayoutParams liParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        liParams.setMargins(0, 0, 0, 15);
        leaveInput.setLayoutParams(liParams);
        mainPanel.addView(leaveInput);
        
        Button saveLeaveBtn = new Button(context);
        saveLeaveBtn.setText("保存退群语");
        saveLeaveBtn.setTextColor(Color.WHITE);
        saveLeaveBtn.setBackground(createRoundRect(C_RED, 20f));
        saveLeaveBtn.setPadding(40, 15, 40, 15);
        saveLeaveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String newLeave = leaveInput.getText().toString().trim();
                if (newLeave.isEmpty()) { toast("不能为空"); return; }
                setString("gm_leave", newLeave);
                toast("退群语已保存");
            }
        });
        mainPanel.addView(saveLeaveBtn);
        
        // 分割线
        View divider2 = new View(context);
        divider2.setBackgroundColor(Color.parseColor("#F0F0F0"));
        LinearLayout.LayoutParams d2Params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2);
        d2Params.setMargins(0, 30, 0, 30);
        divider2.setLayoutParams(d2Params);
        mainPanel.addView(divider2);
        
        // ===== 第三部分：返回按钮 =====
        Button backBtn = new Button(context);
        backBtn.setText("返回主面板");
        backBtn.setTextColor(Color.WHITE);
        backBtn.setBackground(createRoundRect(C_BLUE, 25f));
        backBtn.setPadding(40, 15, 40, 15);
        backBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismissOld();
                showMainPanel();
            }
        });
        mainPanel.addView(backBtn);
        
        rootFrame.addView(mainPanel);
        
        _dlg = new AlertDialog.Builder(context).setView(rootFrame).create();
        Window window = _dlg.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int sz = context.getResources().getDisplayMetrics().widthPixels;
            window.setLayout(sz, sz);
        }
        _dlg.show();
        
        // 入场动画
        rootFrame.startAnimation(createEnterAnimation());
        
    } catch (Exception e) {
        log("[群监控] 设置面板异常: " + e.getMessage());
        toast("界面加载失败");
    }
}
