package com.deezus.wordy.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.deezus.wordy.ui.bookmarks.BookmarksScreen
import com.deezus.wordy.ui.definition.DefinitionScreen
import com.deezus.wordy.ui.game.GameScreen
import com.deezus.wordy.ui.history.HistoryScreen
import com.deezus.wordy.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

/** Every destination in the app. Serializable so the back stack survives process death. */
sealed interface Route : NavKey {
  @Serializable
  data object Game : Route

  @Serializable
  data object History : Route

  @Serializable
  data object Bookmarks : Route

  @Serializable
  data object Settings : Route

  @Serializable
  data class Definition(val word: String) : Route
}

@Composable
fun WordyApp() {
  val backStack = rememberNavBackStack(Route.Game)

  // Ignores a second tap that lands before the first navigation has been drawn.
  val navigateTo: (Route) -> Unit = { route ->
    if (backStack.lastOrNull() != route) backStack.add(route)
  }
  val goBack: () -> Unit = {
    if (backStack.size > 1) backStack.removeLastOrNull()
  }
  val lookUp: (String) -> Unit = { word -> navigateTo(Route.Definition(word)) }

  NavDisplay(
    backStack = backStack,
    onBack = goBack,
    entryDecorators = listOf(
      rememberSaveableStateHolderNavEntryDecorator(),
      rememberViewModelStoreNavEntryDecorator(),
    ),
    entryProvider = entryProvider {
      entry<Route.Game> {
        GameScreen(
          onOpenBookmarks = { navigateTo(Route.Bookmarks) },
          onOpenHistory = { navigateTo(Route.History) },
          onOpenSettings = { navigateTo(Route.Settings) },
          onLookUp = lookUp,
        )
      }
      entry<Route.History> {
        HistoryScreen(onBack = goBack, onLookUp = lookUp)
      }
      entry<Route.Bookmarks> {
        BookmarksScreen(onBack = goBack, onLookUp = lookUp)
      }
      entry<Route.Settings> {
        SettingsScreen(onBack = goBack)
      }
      entry<Route.Definition> { route ->
        DefinitionScreen(word = route.word, onBack = goBack)
      }
    },
  )
}
