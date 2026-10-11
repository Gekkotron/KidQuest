package com.gekkotron.kidquest.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

internal suspend fun <R> KidQuestDatabase.tx(block: suspend () -> R): R =
    useWriterConnection { transactor -> transactor.immediateTransaction { block() } }
