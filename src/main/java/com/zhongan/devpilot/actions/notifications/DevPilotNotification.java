package com.zhongan.devpilot.actions.notifications;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.zhongan.devpilot.settings.state.AIGatewaySettingsState;
import com.zhongan.devpilot.update.DevPilotUpdate;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class DevPilotNotification {

    public static void info(String content) {
        var notification = new Notification(
            "DevPilot Notification Group",
            DevPilotMessageBundle.get("notification.group.devpilot"),
            content,
            NotificationType.INFORMATION);
        Notifications.Bus.notify(notification);
    }

    public static void warn(String content) {
        var notification = new Notification(
            "DevPilot Notification Group",
            DevPilotMessageBundle.get("notification.group.devpilot"),
            content,
            NotificationType.WARNING);
        Notifications.Bus.notify(notification);
    }

    public static void error(String content) {
        var notification = new Notification(
            "DevPilot Notification Group",
            DevPilotMessageBundle.get("notification.group.devpilot"),
            content,
            NotificationType.ERROR);
        Notifications.Bus.notify(notification);
    }

    public static void linkInfo(String content, String text, String url) {
        var notification = new Notification(
            "DevPilot Notification Group",
            DevPilotMessageBundle.get("notification.group.devpilot"),
            content,
            NotificationType.INFORMATION);

        notification.addAction(new NotificationAction(text) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                BrowserUtil.browse(url);
            }
        });

        Notifications.Bus.notify(notification);
    }

    public static void infoAndAction(String content, String display, String url) {
        var notification = new Notification(
            "DevPilot Notification Group",
            DevPilotMessageBundle.get("notification.group.devpilot"),
            content,
            NotificationType.INFORMATION);
        notification.addAction(new NotificationAction(display) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent anActionEvent, @NotNull Notification notification) {
                BrowserUtil.browse(url);
            }
        });
        Notifications.Bus.notify(notification);
    }

    public static void debug(String content) {
        var selectedModel = AIGatewaySettingsState.getInstance().getSelectedModel();
        var host = AIGatewaySettingsState.getInstance().getModelBaseHost(selectedModel);
        if (StringUtils.endsWith(host, "dev") || StringUtils.endsWith(host, "prd")) {
            var notification = new Notification(
                "DevPilot Notification Group",
                DevPilotMessageBundle.get("notification.group.devpilot"),
                content,
                NotificationType.ERROR);
            Notifications.Bus.notify(notification);
        }
    }

    public static void updateNotification(Project project) {
        var notification = new Notification(
                "DevPilot Notification Group",
                DevPilotMessageBundle.get("notification.group.devpilot"),
                DevPilotMessageBundle.get("devpilot.notification.update.message"),
                NotificationType.IDE_UPDATE);
        notification.addAction(NotificationAction
                .createSimpleExpiring(DevPilotMessageBundle.get("devpilot.notification.installButton"), () -> {
                    ApplicationManager.getApplication()
                            .executeOnPooledThread(() -> DevPilotUpdate.installUpdate(project));
                }));
        notification.addAction(NotificationAction
                .createSimpleExpiring(DevPilotMessageBundle.get("devpilot.notification.hideButton"), () -> {
                }));
        Notifications.Bus.notify(notification);
    }
}
