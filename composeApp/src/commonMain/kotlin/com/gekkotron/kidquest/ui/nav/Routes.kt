package com.gekkotron.kidquest.ui.nav

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object GoalsRoute
@Serializable data object SettingsRoute
@Serializable data class ChildDetailRoute(val childId: Long)
@Serializable data class GoalEditRoute(val goalId: Long = 0L)
