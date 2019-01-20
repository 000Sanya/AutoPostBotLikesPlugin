package com.github.insanusmokrassar.AutoPostBotLikesPlugin

import com.github.insanusmokrassar.AutoPostBotLikesPlugin.database.LikesPluginLikesTable
import com.github.insanusmokrassar.AutoPostBotLikesPlugin.database.LikesPluginRegisteredLikesMessagesTable
import com.github.insanusmokrassar.AutoPostBotLikesPlugin.listeners.*
import com.github.insanusmokrassar.AutoPostBotLikesPlugin.models.config.LikePluginConfig
import com.github.insanusmokrassar.AutoPostBotLikesPlugin.utils.extensions.AdminsHolder
import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.Plugin
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.publishers.PostPublisher
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import kotlinx.serialization.Optional
import java.lang.ref.WeakReference

class LikesPlugin(
    @Optional
    private val config: LikePluginConfig = LikePluginConfig()
) : Plugin {
    val likesPluginRegisteredLikesMessagesTable = LikesPluginRegisteredLikesMessagesTable()
    val likesPluginLikesTable = LikesPluginLikesTable(likesPluginRegisteredLikesMessagesTable)

    override suspend fun onInit(executor: RequestsExecutor, baseConfig: FinalConfig, pluginManager: PluginManager) {
        super.onInit(executor, baseConfig, pluginManager)
        val publisher = pluginManager.plugins.firstOrNull { it is PostPublisher } as? PostPublisher ?: return

        val botWR = WeakReference(executor)

        MessagePostedListener(
            publisher.postPublishedChannel,
            likesPluginRegisteredLikesMessagesTable,
            baseConfig.targetChatId,
            config.separateAlways,
            config.separatedText,
            botWR
        )

        RatingChangedListener(
            likesPluginLikesTable,
            likesPluginRegisteredLikesMessagesTable,
            botWR,
            baseConfig.targetChatId,
            config
        )

        config.adaptedGroups.map {
                group ->
            group.items.map {
                    button ->
                MarkListener(
                    baseConfig.targetChatId,
                    likesPluginLikesTable,
                    button,
                    botWR,
                    group.other(button).map { it.id }
                )
            }
        }

        val adminsHolder = AdminsHolder(
            botWR,
            baseConfig.targetChatId
        )

        enableDetectLikesAttachmentMessages(
            adminsHolder,
            baseConfig.targetChatId,
            likesPluginRegisteredLikesMessagesTable,
            botWR
        )

        enableDetectLikesRefreshMessages(
            adminsHolder,
            baseConfig.targetChatId,
            likesPluginLikesTable,
            likesPluginRegisteredLikesMessagesTable,
            botWR
        )
    }
}