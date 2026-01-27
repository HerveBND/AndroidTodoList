package com.example.todolist.presentation.component

import android.os.Parcelable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.example.todolist.data.repository.TodoRepository
import kotlinx.parcelize.Parcelize

/**
 * Component racine de l'application gérant la navigation.
 *
 * Utilise ChildStack de Decompose pour gérer la pile de navigation
 * entre l'écran de liste et l'écran de détail.
 */
interface RootComponent {
    /**
     * Stack de navigation observable.
     */
    val childStack: Value<ChildStack<Config, Child>>

    /**
     * Configuration de navigation (type-safe).
     */
    sealed class Config : Parcelable {
        /**
         * Configuration pour l'écran de liste.
         */
        @Parcelize
        object List : Config()

        /**
         * Configuration pour l'écran de détail/création.
         *
         * @property todoId L'UUID du todo à afficher (null pour création)
         */
        @Parcelize
        data class Detail(val todoId: String?) : Config()
    }

    /**
     * Child components possibles dans la stack.
     */
    sealed class Child {
        /**
         * Child pour l'écran de liste.
         */
        data class ListChild(val component: TodoListComponent) : Child()

        /**
         * Child pour l'écran de détail.
         */
        data class DetailChild(val component: TodoDetailComponent) : Child()
    }
}

/**
 * Implémentation par défaut du RootComponent.
 *
 * @property componentContext Context Decompose pour le lifecycle
 * @property repository Repository injecté pour créer les child components
 */
class DefaultRootComponent(
    componentContext: ComponentContext,
    private val repository: TodoRepository
) : RootComponent, ComponentContext by componentContext {

    private val navigation = StackNavigation<RootComponent.Config>()

    override val childStack: Value<ChildStack<RootComponent.Config, RootComponent.Child>> =
        childStack(
            source = navigation,
            initialConfiguration = RootComponent.Config.List,
            handleBackButton = true,
            childFactory = ::createChild
        )

    /**
     * Factory pour créer les child components en fonction de la configuration.
     */
    private fun createChild(
        config: RootComponent.Config,
        componentContext: ComponentContext
    ): RootComponent.Child {
        return when (config) {
            is RootComponent.Config.List -> {
                RootComponent.Child.ListChild(
                    component = DefaultTodoListComponent(
                        componentContext = componentContext,
                        repository = repository,
                        onNavigateToDetail = { todoId: String? ->
                            navigation.push(RootComponent.Config.Detail(todoId))
                        }
                    )
                )
            }

            is RootComponent.Config.Detail -> {
                RootComponent.Child.DetailChild(
                    component = DefaultTodoDetailComponent(
                        componentContext = componentContext,
                        todoId = config.todoId,
                        repository = repository,
                        onNavigateBack = {
                            navigation.pop()
                        },
                        onTodoCreated = { _ ->
                            // Après création, retour à la liste
                            navigation.pop()
                        }
                    )
                )
            }
        }
    }
}
