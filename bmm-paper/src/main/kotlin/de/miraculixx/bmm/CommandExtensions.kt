package de.miraculixx.bmm

import dev.jorel.commandapi.BukkitExecutable
import dev.jorel.commandapi.executors.CommandArguments
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

inline fun BukkitExecutable<*>.anyExecutorAsync(crossinline executor: (CommandSender, CommandArguments) -> Unit): BukkitExecutable<*> = executes(CommandExecutor { sender, args ->
    CoroutineScope(Dispatchers.Default).launch {
        executor(sender, args)
    }
})

inline fun BukkitExecutable<*>.playerExecutorAsync(crossinline executor: (Player, CommandArguments) -> Unit): BukkitExecutable<*> = executesPlayer(PlayerCommandExecutor { sender, args ->
    CoroutineScope(Dispatchers.Default).launch {
        executor(sender, args)
    }
})